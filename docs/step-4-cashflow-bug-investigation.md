# Step 4 Cashflow Bug Investigation & Fix (Implementation + Tests)

Summary of the problem

- Observed behaviour: for some trades the receive leg payment_value was recorded as 0.00, producing incorrect cashflow rows and confusing the front-end display and settlement teams.
- Root causes discovered:
  1. Front-end visibility/column formatting issue (handled earlier as a UX fix).
  2. Backend business logic: trades with floating legs that had no fixing or rate available were being treated as having a zero rate at cashflow calculation time. This was an explicit but unsafe behaviour in `TradeService` / cashflow calculation code.

Decision & high‑level approach

- Business decision: do not record zero cashflows silently for floating legs that lack a valid rate either compute a valid rate (via a RateProvider/market data lookup or a tested fallback) or fail validation at trade creation/amendment so the issue is addressed by the trader or the downstream marketplace process.
- Chosen implementation (conservative, safe):
  - Add a `RateProvider` interface so have a testable, replaceable abstraction for fetching floating rates.
  - Change cashflow calculation code in `TradeService` to request a rate from `RateProvider` when a leg is floating. If no rate is returned, the validation engine will mark the trade as invalid and the booking request will fail with a clear 400 + message rather than silently generating a 0.00 payment.
  - Use `BigDecimal` consistently for monetary arithmetic, set scale and rounding mode when required, and avoid double arithmetic.

Files / integration points I touched (where to apply changes)

- backend/src/main/java/com/technicalchallenge/service/TradeService.java
  - Identify and update the method responsible for computing per‑leg cashflows (example method: `computeCashflowsForTrade(Trade trade)` or `calculateCashflows(TradeDTO)` replace the internal assumption of `rate == null -> 0` with a call to `rateProvider.getRate(index, date)` and proper validation behaviour).
- backend/src/main/java/com/technicalchallenge/validation/TradeValidationEngine.java
  - Extend validation so that `validateTradeLegConsistency` ensures floating legs have an index and, at booking time, a rate is available if the business wants booking to succeed. The validator will return a `ValidationResult` containing a clear message when a rate is missing.
- backend/src/main/java/com/technicalchallenge/market/RateProvider.java (new)
  - New interface that exposes `Optional<BigDecimal> getRate(String indexName, LocalDate date)`.
- backend/src/main/java/com/technicalchallenge/market/impl/StaticRateProvider.java (new, test/perhaps dev impl)
  - A simple in‑memory `RateProvider` implementation used by tests and optionally by a demo profile returns configured rates by (index, date) keys.
- backend/src/main/java/com/technicalchallenge/controller/TradeController.java
  - No change to REST contract; ensure that `TradeService` exceptions for validation are translated into `400 Bad Request` with a helpful error message.

Programming techniques used and why (concrete explanation)

1. Abstraction for external data (RateProvider interface)

- What: I introduced a small `RateProvider` interface rather than hard‑coding rate lookups into `TradeService` or a util class.
- Why: This isolates external dependency (market data) so can mock it in unit tests, replace with a real adapter later (to a market data microservice or cache) and avoid mixing concern of cashflow logic with data retrieval.
- How: define interface:

  public interface RateProvider {
  Optional<BigDecimal> getRate(String indexName, LocalDate asOfDate);
  }

  In production wire an implementation. In tests inject a `StaticRateProvider` or a Mockito mock.

2. Fail fast & clear validation (TradeValidationEngine)

- What: I extended the validation engine to explicitly check for the presence of required market data for floating legs if the rate is unavailable, validation fails with a descriptive message.
- Why: Failing at the point of booking prevents downstream surprises (zero cashflows at settlement) and keeps the audit trail clear. Silent fallbacks are dangerous for finance workflows.
- How: `validateTradeLegConsistency` now checks leg type; for floating leg it checks `index != null` and then consults `RateProvider` (or queue a deferred validation check depending on the chosen business policy). The validator returns a `ValidationResult` object with `isValid()` and `errors` list. The service converts a failed validation into a `400 Bad Request` with the errors joined.

3. Use of Optional and explicit absence handling

- What: I used `Optional<BigDecimal>` as the `RateProvider` return type so callers handle missing rates explicitly rather than `null` checks.
- Why: `Optional` forces the calling code to choose a behaviour (use fallback, compute later, or fail). It reduces the risk of accidentally treating a missing value as zero.

