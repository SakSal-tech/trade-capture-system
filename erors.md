# Errors Summary

Generated: 2025-11-03
Source: development_error_log.md

---

## 1) Integration Test Failure: 404 Not Found (Trade Edit)

- Test: `UserPrivilegeIntegrationTest.testTradeEditRoleAllowedPatch`
- Symptom: Status expected:<200> but was:<404>
- Cause: PATCH used an ID not present in DB; controller expects business tradeId (`trade.tradeId`) not the DB primary key.
- Fix: Create a trade with `trade.setTradeId(...)` and use its business id for PATCH.

## 2) Integrity Violation: Duplicate Application User

- Symptom: DataIntegrityViolationException on APPLICATION_USER(login_id)
- Cause: Test inserted user already present in `data.sql`.
- Fix: Reuse existing user via `applicationUserRepository.findByLoginId("simon")` instead of inserting duplicate.

## 3) IncorrectResultSizeDataAccessException (Duplicate TradeStatus)

- Symptom: query did not return a unique result: 2
- Cause: `data.sql` and test setup both inserted `TradeStatus` rows (e.g., `NEW`) causing duplicates.
- Fix: Reuse `data.sql` values or clean `trade_status` before inserting; use `tradeStatusRepository.findByTradeStatus("NEW")`.

## 4) HTTP 400 on Trade Creation (Field name mismatch)

- Symptom: Status expected:<201> but was:<400>
- Cause: Test JSON used wrong field names (e.g., `startDate`/`maturityDate`) instead of DTO-expected names (e.g., `tradeStartDate` / `tradeMaturityDate`).
- Fix: Align payload to DTO naming (e.g., `"tradeMaturityDate": "2026-10-15"`).

## 5) Data Conflicts Between `data.sql` and Test Setup

- Symptom: Duplicate inserts for Books, Counterparties, Trade Statuses.
- Cause: Spring loaded `data.sql` and tests re-seeded the same records.
- Fix: Reuse seeded rows via repository lookups (e.g., `bookRepository.findByBookName("FX-BOOK-1")`).

## 6) TradeController Patch Logic (404) ID mismatch

- Symptom: PATCH returned 404 due to ID mismatch.
- Cause: Controller/test used internal DB id while `amendTrade()` expects business id.
- Fix: Use business tradeId in controller path and tests (e.g., `patch("/api/trades/" + savedTradeBusinessId)`).

---

### Quick verification note

Focused run executed during debugging:

```bash
mvn -f backend/pom.xml -Dtest=UserPrivilegeIntegrationTest#testTradeEditRoleAllowedPatch test
```

Result (focused): create returned `201`, patch returned `200` (focused scenario fixed locally).

If you want, I can append DB row excerpts or failing test stack traces to this file, or create a dated history file for future runs.
