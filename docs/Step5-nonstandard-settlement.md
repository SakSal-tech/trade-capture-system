## Identify non-standard settlement instructions

### Summary

- Feature: flag and surface non-standard settlement instructions that require manual handling or operational review.
- Scope: backend detection (service + controller) and frontend detection + accessible UI warning. Also: API changes so Swagger/curl clients can see the detection result.

---

## Backend

### Completed

- Added or wired rule-based detection in the service layer (existing method used: `alertNonStandardSettlementKeyword(...)`).
- Modified GET and PUT settlement endpoints to return a small envelope containing:

  - `additionalInfo` (the DTO that existing clients expect)
  - `fieldValue` (kept top-level for backward compatibility)
  - `nonStandardKeyword` (string|null) the matched keyword, null if standard
  - `message` (string|null) a human-friendly message for UI/Swagger

- When a non-standard keyword is detected, the response also includes a response header `X-NonStandard-Keyword` so API consumers and Swagger UI can highlight the response.

Example response JSON (GET /api/trades/{id}/settlement-instructions):

```json
{
  "additionalInfo": {
    "id": 123,
    "fieldName": "SETTLEMENT_INSTRUCTIONS",
    "fieldValue": "Payment by manual instruction to account ..."
  },
  "fieldValue": "Payment by manual instruction to account ...",
  "nonStandardKeyword": "manual",
  "message": "Contains non-standard settlement instruction: 'manual' please review manually."
}
```

Example controller header when keyword found:

X-NonStandard-Keyword: manual

Example Java snippet (controller envelope and header):

```java
// simplified for log
AdditionalInfoDTO dto = additionalInfoService.getByTradeId(tradeId);
String kw = additionalInfoService.alertNonStandardSettlementKeyword(tradeId);
Map<String,Object> resp = Map.of(
  "additionalInfo", dto,
  "fieldValue", dto.getFieldValue(),
  "nonStandardKeyword", kw,
  "message", kw==null?null: "Contains non-standard settlement instruction: '"+kw+"'"
);
return ResponseEntity.ok()
  .header("X-NonStandard-Keyword", kw==null?"":kw)
  .body(resp);
```

### Learned

- Keep mappers pure enrich DTOs in the service/controller layer rather than introducing service calls in mappers. This keeps tests and inversion of control simple.
- Returning a compatibility top-level `fieldValue` is low-risk; it avoids breaking clients expecting the earlier shape while letting us add additional metadata.
- Adding a small response header (`X-NonStandard-Keyword`) makes it trivial for API clients (curl, Swagger UI) to detect and highlight non-standard responses without changing client JSON parsing.

### Challenges

- Backwards compatibility: changing response shape risks breaking clients. Solution: keep `fieldValue` at top-level and include `additionalInfo` envelope.
- Deciding where to put rule definitions: server-side detection is authoritative, but some quick client-side rules are handy for UX. Consider centralizing rule-config (future work: endpoint to fetch rules).
- Testing: need unit/integration tests to cover the controller envelope and presence of the header not yet added here.

---

## Frontend

### Completed

- Implemented a client-side detector in `SettlementTextArea.tsx` for instant UX. This runs on every change and after template insertion.
- Detection list (examples): `const nonStandardIndicators = ["manual", "non-dvp", "non dvp"]`.
- Added `useEffect(() => runNonStandardDetection(value), [value])` so any change triggers detection.
- Fixed insertion concat issue: `insertAtCursor(text)` now ensures a separating space is inserted when needed.
- Added an accessible, visible red banner (aria-live) that appears when either client or server detection finds non-standard text. Color chosen for danger: red #dc2626 (Tailwind's red-600 / close to it). Banner text is white on red for contrast.

Example TypeScript detection snippet:

```ts
const nonStandardIndicators = ["manual", "non-dvp", "non dvp"];

function runNonStandardDetection(settlementText: string): string | null {
  const lower = settlementText.toLowerCase();
  return nonStandardIndicators.find((i) => lower.includes(i)) || null;
}

useEffect(() => {
  const kw = runNonStandardDetection(value);
  setNonStandardKeyword(kw);
}, [value]);
```

Example insertion-space logic (avoid concatenation):

```ts
function insertAtCursor(insertText: string) {
  const before = value.slice(0, selStart);
  const after = value.slice(selEnd);
  const sep =
    before && !/\s$/.test(before) && !/^\s/.test(insertText) ? " " : "";
  const next = before + sep + insertText + after;
  setValue(next);
  runNonStandardDetection(next);
}
```

Example banner (Markdown with color notes):

```html
<div
  role="status"
  aria-live="polite"
  style="background:#dc2626;color:#ffffff;padding:8px;border-radius:4px;"
>
  ⚠️ Contains non-standard settlement instruction: 'manual' please review
  manually.
</div>
```

### Learned

- Client-side detection is great for immediate feedback while typing. It should be lightweight (simple keyword matching) to avoid performance issues.
- Server detection is authoritative. Best UX: keep client detector for instant feedback and also show server-detected results when loading/saving to avoid mismatch surprises.
- Small UI details matter: insertion spacing and caret restoration are essential for templates to not create accidental false positives (concatenated words can hide keywords).

### Challenges

- Keeping server and client rule lists in sync. Current approach duplicates simple keyword list on client and uses service on server; future improvement: centralize rules on server and expose an endpoint so the UI can fetch them.
- Accessibility: ensuring the banner is announced (aria-live) but not too intrusive. We used polite aria-live so screen-readers announce changes without interrupting.
- Duplicate controls: parent modal had a second Clear this caused confusion. We removed the duplicate so only the local red Clear near the textarea remains.

---

## Example non-standard keywords & sample data

- Non-standard keywords used during development and tests:
  - "manual" (example: settlement text containing the word manual)
  - "non-dvp" / "non dvp"
  - (intentionally omitted: "further credit" this was removed from client detectors because it flagged an approved JPM template)

Sample settlement text that should be flagged:

```
Payment to account 123456 manual instruction via custodian.
```

Sample settlement text that should NOT be flagged (standard):

```
DVP via Euroclear - settlement to account IBAN XXXX.
```

---

## Files edited during this work (high level)

- `backend/src/main/java/.../controller/TradeSettlementController.java` GET/PUT modified to return envelope and `X-NonStandard-Keyword` header.
- `backend/src/main/java/.../service/AdditionalInfoService.java` detection method exists and is used (no breaking change to public API of service).
- `frontend/src/modal/SettlementTextArea.tsx` client-side detection, insertion-space fix, accessible red banner, local Clear button styling.
- `frontend/src/modal/TradeActionsModal.tsx` removed duplicate small Clear adjacent to Save and passed server-provided detection through props (if implemented).

---

## How to test (quick test plan)

1. Backend manual test (curl): GET a trade that has a non-standard settlement text and observe the JSON envelope and header:

```bash
curl -i -u user:pass http://localhost:8080/api/trades/10008/settlement-instructions
```

Expect: response JSON contains `nonStandardKeyword` and `message`, and header `X-NonStandard-Keyword: manual` (or similar).

2. Frontend manual test:

- Open trade in UI, the settlement textarea should show the red banner if server or client detects non-standard text.
- Type to remove the keyword; client banner should disappear immediately. Save and verify server detection clears too (if saved response returns null keyword).
- Use the template insertion buttons to add a template that ends with a word (verify insertion-space prevents concatenation and detection still works).
