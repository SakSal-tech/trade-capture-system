# Developer log 03/11/2025

Timeframe: work started 03/11/2025 at 10:00 (UK time) and carried through into 04/11/2025.

This log is written in the first person. It summarises what I completed today, what I learned, and the challenges I hit for each task. I keep the language plain and British English.

---

## Summary

Today I focused on making validation more consistent and visible, tidying some integration tests, and improving how exceptions are returned to the client so front-end tools (Swagger, UI) get clear messages instead of stack traces. I did not change the dashboard service logic itself.

---

### Task: Add / tidy Global exception handler

### Completed

- Created and then annotated `GlobalExceptionHandler` to return a consistent minimal JSON payload: `{ timestamp, status, message }` for errors.
- Added developer comments and guidance in the handler explaining why we mustn't change the response shape without coordinating with the front-end team.

### Learned

- Returning a single, predictable JSON shape helps the front end handle errors consistently.
- Mapping all `RuntimeException` to 400 is convenient but can hide server-side causes (for example, commit-time UnexpectedRollbackException). The handler should be specialised over time (e.g. add `MethodArgumentNotValidException` handlers) rather than broad.

### Challenges

- There is a tension between giving engineers detailed stack traces for debugging and providing users a clean, safe message. I left the handler conservative (minimal info) and documented recommended non-breaking enhancements.

---

### Task: Investigate and document validation behaviour (DTOs, validators, engine)

### Completed

- Read and analysed the relevant DTOs and validators (`TradeDTO`, `TradeLegDTO`, `TradeLegValidator`, `TradeDateValidator`, `TradeValidationEngine`).
- Confirmed the current business rule: trades must have two legs, and each leg must have a maturity date defined and the two leg maturities must match exactly.
- Noted that cashflow generation still used the top-level trade maturity in some places an inconsistency to address.
- Updated `TradeValidationEngine`, `EntityStatusValidationEngine`, and `EntityStatusValidator` to centralise validation orchestration and to allow repository-backed checks where Spring DI is present.

### Learned

- Centralising validation into an engine makes it easier to add repository-backed checks (existence/active flags) without sprinkling logic across services and tests.
- Unit tests sometimes run without Spring wiring; I kept a no-arg constructor path so tests remain lightweight while production uses the injected beans.

### Challenges

- Balancing strict repository-backed validations with test ergonomics: some tests previously failed because they expected the engine or repositories not to be wired. To avoid breaking many tests I kept a null-safe fallback for the engine where appropriate.
- I found multiple places where field names in test payloads did not match DTO properties (e.g. `maturityDate` vs `tradeMaturityDate`), which caused 400s until corrected.

---

### Task: Update `TradeService` to use validation engine and add defensive wiring

### Completed

- Added wiring in `TradeService` to call the `TradeValidationEngine` when available and to throw a `ResponseStatusException` with useful messages when validation fails.
- Ensured that service code keeps a defensive approach so tests that don't wire the engine keep working.

### Learned

- Validations that run within a transaction can cause the transaction to be marked rollback-only; the actual commit-time `UnexpectedRollbackException` can mask the original error if exception handling is too broad.

### Challenges

- Keeping behaviour backward compatible for tests while improving validation surfaced several test-data and seeding issues that I had to tidy (see tests section below).

---

### Task: Fix and harden integration tests and test setup

### Completed

- Refactored several integration tests to seed reference data idempotently (check before insert) rather than unconditionally inserting duplicate rows that `data.sql` already provides.
- Adjusted test setup to create trades where needed and to use the business `tradeId` for controller path parameters.
- Converted some legacy test payloads to use the DTO field names expected by the back end (e.g. `tradeStartDate`, `tradeMaturityDate`).

### Learned

- `data.sql` and test setup can conflict; tests should either reuse `data.sql` rows or clear the table before seeding.
- MockMvc requests in integration tests run in a different transaction; committing seeded reference data using a transaction template makes the data visible to the web layer.

### Challenges

- A number of tests exposed hidden assumptions (hard-coded IDs, different date fields). Making the tests self-contained required creating trades in the test run and parsing their returned business id to use in PATCH calls.

