# Developer Progress Log October 2025

## Saturday, Oct 11

**Focus:** Project structure and Date Validation foundation

### Completed

- Set up core validation framework:
  - TradeValidationResult
  - TradeValidationEngine
  - TradeDateValidator (basic version implemented)
- Wrote and ran first failing test (failWhenMaturityBeforeStartDate)
- Connected validation flow with TradeValidationServiceTest
- Confirmed integration structure working (TDD RED phase)
- Git operations:
  - Created new branch feat/comprehensive-trade-validation-engine
  - Committed initial failing test and validation classes
- Planned structure for future validators (User, Leg, Entity)

### Learned

- Role of ChronoUnit.DAYS.between() and why it returns a long type
- How to structure reusable validators for maintainability
- Purpose of separating TradeValidationResult and TradeValidationEngine
- Importance of TDD phases (Red → Green → Refactor)

### Challenges

- Took me time to decide the structure of classes and methods
- Needed clarity on exception handling (decided to use result aggregation pattern instead of throwing)
- Found writing tests before logic methods harder than writing the logic then tests
- Progress slower due to balancing with home tasks

### Tomorrow (Sunday, Oct 12)

**Goal:** Complete all Date Validation Rule tests and logic (GREEN phase)

#### Tasks

1. Write new failing tests:
   - failWhenStartBeforeTradeDate()
   - failWhenTradeDateOlderThan30Days()
2. Run tests and confirm RED
3. Implement logic in TradeDateValidator
   - Use ChronoUnit.DAYS for 30-day rule
   - Ensure clear error messages
4. Run tests and confirm all GREEN
5. Commit messages:
   - "test(validation): add failing tests for start date and 30-day trade date rules"
   - "fix(validation): implement complete date validation logic"
6. Optional: test through TradeValidationEngine

## Sunday 12 October 2025

### Focus

Enhancement 2: Comprehensive Trade Validation Engine  
Expanded test coverage and implemented multiple validation layers.

### Tasks Completed

- Completed all **Date Validation Rules**:
  - Wrote and passed tests for:
    - “Start date cannot be before trade date.”
    - “Trade date cannot be more than 30 days in the past.”
  - Implemented corresponding logic in `TradeDateValidator`.
  - All date validation tests now passing.
- Implemented **User Privilege Enforcement** rules:
  - Added `UserPrivilegeValidator` class handling user-type-based permissions.
  - Wrote comprehensive unit test `UserPrivilegeValidatorTest` covering:
    - TRADER: can perform CREATE, AMEND, TERMINATE, CANCEL.
    - SALES: limited to CREATE and AMEND.
    - MIDDLE_OFFICE: can AMEND and VIEW only.
    - SUPPORT: can VIEW only.
  - All privilege validation tests passed successfully.
- Started **Cross-Leg Business Rules**:
  - Added `maturityDate` to `TradeLegDTO`.
  - Created `TradeLegValidator` and corresponding test for rule:
    - “Both legs must have identical maturity dates.”
  - Test passed on first run.
- Created new learning reference file `validation-learning-links.md` covering:
  - ValidationResult and ValidationEngine design patterns.
  - XMLUnit `ValidationResult` inspiration.
  - Articles on Chain of Responsibility and Rule Engine design.

### Key Learnings

- Using `ValidationResult` allows collecting multiple business rule errors without exceptions.
- Separate validators (date, privilege, leg) keep validation logic modular and maintainable.
- Learned difference between user roles and validation engine orchestration.
- Reinforced understanding of TDD writing tests first made implementation clearer and faster.

### Next Steps

- Write and implement tests for remaining Cross-Leg Business Rules:
  - Opposite pay/receive flags.
  - Floating legs must have an index.
  - Fixed legs must have a valid rate.
- Begin planning for Entity Status Validation:
  - Verify user, book, and counterparty activity checks.
- Start drafting internal documentation on validation design approach.

## Monday, Oct 13

**Focus:** Finalising comprehensive trade validation and planning dashboard enhancement

### Completed

- Finished all remaining Cross-Leg Business Rule tests and logic:
  - Opposite pay/receive flags
  - Floating legs must have an index
  - Fixed legs must have a valid rate
- Refactored validation architecture for separation of concerns:
  - Moved entity status and reference validation to new `EntityStatusValidationEngine`
  - Ensured `TradeValidationEngine` only handles business rules
- Implemented and tested Entity Status Validation:
  - User, book, and counterparty must be active in the system
  - All reference data must exist and be valid
- Updated and fixed all failing tests:
  - Aligned test assertions with actual error messages
  - Confirmed all validation tests pass
- Committed and pushed changes to remote branch
- Documented error/fix details in `Development-Errors-and-fixes.md`
- Confirmed correct usage of `TradeValidationResult` and validation engines in all test classes

### Key Learnings

- Modular validation engines make codebase easier to maintain and extend
- Centralised result object (`TradeValidationResult`) simplifies error handling and test assertions
- Refactoring for SRP and separation of concerns improves clarity and scalability
- Systematic test review ensures robust coverage and prevents regression

### Challenges

- Needed to update test assertions to match validator error messages
- Refactoring required careful coordination between engine and validator classes
- Ensured all validation logic is covered by tests before committing

### Tomorrow (Tuesday, Oct 14) Plan

**Goal:** Start Enhancement 3: Trader Dashboard and Blotter System

#### Tasks

1. Design and implement REST endpoints for trader dashboard:
   - `@GetMapping("/my-trades")` Trader's personal trades
   - `@GetMapping("/book/{id}/trades")` Book-level trade aggregation
   - `@GetMapping("/summary")` Trade portfolio summaries
   - `@GetMapping("/daily-summary")` Daily trading statistics
