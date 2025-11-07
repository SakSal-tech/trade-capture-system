# Step 5 — Backend: Settlement Instructions, Audit and Search (Development Notes)

## 1. Why I built it this way

I read the business request **TRADE-2025-REQ-005** and the pain points from Trading Operations. The goal was to capture and manage settlement instructions inside the trading system so operations did not need emails or spreadsheets. I chose to implement this as an **extensible key–value layer** using the existing `AdditionalInfo` model rather than altering the `Trade` table. This kept the design flexible for future fields and avoided schema churn, while allowing strong validation, authorisation and an audit trail.

I used a **thin controller, fat service** approach. `TradeSettlementController` is only responsible for HTTP concerns. All rules are centralised in `AdditionalInfoService` and the validation engine so behaviour is testable and reusable. I enforced access with Spring Security annotations in the controller and a defensive check in the service which reads the real principal from the `SecurityContext` so a caller cannot spoof identity in the payload.

Throughout, I wrote with operational reality in mind. I added partial text search for operations, soft delete with audit, and a small server side classification that flags “non‑standard” settlement strings for risk triage. I also published a simple domain event so a UI or notifier could react to changes without tight coupling.

## 2. Architecture choice and trade‑offs

### 2.1 Option B: Extensible `AdditionalInfo` storage

- **What**: store settlement instructions as a row in `additional_info` with `entityType="TRADE"`, `entityId=<tradeId>`, `fieldName="SETTLEMENT_INSTRUCTIONS"`, `fieldValue=<text>` and a default `fieldType="STRING"`.
- **Why**: future proofing. The desk frequently asks for extra per‑trade text fields. A key–value table avoids repeated schema migrations and lets operations add new tagged fields later.
- **How**: I reused the mapper and repository that already exist for `AdditionalInfo`. I centralised settlement‑specific rules in `TradeValidationEngine.validateSettlementInstructions(...)` so the same checks are reused by all paths.
- **Alternative**: extend `Trade` with a `settlement_instructions` column. This is simpler but less flexible, and every new free‑form per‑trade note would require another column and release. The chosen design aligns better with enterprise platforms.

### 2.2 Separation of concerns

- **Controller**: `TradeSettlementController` only transforms inputs, calls services, and shapes responses. Security annotations live here for first line enforcement.
- **Service**: `AdditionalInfoService` contains business rules, validation, security fallbacks, audit writing and event publishing.
- **Validation**: `TradeValidationEngine` provides a single entry point for settlement text rules so I do not duplicate regex and length checks.
- **Persistence**: repositories expose focused queries like `findActiveByEntityTypeAndEntityIdAndFieldName(...)` and `searchTradeSettlementByKeyword(...)` for performance and clarity.

## 3. Data model and DTOs

### 3.1 Request DTO

`AdditionalInfoRequestDTO` is used for inbound writes only so I can validate incoming data without risking exposure of internal fields.

```java
public class AdditionalInfoRequestDTO {
    private String entityType;   // "TRADE"
    private Long entityId;       // tradeId
    private String fieldName;    // "SETTLEMENT_INSTRUCTIONS"
    private String fieldValue;   // the free text
}
```

### 3.2 Response DTOs

`AdditionalInfoDTO` is returned to clients. It includes metadata for the UI and audit views.

```java
public class AdditionalInfoDTO {
    private Long additionalInfoId;
    private String entityType;
    private Long entityId;
    private String fieldName;
    private String fieldValue;
    private String fieldType;        // defaults to "STRING" if missing
    private Boolean active;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private LocalDateTime deactivatedDate;
    private Integer version;
}
```

`AdditionalInfoAuditDTO` exposes audit trail entries safely.

```java
public class AdditionalInfoAuditDTO {
    private Long id;
    private Long tradeId;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String changedBy;
    private LocalDateTime changedAt;
}
```

### 3.3 Defensive defaults

When creating an `AdditionalInfo`, I ensure `fieldType` is set before saving to avoid a `NOT NULL` constraint failure.

```java
if (entity.getFieldType() == null || entity.getFieldType().trim().isEmpty()) {
    entity.setFieldType("STRING");
}
```

## 4. Validation strategy

I kept all settlement string checks in `TradeValidationEngine.validateSettlementInstructions(text)` and call that from the service during `create`, `update` and `upsert`. The rules are:

- Optional field. Only validate when provided.
- Length limits between 10 and 500 characters so we avoid empty noise and very large payloads.
- Structured characters only. I used a conservative regex to avoid control characters and reduce risk of copy‑pasted SQL being stored in the system.
- Simple injection guards. I reject obvious patterns like `--`, `;`, `drop table`, `delete from`.
- The same rules are reused consistently in `createAdditionalInfo(...)`, `updateAdditionalInfo(...)` and `upOrInsertTradeSettlementInstructions(...)`.

Example call site in `AdditionalInfoService`:

```java
if ("SETTLEMENT_INSTRUCTIONS".equalsIgnoreCase(fieldName)) {
    TradeValidationResult vr = tradeValidationEngine.validateSettlementInstructions(fieldValue);
    if (!vr.isValid()) {
        throw new IllegalArgumentException(vr.getErrors().get(0));
    }
}
```

This approach means the rules evolve in one place and all endpoints inherit the change.

## 5. Security model and authorisation

### 5.1 Controller level

I used `@PreAuthorize` for a first gate that reflects business expectations.

- Read and search endpoints are allowed to `TRADER`, `MIDDLE_OFFICE`, `SUPPORT`.
- Create, update and delete are limited to `TRADER`, `SALES`, `MIDDLE_OFFICE`, `ADMIN` depending on the operation.

Example:

```java
@GetMapping("/{id}/settlement-instructions")
@PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')")
```

### 5.2 Service level defence

