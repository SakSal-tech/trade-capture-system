## NEWlog — 27/10/2025

### Settlement instructions persisted in AdditionalInfo (Option B)

### Completed

- Implemented backend support to store settlement instructions in the existing `AdditionalInfo` table rather than adding a new column on `Trade` (Option B). The implementation inserts/updates an `AdditionalInfo` row with `entity_type = 'TRADE'`, `entity_id = <tradeId>` and `field_name = 'SETTLEMENT_INSTRUCTIONS'`.
- Implemented the read/search/update flows in `AdditionalInfoService` so they operate on that key consistently. The update flow validates the incoming payload, upserts the `AdditionalInfo` row, and writes an `AdditionalInfoAudit` record.
- Relaxed the settlement instruction validation to accept the plus-sign token (`+`) where the business rules require it after reproducing a failure with payloads such as `t+1` (trade date reference). The regex used by `SettlementInstructionValidator` was adjusted to allow `+` in the specific token group rather than permitting all punctuation.

### How

- Reproduced the validation failure by issuing a PUT settlement-instructions request in an integration test and observing the server returned HTTP 500 when the payload contained the `t+1` token. The validator rejected the token and the code path threw an exception. A targeted unit test validated the regex behaviour, then the regex was updated to include `+` in the allowed character class for settlement shorthand.
- The service upsert path fetches the trade via `TradeRepository.findById(tradeId)` to confirm the trade exists and to retrieve `trader_user_id` for ownership checks. The `AdditionalInfoRepository` is then used to find existing `AdditionalInfo` by `(entity_type, entity_id, field_name)` and either update or insert.
- The audit row is created by mapping the old value and new value into an `AdditionalInfoAudit` and saving it via `AdditionalInfoAuditRepository`.

### Why

- Chosen Option B to avoid a schema change and data migration. This minimises impact on existing DTOs and mappers and leverages existing indexed search on `AdditionalInfo` (index `idx_ai_entity_type_name_id`).
- Relaxing the validator only for the `+` token reduces operational friction (operators use `t+1` shorthand) while keeping the rest of the validation intact to catch malformed inputs.

### Challenges

- The initial integration test surfaced a 500 when a legitimate operator shorthand (`t+1`) was used; required careful regex editing and re-running the relevant unit and integration tests.

---

### Server-side ownership and permission enforcement for settlement endpoints

### Completed

- Added server-side ownership checks inside `AdditionalInfoService` for both read and write flows so a trader cannot view or edit another trader’s settlement instructions unless elevated privileges exist (for example `ROLE_SALES`, `ROLE_ADMIN` or a specific `TRADE_EDIT_ALL` privilege).
- The ownership check flow resolves the current principal from `SecurityContextHolder.getContext().getAuthentication()` and compares the principal's login id (or resolved user id via `ApplicationUserService`) against the `Trade.trader_user_id` loaded from the trade record. If the principal is not the owner and lacks the elevated roles/privileges, an `AccessDeniedException` is thrown.

### How

- Implemented by injecting `TradeRepository` into `AdditionalInfoService` and, on write, calling `tradeRepository.findById(tradeId)` to get `Trade.traderUserId`. The principal is resolved via `SecurityContextHolder` and the principal login id is mapped to an `ApplicationUser` (where necessary) to compare ids.
- Role checks use `authentication.getAuthorities()` to short-circuit owner checks when the caller has `ROLE_SALES`, `ROLE_ADMIN` or the `TRADE_EDIT_ALL` privilege. Unit tests mock the `Authentication` and `ApplicationUserService` to simulate different caller roles and owners.

### Why

- Service-level checks provide defence in depth: controller `@PreAuthorize` annotations are useful but do not protect against direct service calls or accidental endpoint changes. Placing the ownership logic in the service ensures the protection is enforced where the data mutation happens.

### Challenges

- Tests relied on seed data (`data.sql`) where trade 200002 is owned by `simon` (application_user id 1002). Some tests initially authenticated as `alice` and therefore failed with 403; tests were updated to use the correct seeded principals.

---

### Simon / Joey incident (reproduction and fix)

### What happened

- Observed that the trader `simon` could view and create settlement instructions for another trader's trade (referred to as Joe's trade in the ticket). The integration reproduction used the seeded trade id (200002) and a PUT request to `/api/trades/200002/settlement-instructions` authenticated as `simon`, which unexpectedly succeeded before ownership checks were added.

### How it was reproduced

- Reproduced in an integration test and in manual test runs by authenticating as `simon` (credentials from `data.sql`) and issuing a PUT with a valid settlement payload. The call returned HTTP 200 and the `AdditionalInfo` row for `tradeId=200002` was created/updated; audit records showed `changedBy=simon` even though the trade's `trader_user_id` belonged to another user.

### Why it happened