---

### Task: Doc updates and error log consolidation

### Completed

- Wrote and appended detailed entries to `Development-Errors-and-fixes.md` and created a shorter `erors.md` synopsis listing problems and resolutions encountered during the day.
- Created this developer log file with a human-readable breakdown of the work done.

### Learned

- Keeping a concise timeline and linking each change to its root cause (test failure, runtime error, or data mismatch) helps reviewers understand why a change was necessary.

### Challenges

- Ensuring log entries are accurate without repeating large stack traces I kept the log focused on cause and remedy; full traces can be attached if reviewers want them.

---

## Notable runtime observation (important)

- When calling the dashboard summary endpoint via Swagger, the client received a 400 with the message: "Transaction silently rolled back because it has been marked as rollback-only". This is the commit-time symptom (UnexpectedRollbackException) and not the original root cause. The original exception occurred earlier in the request and must be located in the server logs to fix the underlying problem.

- I did not modify `TradeDashboardService`. The above behaviour is caused by an exception earlier in the same transaction (for example a validator, a repository query, or a data mapping error). The global exception handler currently maps runtime exceptions to 400 and so the rollback message was returned to the client instead of the original cause.

---

## Next steps (what I'd do next morning)

1. With your permission, I will paste the server log lines around the rollback exception and identify the first "Caused by" stack trace so we can fix the root cause. This is the fastest way to find the actual bug.
2. Decide and implement the maturity propagation rule (we currently have two options: propagate top-level maturity into legs on the server, or require the front end to always supply per-leg maturities). We have a todo item tracking this.
3. Add more specific exception handlers to `GlobalExceptionHandler` (e.g. `MethodArgumentNotValidException`) so field-level errors return an `errors` array for the front end.
4. Update the front end to surface returned `message` / `errors` to users and ensure Swagger payloads include both leg maturities (or the server populates them prior to validation) depending on the chosen strategy.

---

## Detailed examples, test data and step-by-step actions

Below I add concrete examples from the codebase and the JSON payloads I used while applying and integrating validation rules into `TradeService`. I explain the exact steps I ran when tests started failing and how I fixed them.

### Code excerpts (what I changed / relied on)

- Trade leg validation (logic found in `TradeLegValidator`):

  - Key checks enforced by the validator:

    - Ensure there are at least two legs:

      ```java
      if (tradeLegs == null || tradeLegs.size() < 2) {
      		result.setError("Trade must have at least two legs");
      		return false;
      }
      ```

    - Ensure both legs have a maturity date:

      ```java
      if (leg1.getTradeMaturityDate() == null || leg2.getTradeMaturityDate() == null) {
      		result.setError("Both legs must have a maturity date defined");
      		return false;
      }
      ```

    - Ensure the two maturities are identical:

      ```java
      if (!leg1.getTradeMaturityDate().equals(leg2.getTradeMaturityDate())) {
      		result.setError("Both legs must have identical maturity dates");
      		return false;
      }
      ```

- How `TradeService` invokes the validation engine (added defensive call):

  ```java
  if (tradeValidationEngine != null) {
  		TradeValidationResult validationResult = tradeValidationEngine.validateTradeBusinessRules(tradeDTO);
  		if (!validationResult.isValid()) {
  				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
  								"Validation failed: " + String.join("; ", validationResult.getErrors()));
  		}
  }
  ```

  - Note: throwing `ResponseStatusException` inside a `@Transactional` service method will mark the transaction rollback-only; that leads to an `UnexpectedRollbackException` at commit time if something else catches and masks the original exception.

### Example JSON payloads I used in tests (before/after fixes)

- Problematic (previously used) payload this used the wrong field names and omitted per-leg maturities. This produced 400s from DTO validation:

  ```json
  {
    "tradeId": 200002,
    "bookName": "TestBook",
    "counterpartyName": "BigBank",
    "tradeDate": "2025-01-01",
    "startDate": "2025-01-01",
    "maturityDate": "2026-01-01",
    "tradeLegs": [
      {
        "legId": 1,
        "notional": 1000000,
        "currency": "USD",
        "startDate": "2025-01-01",
        "endDate": "2026-01-01"
      },
      {
        "legId": 2,
        "notional": 1000000,
        "currency": "USD",
        "startDate": "2025-01-01",
        "endDate": "2026-01-01"
      }
    ]
  }
  ```