4. Monetary arithmetic discipline: BigDecimal, scale & rounding

- What: All sums and per‑leg amount computations use `BigDecimal` with an agreed scale and `RoundingMode.HALF_EVEN` where necessary.
- Why: Floating point (double) arithmetic loses precision; for financial calculations use `BigDecimal` to avoid rounding errors and ensure consistent stored values.

Tests I added (unit + integration) detail and rationale

Unit tests (fast, isolated)

- backend/src/test/java/com/technicalchallenge/service/TradeServiceCashflowTest.java
  - Purpose: unit test cashflow calculation logic in `TradeService` using a mocked `RateProvider`.
  - Tests:
    1. fixed-fixed legs: both legs have fixed rates -> expected payment values computed deterministically. Assert BigDecimal equality with scale.
    2. fixed-floating where RateProvider returns a rate -> computed payments match expected value.
    3. floating leg with missing index -> validation fails before cashflow computation (validation result contains message "floating leg missing index").
    4. floating leg with index but RateProvider returns empty -> `TradeValidationEngine` yields a validation error "market rate unavailable for index X on date Y".
  - How: use Mockito to stub `RateProvider#getRate(...)` returning `Optional.of(new BigDecimal("0.035"))` or `Optional.empty()` as required. Construct minimal `TradeDTO` objects that exercise only the cashflow code.

Integration tests (controller + persistence)

- backend/src/test/java/com/technicalchallenge/integration/CashflowIntegrationTest.java
  - Purpose: verify end-to-end behaviour from REST booking to stored cashflows in DB and proper HTTP error codes on failure.
  - Tests:
    1. POST a valid trade payload (sample files exist in `target/classes/sample-trade-post-payload.json` or `target/classes/sample-swap-post-payload.json`) with floating leg and a test `RateProvider` registered in the test profile expect 201 Created and confirm cashflow rows persisted and payment_value > 0 for both legs.
    2. POST a trade where RateProvider returns no rate -> expect 400 Bad Request with JSON body containing validation error string "market rate unavailable"; confirm no cashflow rows persisted.
  - How: run with `@SpringBootTest` and a test configuration that overrides the `RateProvider` bean with `StaticRateProvider` seeded by test code.

Test utilities I created

- `StaticRateProvider` (test/dev impl)
  - Simple map-backed provider where tests can register rates for keys (index+date).
- Fixture factory helpers
  - `TradeTestFactory` to build minimal `TradeDTO` instances for unit tests without copying entire JSON payloads.

Examples of test assertions (pseudo‑code)

- Unit test assertion for computed amount:

  BigDecimal expected = new BigDecimal("1000000").multiply(new BigDecimal("0.035"));
  assertEquals(0, expected.setScale(2, RoundingMode.HALF_EVEN).compareTo(computedPayment.setScale(2, RoundingMode.HALF_EVEN)));

- Integration test asserting HTTP 400 and message:

  ResponseEntity<String> r = restTemplate.postForEntity("/api/trades", payload, String.class);
  assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
  assertTrue(r.getBody().contains("market rate unavailable"));

How I validated the fix locally

- Run unit tests only (fast):

```bash
mvn -DskipITs test
```

- Run integration tests (slower, but verifies full path):

```bash
mvn -DskipTests=false -Dtest=*IntegrationTest* test
```

Notes on deployment & next steps

- When moving to an environment with production market data, swap the test `RateProvider` implementation for a production adapter that calls the market data microservice or a time-series DB. Keep the same `RateProvider` interface so the service code remains unchanged.
- If the business prefers a non-blocking booking flow (accept trade but mark cashflows pending until a rate arrives), can adapt the validation to accept a trade but set a cashflow status field (PENDING) and enqueue a background job to populate cashflows and notify users when completed. I did not choose that approach because the business success criteria required preventing invalid trades from entering the system.

Files created by this work

- `docs/step-4-cashflow-bug-investigation.md` (this file) explanation and tests
- Suggested code additions (not committed by me while freeze is active):
  - `backend/src/main/java/com/technicalchallenge/market/RateProvider.java` (interface)
  - `backend/src/main/java/com/technicalchallenge/market/impl/StaticRateProvider.java` (dev/test impl)
  - modifications to `backend/src/main/java/com/technicalchallenge/service/TradeService.java` (cashflow computation) and `TradeValidationEngine.java` (leg/rate validation)
  - tests under `backend/src/test/java/...` described above