- The service layer previously relied solely on controller-level method-security annotations for broad role checks and did not verify ownership at the service boundary. This left a gap where authenticated principals with allowed HTTP access could update `AdditionalInfo` for any trade if controller-level checks were not sufficiently granular.

### What was changed and how

- Injected `TradeRepository` into `AdditionalInfoService` and added a deterministic owner-lookup step: `tradeRepository.findById(tradeId)` to obtain `Trade.trader_user_id`. The current principal is resolved via `SecurityContextHolder.getContext().getAuthentication()`; the principal's login id is mapped to the `ApplicationUser` id where necessary and compared to `trader_user_id`.
- If the principal is not the owner and does not have `ROLE_SALES`, `ROLE_ADMIN` or the `TRADE_EDIT_ALL` privilege, the service now throws `AccessDeniedException` and the controller returns 403. Unit tests were updated to simulate `simon` and `joe` principals and assert the deny/allow outcomes.

### Verification

- Confirmed via an integration test that a PUT authenticated as `simon` to Joe's trade now returns 403. Confirmed audit records are not created for denied attempts and that allowed calls by owners or privileged roles still create audit entries with `changedBy` correctly set.

### Audit entries forced to use authenticated principal (no client-supplied changedBy)

### Completed

- Enforced server-side population of `AdditionalInfoAudit.changedBy` using the resolved principal from the `SecurityContext` instead of any client-supplied value.

### How

- In `AdditionalInfoService` the audit creation code calls `SecurityContextHolder.getContext().getAuthentication().getName()` and sets that value into `audit.setChangedBy(...)` before saving the audit entity. Unit tests that previously supplied a `changedBy` on the DTO were updated to assert the server-populated value instead.

### Why

- Relying on client-supplied `changedBy` permits spoofing of audit records. For compliance and traceability, audit actor must be resolved server-side.

### Challenges

- Tests had to be adjusted: some test fixtures assumed `changedBy` was provided by the client; these were updated to mock the security context and assert the service-set `changedBy` value.

---

### Tests added and updated (unit + integration) and failing-test fixes

### Completed

- Added unit tests simulating different authenticated principals and roles to assert that ownership and role checks behave as intended (e.g., trader editing another trader’s trade -> denied; `ROLE_SALES` allowed).
- Updated integration tests to authenticate as principals matching the seeded `data.sql` values (for example trade 200002 maps to `simon`), and added assertions that `AdditionalInfoAudit.changedBy` equals the authenticated principal.
- Fixed a unit test NullPointerException by stubbing `TradeValidationEngine.validateSettlementInstructions(...)` to return a valid `TradeValidationResult` rather than null so the permission logic is reached and asserted.

### How

- Tests that need authentication use the project's test support to populate the security context via `@WithMockUser` or by performing a login call against `/api/login/<user>` using credentials from `data.sql`.
- Where mocks were used, Mockito `when(...).thenReturn(...)` now returns realistic non-null objects for validation results and repositories. Integration tests run against an in-memory H2 DB populated from `data.sql` so assertions are deterministic.

### Why

- Accurate tests are required to avoid false negatives/positives: mocking a validator to return null produced an NPE and hid the permissions assertion; realistic returns ensure the test exercises the intended branch.

### Challenges

- Aligning test authentication principals with seeded data required searching `data.sql` and updating several tests. The mismatch caused the earlier 403s.

---

### Swagger UI and manual endpoint testing

### Completed

- Verified settlement endpoints via the Swagger UI (`/swagger-ui/index.html`) and through direct HTTP requests using the project's example payloads. Created and updated settlement instructions via the Swagger POST/PUT forms for seeded trades and confirmed successful responses and audit entries when allowed.
- Confirmed the validation behaviour and the updated regex acceptance by submitting payloads containing shorthand tokens such as `t+1` through the Swagger UI and observing HTTP 200 responses when valid or HTTP 400/500 when malformed (then fixed by the regex change).

### How

- Used the running local server during development and opened the Swagger UI to exercise the endpoints interactively. For each endpoint tested (GET, PUT `/api/trades/{id}/settlement-instructions`, GET `/api/trades/{id}/audit-trail`) the request/response bodies were inspected in the browser and in the application logs.
- For PUT operations, the request was authenticated using the same login flow as the integration tests (login via `/api/login/<user>`). After authentication, the Swagger calls succeeded and produced audit rows; denied calls returned 403 as expected and were visible in the Swagger response panel.
- Also executed a few scripted curl requests to reproduce edge cases (e.g., `t+1`, empty payload, long payload) and confirmed server validation and error handling matched expectations.

### Why

- Testing through Swagger provided a quick, human-driven verification of the full stack (controller → service → repository → audit) and made it easier to demonstrate the end-to-end behaviour to stakeholders.
- Manual testing surfaced the `t+1` rejection quickly in a realistic client flow which helped focus the validator fix to the exact token pattern operators use.

### Challenges