2. Create new DTOs:
   - `TradeSummaryDTO` (trade status counts, notional by currency, breakdowns, risk summaries)
   - `DailySummaryDTO` (today's trade count, user metrics, book activity, historical comparison)
3. Implement business requirements:
   - Authenticated user sees only their relevant trades
   - Real-time summary calculations
   - Support for multiple currency exposures
   - Historical comparison capabilities
4. Write unit and integration tests for new endpoints and DTOs
5. Document dashboard design and implementation approach

# Developer Progress Log October 2025

## Tuesday, Oct 14, 2025

**Focus:** Enhancement 3 Trader Dashboard and Blotter System

### Completed

- Fully implemented **Trader Dashboard and Blotter System** to meet the business requirement:  
  _“As a trader, I need personalised dashboard views and summary statistics so that I can monitor my positions and make informed trading decisions.”_

#### Key Deliverables

- Implemented **new REST endpoints** in `TradeDashboardService`:

  - `GET /my-trades` Returns authenticated trader’s personal trades using Spring Security context.
  - `GET /book/{id}/trades` Provides book-level trade aggregation with privilege validation.
  - `GET /summary` Generates real-time portfolio summaries (trades by status, notional by currency, and counterparty breakdowns).
  - `GET /daily-summary` Produces daily trade metrics with historical comparisons (today vs yesterday).

- Created **new DTOs** to structure API responses:

  - `TradeSummaryDTO`:
    - Trade count by status (e.g., NEW, CANCELLED)
    - Notional totals by currency (multi-currency exposure)
    - Breakdown by trade type and counterparty
    - Placeholder for risk exposure summaries (e.g., delta, vega)
  - `DailySummaryDTO`:
    - Today’s trade count and total notional
    - User performance metrics
    - Book-level trade summaries (trade count, notional)
    - Historical comparisons between today and yesterday

- Integrated **UserPrivilegeValidationEngine** to enforce role-based access control:
- Validates TRADER, SALES, MIDDLE_OFFICE, and SUPPORT privileges before any dashboard data is shown.

  - Throws `AccessDeniedException (403)` for unauthorised access attempts.

- Implemented **Spring Security integration**:

  - Added helper methods to automatically resolve user identity and role from the security context:
    - `resolveCurrentTraderId()` → identifies the logged-in trader
    - `resolveCurrentUserRole()` → determines user role (TRADER, SALES, etc.)
  - Ensures authenticated users only see **their own trades**.

- Added **real-time summary and aggregation logic**:

  - Implemented in-memory aggregation using Java Streams and `HashMap.merge()` for performance.
  - Supports **multi-currency portfolios** by summing notional values across trade legs.
  - Added historical comparison capability in `getDailySummary()` for daily performance insights.

- Improved **code clarity and maintainability**:

  - Replaced shortened variables (`t`, `ccy`, `pr`) with meaningful names (`tradeDto`, `currency`, `privilegeResult`).
  - Preserved all original comments and added detailed explanations for new logic.

- Updated **`TradeDashboardServiceTest`**:
  - Added tests for all new endpoints and business logic.
  - Mocked privilege validation to simulate user roles.
  - Verified aggregation results, currency totals, and historical comparisons.
  - Confirmed all test cases pass successfully.

### Key Learnings

- Learned how to integrate **business analytics, privilege validation, and security context** in one cohesive service.
- Gained deeper understanding of **real-time data aggregation** using Java Streams.
- Improved understanding of enforcing **role-based data visibility** and user privilege boundaries.
- Reinforced good naming conventions and documentation practices for complex business logic.

### Challenges

- Integrating privilege validation with existing search logic required careful refactoring to avoid regressions.
- Multi-currency aggregation across trade legs introduced complexity and required multiple null checks.
- Ensuring consistent results for today/yesterday comparisons in tests required dynamic date handling.
- Test configuration for privilege simulation took longer than expected.

### Tomorrow (Wednesday, Oct 15, 2025) Plan

**Goal:** Integration Testing + Begin Step 4 (Bug Investigation and Fix)

#### Tasks

1. **Integration Testing:**

   - Write integration tests for all new dashboard endpoints (`/my-trades`, `/book/{id}/trades`, `/summary`, `/daily-summary`).
   - Verify role-based privilege enforcement, trade filtering, and real-time aggregation.
   - Confirm authenticated user context resolves correctly within test setup.

2. **Step 4: Bug Investigation and Fix (Required)**

   - **Bug ID:** TRD-2025-001
   - **Severity:** High
   - **Symptoms:**
     - Fixed-leg cashflows showing ~100× larger than expected.
     - $10M trade with 3.5% rate generating ~$875,000 instead of ~$87,500.
     - Precision inconsistencies in monetary values.

   **Tasks:**

   - Investigate `calculateCashflowValue()` in `TradeService.java`.
   - Review formulas, data types, and rate conversions for potential precision errors.
   - Identify root cause (likely missing rate-to-decimal conversion and use of `double` instead of `BigDecimal`).
   - Produce a detailed **Root Cause Analysis** report including:
     - Executive Summary
     - Technical Investigation
     - Root Cause Description
     - Proposed Solution and Alternatives
   - Implement bug fix:
     - Convert percentage rates correctly (`3.5% → 0.035`).
     - Replace `double` with `BigDecimal` for monetary accuracy.
   - Write regression tests to validate correct cashflow results and prevent recurrence.

All possible user privilege validation integration tests:
adeController Endpoints:

GET /api/trades getAllTrades
GET /api/trades/{id} getTradeById
POST /api/trades createTrade
PATCH /api/trades/{id} updateTrade (partial)
PUT /api/trades/{id} updateTrade (full)
DELETE /api/trades/{id} deleteTrade
POST /api/trades/{id}/terminate terminateTrade
POST /api/trades/{id}/cancel cancelTrade
TradeDashboardController Endpoints:

GET /api/dashboard/filter filterTrades
GET /api/dashboard/search searchTrades
GET /api/dashboard/rsql searchTradesRsql
GET /api/dashboard/my-trades getMyTrades
GET /api/dashboard/book/{bookId}/trades getTradesByBook
GET /api/dashboard/summary getTradeSummary
GET /api/dashboard/daily-summary getDailySummary
Next step: I will recommend specific privilege validation integration tests for each endpoint, specifying expected roles and HTTP status codes.
To fully resolve request, here is a privilege validation integration test plan for all endpoints:

TradeController:

GET /api/trades Test: Only users with "TRADE_VIEW" or "ADMIN" role can access; others get 403.
GET /api/trades/{id} Test: Only "TRADE_VIEW" or "ADMIN" role; others get 403.
POST /api/trades Test: Only "TRADE_CREATE" or "ADMIN" role; others get 403.
PATCH /api/trades/{id} Test: Only "TRADE_EDIT" or "ADMIN" role; others get 403.
PUT /api/trades/{id} Test: Only "TRADE_EDIT" or "ADMIN" role; others get 403.
DELETE /api/trades/{id} Test: Only "TRADE_DELETE" or "ADMIN" role; others get 403.
POST /api/trades/{id}/terminate Test: Only "TRADE_TERMINATE" or "ADMIN" role; others get 403.
POST /api/trades/{id}/cancel Test: Only "TRADE_CANCEL" or "ADMIN" role; others get 403.
TradeDashboardController:

GET /api/dashboard/filter Test: Only "TRADE_VIEW" or "ADMIN" role; others get 403.
GET /api/dashboard/search Test: Only "TRADE_VIEW" or "ADMIN" role; others get 403.
GET /api/dashboard/rsql Test: Only "TRADE_VIEW" or "ADMIN" role; others get 403.
GET /api/dashboard/my-trades Test: Only "TRADER" or "ADMIN" role; others get 403.
GET /api/dashboard/book/{bookId}/trades Test: Only "BOOK_VIEW" or "ADMIN" role; others get 403.
GET /api/dashboard/summary Test: Only "TRADER" or "ADMIN" role; others get 403.
GET /api/dashboard/daily-summary Test: Only "TRADER" or "ADMIN" role; others get 403.
For each endpoint, write integration tests using @WithMockUser for:

Allowed roles (expect 200/204)
Disallowed roles (expect 403)

# Developer Progress Log 18 to 24 October 2025

These entries cover day by day work I did on security and privilege issues across the controllers, service layer and configuration. I write them in full sentences to capture my thinking, the technical changes I made, and the tests I added so I can revisit decisions later.

---

## Saturday 18 October 2025

Focus: Scoped access and the first pass at controller level authorisation

### Completed

- Reviewed all endpoints on TradeController and TradeDashboardController to document which endpoints should be restricted by role and by resource ownership.
- Implemented controller-level security checks for a first pass: added method-level annotations and explicit checks inside endpoints where resource ownership is required. Examples:
  - GET /api/trades/{id} now checks that the authenticated user has either global view privileges or is the owner of the trade before returning details.
  - GET /api/dashboard/my-trades returns only trades belonging to the authenticated trader by scoping the query to the current user id.
- Wrote unit tests for controller behaviour using @WithMockUser to assert that authorised roles receive 200 and unauthorised roles receive 403 for several endpoints.
- Created the initial UserPrivilegeIntegrationTest class and added placeholder test methods that assert correct HTTP status codes for obvious cases.

### Learned

- The practical difference between role-based annotations and resource-level checks. Annotations like @PreAuthorize are concise for role checks but do not capture ownership rules, so I combined both approaches.
- How to extract the current user identity safely from the SecurityContextHolder and pass it down to repositories and services as a filter.
- The value of writing small, focused integration tests that assert status codes as a first line of defence for security regressions.

### Challenges

- I initially relied too much on controller annotations and missed a case where a trader could call the endpoint for another trader's trade id. I had to add explicit ownership checks at the start of those controller methods.
- Tests ran quickly but I found mocking authority sets across many test cases resulted in some duplicated setup code. I made a helper method to build the mock authority set for reuse.
- Early on I experienced a number of test failures caused by mock-user mismatches and incomplete test security configuration. Tests that used @WithMockUser were passing locally but failing in the full integration context because TestSecurityConfig was not imported or because the DatabaseUserDetailsService behaviour differed from the mocked authorities. This required careful alignment between real user details and the mocked setups.
- Cross-cutting with the mock-user issues, CSRF protection caused several POST/PATCH requests in integration tests to be blocked with 403 unless I explicitly supplied CSRF tokens or disabled CSRF for the test profile. I documented a consistent approach in TestSecurityConfig so tests could opt into CSRF behaviour deterministically.

## Sunday 19 October 2025

Focus: Service layer enforcement and query scoping

### Completed

- Moved ownership enforcement into the service layer where most business rules live, so controllers only orchestrate and services decide access. Changes included:
  - TradeDashboardService.getTradesForBook now requires a privilege check and filters results by book membership and user role.
  - TradeService.getTradeById performs a final check to ensure the caller can access the trade before returning it.
- Updated repository queries to accept an optional ownerId parameter so queries can be scoped in a single database call rather than filter results in memory.
- Added tests in TradeServiceTest to assert that when the ownerId parameter is provided, repository calls return only owned trades and that unauthorised access results in AccessDeniedException bubbling back to be translated into 403.
- Extended UserPrivilegeIntegrationTest with an explicit test: traderCannotAccessAnotherTradersTrade_shouldReturn403.

### Learned

- Placing access checks in services reduces duplication and reduces the risk of forgetting ownership checks in controllers or other callers.
- How to design repository methods that support both global and scoped queries; for example findByIdAndOwnerId and findById for admin-level access.
- The difference between throwing AccessDeniedException inside a service and allowing Spring Security to convert exceptions to 403 responses later in the filter chain.

### Challenges

- I had to refactor some DTO mapping logic because scoping earlier in the call chain removed the need to fetch some linked entities. I made the mappers resilient to partial data to keep integration tests simpler.
- Ensuring consistent error messages across service and controller layers required standardising on a small set of exceptions and mapping them in a ControllerAdvice.
- A recurring cause of failing tests was the difference between @WithMockUser and the database-backed users used by @WithUserDetails. I needed to normalise privilege naming and ensure test users seeded into the test database used the same authority naming as the application expects. Until this was fixed, tests would show unexpected 403 responses.

## Monday 20 October 2025

Focus: Security configuration and creating database-backed UserDetails

### Completed

- Implemented DatabaseUserDetailsService that loads user credentials and granted authorities from the users and privileges tables.
- Reworked SecurityConfig to use the DatabaseUserDetailsService as the primary UserDetailsService and configured the password encoder, session and CSRF basics for tests and local development.
- Created TestSecurityConfig used for integration tests so I can inject mock users or use an in-memory authentication provider when I want simpler setups.
- Wrote integration tests using @WithUserDetails for a subset of endpoints to verify that the database-backed user details are loaded correctly and roles are interpreted as expected.

### Learned

- Why using a database-backed UserDetailsService is important for production parity in integration tests and how it differs from the @WithMockUser approach used in fast unit tests.
- How authorities and privileges map through UserDetails to Spring Security expressions; specifically how to represent complex privilege sets (for example VIEW + EDIT) as GrantedAuthority strings so @PreAuthorize("hasAuthority('TRADE_EDIT')") works.
- How to isolate security configuration for tests by providing a test profile configuration class and using @Import in test classes to avoid conflicting beans.

### Challenges

- I ran into a mismatch between the privileges stored in the database and the GrantedAuthority strings expected by the code; I added a small mapping layer in DatabaseUserDetailsService to normalise database privileges into the authority names used across the application.
- Ensuring password encoding compatibility for test users required seeding the test database with encoded passwords; I added a TestDataBuilder to create those records.

---

## Monday, Oct 27, 2025

**Focus:** Safe startup remediation stop runtime reseed of file-backed H2

### Summary

- Problem: `src/main/resources/data.sql` contains many non-idempotent INSERT statements using fixed IDs. With `spring.sql.init.mode=always` enabled in the runtime profile the application attempted to re-apply the seed against the file-backed H2 DB (`./data/tradingdb.mv.db`) on startup which caused unique-key violations and prevented the ApplicationContext from refreshing.

- Immediate remediation applied (27/10/2025): the runtime profile now sets `spring.sql.init.mode=never` so the `data.sql` file is not re-applied on local developer startup. The test profile retains SQL initialization for the in-memory DB so tests continue to seed deterministically.

### Rationale

- This is a low-risk, fast fix that avoids data loss (no deletion of `.mv.db`) and prevents accidental reseed failures during normal development. It buys time to make the seed idempotent or adopt a migration tool.

### Recommended next steps

1. Convert non-idempotent `INSERT` statements to idempotent `MERGE INTO ... KEY(id)` or use database `UPSERT` semantics to make `data.sql` safe to re-run.
2. Consider adopting Flyway or Liquibase for schema and data migrations to get reliable, repeatable boots across environments.
3. Add a small regression test that starts the app with an existing file-backed H2 DB and asserts startup succeeds (no SQL-init exceptions).

Recorded by: developer automation merged and logged after feature branch merge.

- Swagger and the API explorer were intermittently inaccessible during development because the tightened security configuration blocked the swagger UI and the OpenAPI endpoints. I solved this by explicitly permitting the swagger UI endpoints under the local/test profile and ensuring health and swagger paths are only open in non-production profiles.

### Concept: Ownership and access control enforcement (service level)

### Completed

- Added defence-in-depth ownership checks at service layer in `AdditionalInfoService` to ensure a trader cannot read or modify another trader's settlement instructions unless authorised. The service resolves the authenticated principal via `SecurityContextHolder.getContext().getAuthentication().getName()` and compares against the trade owner (example: trade id 200001 maps to `trade` row id 2000 with `trader_user_id=1002` for login `simon`).
- Kept controller-level `@PreAuthorize` annotations but moved final access decisions to the service so callers outside controllers also get consistent enforcement.

### Challenges

- Tests and local runs used different DB profiles initially which made reproducing the ownership failures harder (some runs used in-memory DBs so behaviour differed from the persisted dev DB).

### Learned

- Service-level checks remove duplication and reduce the risk of missing ownership checks when endpoints or internal callers change. Relying solely on controller annotations is insufficient for resource ownership rules.

---

### Concept: Audit records with authenticated principal (no client-supplied actor)

### Completed

- Ensured `AdditionalInfoAudit` records are created by the server and the `changedBy` column is set from the authenticated principal (obtained from the SecurityContext) rather than from any client-supplied header or payload.
- Audit write occurs immediately after the `AdditionalInfo` save and is part of the same transaction. The audit row records the `additional_info` FK, timestamp, change type (CREATE/UPDATE) and `changedBy` (e.g. 'simon').

### Challenges

- Needed to normalise how the authenticated username maps to the seeded users in `data.sql` (for example `simon` -> application_user id 1002, `alice` -> id 1000) so tests and manual checks assert the expected `changedBy` values.

### Learned

- Using the server-side principal removes the risk of forged actor data in audit records and keeps audit trails trustworthy.

---

### Concept: Test isolation and DB configuration changes

### Completed

- Changed test profile configuration so integration tests use an in-memory H2 database and `create-drop` lifecycle. File changed: `backend/src/test/resources/application-test.properties` (set `jdbc:h2:mem:testdb` and `spring.jpa.hibernate.ddl-auto=create-drop`).
- Annotated the affected integration test (`AdditionalInfoIntegrationTest`) with `@ActiveProfiles("test")` so the test picks up the in-memory configuration.
- Kept `src/main/resources/application.properties` pointed at the file H2 DB for dev, with `spring.jpa.hibernate.ddl-auto=update` so local data persists between runs when desired.

### Challenges

- Running tests against the same file-backed DB caused duplicate-key failures because `data.sql` re-applied seed rows on an already-populated DB. That led to ApplicationContext load failures for tests that expected a clean schema.

### Learned

- Tests must be isolated and deterministic. Using an in-memory DB with `create-drop` ensures fresh schema and avoids `data.sql` conflicts with a persistent dev DB.

---

### Concept: Diagnostics, H2 file deletion and final fix

### Completed

- Investigated startup logs and reproduced the failure locally using `mvn -DskipTests -Dspring-boot.run.fork=false -e spring-boot:run` to force visible error stack traces.
- Observed `ScriptStatementFailedException` caused by `data.sql` INSERT statements raising H2 error 23505. The critical message: `Unique index or primary key violation` on `desk(id)`.
- Removed the file H2 database files (`backend/data/tradingdb.mv.db` and `backend/data/tradingdb.trace.db`) to allow Spring to create a fresh DB and apply `data.sql` successfully.
- After deletion, the application initialised cleanly, seeded the database, started Tomcat and accepted requests. A sample PUT by `simon` (login `simon`, application_user id 1002) on trade `trade_id=200001` (trade row id 2000) produced a new `additional_info` entry and a corresponding `additional_info_audit` row. When Ashley (login `ashley`, application_user id 1003) and Alice (login `alice`, application_user id 1000) previously logged into the running system before the fix they could not find the expected audit entries; following the reseed and clean startup both Ashley and Alice can now retrieve the audit records (for example, the settlement change by `simon` for trade `200001` is visible in `additional_info_audit.changedBy='simon'`).

### Challenges

- Deleting DB files is destructive for dev data; action was taken only after confirming it was acceptable for immediate debugging. Long-term solution requires making seed scripts idempotent or changing initialisation strategy.

### Learned

- A transient fix (deleting DB files) proves the root cause and restores a working environment quickly, but permanent remediation should avoid destructive steps for routine debugging.

---

### Concept: Transactional guarantees and verification

### Completed

- Confirmed the upsert + audit logic runs inside one transactional boundary so both `additional_info` and `additional_info_audit` commits together. This protects the audit from showing a change that never committed or leaving data without a matching audit.
- Verified via integration test and Hibernate SQL logs that both INSERT (or UPDATE) statements and the audit INSERT appear in the same transaction and commit.

### Challenges

- Ensuring test coverage covers both happy and error paths; added assertions that audit rows exist for CREATE and for UPDATE flows.

### Learned

- Transactions across multiple repository saves provide atomicity for domain-level changes and their audit trails; tests should assert both data and audit persistence.

---

### Files changed (high level)

- `backend/src/main/java/com/technicalchallenge/service/AdditionalInfoService.java` added ownership checks, upsert logic and audit write.
- `backend/src/main/java/com/technicalchallenge/controller/TradeSettlementController.java` delegated GET/PUT to service methods and clarified behaviour.
- `backend/src/main/resources/application.properties` left file-based H2 + `spring.sql.init.mode=always` (diagnosed as part of the root cause).
- `backend/src/test/resources/application-test.properties` switched to in-memory H2 and `create-drop` for tests.
- `backend/src/test/java/com/technicalchallenge/controller/AdditionalInfoIntegrationTest.java` added `@ActiveProfiles("test")` to ensure test runs against in-memory DB.

---

### Immediate next steps (recommended)

1. Make `data.sql` idempotent (recommended): replace plain INSERTs with H2 `MERGE INTO ... KEY(id)` or equivalent so re-running initialisation does not fail on duplicates.
2. Alternatively, restrict `spring.sql.init.mode` to the test profile only and set `spring.sql.init.mode=never` for `application.properties` so dev persisted DB is not re-seeded automatically.
3. Add a short DEVLOG entry documenting the destructive DB deletion and note the preferred remediation chosen (idempotent seeds or move to test-only initialisation).

---

### Closing notes

Today’s work fixed the immediate availability problem: the service now persists settlement instructions and writes verifiable audit records with the authenticated principal (examples: `simon` changed settlement for trade `200001` → `additional_info_audit.changedBy='simon'`) and admin (`alice`) can view the audit history. The underlying root cause was non-idempotent SQL initialisation against a persistent H2 file DB; permanent fixes have been proposed above.

## Tuesday 21 October 2025

Focus: Diagnosing repeated 403s and 200s where they should not occur, and fixing tests

### Completed

- Investigated several failing integration tests that previously returned 403 when they should have returned 200 and vice versa. I triaged the cases into two classes:
  - failures caused by misconfigured test security context (test wiring did not load TestSecurityConfig), and
  - failures caused by missing or incorrect privilege checks in services.
- Corrected TestConfig imports and adjusted test application context so the test beans were consistent across integration tests.
- Updated UserPrivilegeIntegrationTest with clearer test names and added assertions that verify not only HTTP status codes, but also that the returned payload does not contain trades belonging to other users.
- Added summary tests that verify common happy path scenarios across roles. Example test titles added:
  - UserPrivilegeIntegrationTest shouldAllowTraderToGetOwnTrades
  - UserPrivilegeIntegrationTest shouldDenyTraderAccessToOtherTradersTrades
  - UserPrivilegeIntegrationTest shouldAllowAdminFullTradeAccess

### Learned

- How fragile integration tests can be when context configuration drifts. I now ensure TestSecurityConfig is explicitly imported in every integration test that relies on database user details.
- The value of verifying payload contents in addition to status codes. For example, a 200 response could still leak other users data unless I check the response payload.

### Challenges

- Some tests were slow because they re-initialised the DB multiple times. I grouped similar tests and re-used a prepopulated schema where safe to reduce test time.
- A handful of edge cases remained where read-only endpoints were accessible without a role because I had left @PermitAll on a controller method from an earlier iteration; I tightened those permissions.
- There were some surprising database duplication and data integrity issues when running the test suite in sequence: test data builders were inserting duplicate records across tests and some migrations did not enforce expected unique constraints in the test database. I fixed this by making the test data builders idempotent, adding unique constraints in migrations where appropriate, and cleaning the schema between certain integration test groups so seed data could be deterministic.

## Wednesday 22 October 2025

Focus: Fixing traders accessing other traders and tightening dashboard privileges

### Completed

- Tightened TradeDashboardService to enforce both role and ownership boundaries:
  - getMyTrades returns only trades owned by the resolved current trader id.
  - getBookTrades checks that a caller has BOOK_VIEW privilege for that book, otherwise returns 403.
- Replaced some broad repository calls with explicit repository methods that include ownerId or bookId filters so filtering happens in the database and there is no possibility of returning data for other users in the service layer.
- Wrote integration tests specifically for TradeDashboardController including:
  - TradeDashboardIntegrationTest shouldReturnMyTradesForTrader
  - TradeDashboardIntegrationTest shouldReturn403WhenTraderRequestsOtherTradersBookAggregation
- Added guard rails in services to log suspicious attempts where user id in path does not match authenticated user and return AccessDeniedException quickly.

### Learned

- How to structure the service API so authorisation decisions are clear: a method either accepts an optional caller id (for admin callers) or strictly uses the authenticated principal.
- That moving filtering logic to repository queries not only improves performance but also removes accidental leaks that can happen with in-memory filtering.

### Challenges

- Rewriting queries required small database migration adjustments because I added a few composite indexes (for example trade.owner_id plus maturity_date) to keep the new queries efficient.
- I had to coordinate these schema changes with the test data builders so integration tests still passed locally.

## Thursday 23 October 2025

Focus: Security configuration polish, logging and merging workflow improvements

### Completed

- Improved SecurityConfig by centralising authority naming and role mappings into a single helper class. This reduced chance of typos like TRADE_VIEW vs TRADE-VIEW appearing in different places.
- Added structured security logging to capture decisions that cause 403 responses. This includes user, attempted endpoint, required authority, and whether the denial came from a role check or an ownership check.
- Documented merging steps I used while integrating security branches into main:
  - Created a dedicated feature branch for each security subtask (for example feature/security-userdetails, feature/security-tests).
  - Frequently merged main into feature branches to keep them up to date and resolved small conflicts at source, committing the conflict resolutions with clear messages.
  - When a complex conflict occurred, I created a temporary merge branch to resolve conflicts and run tests; only then did I fast-forward the feature branch or open a pull request.
- Ran a focused test suite for all UserPrivilegeIntegrationTest and TradeDashboardIntegrationTest classes and verified they passed after the merge conflict fixes.

### Learned

- Practical merge discipline: smaller, more frequent merges reduce conflict surface; resolving conflicts locally on a dedicated merge branch allows me to run tests before updating the feature branch.
- How to coordinate schema and code changes in the same branch so CI can apply migrations before the service starts. For local runs this meant seeding the test DB after migrations using a consistent test data builder.

### Challenges

- Some merges required manual reconciliation between test configuration classes that had similar bean names; I standardised naming to avoid future conflicts.
- Ensuring that merge commit messages clearly explained why a change was kept or altered helped when I reviewed history later.
- When resolving merge conflicts I sometimes introduced test regressions by accidentally changing security-related bean imports. I adopted the pattern of a dedicated merge branch for conflict resolution so I could run the full security integration tests before finalising the merge.

## Friday 24 October 2025

Focus: Regression testing, finalising integration test names and test coverage for 200 and 403 behaviour

### Completed

- Added final integration tests that assert the combination of status codes and payload content across roles and endpoints. New test titles added:
  - UserPrivilegeIntegrationTest shouldReturn200AndOnlyOwnedTradesForTrader
  - UserPrivilegeIntegrationTest shouldReturn403WhenTraderAttemptsToAmendOtherTradersTrade
  - TradeDashboardIntegrationTest shouldReturn200ForAdminBookAggregation
  - TradeDashboardIntegrationTest shouldReturn403ForTraderRequestingUnauthorizedBook
- Completed a regression run over the backend test suite focusing on the security tests and fixed two flaky tests by refining test data setup to be deterministic.
- Wrote a one page summary for the team describing the security model and the tests to run locally. The summary includes:
  - Roles and example privileges (TRADER, SALES, MIDDLE_OFFICE, SUPPORT, ADMIN)
  - Which endpoints require ownership checks as well as which only require role checks
  - How to run the security integration tests with the TestSecurityConfig profile
- Committed and pushed the feature branch containing the final security work and opened a pull request for review.

### Learned

- How a combination of role checks and ownership checks provides both coarse and fine-grained protection and how to document those decisions to keep the rest of the team aligned.
- Naming conventions for integration tests that make the intent and expected HTTP status obvious to reviewers and that help in triaging failures quickly.

### Challenges

- Balancing thorough test coverage against acceptable execution time required me to categorise tests into fast unit, medium integration and slow end-to-end tests and to make that matrix clear in the developer documentation.
- Before pushing I double checked that the pull request included a clear description of the security changes and links to the new integration tests so reviewers can focus on the security surface area.
- Final regression runs uncovered a small number of remaining data integrity problems caused by the way test setup re-used database state between test classes. Converting some test fixtures to be created and torn down per-class resolved flakiness and prevented unexpected duplicates from appearing in later tests.

## Summary for 18 to 24 October 2025

Over the last seven days I focused heavily on correcting security and privilege issues that caused incorrect 200 and 403 behaviours and allowed traders to access other traders data. The work included controller and service layer changes, creating a database-backed UserDetailsService, test configuration improvements, writing clear integration tests that assert both status codes and payload content, and improving the merge workflow so conflicts are resolved safely.

Key actions to carry forward

- Keep repository queries owner-aware so filtering is done at the database level where possible.
- Maintain TestSecurityConfig and ensure it is explicitly imported by integration tests that validate database user details.
- Keep integration test names explicit about expected HTTP status codes and payload assertions so failures are quick to diagnose.
- Continue the merge discipline of frequent small merges, and use dedicated merge branches to run tests before updating feature branches.

## Dev Log 2025 10 22

### Completed

- Refactored the `TradeSettlementController` so that settlement instructions are now optional, matching the assignment’s requirements.
- Removed the incorrect validation that rejected missing settlement instructions.
- Implemented input cleaning using `.trim()` and normalised blank-only values to `null` before saving.
- Added SQL injection protection by validating user input patterns and ensuring unsafe characters cannot be persisted.
- Updated the `TradeService` and `TradeRepository` to support settlement instruction retrieval and storage.
- Preserved all role-based access control using `@PreAuthorize` annotations.
- Wrote detailed, explanatory comments throughout the controller and service to make the logic clear and consistent.

### Learned

- Implementing SQL injection prevention properly for user-provided text fields was new to me. I learned how to validate and sanitise user input safely before it reaches the database.
- I spent time reasoning through why and how to structure two separate DTOs one for incoming API requests (`AdditionalInfoRequestDTO`) and another for backend use (`AdditionalInfoDTO`). At first, it felt redundant, but I understood that it keeps the API layer clean and avoids exposing unnecessary internal fields.
- I had to think carefully about how settlement instruction data should be represented. Initially, I considered adding new fields to the Trade model, but decided it was more flexible to store them in the `AdditionalInfo` table as dynamic key-value pairs.
- The process of validating an optional field was interesting. I implemented the logic so that settlement instructions can be omitted but still cleaned and validated when provided.
- I gained a better understanding of how trimming, null-checking, and normalisation work together to maintain both clean data and flexibility in input handling.

### Challenges

- At the beginning, deciding how to model settlement instructions was not straightforward. I debated whether they should live inside the trade table or as a separate linked entity.
- Handling optional fields required careful validation flow to prevent contradictions between the controller and service.
- SQL injection protection was challenging, especially figuring out the right balance between allowing normal punctuation and blocking dangerous characters.
- Managing two DTOs added extra complexity, especially making sure the mapping between request and response was consistent.
- Updating the service and repository to integrate the new logic took time, as I had to ensure that existing trade lookups still worked correctly.
- Overall, today’s work felt like a good combination of design thinking, validation logic, and improving application safety.

### Development Log Settlement Instructions Integration (Steps 1–7)

Feature: Integration of Settlement Instructions into Trade Capture Workflow
Objective: To enable traders to capture settlement instructions directly during trade booking, reducing operational delays and errors.

#### Repository Layer: Focused Queries for Faster Retrieval

I started by enhancing the AdditionalInfoRepository to add purpose-built queries that allow focused lookups instead of scanning the entire table.

The key addition was:
`@Query("""
       SELECT a FROM AdditionalInfo a
       WHERE a.entityType = 'TRADE'
         AND a.fieldName  = 'SETTLEMENT_INSTRUCTIONS'
         AND a.active     = true
         AND LOWER(a.fieldValue) LIKE LOWER(CONCAT('%', :keyword, '%'))
       """)
List<AdditionalInfo> searchTradeSettlementByKeyword(@Param("keyword") String keyword);`

This supports case-insensitive, partial text searches on settlement instructions an explicit business requirement for the operations team.
I used the SQL LIKE wildcard (%keyword%) so users can search for phrases like “Euroclear” even if it appears mid-sentence.

This is much more efficient than the previous findAll() and in-memory .filter() approach, which had a time complexity of O(n) on the application side.
By pushing the filtering down to the database layer, move closer to O(log n) due to indexed lookups (once indexing was added in Step 2).

#### Database Optimisation: Adding Indexes

Next, I added composite indexes to the AdditionalInfo entity to improve query performance.

`@Table(
  name = "additional_info",
  indexes = {
    @Index(name = "idx_entity_type_field_entity", columnList = "entity_type, field_name, entity_id"),
    @Index(name = "idx_field_value", columnList = "field_value")
  }
)`

The first index accelerates lookups for queries that identify records by entity type and ID (used in findActiveOne() and similar methods).
The second index supports LIKE-based text searches in searchTradeSettlementByKeyword().

Although the second index doesn’t make LIKE '%keyword%' truly constant time, it does help the database prune results faster, improving real-world performance.

#### Service Layer Refactor: Business Logic and Validation

I refactored AdditionalInfoService to include clear, business-focused methods for settlement instructions.
Previously, the controller directly performed validation and persistence logic, which violated separation of concerns.

The refactored methods (createAdditionalInfo, updateAdditionalInfo, and the new settlement-specific methods) now handle:

    Length checks (10–500 chars)

    SQL injection prevention (rejects dangerous patterns like ;, --, DROP TABLE)

    Regex-based content validation

    Structured text support for multiline “label: value” formats

This centralises validation in the service layer, ensuring consistent rules whether data comes from the UI or an API.
Complexity-wise, validation operations are O(n) in string length, which is negligible since the field has a capped size.

#### Audit Trail Implementation

To meet the Audit Trail business requirement, I introduced a new entity:

AdditionalInfoAudit

This records every change to settlement instructions, storing:

    Old and new values

    User who made the change

    Timestamp

While not all user roles need to view this data, ADMIN and MIDDLE_OFFICE roles can retrieve it via a dedicated endpoint:
`@GetMapping("/{id}/audit-trail")`
This ensures full traceability for compliance, regulatory reporting, and internal controls.
Each change record is independent of the main AdditionalInfo table, so write operations have slightly higher cost (O(1) insert for each change), but with high accountability benefits.

#### Controller Refactor: Clean Delegation

I replaced inline controller logic with clean service-layer calls in TradeSettlementController.
For example, the updateSettlementInstructions() method was refactored to delegate to a single service method:
`AdditionalInfoDTO result =
additionalInfoService.upOrInsertTradeSettlementInstructions(id, text, changedBy);`
This “upInsert” (update-or-insert) approach simplifies handling of both new and amended settlement instructions.
It aligns with business rules allowing traders to edit settlement instructions at any stage, while maintaining audit tracking automatically.

This also improves maintainability the controller is now focused solely on HTTP and role logic, not database operations.

#### Mapper Sanity Check and Versioning

I revisited AdditionalInfoMapper to ensure that conversions between entities and DTOs were safe and future-proof.

I added a version field to the entity, annotated with:
`@Version`
This enables optimistic locking preventing two users from overwriting each other’s changes to the same record.
The first update succeeds and increments the version (starting at 1), while any concurrent update throws an OptimisticLockException.

This is an essential part of data integrity in multi-user systems and meets audit and risk management expectations.

The mapper also includes a small “sanity check” step: it ensures that null DTOs or entities don’t cause runtime exceptions and that field values are correctly defaulted where missing (e.g., fieldType = "STRING").

#### Repository Enhancements and Final Linkages

In TradeRepository, I confirmed the existence of:
`List<Trade> findAllByTradeIdIn(List<Long> tradeIds);`
This method allows retrieving all trades by a list of IDs, used by the searchBySettlementInstructions() controller.

It is a derived query method generated automatically by Spring Data JPA clean, type-safe, and efficient (translates to SQL WHERE trade_id IN (...)).

I also ensured that all Optional imports were correctly included (for example, in findActiveOne()), so that the service layer can handle “record not found” scenarios safely, avoiding NullPointerException.

#### Reflection and Alternatives Considered

Alternative to AdditionalInfo Table Extension

I could have added settlement_instructions directly into the Trade table (simpler schema).

However, I chose to use the existing AdditionalInfo extensible architecture for long-term scalability it allows storing future optional fields (like “Delivery Notes”) without schema changes.

#### Audit Trail Alternatives

Could have used database triggers or Hibernate Envers, but I implemented a lightweight manual table for clarity and control.

#### Performance Considerations

Indexes ensure faster lookups. Without them, most repository searches would remain O(n), which isn’t scalable for production data volumes.

#### Validation and Security

Validation at service level prevents SQL injection and malicious payloads early in the request cycle, which is both safer and easier to test.

#### Learned

Planning for betternService Centralised validation + audit logic Consistency and security
Controller Cleaned endpoints to delegate to service Better architecture
Mapper Added safety checks and versioning Prevent data corruption
Audit Trail Implemented via new entity and endpoint Traceability for compliance
Versioning Enabled optimistic locking Data integrity and concurrency safety

Overall, these steps transformed the system from a procedural, controller-heavy design into a layered, maintainable architecture that meets all business, audit, and performance requirements.

The settlement instruction feature is now searchable, editable, auditable, and efficient, with all logic cleanly separated across the repository, service, and controller layers ready for front-end integration.

### Sunday, 26 October 2025 Settlement instructions validation refactor

**Focus:** Settlement instructions validation refactor, audit wiring and service integration

### Completed

- I centralised settlement-instruction validation into a single field-level validator class, `SettlementInstructionValidator`.

  - Rules implemented: optional field; trim/normalise input; length 10–500 characters; forbid semicolons; detect and reject unescaped single/double quotes; deny common SQL-like tokens (for example `DROP TABLE`, `DELETE FROM`); and a final allowed-character check that permits escaped quotes and structured punctuation.
  - Error messages were improved to be user-friendly and include a copyable example for escaping quotes (for example: `Settle note: client said \"urgent\"`).

- I added a small adapter entry point to the existing validation orchestration: `TradeValidationEngine.validateSettlementInstructions(String)`.

  - This adapter delegates to the field-level validator and returns a `TradeValidationResult` to callers, enabling consistent access to settlement validation from services that already use the engine.

- I integrated the validation into `AdditionalInfoService` at three locations: `createAdditionalInfo`, `updateAdditionalInfo` and `upOrInsertTradeSettlementInstructions`.

  - When `fieldName` equals `SETTLEMENT_INSTRUCTIONS` the service calls the engine adapter and throws `IllegalArgumentException` with the first validation message when validation fails.
  - Non-settlement `AdditionalInfo` entries retain the previous lightweight checks so behaviour for other fields is unchanged.

- Small naming clarifications were made at integration points (replaced short variable `vr` with `validationResult`) and concise refactor comments were added to explain why validation was centralised.

- Audit-trail wiring was verified: `AdditionalInfoAudit` records old and new values, the actor (`changedBy`) is resolved from the Spring Security context with a sensible fallback, and timestamps are recorded for each change.

### Rationale & business mapping

- Centralising validation provides a single source of truth for settlement-business rules and supports the operations requirement that settlement instructions are safe, consistent and searchable.

- Business-driven rules implemented:

  - Optional field operations can omit instructions when unnecessary.
  - Length bounds (10–500) ensures instructions are informative but bounded for storage, display and index performance.
  - Semicolon ban & SQL-token blacklist mitigate common injection patterns before persistence.
  - Escaped-quote enforcement preserves user intent and avoids ambiguity while keeping stored text safe to display.

- The engine adapter allows callers to access field-level validation from the same orchestration point used for trade-level rules, avoiding awkward caller-side wiring.

### Learned

- Field-level validators are the right abstraction for free-text fields they are small, focused and easy to unit test.
- Providing a single engine entry point keeps validation discoverable and consistent across services.
- Example-based error messages materially help inexperienced users correct input (the escape example was added for exactly this reason).

### Challenges

- A small non-functional inconsistency remains: the `settlementInstructionValidator` field is still present in the service while validation is routed via the `TradeValidationEngine` adapter. This causes a compiler warning for an unused field and should be tidied in a follow-up.

- Balancing strict safety rules (for example forbidding semicolons) with everyday user convenience required a deliberate decision in favour of safety, plus clearer messaging to users.

### Next steps

1. To Add unit tests for `SettlementInstructionValidator` covering:\
   - Accept: escaped quotes and valid length boundaries.\
   - Reject: unescaped quotes, semicolons, SQL-like tokens, and out-of-range lengths.
     2.To Add a small integration test for `AdditionalInfoService` asserting that invalid settlement instructions throw `IllegalArgumentException`, that valid values persist, and that audit records are created.
2. Consider moving settlement `AdditionalInfo` creation into the same transactional boundary as `TradeService.createTrade` so trade creation and settlement persistence are atomic.

### Summary

These changes make settlement instructions safer to store and simpler to validate consistently across the application. They support the business goals of searchable settlement text for operations, auditable changes for compliance, and robust input validation to reduce operational risk.

## Tests implemented (26 October 2025)

Below I document the three tests I added: two unit tests and one integration test. For each I explain how it is implemented, why I wrote it that way, and what I learned concentrating on the hardest parts that required iteration.

### 1) Unit: `SettlementInstructionValidatorTest`

What it tests

- The validator enforces length boundaries, rejects unescaped quotes and semicolons, and detects blacklisted SQL-like tokens. It also accepts properly escaped quotes and emphasises user-friendly error messages.

How it's implemented

- Frameworks: JUnit 5 with plain assertions.
- Tests include:
  - validLongAndShort(): asserts that strings at or inside the 10..500 boundary are accepted.
  - rejectUnescapedQuotes(): passes a string that contains an unescaped single or double quote and asserts the validator returns a failing `TradeValidationResult` with a message mentioning escaping.
  - rejectSemicolonAndSqlTokens(): asserts the presence of `;` and tokens like `DELETE FROM` cause rejection and that the returned error contains the blacklist token name.

Why this shape

- The validator is small and deterministic. Unit tests exercise only the validator logic so failures are fast and obvious, which helps when iterating on regexes and token lists.

Hardest parts / what I learned

- Edge-case test data matters: some inputs that look obviously invalid at a glance can pass naive regexes (for example, `O'Connor`), which forced me to refine the escape detection logic to permit legitimate names when they are escaped correctly.
- Writing clear, actionable error messages is as important as the rule itself. I added tests that assert specific substrings in the message so future refactors don't degrade UX.

### 2) Unit: `AdditionalInfoServiceTest` (service-level)

What it tests

- That `AdditionalInfoService` delegates settlement instruction checks to the `TradeValidationEngine` adapter and reacts correctly to failures and successes. It also asserts audit creation behaviour when the change is accepted.

How it's implemented

- Frameworks: JUnit 5 + Mockito.
- Mocks: `TradeValidationEngine`, `AdditionalInfoRepository`, `AdditionalInfoAuditRepository` (or the higher-level repo that persists audit records).
- Tests include:
  - whenValidationFails_thenThrow(): stub the `TradeValidationEngine` to return a `TradeValidationResult` containing errors; call `upOrInsertTradeSettlementInstructions(...)` and assert an `IllegalArgumentException` is thrown and that no audit record was saved (verify zero interactions with audit repository).
  - whenValidationPasses_thenPersistAndAudit(): stub the engine to return an empty/OK `TradeValidationResult`; call the service and verify that `AdditionalInfoRepository.save(...)` and `AdditionalInfoAuditRepository.save(...)` were invoked with expected fields (old/new values, `fieldName` set to `SETTLEMENT_INSTRUCTIONS`). The test also verifies that the `changedBy` value is what the service was passed or resolved (in unit tests I pass a username or stub SecurityContext as needed).

Why this shape

- Service unit tests focus on behaviour and side effects (exceptions, repository interactions) rather than low-level parsing. By mocking the engine I isolate service logic from validator internals, making test failures indicate exactly where the problem lies.

Hardest parts / what I learned

- Passing the authenticated username into the service required a conscious decision: either read `SecurityContextHolder` inside the service or pass an explicit `changedBy` parameter from the controller. I chose to read from the `SecurityContext` with defensive fallbacks, but unit tests must then either set the SecurityContext or call the service with an explicit username. I added both styles in tests to keep service usage flexible.
- Verifying that no audit is saved on validation failure is important; without that assertion a silent error would leave stale or inconsistent audit data.

### 3) Integration: `AdditionalInfoIntegrationTest`

What it tests

- The full controller → service → repository flow for settlement-instruction upsert including validation, persistence and audit writing. The primary assertion is that when an authenticated user performs the request the resulting `AdditionalInfoAudit.changedBy` is populated with the real authenticated username (not a placeholder).

How it's implemented

- Frameworks: Spring Boot Test with `@SpringBootTest`, `@AutoConfigureMockMvc`, and `MockMvc`.
- Test configuration:
  - Uses the real application context and an in-memory H2 database initialised from `src/main/resources/data.sql` to match production-like seeds.
  - `@WithMockUser(username = "alice", roles = {"TRADER"})` provides a real principal for the request.
  - The test performs a `PUT /api/trades/{id}/settlement-instructions` using the seeded `trade_id` (200001) and JSON body containing a valid settlement instruction.
  - The MockMvc request includes `.with(csrf())` so CSRF-protected write endpoints succeed.
  - After the controller returns 200 OK the test queries the `AdditionalInfoAudit` repository (or endpoint) and asserts:
    - an audit record exists for `trade_id` 200001 and `fieldName` equals `SETTLEMENT_INSTRUCTIONS`;
    - `changedBy` equals `alice` (the authenticated `@WithMockUser` username);
    - the persisted value matches the submitted instruction.

Why this shape

- Integration tests of this shape are the most valuable safety net for refactors that change wiring between layers (for example: moving validation from inline checks to the `TradeValidationEngine`). They expose problems that unit tests can miss (for example, missing bean registration, security config mismatches, data-seed vs PK confusion, or JPQL parsing failures at context initialisation).

Hardest parts / what I learned

- Bean registration: the validator initially wasn't annotated as a Spring bean which caused context failures. Integration tests fail early and loudly for these mistakes; I used the failure logs to find the missing `@Component`/`@Service` annotation.
- Security in tests: state-changing endpoints require CSRF and a suitable principal. Omitting `.with(csrf())` or using the wrong role caused 403s. I had to ensure the test principal had the appropriate authority and that CSRF was applied.
- Seed data vs PK confusion: earlier tests used the wrong identifier (a DB PK) rather than the seeded `trade_id`. Using the canonical `trade_id=200001` from `data.sql` makes the test deterministic.
- Mocks vs real components: I intentionally reduced mocking in this integration test so the real mapper, repositories and services execute. This surfaced a subtle mapper mismatch where a mocked mapper previously hid a missing `tradeId` field in serialized responses.

### Overall testing strategy and final notes

- Layered tests: small fast unit tests for validation rules; service unit tests for delegation and side-effects; a single focused integration test to validate end-to-end behaviour. This combination keeps feedback loops short while providing broad coverage for wiring and configuration issues.
- Fail-fast benefits: the integration test revealed configuration and bean errors that unit tests would not show. Running the integration test early in the iteration saved time.
- Maintainability: I added comments in the tests describing why particular setup choices were made (for example why `trade_id=200001` must be used), to prevent future accidental regressions.

## NEWlog 27/10/2025

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

- Chosen to avoid a schema change and data migration. This minimises impact on existing DTOs and mappers and leverages existing indexed search on `AdditionalInfo` (index `idx_ai_entity_type_name_id`).
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

## Monday, 27 October 2025 Post previous log: settlement & audit persistence incident

### Context

Today focused on diagnosing and fixing an issue where settlement instructions and audit records were not persisting across application restarts and therefore not visible to admin or middle-office users. The failure mode originated from SQL initialisation attempting to re-run non-idempotent seed statements (`src/main/resources/data.sql`) against an existing file-backed H2 database (`jdbc:h2:file:./data/tradingdb`) which caused primary-key violations and aborted the Spring ApplicationContext on startup.

---

### Concept: Store settlement instructions in `AdditionalInfo` (Option B)

### Completed

- Implemented the chosen approach to persist settlement instructions in the `additional_info` table rather than a new dedicated table. The service-level upsert is implemented in `backend/src/main/java/com/technicalchallenge/service/AdditionalInfoService.java` and is invoked by the controller at `backend/src/main/java/com/technicalchallenge/controller/TradeSettlementController.java`.
- The upsert flow: validate input → ownership check → insert or update `AdditionalInfo` → write `AdditionalInfoAudit` record.
- Wrote and ran integration coverage that verifies the upsert writes both `additional_info` and `additional_info_audit` rows (existing `AdditionalInfoIntegrationTest` was adjusted to run against the test profile).

### Challenges

- Encountered a startup failure caused by `data.sql` re-applying INSERT statements into an already-seeded file H2 DB. The script contains plain INSERTs (for example, `INSERT INTO desk (id, desk_name) VALUES (1000, 'FX')`) so re-running produced H2 error 23505 (unique constraint violation) and Spring aborted context initialisation.
- This prevented the app from reaching a steady-state, which in turn meant no runtime PUT requests would commit and produce audit rows for admin to view.

### Learned

- Seed data must be idempotent or restricted to test runs to avoid fatal initialisation failures in a persistent dev DB. A safe approach is `MERGE INTO ... KEY(id)` or moving initialisation to the test profile.
- Upsert + audit should run within a single transactional boundary so that audit and data changes commit or roll back together.

---

## Implementation plan: Convert `data.sql` INSERTs to idempotent MERGE statements (planned)

Why need this

- The current `src/main/resources/data.sql` is packed with many fixed‑ID `INSERT` statements. Re-running that file against an existing, file‑backed H2 database causes duplicate primary/unique key errors and aborts Spring startup. Temporarily disabling runtime SQL init (set `spring.sql.init.mode=never`) prevents the immediate crash, but it is a mitigation rather than a durable fix.
- Converting the non‑idempotent `INSERT` statements to idempotent `MERGE` (H2) or `UPSERT` statements (DB specific) means the seed becomes safe to re-run: it will insert missing rows and update existing ones. That allows developers to safely re-seed environments, makes local bootstraps deterministic, and reduces accidental production/CI surprises.

Detailed implementation plan (step-by-step)

1. Scope & plan

- Start by converting a small, high‑value subset of the seed: `application_user`, `book`, and `trade` blocks. These are the most likely to cause duplicate key failures and are referenced by many tests and code paths.
- Create a short PR with just that subset so reviewers can validate semantics.

2. Conversion rules (H2 specific)

- For each single‑row INSERT like:
  ```sql
  INSERT INTO book (id, book_name, active, version, cost_center_id)
  VALUES (1000, 'FX-BOOK-1', true, 1, 1000);
  ```
  replace with per‑row MERGE:
  ```sql
  MERGE INTO book (id, book_name, active, version, cost_center_id)
  KEY(id)
  VALUES (1000, 'FX-BOOK-1', TRUE, 1, 1000);
  ```
- For multi‑row INSERTs, split into multiple MERGE statements (H2's MERGE is evaluated row‑by‑row).
- Preserve the original ordering of parent tables before child tables to honour FK constraints (e.g., desk → sub_desk → cost_center → book → trade → trade_leg).
- For any timestamp/date literals, keep the format already used in the file (the current file has ISO strings which H2 accepts in many cases). If a conversion exposes a parsing issue, switch to explicit TIMESTAMP literals or CASTs.

3. Safety & verification (local)

- Run the app against the existing `.mv.db` with `spring.sql.init.mode=never` still set; confirm the app starts (baseline).
- Temporarily set `spring.sql.init.mode=always` in a copied `application-local-merge-test.properties` to run only the revised `data.sql` against a fresh, file‑backed DB (or use a separate directory) to confirm the MERGE script runs without errors.
- Start the app and confirm there are no H2 error 23505 duplicates in the logs.
- Verify critical endpoints (GET /api/trades, GET /api/users) return expected seeded data.

4. Test suite

- Run `mvn test` (backend) to ensure integration tests still pass against the test profile (in‑memory DB). The in‑memory test DB will still use the original `data.sql` unless also update the test data. Optionally, update the `src/test/resources/data/data.sql` to match MERGE semantics so both runtime and test seeds are consistent.

5. Rollout

- Open a small PR with converted blocks and a clear PR description explaining the reason and verification steps.
- After PR approval, either convert the entire `data.sql` or iterate block by block until all problematic INSERTs are replaced.

Potential risks and mitigations

- Risk: MERGE may unintentionally update rows that the team expects to remain unchanged in the dev DB. Mitigation: Scope initial PR to a few tables and review changes; avoid changing values that tests rely on unless tests are updated accordingly.
- Risk: Formatting or type mismatches (date parsing) could surface when converting many rows. Mitigation: run small batches and fix issues per table.
- Risk: Tests referencing exact creation timestamps may fail if MERGE overwrites them. Mitigation: keep the same literal timestamps where possible; when updating, prefer not to change timestamp columns.

Impact (what changes for developers)

- Short term (after conversion): Developers can safely re-run `data.sql` against a file‑backed H2 DB without deleting `.mv.db` or seeing startup failures. This makes local resets and environment bootstraps predictable.
- Medium term: Reduced chance of accidental startup aborts in CI or shared environments, fewer manual recovery steps, and clearer provenance for seeded data changes.
- Tests: If choose to update `src/test/resources/data/data.sql` to use MERGE as well, test runs become more consistent with runtime behaviour. Otherwise, tests will continue to use the in‑memory seeding approach and pass as before.

Notes and next actions

- I will not apply these changes automatically now since I am moving to front-end work. Keep this plan in the log as a task Ior someone on the team can pick up later.
- When Iwant, I can implement Option A (subset conversion) and open a PR with the converted blocks for review.