I never trust client supplied identifiers such as `changedBy`. I read the principal from `SecurityContextHolder`, then check ownership or elevated roles. I prefer a central `UserPrivilegeValidator` when available and fall back to inline checks so unit tests can run without wiring full security.

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
Trade trade = tradeRepository.findLatestActiveVersionByTradeId(tradeId).orElse(null);
if (userPrivilegeValidator != null) {
    if (!userPrivilegeValidator.canEditTrade(trade, auth)) {
        throw new AccessDeniedException("Insufficient privileges ...");
    }
} else {
    boolean canEditOthers = auth.getAuthorities().stream()
        .anyMatch(a -> Set.of("ROLE_SALES","ROLE_SUPERUSER","ROLE_MIDDLE_OFFICE","ROLE_ADMIN","TRADE_EDIT_ALL")
        .contains(a.getAuthority()));
    String ownerLogin = trade.getTraderUser() != null ? trade.getTraderUser().getLoginId() : null;
    if (ownerLogin != null && !ownerLogin.equalsIgnoreCase(auth.getName()) && !canEditOthers) {
        throw new AccessDeniedException("Insufficient privileges ...");
    }
}
```

This two‑layer model prevents a trader from reading or altering another trader’s settlement instructions by guessing a trade id.

## 6. Service layer: how I implemented each operation

Class: `AdditionalInfoService`

### 6.1 Create a new row

`createAdditionalInfo(AdditionalInfoRequestDTO)` performs null checks, runs settlement validation when appropriate, sets a default `fieldType`, then saves and returns an `AdditionalInfoDTO`.

- Meets: optional field, structured validation, consistent responses.

### 6.2 Update an existing row

`updateAdditionalInfo(Long id, AdditionalInfoRequestDTO)` loads the target row, validates the new value and persists it. If it is a settlement record I also write an audit row when `oldValue` and `newValue` differ.

```java
if (oldValue != null && !oldValue.equals(newValue)) {
    AdditionalInfoAudit audit = new AdditionalInfoAudit();
    audit.setTradeId(existing.getEntityId());
    audit.setFieldName(existing.getFieldName());
    audit.setOldValue(oldValue);
    audit.setNewValue(newValue);
    audit.setChangedBy(changedByFromSecurityContext);
    additionalInfoAuditRepository.save(audit);
}
```

- Meets: audit trail requirement, business visibility and accountability.

### 6.3 Read settlement by trade id

`getSettlementInstructionsByTradeId(Long tradeId)` uses a focused repository method and runs a view check against the trade owner and the caller’s roles. It returns `null` when there is no active record because settlement text is optional.

### 6.4 Upsert with authorisation and audit

`upOrInsertTradeSettlementInstructions(Long tradeId, String settlementText, String changedBy)` implements the business happy path traders and sales use in the UI. I authorise via security context, validate content, then either update the active row or insert a new one. Every change writes an `AdditionalInfoAudit` entry and publishes a `SettlementInstructionsUpdatedEvent`.

```java
applicationEventPublisher.publishEvent(
    new SettlementInstructionsUpdatedEvent(
        String.valueOf(tradeId),
        tradeId,
        authUser,
        Instant.now().truncatedTo(ChronoUnit.MILLIS),
        Map.of("oldValue", oldValue, "newValue", settlementText)
    )
);
```

This keeps the service open for future real‑time notifications without coupling the controller to messaging.

### 6.5 Search by keyword

`searchTradesBySettlementText(String keyword)` delegates to `additionalInfoRepository.searchTradeSettlementByKeyword(...)` which performs a case‑insensitive partial match in the database. The controller then turns the matching `entityId` values into a list of `TradeDTO` for display. This supports operations users during daily reconciliations.

### 6.6 Non‑standard detector

`alertNonStandardSettlementKeyword(Long tradeId)` is a pragmatic server side classifier. It highlights words that often imply manual handling such as `manual`, `non‑dvp`, `physical`, `warehouse`. I return the first matching keyword or `null`. This is used by the get and update endpoints to add an informative header and a friendly message to the response.

### 6.7 Soft delete with audit

`deleteSettlementInstructions(Long tradeId)` locates the active row, authorises the caller, sets `active=false` and `deactivatedDate`, then writes an audit record with `newValue=null`. I prefer soft delete to retain historical context and to allow recovery in error cases.

### 6.8 Operator delete by id

`deleteAdditionalInfoById(Long additionalInfoId)` provides a precise tool for admins to deactivate a specific row. The method enforces stricter roles for non‑trade entity types. I added this after finding duplicate rows during manual testing.

## 7. Controller endpoints

Class: `TradeSettlementController`

I kept endpoints small and consistent. The summary is:

- `GET /api/trades/search/settlement-instructions?instructions=...`  
  Accepts a search term, calls the service search, deduplicates trade ids, then returns a list of `TradeDTO`.  
  Snippet:

  ```java
  List<AdditionalInfoDTO> infos = additionalInfoService.searchTradesBySettlementText(trimmedInput);
  Set<Long> tradeIds = infos.stream()
      .filter(i -> "TRADE".equalsIgnoreCase(i.getEntityType()))
      .map(AdditionalInfoDTO::getEntityId)
      .collect(Collectors.toSet());
  ```

- `GET /api/trades/{id}/settlement-instructions`  
  Returns the settlement record for a trade when present. Adds `nonStandardKeyword` and a short message in the payload, and an `X-NonStandard-Keyword` header to help the UI.

- `PUT /api/trades/{id}/settlement-instructions`  
  Validates `fieldName=SETTLEMENT_INSTRUCTIONS`, delegates to the upsert service and returns the saved DTO plus the non‑standard detection result.

- `DELETE /api/trades/{id}/settlement-instructions`  
  Soft deletes the active record and writes an audit entry.

- `DELETE /api/trades/additional-info/{additionalInfoId}`  
  Operator tool to deactivate a single row by primary key.

- `GET /api/trades/{id}/audit-trail`  
  Returns `AdditionalInfoAuditDTO` list ordered by `changedAt` for admin and middle office users.

- `GET /api/trades/{id}/settlement-instructions/identify-nonstandard`  
  Convenience read for detection during triage.

## 8. Audit trail design

I wrote a dedicated entity and repository for audit, surfaced via `AdditionalInfoAuditDTO`. I record `tradeId`, `fieldName`, `oldValue`, `newValue`, `changedBy`, and `changedAt`. I write the audit entry **after** the main save so a failed update does not produce a false audit line. Reads are restricted to admin and middle office.

This meets compliance expectations by providing a tamper‑evident history of changes including the actor and timestamp.

## 9. Events and integration readiness

I publish a lightweight `SettlementInstructionsUpdatedEvent` on create, update and delete. The payload includes the trade id, the authenticated username and a map of `oldValue` and `newValue`. This is enough for a toast in the UI, a Slack notification, or a downstream export job without introducing a coupling to any one consumer.

If no listeners are configured the service still works, since event publication is wrapped in a try catch and failures do not affect the main transaction.

## 10. Performance notes

- I use **focused repository queries** to avoid loading all rows. Search uses `LIKE %keyword%` at the database and returns only matching records.
- I **deduplicate trade ids** in memory to avoid repeated entity mapping.
- I **soft delete** rather than hard delete so historical reads are simple and write amplification is minimal.
- Validation is **O(n)** in the length of the string and is executed only when settlement text is provided.

## 11. Error handling and messages

I throw `IllegalArgumentException` for input issues and let Spring map that to HTTP 400. Authorisation failures throw `AccessDeniedException` and return HTTP 403. Missing data returns HTTP 404 where sensible, for example when a trade id does not exist or there is no settlement text for that trade. These are the messages users actually see in integration tests and Swagger UI.

Examples:

```java
return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No settlement instructions found for trade ID: " + id);
```

```java
return ResponseEntity.status(HttpStatus.FORBIDDEN)
    .body("Insufficient privileges to delete settlement instructions for trade " + id);
