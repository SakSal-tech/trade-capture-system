## RSQL issues: wrong imports, missing operator, empty results and parsing errors

### Problem

RSQL search returned empty results or threw parsing errors for malformed queries. Tests such as AdvanceSearchDashboardIntegrationTest previously failed due to parsing exceptions or incorrect predicate construction.

### Root Cause

- Wrong imports in RSQL visitor: `java.util.function.Predicate` and `java.nio.file.Path` were used instead of `jakarta.persistence.criteria.Predicate` and `jakarta.persistence.criteria.Path`.
- The wildcard operator `=like=` was not registered with the `RSQLParser` by default.
- Malformed RSQL queries were not caught and propagated as 400 client errors; instead an uncaught parser exception produced 500 server errors.

### Solution

- Replace incorrect imports with `jakarta.persistence.criteria.Predicate` and `jakarta.persistence.criteria.Path`, and avoid parameterising non-generic JPA types.
- Register a custom `ComparisonOperator` for `=like=` in parser setup and implement case-insensitive `LIKE` handling in the visitor by converting `*` to `%` and using `criteriaBuilder.lower(... )`.
- Add robust value conversion utility `convertValue(Class<?> type, String value)` that throws `IllegalArgumentException` on invalid conversions.
- Catch `RSQLParserException` at service level and return 400 Bad Request (controller should map null/invalid service result to 400).

### Impact

RSQL endpoint now supports wildcard `=like=` queries and returns client errors (400) for malformed queries rather than internal server errors. Search results match expected records.

---

## TradeRsqlVisitor missing return and raw type warnings

### Problem

Compilation errors such as "missing return statement" and raw-type warnings for `Predicate` were observed when implementing dynamic Specification<Trade> logic.

### Root Cause

- The `toPredicate` method had conditional branches with return statements but no unconditional final return, so the compiler reported a missing return for some code paths.
- The wrong `Predicate` type (java.util.function.Predicate) or raw `Predicate` usage caused raw-type warnings.

### Solution

- Ensure `toPredicate` handles all operators explicitly and, where appropriate, throw an `IllegalArgumentException` for unsupported operators rather than returning `null` (safer and clearer).
- Use `jakarta.persistence.criteria.Predicate` everywhere and remove imports of `java.util.function.Predicate`.

### Impact

Compilation errors were resolved. The visitor fails fast for unsupported operators and returns correct JPA predicates for supported operators.

---

## RSQL parser unknown operator (`=like=`) and token errors

### Problem

Parser exceptions occurred for queries using unknown or malformed operators, leading to test errors and 500 responses in API tests.

### Root Cause

The custom operator `=like=` had not been registered with the parser; malformed token sequences reached the parser without service-level validation.

### Solution

- Add `ComparisonOperator LIKE = new ComparisonOperator("=like=");` and include it in the operators `Set` passed to the `RSQLParser` constructor.
- Add service-level parsing exception handling to return a 400 response for invalid queries.

### Impact

Wildcard searches now work and malformed queries produce expected 400 client errors in tests.

---

## Unit/integration test failures (Mockito type-safety, value conversion and mocking gaps)

### Problem

Several unit tests failed due to Mockito generics/type-safety issues and type conversion errors when tests supplied invalid values for numeric fields.

### Root Cause

- Mockito stubbing of JPA `Path` and generics produced `Type safety` warnings and `thenReturn` mismatches because of the `Path<T>` generic type.
- Tests passed string values that could not convert to the field's Java type (for example 'NotANumber' for a Long field), which threw `IllegalArgumentException` in conversion utilities.

### Solution

- Use raw-type casting in test code with method-level `@SuppressWarnings({"unchecked","rawtypes"})` where necessary to mock `Path` and toReturn raw `Path` objects for tests.
- Harden `convertValue(Class<?> type, String value)` to catch parse exceptions and rethrow a clear `IllegalArgumentException` when conversion fails. Tests then assert the exception is thrown when invalid input is given.

### Impact

Tests compile and properly verify error handling for invalid input. Mocking issues are contained via annotated suppressions in tests that mock low-level JPA constructs.

---

## NullPointerExceptions in validation due to null trade legs

### Problem