- Corrected payload (matches DTO fields and includes per-leg `tradeMaturityDate`):

  ```json
  {
    "tradeId": 200002,
    "bookName": "TestBook",
    "counterpartyName": "BigBank",
    "tradeDate": "2025-10-15",
    "tradeStartDate": "2025-10-15",
    "tradeMaturityDate": "2026-10-15",
    "tradeType": "SWAP",
    "tradeStatus": "NEW",
    "tradeLegs": [
      {
        "legId": 1,
        "notional": 1000000,
        "currency": "USD",
        "legType": "FIXED",
        "payReceiveFlag": "PAY",
        "rate": 1.5,
        "tradeMaturityDate": "2026-10-15"
      },
      {
        "legId": 2,
        "notional": 1000000,
        "currency": "USD",
        "legType": "FIXED",
        "payReceiveFlag": "RECEIVE",
        "rate": 1.5,
        "tradeMaturityDate": "2026-10-15"
      }
    ]
  }
  ```

  - The corrected payload fixes two issues: DTO property names and the per-leg maturity requirement.

### Test data and seeding issues I encountered (concrete examples)

- `data.sql` seeds a set of books, counterparties, users and trade statuses. Some integration tests were also inserting the same rows unconditionally in `@BeforeEach` setup which produced `DataIntegrityViolationException`.

  - Example failure seen during testing (summary):

    - `DataIntegrityViolationException: Unique index or primary key violation on PUBLIC.APPLICATION_USER(LOGIN_ID NULLS FIRST) VALUES ('simon')`

  - Fix: in test setup I changed insertion code to be idempotent, checking `findByLoginId("simon")` before inserting. Example:

    ```java
    if (applicationUserRepository.findByLoginId("simon").isEmpty()) {
    		ApplicationUser trader = new ApplicationUser();
    		trader.setLoginId("simon");
    		trader.setFirstName("Simon");
    		trader.setActive(true);
    		applicationUserRepository.saveAndFlush(trader);
    }
    ```

- Another test failure class came from duplicate `TradeStatus` rows: tests inserted `NEW` while `data.sql` already had `NEW`.

  - Fix: reuse `tradeStatusRepository.findByTradeStatus("NEW").orElseThrow()` and do not insert duplicates.

### Steps I performed to reproduce, diagnose and fix failing tests

1. Re-ran the focused failing integration test(s) using Maven to reproduce the failure locally. Example command I used:

   ```bash
   mvn -f backend/pom.xml -Dtest=UserPrivilegeIntegrationTest#testTradeEditRoleAllowedPatch test
   ```

2. Read the failing test stack trace and identified two classes of issues:

   - DTO / JSON mismatches (wrong property names such as `startDate` instead of `tradeStartDate`).
   - Database/data seeding collisions (test inserted rows already present in `data.sql`).

3. For DTO mismatches I corrected test payloads to use DTO field names and added the required per-leg `tradeMaturityDate` fields (see corrected JSON above).

4. For seeding collisions I made the test setup idempotent: check `findBy...` before saving, or clear the relevant tables in a transactional helper before inserting.

5. For tests that relied on an existing trade business id, I changed them to create a trade first via the controller (POST), parse the returned JSON to retrieve `tradeId`, and then call PATCH/GET using that business id. This removed fragile hard-coded IDs.

   - Example sequence inside a test:

     1. POST /api/trades with a valid payload.
     2. parse response to `TradeDTO created = mapper.readValue(responseJson, TradeDTO.class);` then `Long createdTradeId = created.getTradeId();`
     3. PATCH /api/trades/{createdTradeId} ...

6. For integration tests using MockMvc I discovered a visibility issue: data seeded inside the test's transaction was not visible to web-layer requests. I used a `TransactionTemplate` to persist seed data in a committed transaction before MockMvc calls.