```

## 12. Testing approach

I broke tests into layers:

- **Service tests** focus on `AdditionalInfoService` behaviour, especially validation, authorisation fallbacks, audit writing and event publication resilience. I mock repositories and the `SecurityContext`.
- **Controller integration tests** verify the JSON contracts, the security annotations, and that non‑standard detection headers and messages are present where expected.
- **Search tests** assert case‑insensitive partial matching and deduplication of trade ids.

I mirrored the approach I used in Step 4 for cashflows by capturing saved entities with `ArgumentCaptor` when I needed to verify the exact audit fields.

## 13. How this meets the UBS requirements

- Settlement instructions are captured during trade booking and amendment using `PUT /{id}/settlement-instructions` and the service upsert.
- Operations can search by partial text and export matching trades using `GET /search/settlement-instructions`.
- Every change is auditable with actor and timestamp through `AdditionalInfoAudit` and the audit endpoint.
- Access is enforced at two layers. Traders see and edit their own; middle office and admins can assist. Support can read but not write.
- The solution is flexible. If the desk needs new tagged fields, they can be added without schema changes.
- Non‑standard detection and domain events improve operational awareness immediately after a change.

## 14. Alternatives I considered

- **Direct Trade column**: straightforward but grows the `trade` table and encourages putting every free‑text operational note there. The key–value design scales better.
- **Hard delete**: simpler but removes history and weakens audit. I chose soft delete to support compliance and root‑cause analysis.
- **Client‑side only validation**: faster perceived UX but unsafe. I kept server side validation authoritative and kept the regex rules in one place.

## 15. Small code snippets that capture the essence

Authorisation fallback:

```java
boolean canEditOthers = auth.getAuthorities().stream().anyMatch(a ->
    Set.of("ROLE_SALES","ROLE_SUPERUSER","ROLE_MIDDLE_OFFICE","ROLE_ADMIN","TRADE_EDIT_ALL")
      .contains(a.getAuthority())
);
String ownerLogin = trade.getTraderUser() != null ? trade.getTraderUser().getLoginId() : null;
if (ownerLogin != null && !ownerLogin.equalsIgnoreCase(auth.getName()) && !canEditOthers) {
    throw new AccessDeniedException("Insufficient privileges ...");
}
```

Upsert flow:

```java
AdditionalInfo existing = repo.findActiveByEntityTypeAndEntityIdAndFieldName("TRADE", tradeId, "SETTLEMENT_INSTRUCTIONS");
if (existing != null) {
    oldValue = existing.getFieldValue();
    existing.setFieldValue(settlementText);
    target = repo.save(existing);
} else {
    AdditionalInfoRequestDTO dto = new AdditionalInfoRequestDTO("TRADE", tradeId, "SETTLEMENT_INSTRUCTIONS", settlementText);
    target = repo.save(mapper.toEntity(dto));
    if (target.getFieldType() == null) target.setFieldType("STRING");
}
auditRepo.save(new AdditionalInfoAudit(tradeId, "SETTLEMENT_INSTRUCTIONS", oldValue, settlementText, authUser, now));
```

Search bridge to trades:

```java
List<AdditionalInfoDTO> infos = additionalInfoService.searchTradesBySettlementText(keyword);
Set<Long> ids = infos.stream().filter(i -> "TRADE".equalsIgnoreCase(i.getEntityType()))
    .map(AdditionalInfoDTO::getEntityId).collect(Collectors.toSet());
List<Trade> trades = tradeService.getTradesByIds(new ArrayList<>(ids));
List<TradeDTO> out = trades.stream().map(tradeMapper::toDto).toList();
```

## 16. What I would do next

- Add settlement templates and server side validation tuned by counterparty and trade type.
- Persist the non‑standard classification result so risk reports can filter by it without recalculating.
- Add SSE or WebSocket notifications that listen to the domain event and update the blotter in real time.
- Extend audit to include the reason for change and an optional ticket id to support operational sign‑off.