Validator tests (for example TradeDateValidatorTest) threw `NullPointerException` because `tradeDTO.getTradeLegs()` returned null and code attempted to iterate over it.

### Root Cause

The validation engine always called leg validation without confirming presence of legs; tests sometimes constructed DTOs without initialising `tradeLegs`.

### Solution

Change validation flow to only call leg validation when `tradeDTO.getTradeLegs()` is not null and not empty. Add guards such as:

```
if (tradeDTO.getTradeLegs() != null && !tradeDTO.getTradeLegs().isEmpty()) {
    validateTradeLeg(tradeDTO.getTradeLegs(), errors);
}
```

### Impact

Date-only validation tests no longer fail due to missing legs. The validation engine is robust to DTOs lacking optional collections.

---

## TradeLegValidator pay/receive flag NPE and logic fix

### Problem

`NullPointerException` and incorrect validation logic when comparing pay/receive flags across legs.

### Root Cause

Code called `.equals()` on potentially null flags and the logic that checked for opposite flags was inverted or incorrect.

### Solution

- Add explicit null checks before `.equals()` comparisons.
- Refactor logic to check for opposite flags using `if (!leg1.getPayReceiveFlag().equals(leg2.getPayReceiveFlag())) { /* flags are opposite */ }` and include null guards.

### Impact

Validator tests now pass and error messages are clearer. Edge cases with missing flags are handled gracefully.

---

## EntityStatusValidator assertion message mismatch

### Problem

Tests failed because expected assertion strings did not match actual validator error messages (for example expected "User must be active" but validator returned "ApplicationUser must be active").

### Root Cause

Tests assumed older, more generic error messages while validator produced more specific messages after refactor.

### Solution

Update test assertions to match the actual messages produced by `EntityStatusValidator` (for example assert for "ApplicationUser must be active"). Where appropriate, normalise message text or add test helper methods to compare messages in a tolerant way.

### Impact

Tests align with current validator outputs and pass. The validator remains precise in its messages which aids debugging.

---

## ApplicationContext failed to load due to data.sql reseed and missing beans

### Problem

ApplicationContext failed during startup because `data.sql` attempted to insert rows that already existed in a persisted H2 file DB; other causes included malformed JPQL and missing validator beans.

### Root Cause

- File-based H2 DB retained previous seed rows, causing duplicate key violations when `data.sql` ran again.
- Some repository JPQL and missing `@Bean` annotations caused context initialisation to fail.

### Solution

- Short-term: remove file-based H2 DB files in developer environment to force a fresh seed.
- Medium-term: scope SQL initialisation to the `test` profile or make `data.sql` idempotent (UPSERT/MERGE statements). Prefer scoping to `test` as quick mitigation.
- Fix malformed JPQL and register missing validator beans so all required beans are available at startup.

### Impact

Application context loads reliably after fixes. Developer and CI environments are more deterministic.

---

## Duplicate seed data and primary-key collisions in tests

### Problem

`DataIntegrityViolationException` occurred because both `data.sql` and tests inserted rows with the same primary keys.

### Root Cause

Seed data existed both in production `data.sql` and in test scripts or programmatic test inserts with hard-coded IDs, causing collisions.

### Solution

- Consolidate seed data and avoid duplication between `src/main/resources/data.sql` and `src/test/resources/data.sql`.
- Use programmatic creation with generated IDs in tests (or use a TestDataFactory that looks up seeded rows and only creates missing entities), avoid hard-coded IDs.

### Impact

No more primary-key collisions; tests become order-independent and stable.

---

## Security / Authorization test failures after re-enabling Spring Security

### Problem

Integration tests began failing with 401/403 responses when real security was re-enabled; some ApplicationContext load errors appeared due to management/actuator security wiring in slices.

### Root Cause

- Tests did not provide authentication (no `@WithMockUser` or MockMvc user), or lacked CSRF tokens for mutating requests.
- Some tests relied on a permissive global test security config introduced earlier, which masked real behaviour.

### Solution

