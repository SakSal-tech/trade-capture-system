# Developer Progress Log — October 2025

## Saturday, Oct 11

**Focus:** Project structure and Date Validation foundation  
**Hours active:** 3.5 hrs

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
**Estimated time:** ~4–5 hours

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
- Reinforced understanding of TDD — writing tests first made implementation clearer and faster.

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
**Hours active:** 4 hrs

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

### Tomorrow (Tuesday, Oct 14) — Plan

**Goal:** Start Enhancement 3: Trader Dashboard and Blotter System  
**Estimated time:** ~4–5 hours

#### Tasks

1. Design and implement REST endpoints for trader dashboard:
   - `@GetMapping("/my-trades")` — Trader's personal trades
   - `@GetMapping("/book/{id}/trades")` — Book-level trade aggregation
   - `@GetMapping("/summary")` — Trade portfolio summaries
   - `@GetMapping("/daily-summary")` — Daily trading statistics
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

# Developer Progress Log — October 2025

## Tuesday, Oct 14, 2025

**Focus:** Enhancement 3 — Trader Dashboard and Blotter System  
**Hours active:** 5 hrs

### Completed

- Fully implemented **Trader Dashboard and Blotter System** to meet the business requirement:  
  _“As a trader, I need personalised dashboard views and summary statistics so that I can monitor my positions and make informed trading decisions.”_

#### Key Deliverables

- Implemented **new REST endpoints** in `TradeDashboardService`:

  - `GET /my-trades` — Returns authenticated trader’s personal trades using Spring Security context.
  - `GET /book/{id}/trades` — Provides book-level trade aggregation with privilege validation.
  - `GET /summary` — Generates real-time portfolio summaries (trades by status, notional by currency, and counterparty breakdowns).
  - `GET /daily-summary` — Produces daily trade metrics with historical comparisons (today vs yesterday).

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

### Tomorrow (Wednesday, Oct 15, 2025) — Plan

**Goal:** Integration Testing + Begin Step 4 (Bug Investigation and Fix)  
**Estimated time:** ~5–6 hours

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

GET /api/trades — getAllTrades
GET /api/trades/{id} — getTradeById
POST /api/trades — createTrade
PATCH /api/trades/{id} — updateTrade (partial)
PUT /api/trades/{id} — updateTrade (full)
DELETE /api/trades/{id} — deleteTrade
POST /api/trades/{id}/terminate — terminateTrade
POST /api/trades/{id}/cancel — cancelTrade
TradeDashboardController Endpoints:

GET /api/dashboard/filter — filterTrades
GET /api/dashboard/search — searchTrades
GET /api/dashboard/rsql — searchTradesRsql
GET /api/dashboard/my-trades — getMyTrades
GET /api/dashboard/book/{bookId}/trades — getTradesByBook
GET /api/dashboard/summary — getTradeSummary
GET /api/dashboard/daily-summary — getDailySummary
Next step: I will recommend specific privilege validation integration tests for each endpoint, specifying expected roles and HTTP status codes.
To fully resolve request, here is a privilege validation integration test plan for all endpoints:

TradeController:

GET /api/trades — Test: Only users with "TRADE_VIEW" or "ADMIN" role can access; others get 403.
GET /api/trades/{id} — Test: Only "TRADE_VIEW" or "ADMIN" role; others get 403.
POST /api/trades — Test: Only "TRADE_CREATE" or "ADMIN" role; others get 403.
PATCH /api/trades/{id} — Test: Only "TRADE_EDIT" or "ADMIN" role; others get 403.
PUT /api/trades/{id} — Test: Only "TRADE_EDIT" or "ADMIN" role; others get 403.
DELETE /api/trades/{id} — Test: Only "TRADE_DELETE" or "ADMIN" role; others get 403.
POST /api/trades/{id}/terminate — Test: Only "TRADE_TERMINATE" or "ADMIN" role; others get 403.
POST /api/trades/{id}/cancel — Test: Only "TRADE_CANCEL" or "ADMIN" role; others get 403.
TradeDashboardController:

GET /api/dashboard/filter — Test: Only "TRADE_VIEW" or "ADMIN" role; others get 403.
GET /api/dashboard/search — Test: Only "TRADE_VIEW" or "ADMIN" role; others get 403.
GET /api/dashboard/rsql — Test: Only "TRADE_VIEW" or "ADMIN" role; others get 403.
GET /api/dashboard/my-trades — Test: Only "TRADER" or "ADMIN" role; others get 403.
GET /api/dashboard/book/{bookId}/trades — Test: Only "BOOK_VIEW" or "ADMIN" role; others get 403.
GET /api/dashboard/summary — Test: Only "TRADER" or "ADMIN" role; others get 403.
GET /api/dashboard/daily-summary — Test: Only "TRADER" or "ADMIN" role; others get 403.
For each endpoint, write integration tests using @WithMockUser for:

Allowed roles (expect 200/204)
Disallowed roles (expect 403)

# Developer Progress Log — 18 to 24 October 2025

These entries cover day by day work I did on security and privilege issues across the controllers, service layer and configuration. I write them in full sentences to capture my thinking, the technical changes I made, and the tests I added so I can revisit decisions later.

---

## Saturday 18 October 2025

Focus: Scoped access and the first pass at controller level authorisation

Hours active: 4

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

Hours active: 5

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

Hours active: 6

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
- Swagger and the API explorer were intermittently inaccessible during development because the tightened security configuration blocked the swagger UI and the OpenAPI endpoints. I solved this by explicitly permitting the swagger UI endpoints under the local/test profile and ensuring health and swagger paths are only open in non-production profiles.

## Tuesday 21 October 2025

Focus: Diagnosing repeated 403s and 200s where they should not occur, and fixing tests

Hours active: 5

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

Hours active: 5.5

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

Hours active: 4.5

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

Hours active: 6

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
- I spent time reasoning through why and how to structure two separate DTOs — one for incoming API requests (`AdditionalInfoRequestDTO`) and another for backend use (`AdditionalInfoDTO`). At first, it felt redundant, but I understood that it keeps the API layer clean and avoids exposing unnecessary internal fields.
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

### Development Log — Settlement Instructions Integration (Steps 1–7)

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

This supports case-insensitive, partial text searches on settlement instructions — an explicit business requirement for the operations team.
I used the SQL LIKE wildcard (%keyword%) so users can search for phrases like “Euroclear” even if it appears mid-sentence.

This is much more efficient than the previous findAll() and in-memory .filter() approach, which had a time complexity of O(n) on the application side.
By pushing the filtering down to the database layer, we move closer to O(log n) due to indexed lookups (once indexing was added in Step 2).

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

This also improves maintainability — the controller is now focused solely on HTTP and role logic, not database operations.

#### Mapper Sanity Check and Versioning

I revisited AdditionalInfoMapper to ensure that conversions between entities and DTOs were safe and future-proof.

I added a version field to the entity, annotated with:
`@Version`
This enables optimistic locking — preventing two users from overwriting each other’s changes to the same record.
The first update succeeds and increments the version (starting at 1), while any concurrent update throws an OptimisticLockException.

This is an essential part of data integrity in multi-user systems and meets audit and risk management expectations.

The mapper also includes a small “sanity check” step: it ensures that null DTOs or entities don’t cause runtime exceptions and that field values are correctly defaulted where missing (e.g., fieldType = "STRING").

#### Repository Enhancements and Final Linkages

In TradeRepository, I confirmed the existence of:
`List<Trade> findAllByTradeIdIn(List<Long> tradeIds);`
This method allows retrieving all trades by a list of IDs, used by the searchBySettlementInstructions() controller.

It is a derived query method generated automatically by Spring Data JPA — clean, type-safe, and efficient (translates to SQL WHERE trade_id IN (...)).

I also ensured that all Optional imports were correctly included (for example, in findActiveOne()), so that the service layer can handle “record not found” scenarios safely, avoiding NullPointerException.

#### Reflection and Alternatives Considered

Alternative to AdditionalInfo Table Extension

I could have added settlement_instructions directly into the Trade table (simpler schema).

However, I chose to use the existing AdditionalInfo extensible architecture for long-term scalability — it allows storing future optional fields (like “Delivery Notes”) without schema changes.

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

The settlement instruction feature is now searchable, editable, auditable, and efficient, with all logic cleanly separated across the repository, service, and controller layers — ready for front-end integration.