- Swagger UI required an authenticated session to exercise protected endpoints; needed to login first and ensure the session cookie or Authorization header was available to Swagger calls.

### Learned

- Interactive Swagger testing is an efficient sanity-check for API behaviour and complements automated tests by exercising the live stack and showing concrete request/response payloads.

### Improved 403 messaging for the audit endpoint

### Completed

- Left the access policy unchanged: GET `/api/trades/{id}/audit-trail` remains restricted to `ROLE_ADMIN` and `ROLE_MIDDLE_OFFICE`.
- Updated `ApiExceptionHandler` so that when an `AccessDeniedException` is thrown for that specific endpoint and HTTP method (GET + path matching `/api/trades/.+/audit-trail`), the JSON 403 body uses the message: "Only Admin or Middle Office users may view audit history for trades." For other 403s the existing contextual messages remain.

### How

- The `@ControllerAdvice` method `handleAccessDenied` inspects `HttpServletRequest.getMethod()` and `getRequestURI()`; if the request matches the audit GET path, the handler returns a `ResponseEntity` with the audit-specific message. Otherwise it returns the standard message previously used.

### Why

- A trade owner receiving a generic 403 message for the audit endpoint caused confusion. Clarifying the message for the audit endpoint improves operational clarity without changing the underlying security policy.

### Challenges

- Implementing a path-and-method-specific override in the global exception handler required careful checks so no other 403 messages are unintentionally altered.

---

### Code annotations and small cleanups

### Completed

- Added rationale comments to `ApiExceptionHandler` and `AdditionalInfoService` documenting the business reasons for the ownership checks and the audit behaviour.

### How

- Comments reference the Simon/Joey scenario and point to the relevant requirement ticket (TRADE-2025-REQ-005) to help reviewers understand the why behind the changes.

### Why

- Embedding business rationale directly near the implementation reduces cognitive load during future maintenance and aids code review.

---

### CI / full test-suite verification

### Completed

- Executed a full Maven test run for the backend `pom.xml` after the changes and confirmed BUILD SUCCESS.
- Verified an unauthorised GET to the audit endpoint produced the updated JSON 403 message in the server logs.

### How

- Ran `mvn -f backend/pom.xml test` from the repository root. The test run initialised the in-memory database from `data.sql`, started the Spring context and executed unit and integration tests.

### Why

- Running the full test-suite validated cross-cutting behaviour between security, validation and audit changes; this prevented regressions from being introduced.

---

### Database index and performance verification

### Completed

- Added a Postgres migration SQL file `docs/postgres/add_lower_index_additional_info.sql` which creates a functional `lower(field_value)` index (`idx_ai_field_value_lower`) on `additional_info.field_value`. The file includes guidance for verification and rollback.

### How

- The migration SQL issues `CREATE INDEX IF NOT EXISTS idx_ai_field_value_lower ON additional_info (lower(field_value));` and documents the following verification workflow:
  - Capture a baseline plan and timing with `EXPLAIN ANALYZE` for representative queries that use `LOWER(field_value)` or `ILIKE` (for example searches for settlement text and counterparty names).
  - Apply the index in a staging or productionised testing environment during a low-traffic window.
  - Re-run the same `EXPLAIN ANALYZE` queries and compare execution time and query plan to ensure the planner uses the index and the total execution time reduces.

### Why

- The application performs case-insensitive searches over `additional_info.field_value` for settlement instruction lookups. Without a functional `lower()` index these queries can require a sequential scan on larger tables, increasing latency and CPU usage. The `lower()` index enables efficient case-insensitive equality and prefix searches without requiring the pg_trgm extension.

### Challenges

- Creating an index on a large table needs careful scheduling to avoid I/O contention. The SQL includes `IF NOT EXISTS` and rollback notes to reduce deployment risk.

### Learned

- A small functional index gives measurable benefits for case-insensitive prefix/equality queries, and the `EXPLAIN ANALYZE` before/after approach provides objective evidence for the DBA and release owners.

### Fixes applied during the day (summary)

### Completed

- Implemented service-level ownership checks for settlement instructions.
- Enforced server-side audit.actor resolution.
- Relaxed settlement validation to accept required `+` token after reproducing a failing `t+1` case.
- Added and updated unit and integration tests to reflect seeded data and to stub dependencies that previously caused NPEs.
- Clarified audit-specific 403 message while preserving other contextual denial messages.
- Annotated exception handler and service methods with rationale comments.
- Performed targeted and full Maven test runs; resolved test failures and confirmed green build.

### Challenges

- Aligning seed data, test principals and production behaviour exposed by tests.
- Diagnosing a null-returning mock that led to a misleading NPE rather than an authorisation assertion.

### Learned

- Seed data and test authentication identities must be synchronised for reliable tests.
- Defensive test design (provide realistic non-null mock returns) improves diagnostic value.

---