- Create a `TestSecurityConfig` for tests that need an active `SecurityFilterChain` and an in-memory `UserDetailsService`.
- Use `@WithMockUser` for tests that require authentication; add `.with(csrf())` for POST/PUT/DELETE requests in MockMvc tests.
- For controller slice tests where filters are not required, use `@AutoConfigureMockMvc(addFilters = false)` to disable HTTP filters and test the controller logic in isolation.
- Remove global permit-all test security before merging; make test security explicit per-class or per-test.

### Impact

Tests exercise real security semantics and failures (401/403) indicate genuine authorisation issues rather than test configuration problems. The approach allows gradual hardening of tests to reflect production security.

---

## Controller and service-level authorization mismatches and programmatic login issues

### Problem

A logged-in user could sometimes view or mutate resources that should have been blocked. Programmatic login occasionally failed to persist the SecurityContext into the HTTP session, resulting in anonymous requests.

### Root Cause

- Authorization checks existed both as `@PreAuthorize` in controllers and as programmatic checks in services; mismatch in authority naming conventions caused inconsistencies.
- `AuthorizationController.login` did not persist the SecurityContext into the session in some flows.

### Solution

- Implement `DatabaseUserDetailsService` that maps domain users and privileges to Spring Security `GrantedAuthority` objects, emitting both role-prefixed (`ROLE_*`) and plain privilege authorities (`TRADE_VIEW`) as needed.
- Update `hasPrivilege(...)` helper to a deny-by-default implementation that consults both current `Authentication` authorities and DB-driven privileges.
- Ensure programmatic login persists SecurityContext into the HTTP session where session-based flows are required:

```
request.getSession(true).setAttribute(
  HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
  SecurityContextHolder.getContext()
);
```

- Add `ApiExceptionHandler` to map `AccessDeniedException` to compact JSON 403 responses instead of HTML pages.

### Impact

Authorisation became consistent across controller-level annotations and programmatic checks. Programmatic login now behaves correctly when session cookies are retained. Tests and API clients receive consistent JSON 403 payloads.

---

## Integration tests relying on mocks and unexpected JSON response shapes

### Problem

Integration tests failed because mocks returned DTOs without expected fields (for example `tradeId` missing), or controller endpoints returned a plain list whereas tests expected a paginated `{count, content}` structure.

### Root Cause

- Mixing mocks in integration tests produced DTOs that did not match real serialised responses.
- Tests assumed a different JSON contract than the controller returned.

### Solution

- Reduce mocks in integration tests; use real mappers/services and programmatic test fixtures for end-to-end tests.
- Where mocks are required, ensure the mock returns DTOs containing the exact fields asserted by the test.
- Update controller responses to match test expectations (for example wrap results in a map with `content` and `count` if tests expect that shape) or update tests to match the API contract.

### Impact

Integration tests reflect real behaviour and become less brittle. Mocks are used only for controller-unit tests where full end-to-end wiring is not required.

---

## Miscellaneous test fixes and process notes

### Problem

A variety of test failures and flaky behaviour was traced to: missing CSRF tokens, role/authority mismatches, wrongly-named JSON properties (`legs` vs `tradeLegs`), and reliance on global seeded IDs.

### Root Cause

- Tests were written during multiple refactors and assumptions changed. Some used global seeded IDs while others deleted seed rows in `@BeforeEach` causing 404s.
- CSRF and role/authority mismatch issues arose when security was tightened.

### Solution

- Use `@WithMockUser` or `.with(user("...").roles(...))` consistently where authentication is needed.
- Add `.with(csrf())` to mutating MockMvc requests.
- Ensure DTO field names in test JSON match DTOs (`tradeLegs` not `legs`).
- Prefer creating required fixtures per-test rather than relying on global seeds, or centralise test fixtures in a `TestDataFactory` helper.

### Impact

Tests are predictable and isolated, less sensitive to execution order. Security and validation tests now assert the correct layer (validation vs authorisation).

---

# Notes and next actions

- Add Vitest tests for `SettlementTextArea` to `frontend/src/__tests__` (priority: medium).
- Add `vite-plugin-visualizer` to the frontend dev-dependencies and run a build to quantify chunk contributions (priority: low-medium).
- Replace any remaining permissive `hasPrivilege(...)` stubs with a DB-driven deny-by-default implementation (priority: high for security correctness).

---