### Why previously-passing tests started to fail (root causes, explained)

1. Tightening validators and wiring repository-backed validators

   - I introduced stricter repository-backed validation (`EntityStatusValidator`) and made `EntityStatusValidationEngine` a Spring `@Component`. That validator's constructor became strict (requiring repositories). Some older unit tests expected to instantiate a validator with no repositories or a no-arg engine they failed because the stricter constructor now threw or required mocks.

   - Fix: I kept a no-arg constructor path in `TradeValidationEngine` and made the `EntityStatusValidationEngine` optional (null-safe) when Spring DI is not present. I also updated unit tests to mock repositories and inject them where the strict validator was directly tested (see updated `EntityStatusValidatorTest`).

2. Field name mismatches in test payloads

   - Many tests sent payloads with the wrong property names (for example `maturityDate` vs `tradeMaturityDate`, `startDate` vs `tradeStartDate`). With stricter DTO validation those payloads began failing.

   - Fix: updated the test JSON payloads to match DTO properties. This was the most common cause of 400s.

3. Duplicate seed data and unique-constraint failures

   - Tests that inserted reference data without checking for existing rows caused `DataIntegrityViolationException`. Those exceptions often happened early in a test, marked the transaction rollback-only, and later surfaced as the `UnexpectedRollbackException` when the test framework attempted to commit.

   - Fix: made seeding idempotent (check before insert) or reused `data.sql` entries.

4. Transaction visibility between test code and MockMvc

   - MockMvc executes within the application's web layer which may be in a different transaction than the test's. When test setup used the same test transaction without committing, the MockMvc request could not see the seeded rows tests then failed in non-obvious ways.

   - Fix: use `TransactionTemplate` to commit seed data, ensuring it is visible to MockMvc calls.

### Practical notes and examples used during debugging

- When a validation failed inside `TradeService.createTrade(...)` the code now throws a `ResponseStatusException` with a message that concatenates the validation errors. Example message: `Validation failed: Both legs must have a maturity date defined; Book not found`.

- Because the service method is `@Transactional`, that exception causes the transaction to be marked rollback-only. The later `UnexpectedRollbackException` can obscure the original validation message if the global exception handler is too broad; that's why I added comments in `GlobalExceptionHandler` recommending more specific handlers and suggested returning an `errors` array for field-level problems.

### Short checklist for reviewers reproducing my changes locally

1. Start the backend locally (from `backend/`):

```bash
mvn spring-boot:run
```

2. Run a focused integration test after my fixes to verify the create/patch flow:

```bash
mvn -f backend/pom.xml -Dtest=UserPrivilegeIntegrationTest#testTradeEditRoleAllowedPatch test
```

3. If you get `DataIntegrityViolationException` for duplicates, inspect `data.sql` and remove redundant inserts from the test setup or make `findBy...` checks before insert.

4. If a request returns the rollback-only message, paste ~40 lines of server logs (the lines leading up to the rollback stack). I will extract the first "Caused by" exception and propose the exact code fix.

---

ADDED: Propagation CLASS
is a small Spring enum used by @Transactional to tell Spring how the annotated method should behave relative to any existing transaction. Each value controls whether the method should join the caller’s transaction (REQUIRED), always run in a new independent transaction (REQUIRES_NEW), run non‑transactionally and suspend any existing transaction (NOT_SUPPORTED), or one of several other semantics (e.g., SUPPORTS, MANDATORY, NEVER, NESTED). You “need” it whenever you care whether work should be atomic with the caller (default REQUIRED) or isolated/suspended; picking the right propagation avoids problems like the rollback-only UnexpectedRollbackException you saw (e.g., marking dashboard reads NOT_SUPPORTED prevents inner failures from marking a transaction to roll back). It’s safe to use the enum is just configuration but the choices have real consistency implications: REQUIRES_NEW can produce independently committed side‑effects, NOT_SUPPORTED removes transactional guarantees for that call, and NESTED requires a transaction manager that supports savepoints. In short, use propagation deliberately: annotate specific methods (not whole classes) with the propagation that matches the business consistency you need.
