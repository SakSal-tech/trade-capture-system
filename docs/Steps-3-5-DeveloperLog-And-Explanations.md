# Developer Progress Log October 2025

I did not write developer logs before this date as I was fixing the errors that came in the project. Please see `test-fixes-template.md` for the test fixes log.

## Date Validation foundation Oct 11 2025

### Completed

- Today I Set up core validation framework:
  I created a separate folder for validation files. The aim to centralise business rules and to separate validations from service classes so only those methods that need validations to be applied call the validation engine. This is to follow SOLID principle.
  These ar the classes I managed to implement today:

  - TradeValidationResult: TradeValidationResult acts as a single container for all validation feedback. I designed it so that validators do not throw exceptions immediately when they detect a problem. Instead, each validator adds error m
    essages to the shared TradeValidationResult object.

  - TradeValidationEngine: The engine serves as the orchestrator of all validators. It ensures that the rules run in the correct order and that each rule has access to the shared TradeValidationResult.If UBS later turns off a validation rule (e.g. changes in trading business or in regulatory), I can easily disable or replace validators in the engine without touching service code.

  - TradeDateValidator (This is a basic trade date validator version implementation and I plan to improve this)
    -To develop TDD unit tests, which was a challenge for me to write a test for a method that I did not write code yet. I wrote and ran first failing test (failWhenMaturityBeforeStartDate)

- I Connected validation flow with TradeValidationServiceTest
- I Confirmed integration structure working (TDD RED phase)
- Git operations:
  - I Created new branch feat/comprehensive-trade-validation-engine. This branch will have all business validations code. I plan to merge this into main when all validations completed.
- I planned structure for future validators (User previleges, Leg, Entity)

### Learned

- Role of ChronoUnit.DAYS.between() which I used calculate the number of days between two dates or times and comes from the java.time package ` java.time.temporal package`, a Java date-time API.
- How to structure reusable validators for maintainability
- Purpose of separating TradeValidationResult and TradeValidationEngine
- Practising TDD phases (Red → Green → Refactor) and how to write a tests and expect it to fail.

### Challenges

- It took me time to decide the structure of classes and methods of validations
- I had to research and get clarity on exception handling (decided to use result aggregation pattern instead of throwing). I chose not to throw exceptions when something goes wrong. Instead, each operation returns a result object that contains both success data and error information. This makes errors clearer and I believe this is very useful as I am validating many fields multiple trules.asks

- I found writing tests before logic methods harder than writing the logic then tests. However, I can see now why TDD is important. I had to think about:
  - What should this method do?
  - What parameters should it accept?
  - What outputs do I expect?
  - What edge cases matter?

## Plan for tomorrow (Sunday, Oct 12) Date Validation Rule tests and logic (GREEN phase)

### Tasks

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

## Comprehensive Trade Validation Engine Sunday 12 October 2025 Enhancement 2

Expanded test coverage and implemented multiple validation layers.

### Tasks Completed

- Completed all **Date Validation Rules**:
  - I completed tests and passed tests for:
    - “Start date cannot be before trade date.”
    - “Trade date cannot be more than 30 days in the past.”
  - Implemented corresponding logic in `TradeDateValidator`.
  - All date validation tests now passing.
- I implemented **User Privilege Enforcement** rules:
  - Added `UserPrivilegeValidator` class handling user-type-based permissions.
  - Wrote comprehensive unit test `UserPrivilegeValidatorTest` covering:
    - TRADER: can perform CREATE, AMEND, TERMINATE, CANCEL.
    - SALES: limited to CREATE and AMEND.
    - MIDDLE_OFFICE: can AMEND and VIEW only.
    - SUPPORT: can VIEW only.
  - All privilege validation tests passed successfully.
- I started **Cross-Leg Business Rules**:
  - Added `maturityDate` to `TradeLegDTO`.
  - I created `TradeLegValidator` and corresponding test for rule:
    - “Both legs must have identical maturity dates.”
  - Test passed after few changes and run.
- I added learning validation learning links to the file file `LearningLinksUsed.md` covering:
  - ValidationResult and ValidationEngine design patterns.
  - XMLUnit `ValidationResult` inspiration.
  - Articles on Chain of Responsibility and Rule Engine design.

### Key Learnings

- Using `ValidationResult` allows collecting multiple business rule errors without exceptions.
- Separate validators (date, privilege, leg) keep validation logic modular and maintainable.
- Learned difference between user roles and validation engine orchestration.
- More understanding of TDD writing tests first made implementation clearer and faster.

### Next Steps

- I will need to Write and implement tests for remaining Cross-Leg Business Rules:
  - Opposite pay/receive flags.
  - Floating legs must have an index.
  - Fixed legs must have a valid rate.
- I need to start planning for Entity Status Validation:
  - Verify user, book, and counterparty activity checks.

## Finalising comprehensive trade validation and planning dashboard enhancement, Monday 13 Oct

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

- How practically modular validation engines make codebase easier to maintain and extend
- Practised how centralised result object (`TradeValidationResult`) simplifies error handling and test assertions
- I have realised that when I refactor my code to follow SRP/SOLID and properly separate concerns, everything becomes clearer and easier to scale. It feels much less overwhelming to work with.
- Reviewing my tests more systematically has helped me catch gaps in my coverage and avoid regressions. It’s giving me more confidence that my code actually does what I think it does.

### Challenges

- I made silly mistakes and kept forgetting to update test assertions to match validator error messages which made tests fail
- Refactoring required careful coordination between engine and validator classes
- I needed to ensure all validation logic is covered by tests before committing

## Tomorrow (Tuesday, Oct 14) Plan Start Enhancement 3: Trader Dashboard and Blotter System

### Tasks

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

## Enhancement 3 Trader Dashboard and Blotter System, Tuesday, Oct 14, 2025

### Completed

- I implemented **Trader Dashboard and Blotter System** to meet the business requirement:  
  _“As a trader, I need personalised dashboard views and summary statistics so that I can monitor my positions and make informed trading decisions.”_

### Key Deliverables

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

### TradeDashboardService

- I added helper methods to TradeDashboardService to automatically resolve user identity and role from the security context:
  - `resolveCurrentTraderId()` → identifies the logged-in trader
  - `resolveCurrentUserRole()` → determines user role (TRADER, SALES, etc.)
- Ensures authenticated users only see **their own trades**.

- I added Added **real-time summary and aggregation logic** to to TradeDashboardService:

  - I Implemented in-memory aggregation using Java Streams and `HashMap.merge()` for performance.
  - This supports **multi-currency portfolios** by summing notional values across trade legs.
  - I added historical comparison capability in `getDailySummary()` for daily performance insights. I am not sure how many days UBS requires but I currently it is today and yesterday. Once I conform from UBS, I will change it accordingly maybe today's sumamry and the last 2 days, so in total of 3 days.

### Unit Testing TradeDashboardServiceTest

- I updated and added more test methods**`TradeDashboardServiceTest`**:
  - Added tests for all new endpoints and business logic.
  - Mocked privilege validation to simulate user roles.
  - Verified aggregation results, currency totals, and historical comparisons.
  - Confirmed all test cases pass successfully.

### Key Learnings

-I learned how to integrate **business analytics, privilege validation, and security context** in one cohesive service.

- I gained deeper understanding of **real-time data aggregation** using Java Streams.
- I improved understanding of enforcing **role-based data visibility** and user privilege boundaries.
- Reinforced good naming conventions and documentation practices for complex business logic.

### Challenges

- I had to refactor parts of the search logic to fit in the privilege validation, and I experienced accidentally breaking something that already worked.
- Handling multi-currency aggregation across different trade legs was trickier than I expected I kept running into null checks and edge cases I didn’t anticipate.
- Getting the tests to produce consistent results for today/yesterday comparisons pushed me to rethink how I handled dates dynamically e.g. `LocalDate.now().minusDays(1)`.
- Setting up the test configuration for privilege simulation took much longer than I thought, and I found myself going back and forth to make sure everything behaved the way I intended especially Spring Security and tests failings with 403.

## Plan for Tomorrow (Wednesday, Oct 15, 2025) Plan Integration Testing + Begin Step 4 (Bug Investigation and Fix)

### Tasks

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
     -Problem Technical Investigation
     - Root Cause Description
     - Proposed Solution and Alternatives
   - Implement bug fix:
     - Convert percentage rates correctly (`3.5% → 0.035`).
     - Replace `double` with `BigDecimal` for monetary accuracy.
   - Write regression tests to validate correct cashflow results and prevent recurrence.

All possible user privilege validation integration tests:

### TradeController Endpoints:

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
GET /api/dashboard/daily-summary getDailySummary.

### Privilege validation integration test plan for all endpoints:

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

---

## Scoped access and the first pass at controller level authorisation Saturday 18 October 2025

### Completed

- I reviewed all endpoints on TradeController and TradeDashboardController to document which endpoints should be restricted by role and by resource ownership.
  -I iImplemented `TradeController` controller-level security checks for a first pass: added method-level annotations and explicit checks inside endpoints where resource ownership is required. Examples:
  - `GET /api/trades/{id}` now checks that the authenticated user has either global view privileges or is the owner of the trade before returning details.
  - `GET /api/dashboard/my-trades `returns only trades belonging to the authenticated trader by scoping the query to the current user id.
    -I wrote unit tests for controller behaviour using `@WithMockUse`r to assert that authorised roles receive 200 and unauthorised roles receive 403 for several endpoints.
- Created the initial `UserPrivilegeIntegrationTest` class and added placeholder test methods that assert correct HTTP status codes for obvious cases.

### Learned

- The practical difference between role-based annotations and resource-level checks. Annotations like `@PreAuthorize` are concise for role checks but do not capture ownership rules, so I combined both approaches.
- How to extract the current user identity safely from the `SecurityContextHolder` and pass it down to repositories and services as a filter.

### Challenges

- I initially relied too much on controller annotations and missed a case where a trader could call the endpoint for another trader's trade id e.g Simon could see Joey's trades. I had to add explicit ownership checks at the start of those controller methods.
- Tests ran quickly but I found mocking authority sets across many test cases resulted in some duplicated setup code. I made a helper method to build the mock authority set for reuse.
- Early on I experienced a number of test failures caused by mock-user mismatches and incomplete test security configuration. Tests that used `@WithMockUser` were passing locally but failing in the full integration context because `TestSecurityConfi`g was not imported or because the `DatabaseUserDetailsService` behaviour differed from the mocked authorities. This required careful alignment between real user details and the mocked setups.
- Cross-cutting with the mock-user issues, `CSRF `protection caused several POST/PATCH requests in integration tests to be blocked with 403 unless I explicitly supplied CSRF tokens or disabled CSRF for the test profile. I documented a consistent approach in TestSecurityConfig so tests could opt into CSRF behaviour deterministically.

Overall, I found security configurations difficult especially to satisfy user priveleges, h2 database is used and works well, while tests also need to pass but I learned a lot.

## User access rights service layer enforcement and query scoping Sunday 19 October 2025

### Completed

- I moved ownership enforcement into the service layer where most business rules live, so controllers only orchestrate and services decide access. Changes included:
  - `TradeDashboardService.getTradesForBook` now requires a privilege check and filters results by book membership and user role.
  - `TradeService.getTradeById` performs a final check to ensure the caller can access the trade before returning it.
- Updated repository queries to accept an optional `ownerId `parameter so queries can be scoped in a single database call rather than filter results in memory.
- I addedAdded tests in TradeServiceTest to assert that when the ownerId parameter is provided, repository calls return only owned trades and that unauthorised access results in `AccessDeniedException` bubbling back to be translated into 403.
- Extended UserPrivilegeIntegrationTest with an explicit test: `traderCannotAccessAnotherTradersTrade_shouldReturn403`.

### Learned

- Placing access checks in services reduces duplication and reduces the risk of forgetting ownership checks in controllers or other callers.
- How to design repository methods that support both global and scoped queries; for example findByIdAndOwnerId and findById for admin-level access.
- The difference between throwing AccessDeniedException inside a service and allowing Spring Security to convert exceptions to 403 responses later in the filter chain.

### Challenges

- I had to refactor some `DTO mapping` logic because scoping earlier in the call chain, removed the need to fetch some linked entities. I made the mappers resilient to partial data to keep integration tests simpler.
- I created consistent error messages across service and controller layers `ApiExceptionHandler`which required standardising on a small set of exceptions and mapping them in a ControllerAdvice.
- A recurring cause of failing tests was the difference between `@WithMockUser` and the database-backed users used by `@WithUserDetails`. I needed to normalise privilege naming and ensure test users seeded into the test database used the same authority naming as the application expects. Until this was fixed, tests would show unexpected 403 responses.

## Security configuration and creating database-backed `UserDetails`Monday 20 October 2025

### Completed

- to permanantely deal with Spring security and access previleges recurring, and for the current logged in Principal user rights to be applied I Implemented `DatabaseUserDetailsService` that loads user credentials and granted authorities from the users and privileges tables.
- I had to refactor `SecurityConfig` to use the DatabaseUserDetailsService as the primary UserDetailsService and configured the password encoder, session and CSRF basics for tests and local development.
- Created TestSecurityConfig used for integration tests so I can inject mock users or use an in-memory authentication provider when I want simpler setups.
- Wrote integration tests using `@WithUserDetails` for a subset of endpoints to verify that the database-backed user details are loaded correctly and roles are interpreted as expected.

### Learned

- I learned using a database-backed UserDetailsService is important for production parity(production like environment) in integration tests and how it differs from the `@WithMockUser approach used in fast unit tests.
- How authorities and privileges map through UserDetails to Spring Security expressions; specifically how to represent complex privilege sets (for example VIEW + EDIT) as `GrantedAuthority `strings so `@PreAuthorize("hasAuthority('TRADE_EDIT')")` works.
- How to isolate security configuration for tests by providing a test profile configuration class and using @Import in test classes to avoid conflicting beans.

### Challenges

- I ran into a mismatch between the privileges stored in the database and the GrantedAuthority strings expected by the code; I added a small mapping layer in DatabaseUserDetailsService to normalise database privileges into the authority names used across the application.
- Ensuring `password encoding `compatibility for test users required seeding the test database with encoded passwords; I added a `TestDataBuilder` to create those records.

---

## Diagnosing repeated 403s and 200s where they should not occur, and fixing tests Tuesday 21 October 2025

### Completed

- I Investigated several failing integration tests that previously returned 403 when they should have returned 200 and vice versa. I triaged the cases into two classes:
- failures caused by misconfigured test security context (test wiring did not load `TestSecurityConfig`), and
- failures caused by missing or incorrect privilege checks in services.
- Corrected TestConfig imports and adjusted test application context so the test beans were consistent across integration tests.
- Updated `UserPrivilegeIntegrationTest` with clearer test names and added assertions that verify not only HTTP status codes, but also that the returned payload does not contain trades belonging to other users.
- I added further summary tests that verify common happy path scenarios across roles:
  - UserPrivilegeIntegrationTest `shouldAllowTraderToGetOwnTrades`
  - UserPrivilegeIntegrationTest `shouldDenyTraderAccessToOtherTradersTrades`
  - UserPrivilegeIntegrationTest `shouldAllowAdminFullTradeAccess`

### Learned

- How fragile integration tests can be when context configuration drifts. I now ensure TestSecurityConfig is explicitly imported in every integration test that relies on database user details.
- The value of verifying payload contents in addition to status codes. For example, a 200 response could still leak other users data unless I check the response payload.

### Challenges

-Time spent trying to solve the above issues and fix tests caused that I could not move on to the next stage as I believe security and access rights are vital

- Some tests were slow because they re-initialised the DB multiple times. I grouped similar tests and re-used a prepopulated schema where safe to reduce test time.
- A handful of edge cases remained where read-only endpoints were accessible without a role because I had left `@PermitAl`l on a controller method from an earlier iteration; I tightened those permissions.
- There were some surprising database duplication and data integrity issues when running the test suite in sequence: test data builders were inserting duplicate records across tests and some migrations did not enforce expected unique constraints in the test database. I fixed this by making the test data builders idempotent, adding unique constraints in migrations where appropriate, and cleaning the schema between certain integration test groups so seed data could be deterministic.

## Settlement SQL Injection Validations Wednesday 22- 10- 2025

### Completed

- I had to refactor the `TradeSettlementController` so that settlement instructions are now optional, matching the assignment’s requirements.
- Then removed the incorrect validation that rejected missing settlement instructions.
- I added input cleaning using `.trim()` and normalised blank-only values to `null` before saving.
- Using `Regex` I used SQL injection protection by validating user input patterns and ensuring unsafe characters cannot be persisted.
- I u[dated] the `TradeService` and `TradeRepository` to support settlement instruction retrieval and storage.
- I applied settlements same rules and preserved all role-based access control using `@PreAuthorize` annotations.

### Learned

- Implementing SQL injection prevention properly for user-provided text fields was new to me, I learned how to validate and sanitise user input safely before it reaches the database.
- I spent time reasoning through why and how to structure two separate DTOs one for incoming API requests (`AdditionalInfoRequestDTO`) and another for backend use (`AdditionalInfoDTO`). At first, it felt redundant, but I understood that it keeps the API layer clean and avoids exposing unnecessary internal fields.
- I had to think carefully about how settlement instruction data should be represented. Initially, I considered adding new fields to the Trade model, but decided it was more flexible to store them in the `AdditionalInfo` table as dynamic key-value pairs.
- The process of validating an optional field was interesting. I implemented the logic so that settlement instructions can be omitted but still cleaned and validated when provided. I am actullay proud of today's achievement especially making the system robust and against SQL injection which can have detrimental affect.
- I gained a better understanding of how trimming, null-checking, and normalisation work together to maintain both clean data and flexibility in input handling.

### Challenges

- At the beginning, deciding how to model settlement instructions was not straightforward. I considered whether they should live inside the trade table or as a separate linked entity.
- Handling optional fields required careful validation flow to prevent contradictions between the controller and service.
- SQL injection protection was challenging, especially figuring out the right balance between allowing normal punctuation and blocking dangerous characters.
- Managing two DTOs added extra complexity, especially making sure the mapping between request and response was consistent.
- Updating the service and repository to integrate the new logic took time, as I had to ensure that existing trade lookups still worked correctly.
- Overall, today’s work felt like a good combination of design thinking, validation logic, and improving application safety.

### Settlement Instructions Integration

Feature: Integration of Settlement Instructions into Trade Capture Workflow
Objective: To enable traders to capture settlement instructions directly during trade booking, reducing operational delays and errors.

### Repository Layer: Focused Queries for Faster Retrieval

I started by enhancing the AdditionalInfoRepository to add purpose-built queries that allow focused lookups instead of scanning the entire table.

The key addition was:

```
@Query("""
       SELECT a FROM AdditionalInfo a
       WHERE a.entityType = 'TRADE'
         AND a.fieldName  = 'SETTLEMENT_INSTRUCTIONS'
         AND a.active     = true
         AND LOWER(a.fieldValue) LIKE LOWER(CONCAT('%', :keyword, '%'))
       """)
List<AdditionalInfo> searchTradeSettlementByKeyword(@Param("keyword") String keyword);
```

This supports case-insensitive, partial text searches on settlement instructions an explicit business requirement for the operations team.
I used the SQL LIKE wildcard (%keyword%) so users can search for phrases like “Euroclear” even if it appears mid-sentence.

This is much more efficient than the previous findAll() and in-memory .filter() approach, which had a time complexity of O(n) on the application side.
By pushing the filtering down to the database layer, move closer to `O(log n)` due to indexed lookups (once indexing was added in Step 2).

#### Database Optimisation: Adding Indexes

Next, I added composite indexes to the AdditionalInfo entity to improve query performance.

```
@Table(
  name = "additional_info",
  indexes = {
    @Index(name = "idx_entity_type_field_entity", columnList = "entity_type, field_name, entity_id"),
    @Index(name = "idx_field_value", columnList = "field_value")
  }
)
```

The first index accelerates lookups for queries that identify records by entity type and ID (used in `findActiveOne() `and similar methods).
The second index supports LIKE-based text searches in searchTradeSettlementByKeyword().

Although the second index doesn’t make LIKE '%keyword%' truly constant time, it does help the database prune results faster, improving real-world performance.

### Service Layer Refactor: Business Logic and Validation

I refactored AdditionalInfoService to include clear, business-focused methods for settlement instructions.
Previously, the controller directly performed validation and persistence logic, which violated separation of concerns.

The refactored methods (createAdditionalInfo, updateAdditionalInfo, and the new settlement-specific methods) now handle:

    - Length checks (10–500 chars)

    - SQL injection prevention (rejects dangerous patterns like ;, --, DROP TABLE)

    - Regex-based content validation

    - Structured text support for multiline “label: value” formats

This centralises validation in the service layer, ensuring consistent rules whether data comes from the UI or an API.
Complexity-wise, validation operations are O(n) in string length, which is negligible since the field has a capped size.

#### Audit Trail Implementation

To meet the Audit Trail business requirement, I introduced a new entity:

`AdditionalInfoAudit`

This records every change to settlement instructions, storing:

    - Old and new values

    - User who made the change

- Timestamp

While not all user roles need to view this data, ADMIN and MIDDLE_OFFICE roles can retrieve it via a dedicated endpoint:
`@GetMapping("/{id}/audit-trail")`
This ensures full traceability for compliance, regulatory reporting, and internal controls.
Each change record is independent of the main AdditionalInfo table, so write operations have slightly higher cost (O(1) insert for each change), but with high accountability benefits.

#### Controller Refactor: Clean Delegation

I replaced inline controller logic with clean service-layer calls in TradeSettlementController.
For example, the `updateSettlementInstructions() `method was refactored to delegate to a single service method:

```
AdditionalInfoDTO result =
additionalInfoService.upOrInsertTradeSettlementInstructions(id, text, changedBy);
```

This “upInsert” (update-or-insert) approach simplifies handling of both new and amended settlement instructions.
It aligns with business rules allowing traders to edit settlement instructions at any stage, while maintaining audit tracking automatically. This also improves maintainability the controller is now focused solely on HTTP and role logic, not database operations.

### Mapper Sanity Check and Versioning

I revisited AdditionalInfoMapper to ensure that conversions between entities and DTOs were safe and future-proof.
I added a version field to the entity, annotated with:
`@Version`
This enables optimistic locking preventing two users from overwriting each other’s changes to the same record.
The first update succeeds and increments the version (starting at 1), while any concurrent update throws an `OptimisticLockException`.
This is an essential part of data integrity in multi-user systems like UBS and meets audit and risk management expectations.
The mapper also includes a small “sanity check” step: it ensures that null DTOs or entities don’t cause runtime exceptions and that field values are correctly defaulted where missing (e.g., `fieldType = "STRING"`).

### Repository Enhancements and Final Linkages

In TradeRepository, I confirmed the existence of:
`List<Trade> findAllByTradeIdIn(List<Long> tradeIds);`
This method allows retrieving all trades by a list of IDs, used by the searchBySettlementInstructions() controller.
It is a derived query method generated automatically by Spring Data JPA clean, type-safe, and efficient (translates to SQL WHERE trade_id IN (...)).
I also ensured that all Optional imports were correctly included (for example, in findActiveOne()), so that the service layer can handle “record not found” scenarios safely, avoiding NullPointerException.

### Reflection and Alternatives Considered

Alternative to AdditionalInfo Table Extension:I could have added settlement_instructions directly into the Trade table (simpler schema). However, I chose to use the existing AdditionalInfo extensible architecture for long-term scalability for UBS it allows storing future optional fields (like “Delivery Notes”) without schema changes.

### Audit Trail Alternatives

I considered using database triggers, but I implemented a lightweight manual table for clarity and control.

### Performance Considerations

Indexes ensure faster lookups. Without them, most repository searches would remain O(n), which isn’t scalable for production data volumes.

### Validation and Security

Validation at service level prevents SQL injection and malicious payloads early in the request cycle, which is both safer and easier to test.

### Learned

- I learned planning for betternService Centralised validation + audit logic Consistency and security
- How Controller cleaned endpoints to delegate to service Better architecture
- How adding safety checks and versioning at Mapper, prevent data corruption
- How Audit Trail Implementedimplemented via new entity and endpoint Traceability for compliance
- How versioning Enabled optimistic locking Data integrity and concurrency safety

The settlement instruction feature is now searchable, editable, auditable, and efficient, with all logic cleanly separated across the repository, service, and controller layers ready for front-end integration.

## Fixing traders accessing other traders and tightening dashboard privileges Wednesday 22 October 2025

### Completed

-I investigated and tightened TradeDashboardService to enforce both role and ownership boundaries:

- getMyTrades returns only trades owned by the resolved current trader id.
- getBookTrades checks that a caller has BOOK_VIEW privilege for that book, otherwise returns 403.
- I replaced some broad repository calls with explicit repository methods that include ownerId or bookId filters so filtering happens in the database and there is no possibility of returning data for other users in the service layer.
- I implemented integration tests specifically for TradeDashboardController including:
  - TradeDashboardIntegrationTest `shouldReturnMyTradesForTrader`
  - TradeDashboardIntegrationTest `shouldReturn403WhenTraderRequestsOtherTradersBookAggregation`
- Added guard rails in AddionalInfoservices to log suspicious attempts where user id in path does not match authenticated user and return `AccessDeniedException` quickly.

### Learned

- How to structure the service API so authorisation decisions are clear: a method either accepts an optional caller id (for admin callers) or strictly uses the authenticated principal.
- That moving filtering logic to repository queries not only improves performance but also removes accidental leaks that can happen with in-memory filtering.

### Challenges

- Rewriting queries required small database migration adjustments because I added a few composite indexes (for example `trade.owner_id` plus `maturity_date`) to keep the new queries efficient.
- I had to coordinate these schema changes with the test data builders so integration tests still passed locally.

## Security configuration & logging improvements and merging workflow, Thursday 23 October 2025

### Completed

- I made additional improvements to `SecurityConfig` by centralising authority naming and role mappings into a single helper class. This reduced chance of typos like `TRADE_VIEW` vs `TRADE-VIEW` appearing in different places.
- Added structured security logging to capture decisions that cause 403 responses. This includes user, attempted endpoint, required authority, and whether the denial came from a role check or an ownership check.
- I planned well merging steps I used while integrating security branches into main by:
  - Creating a dedicated feature branch for each security subtask.
  - When a complex conflict occurred, I created a temporary merge branch to resolve conflicts and run tests; only then did I fast-forward the feature branch or open a pull request.
- I run a focused test suite for all `UserPrivilegeIntegrationTest` and `TradeDashboardIntegrationTest classes and verified they passed after the merge conflict fixes.

### Learned

- Practical merge discipline: smaller, more frequent merges reduce conflict surface; resolving conflicts locally on a dedicated merge branch allows me to run tests before updating the feature branch.
- How to coordinate schema and code changes in the same branch so CI can apply migrations before the service starts. For local runs this meant seeding the test DB after migrations using a consistent test data builder.

### Challenges

- I found merging hard, I believe because I worry getting it wrong, so I learned to be careful when solving conflicts.
- Some merges required manual reconciliation between test configuration classes that had similar bean names; I standardised naming to avoid future conflicts.
- Ensuring that merge commit messages clearly explained why a change was kept or altered helped when I reviewed history later.
- When resolving merge conflicts I sometimes introduced test regressions by accidentally changing security-related bean imports. I adopted the pattern of a dedicated merge branch for conflict resolution so I could run the full security integration tests before finalising the merge.

## Regression testing, finalising integration test names and test coverage for 200 and 403 behaviour, Friday 24 October 2025

### Completed

- Added final integration tests that assert the combination of status codes and payload content across roles and endpoints. New test titles added:
  - UserPrivilegeIntegrationTest `shouldReturn200AndOnlyOwnedTradesForTrader`
  - UserPrivilegeIntegrationTest `shouldReturn403WhenTraderAttemptsToAmendOtherTradersTrade`
  - TradeDashboardIntegrationTest `shouldReturn200ForAdminBookAggregation`
  - TradeDashboardIntegrationTest `shouldReturn403ForTraderRequestingUnauthorizedBook`
- Completed a regression run over the backend test suite focusing on the security tests and fixed two flaky tests by refining test data setup to be deterministic.
- Wrote a one page summary for the team describing the security model and the tests to run locally. The summary includes:
  - Roles and example privileges (TRADER, SALES, MIDDLE_OFFICE, SUPPORT, ADMIN)
  - Which endpoints require ownership checks as well as which only require role checks
  - How to run the security integration tests with the TestSecurityConfig profile
- Committed and pushed the feature branch containing the final security work and opened a pull request for review.

### Learned

- How a combination of role checks and ownership checks provides both coarse and fine-grained protection and how to document those decisions to keep the rest of the team aligned.
- Naming conventions for integration tests that make the intent and expected HTTP status obvious to reviewers and that help in solving failures quickly.

### Challenges

- Balancing thorough test coverage against acceptable execution time required me to categorise tests into fast unit, medium integration and slow end-to-end tests and to make that matrix clear in the developer documentation.
- Before pushing I double checked that the pull request included a clear description of the security changes and links to the new integration tests so reviewers can focus on the security surface area.
- Final regression runs uncovered a small number of remaining data integrity problems caused by the way test setup re-used database state between test classes. Converting some test fixtures to be created and torn down per-class resolved flakiness and prevented unexpected duplicates from appearing in later tests.

## Summary and issues for 18 to 24 October 2025

Over the last seven days I focused heavily on correcting security and privilege issues that caused incorrect 200 and 403 behaviours and allowed traders to access other traders data. The work included controller and service layer changes, creating a database-backed UserDetailsService, test configuration improvements, writing clear integration tests that assert both status codes and payload content, and improving the merge workflow so conflicts are resolved safely.

Key actions to carry forward

- Keep repository queries owner-aware so filtering is done at the database level where possible.
- Maintain TestSecurityConfig and ensure it is explicitly imported by integration tests that validate database user details.
- Keep integration test names explicit about expected HTTP status codes and payload assertions so failures are quick to diagnose.
- Continue the merge discipline of frequent small merges, and use dedicated merge branches to run tests before updating feature branches.

## Settlement instructions validation refactor + audit wiring and service integration, Sunday 26 October 2025

### Completed

- I centralised settlement-instruction validation into a single field-level validator class, `SettlementInstructionValidator`.

  - Rules implemented: optional field; trim/normalise input; length 10–500 characters; forbid semicolons; detect and reject unescaped single/double quotes; deny common SQL-like tokens (for example `DROP TABLE`, `DELETE FROM`); and a final allowed-character check that permits escaped quotes and structured punctuation.
  - Error messages were improved to be user-friendly and include a copyable example for escaping quotes (for example: `Settle note: client said \"urgent\"`).

- I added a small adapter entry point to the existing validation orchestration: `TradeValidationEngine.validateSettlementInstructions(String)`. This adapter delegates to the field-level validator and returns a `TradeValidationResult` to callers, enabling consistent access to settlement validation from services that already use the engine.

- I integrated the validation into `AdditionalInfoService` at three locations: `createAdditionalInfo`, `updateAdditionalInfo` and `upOrInsertTradeSettlementInstructions` so business validation rules are followed.

  - When `fieldName` equals `SETTLEMENT_INSTRUCTIONS` the service calls the engine adapter and throws `IllegalArgumentException` with the first validation message when validation fails.
  - Non-settlement `AdditionalInfo` entries retain the previous lightweight checks so behaviour for other fields is unchanged.

- Audit-trail wiring was verified: `AdditionalInfoAudit` records old and new values, the actor (`changedBy`) is resolved from the Spring Security context with a sensible fallback, and timestamps are recorded for each change.

### Rationale & business mapping

### Completed

- Centralising validation provides a single source of truth for settlement-business rules and supports the operations requirement that settlement instructions are safe, consistent and searchable.

- Business-driven rules implemented:

  - Optional field operations can omit instructions when unnecessary.
  - Length bounds (10–500) ensures instructions are informative but bounded for storage, display and index performance.
  - Semicolon ban & SQL-token blacklist mitigate common injection patterns before persistence.
  - Escaped-quote enforcement preserves user intent and avoids ambiguity while keeping stored text safe to display.

- The validation engine allows callers to access field-level validation from the same orchestration point used for trade-level rules, avoiding awkward caller-side wiring.

### Learned

- Field-level validators are the right abstraction for free-text fields they are small, focused and easy to unit test.
- Providing a single engine entry point keeps validation discoverable and consistent across services.
- Example-based error messages materially help inexperienced users correct input (the escape example was added for exactly this reason).

### Challenges

- I was not sure if a trader will add some characters to settlement and waht it will mean to them. Balancing strict safety rules (for example forbidding semicolons) with everyday user convenience required a deliberate decision in favour of safety, plus clearer messaging to users.

### Next step plan

1. To Add unit tests for `SettlementInstructionValidator` covering:\
   - Accept: escaped quotes and valid length boundaries.\
   - Reject: unescaped quotes, semicolons, SQL-like tokens, and out-of-range lengths.
     2.To Add a small integration test for `AdditionalInfoService` asserting that invalid settlement instructions throw `IllegalArgumentException`, that valid values persist, and that audit records are created.
2. To move settlement `AdditionalInfo` creation into the same transactional boundary as `TradeService.createTrade` so trade creation and settlement persistence are atomic using same tradeid.

### Summary

These changes make settlement instructions safer to store and simpler to validate consistently across the application. They support the business goals of searchable settlement text for operations, auditable changes for compliance, and robust input validation to reduce operational risk.

## Unit and Integration tests(Settlemenent) implemented, 26 October 2025)

Completed

### 1) Unit: `SettlementInstructionValidatorTest`

- The validator enforces length boundaries, rejects unescaped quotes and semicolons, and detects blacklisted SQL-like tokens. It also accepts properly escaped quotes and emphasises user-friendly error messages.

How I implemented

- Frameworks: JUnit 5 with plain assertions.
- Tests include:
  - validLongAndShort(): asserts that strings at or inside the `10..500` boundary are accepted.
  - rejectUnescapedQuotes(): passes a string that contains an unescaped single or double quote and asserts the validator returns a failing `TradeValidationResult` with a message mentioning escaping.
  - rejectSemicolonAndSqlTokens(): asserts the presence of `;` and tokens like `DELETE FROM` cause rejection and that the returned error contains the blacklist token name.

Because:

- The validator is small and deterministic. Unit tests exercise only the validator logic so failures are fast and obvious, which helps when iterating on regexes and token lists.

### Hardest parts / what I learned

- Edge-case test data matters: some inputs that look obviously invalid at a glance can pass naive regexes (for example, `O'Connor`), which forced me to refine the escape detection logic to permit legitimate names when they are escaped correctly.
- Writing clear, actionable error messages is as important as the rule itself. I added tests that assert specific substrings in the message so future refactors don't degrade UX.

### 2) Unit: `AdditionalInfoServiceTest` (service-level)

- That `AdditionalInfoService` delegates settlement instruction checks to the `TradeValidationEngine` adapter and reacts correctly to failures and successes. It also asserts audit creation behaviour when the change is accepted.

### How I implemented it

- Frameworks: JUnit 5 + Mockito.
- Mocks: `TradeValidationEngine`, `AdditionalInfoRepository`, `AdditionalInfoAuditRepository` (or the higher-level repo that persists audit records).
- Tests include:

  - whenValidationFails_thenThrow(): stub the `TradeValidationEngine` to return a `TradeValidationResult` containing errors; call `upOrInsertTradeSettlementInstructions(...)` and assert an `IllegalArgumentException` is thrown and that no audit record was saved (verify zero interactions with audit repository).
  - whenValidationPasses_thenPersistAndAudit(): stub the engine to return an empty/OK `TradeValidationResult`; call the service and verify that `AdditionalInfoRepository.save(...)` and `AdditionalInfoAuditRepository.save(...)` were invoked with expected fields (old/new values, `fieldName` set to `SETTLEMENT_INSTRUCTIONS`). The test also verifies that the `changedBy` value is what the service was passed or resolved (in unit tests I pass a username or stub SecurityContext as needed).

- Service unit tests focus on behaviour and side effects (exceptions, repository interactions) rather than low-level parsing. By mocking the engine I isolate service logic from validator internals, making test failures indicate exactly where the problem lies.

### Challenges / what I learned

- Passing the authenticated username into the service required a conscious decision: either read `SecurityContextHolder` inside the service or pass an explicit `changedBy` parameter from the controller. I chose to read from the `SecurityContext` with defensive fallbacks, but unit tests must then either set the SecurityContext or call the service with an explicit username. I added both styles in tests to keep service usage flexible.
- Verifying that no audit is saved on validation failure is important; without that assertion a silent error would leave stale or inconsistent audit data.

### 3) Integration: `AdditionalInfoIntegrationTest`

- The full controller → service → repository flow for settlement-instruction upsert including validation, persistence and audit writing. The primary assertion is that when an authenticated user performs the request the resulting `AdditionalInfoAudit.changedBy` is populated with the real authenticated username (not a placeholder).

How I implemented it

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

Why:

- Integration tests of this shape are the most valuable safety net for refactors that change wiring between layers (for example: moving validation from inline checks to the `TradeValidationEngine`). They expose problems that unit tests can miss (for example, missing bean registration, security config mismatches, data-seed vs PK confusion, or JPQL parsing failures at context initialisation).

### Challenges / what I learned

- Bean registration: the validator initially wasn't annotated as a Spring bean which caused context failures. Integration tests fail early and loudly for these mistakes; I used the failure logs to find the missing `@Component`/`@Service` annotation.
- Security in tests: state-changing endpoints require CSRF and a suitable principal. Omitting `.with(csrf())` or using the wrong role caused 403s. I had to ensure the test principal had the appropriate authority and that CSRF was applied.
- Seed data vs PK confusion: earlier tests used the wrong identifier (a DB PK) rather than the seeded `trade_id`. Using the canonical `trade_id=200001` from `data.sql` makes the test deterministic.
- Mocks vs real components: I intentionally reduced mocking in this integration test so the real mapper, repositories and services execute. This surfaced a subtle mapper mismatch where a mocked mapper previously hid a missing `tradeId` field in serialized responses.

### Overall testing strategy and final notes

- Layered tests: small fast unit tests for validation rules; service unit tests for delegation and side-effects; a single focused integration test to validate end-to-end behaviour. This combination keeps feedback loops short while providing broad coverage for wiring and configuration issues.
- Fail-fast benefits: the integration test revealed configuration and bean errors that unit tests would not show. Running the integration test early in the iteration saved time.
- Maintainability: I added comments in the tests describing why particular setup choices were made (for example why `trade_id=200001` must be used), to prevent future accidental regressions.

## Safe startup remediation stop runtime reseed of file-backed H2, Monday 27 Oct 2025

### Completed

- Problem: `src/main/resources/data.sql` contains many non-idempotent INSERT statements using fixed IDs. With `spring.sql.init.mode=always` enabled in the runtime profile the application attempted to re-apply the seed against the file-backed H2 DB (`./data/tradingdb.mv.db`) on startup which caused unique-key violations and prevented the ApplicationContext from refreshing.

- Immediate remediation applied (27/10/2025): the runtime profile now sets `spring.sql.init.mode=never` so the `data.sql` file is not re-applied on local developer startup. The test profile retains SQL initialization for the in-memory DB so tests continue to seed deterministically.

- Swagger and the API explorer were intermittently inaccessible during development because the tightened security configuration blocked the swagger UI and the OpenAPI endpoints. I solved this by explicitly permitting the swagger UI endpoints under the local/test profile and ensuring health and swagger paths are only open in non-production profiles.

### Why I have changed the mode to 'never'

- This is a low-risk, fast fix that avoids data loss (no deletion of `.mv.db`) and prevents accidental reseed failures during normal development. It buys time to make the seed idempotent or adopt a migration tool.

### Concept: Ownership and access control enforcement (service level)

### Completed

- I added defence-in-depth ownership checks at service layer in `AdditionalInfoService` to ensure a trader cannot read or modify another trader's settlement instructions unless authorised. The service resolves the authenticated principal via `SecurityContextHolder.getContext().getAuthentication().getName()` and compares against the trade owner (example: trade id 200001 maps to `trade` row id 2000 with `trader_user_id=1002` for login `simon`).
- I kept controller-level `@PreAuthorize` annotations but moved final access decisions to the service so callers outside controllers also get consistent enforcement.

### Challenges

- Tests and local runs used different DB profiles initially which made reproducing the ownership failures harder (some runs used in-memory DBs so behaviour differed from the persisted dev DB).

### Learned

- Service-level checks, remove duplication and reduce the risk of missing ownership checks when endpoints or internal callers change. Relying solely on controller annotations is insufficient for resource ownership rules.

---

### Concept: Audit records with authenticated principal (no client-supplied actor)

### Completed

#### AdditionalInfoAudit

- Ensured `AdditionalInfoAudit` records are created by the server and the `changedBy` column is set from the authenticated principal (obtained from the SecurityContext) rather than from any client-supplied header or payload.
- Audit write occurs immediately after the `AdditionalInfo` save and is part of the same transaction. The audit row records the `additional_info` FK, timestamp, change type (CREATE/UPDATE) and `changedBy` (e.g. 'simon').

### Challenges

- Needed to normalise how the authenticated username maps to the seeded users in `data.sql` (for example `simon` -> application_user id 1002, `alice` -> id 1000) so tests and manual checks assert the expected `changedBy` values.

### Learned

- Using the server-side principal removes the risk of unauthorised access to data in audit records and keeps audit trails trustworthy.

---

### Concept: Test isolation and DB configuration changes

### Completed

- I changed test profile configuration so integration tests use an in-memory H2 database and `create-drop` lifecycle. File changed: `backend/src/test/resources/application-test.properties` (set `jdbc:h2:mem:testdb` and `spring.jpa.hibernate.ddl-auto=create-drop`).
- Annotated the affected integration test (`AdditionalInfoIntegrationTest`) with `@ActiveProfiles("test")` so the test picks up the in-memory configuration.
- I Kept `src/main/resources/application.properties` pointed at the file H2 DB for dev, with `spring.jpa.hibernate.ddl-auto=update` so local data persists between runs when desired.

### Challenges

- Running tests against the same file-backed DB caused duplicate-key failures because `data.sql` re-applied seed rows on an already-populated DB. That led to ApplicationContext load failures for tests that expected a clean schema.

### Learned

- Tests must be isolated and deterministic. Using an in-memory DB with `create-drop` ensures fresh schema and avoids `data.sql` conflicts with a persistent dev DB.

---

### Concept: Diagnostics, H2 file deletion and final fix

### Completed

-I investigated startup logs and reproduced the failure locally using `mvn -DskipTests -Dspring-boot.run.fork=false -e spring-boot:run` to force visible error stack traces.

- Observed `ScriptStatementFailedException` caused by `data.sql` INSERT statements raising H2 error 23505. The critical message: `Unique index or primary key violation` on `desk(id)`.
- I then removed the file H2 database files (`backend/data/tradingdb.mv.db` and `backend/data/tradingdb.trace.db`) to allow Spring to create a fresh DB and apply `data.sql` successfully.
- After deletion, the application initialised cleanly, seeded the database, started Tomcat and accepted requests. A sample PUT by `simon` (login `simon`, application_user id 1002) on trade `trade_id=200001` (trade row id 2000) produced a new `additional_info` entry and a corresponding `additional_info_audit` row. When Ashley (login `ashley`, application_user id 1003) and Alice (login `alice`, application_user id 1000) previously logged into the running system before the fix they could not find the expected audit entries; following the reseed and clean startup both Ashley and Alice can now retrieve the audit records (for example, the settlement change by `simon` for trade `200001` is visible in `additional_info_audit.changedBy='simon'`).

### Challenges

- Deleting DB files is destructive for dev data. Long-term solution requires making seed scripts idempotent or changing initialisation strategy.

### Learned

- Deleting DB files proves the root cause and restores a working environment quickly, but permanent remediation should avoid destructive steps for routine debugging.

---

### Concept: Transactional guarantees and verification

### Completed

- I checked the upsert + audit logic runs inside one transactional boundary so both `additional_info` and `additional_info_audit` commits together. This protects the audit from showing a change that never committed or leaving data without a matching audit.
- Verified via integration test and Hibernate SQL logs that both INSERT (or UPDATE) statements and the audit INSERT appear in the same transaction and commit.

### Challenges

- Ensuring test coverage covers both happy and error paths; added assertions that audit rows exist for CREATE and for UPDATE flows.

### Learned

- Transactions across multiple repository saves, provide atomicity for domain-level changes and their audit trails; tests should assert both data and audit persistence.

### Files changed

- `backend/src/main/java/com/technicalchallenge/service/AdditionalInfoService.java` added ownership checks, upsert logic and audit write.
- `backend/src/main/java/com/technicalchallenge/controller/TradeSettlementController.java` delegated GET/PUT to service methods and clarified behaviour.
- `backend/src/main/resources/application.properties` left file-based H2 + `spring.sql.init.mode=always` (diagnosed as part of the root cause).
- `backend/src/test/resources/application-test.properties` switched to in-memory H2 and `create-drop` for tests.
- `backend/src/test/java/com/technicalchallenge/controller/AdditionalInfoIntegrationTest.java` added `@ActiveProfiles("test")` to ensure test runs against in-memory DB.

### Future plan if time permits before deadline

1. To make `data.sql` idempoten: replace plain INSERTs with H2 `MERGE INTO ... KEY(id)` or equivalent so re-running initialisation does not fail on duplicates.
2. Alternatively, to restrict `spring.sql.init.mode` to the test profile only and set `spring.sql.init.mode=never` for `application.properties` so dev persisted DB is not re-seeded automatically.
3. To add a short DEVLOG entry documenting the destructive DB deletion and note the preferred remediation chosen (idempotent seeds or move to test-only initialisation).

### Impacts/achievement of today's work

Today’s work fixed the immediate availability problem: the service now persists settlement instructions and writes verifiable audit records with the authenticated principal (examples: `simon` changed settlement for trade `200001` → `additional_info_audit.changedBy='simon'`) and admin (`alice`) can view the audit history. The underlying root cause was non-idempotent SQL initialisation against a persistent H2 file DB; permanent fixes have been proposed above.

## Settlement instructions persisted in AdditionalInfo (Option B) 27/10/2025

### Completed

- I implemented backend support to store settlement instructions in the existing `AdditionalInfo` table rather than adding a new column on `Trade` (Option B). The implementation inserts/updates an `AdditionalInfo` row with `entity_type = 'TRADE'`, `entity_id = <tradeId>` and `field_name = 'SETTLEMENT_INSTRUCTIONS'`.
- I added the read/search/update flows in `AdditionalInfoService` so they operate on that key consistently. The update flow validates the incoming payload, upserts the `AdditionalInfo` row, and writes an `AdditionalInfoAudit` record.
- Relaxed the settlement instruction validation to accept the plus-sign token (`+`) where the business rules require it after reproducing a failure with payloads such as `t+1` (trade date reference). The regex used by `SettlementInstructionValidator` was adjusted to allow `+` in the specific token group rather than permitting all punctuation.

- Reproduced the validation failure by issuing a PUT settlement-instructions request in an integration test and observing the server returned HTTP 500 when the payload contained the `t+1` token. The validator rejected the token and the code path threw an exception. A targeted unit test validated the regex behaviour, then the regex was updated to include `+` in the allowed character class for settlement shorthand.
- The service upsert path fetches the trade via `TradeRepository.findById(tradeId)` to confirm the trade exists and to retrieve `trader_user_id` for ownership checks. The `AdditionalInfoRepository` is then used to find existing `AdditionalInfo` by `(entity_type, entity_id, field_name)` and either update or insert.
- The audit row is created by mapping the old value and new value into an `AdditionalInfoAudit` and saving it via `AdditionalInfoAuditRepository`.

- I have Chosen to avoid a schema change and data migration. This minimises impact on existing DTOs and mappers and leverages existing indexed search on `AdditionalInfo` (index `idx_ai_entity_type_name_id`).
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

- I noticed that the trader `simon` could view and create settlement instructions for another trader's trade (referred to as Joe's trade in the ticket). The integration reproduction used the seeded trade id (200002) and a PUT request to `/api/trades/200002/settlement-instructions` authenticated as `simon`, which unexpectedly succeeded before ownership checks were added.

### How it was reproduced

- I reproduced in an integration test and in manual test runs by authenticating as `simon` (credentials from `data.sql`) and issuing a PUT with a valid settlement payload. The call returned HTTP 200 and the `AdditionalInfo` row for `tradeId=200002` was created/updated; audit records showed `changedBy=simon` even though the trade's `trader_user_id` belonged to another user.

### Why it happened

- The service layer previously relied solely on controller-level method-security annotations for broad role checks and did not verify ownership at the service boundary. This left a gap where authenticated principals with allowed HTTP access could update `AdditionalInfo` for any trade if controller-level checks were not sufficiently granular.

### What I changed and how

- I injected `TradeRepository` into `AdditionalInfoService` and added a deterministic owner-lookup step: `tradeRepository.findById(tradeId)` to obtain `Trade.trader_user_id`. The current principal is resolved via `SecurityContextHolder.getContext().getAuthentication()`; the principal's login id is mapped to the `ApplicationUser` id where necessary and compared to `trader_user_id`.
- If the principal is not the owner and does not have `ROLE_SALES`, `ROLE_ADMIN` or the `TRADE_EDIT_ALL` privilege, the service now throws `AccessDeniedException` and the controller returns 403. Unit tests were updated to simulate `simon` and `joe` principals and assert the deny/allow outcomes.

### Verification

- I checked via an integration test that a PUT authenticated as `simon` to Joe's trade now returns 403. Confirmed audit records are not created for denied attempts and that allowed calls by owners or privileged roles still create audit entries with `changedBy` correctly set.

### Audit entries forced to use authenticated principal (no client-supplied changedBy)

### Completed

- Enforced server-side population of `AdditionalInfoAudit.changedBy` using the resolved principal from the `SecurityContext` instead of any client-supplied value.

### How

- In `AdditionalInfoService` the audit creation code calls `SecurityContextHolder.getContext().getAuthentication().getName()` and sets that value into `audit.setChangedBy(...)` before saving the audit entity. Unit tests that previously supplied a `changedBy` on the DTO were updated to assert the server-populated value instead.

### Why

- Relying on client-supplied `changedBy` permits spoofing of audit records. For compliance and traceability, audit actor must be resolved server-side.

### Challenges

- TeI had to adjust tests: some test fixtures assumed `changedBy` was provided by the client; these were updated to mock the security context and assert the service-set `changedBy` value.

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

### How I did this

- Used the running local server during development and opened the Swagger UI to exercise the endpoints interactively. For each endpoint tested (GET, PUT `/api/trades/{id}/settlement-instructions`, GET `/api/trades/{id}/audit-trail`) the request/response bodies were inspected in the browser and in the application logs.
- For PUT operations, the request was authenticated using the same login flow as the integration tests (login via `/api/login/<user>`). After authentication, the Swagger calls succeeded and produced audit rows; denied calls returned 403 as expected and were visible in the Swagger response panel.
- Also executed a few scripted curl requests to reproduce edge cases (e.g., `t+1`, empty payload, long payload) and confirmed server validation and error handling matched expectations.

### Why I did it

- I found using swagger easy and testing through Swagger provided a quick, human-driven verification of the full stack (controller → service → repository → audit) and made it easier to demonstrate the end-to-end behaviour to stakeholders.
- Manual testing surfaced the `t+1` rejection quickly in a realistic client flow which helped focus the validator fix to the exact token pattern operators use.

### Challenges

- Swagger UI required an authenticated session to exercise protected endpoints; needed to login first and ensure the session cookie or Authorization header was available to Swagger calls.

### Learned

- Interactive Swagger testing is an efficient sanity-check for API behaviour and complements automated tests by exercising the live stack and showing concrete request/response payloads.

### Improved 403 messaging for the audit endpoint

### Completed

- I kept the access policy unchanged: GET `/api/trades/{id}/audit-trail` remains restricted to `ROLE_ADMIN` and `ROLE_MIDDLE_OFFICE`.
- But updated `ApiExceptionHandler` so that when an `AccessDeniedException` is thrown for that specific endpoint and HTTP method (GET + path matching `/api/trades/.+/audit-trail`), the JSON 403 body uses the message: "Only Admin or Middle Office users may view audit history for trades." For other 403s the existing contextual messages remain.

### How I done it

- The `@ControllerAdvice` method `handleAccessDenied` inspects `HttpServletRequest.getMethod()` and `getRequestURI()`; if the request matches the audit GET path, the handler returns a `ResponseEntity` with the audit-specific message. Otherwise it returns the standard message previously used.
- A trade owner receiving a generic 403 message for the audit endpoint caused confusion. Clarifying the message for the audit endpoint improves operational clarity without changing the underlying security policy.

### Challenges

- Implementing a path-and-method-specific override in the global exception handler required careful checks so no other 403 messages are unintentionally altered.

### Code annotations and small cleanups

### Completed

- Added rationale comments to `ApiExceptionHandler` and `AdditionalInfoService` documenting the business reasons for the ownership checks and the audit behaviour.
- Comments reference the Simon/Joey scenario and point to the relevant requirement ticket (TRADE-2025-REQ-005) to help reviewers understand the why behind the changes.
- Embedding business rationale directly near the implementation reduces cognitive load during future maintenance and aids code review.

### CI / full test-suite verification

### Completed

### Database index and performance verification

### Completed

- Added a Postgres migration SQL file `docs/postgres/add_lower_index_additional_info.sql` which creates a functional `lower(field_value)` index (`idx_ai_field_value_lower`) on `additional_info.field_value`. The file includes guidance for verification and rollback.
- The migration SQL issues `CREATE INDEX IF NOT EXISTS idx_ai_field_value_lower ON additional_info (lower(field_value));` and documents the following verification workflow:
  - Capture a baseline plan and timing with `EXPLAIN ANALYZE` for representative queries that use `LOWER(field_value)` or `ILIKE` (for example searches for settlement text and counterparty names).
  - Apply the index in a staging or productionised testing environment during a low-traffic window.
  - Re-run the same `EXPLAIN ANALYZE` queries and compare execution time and query plan to ensure the planner uses the index and the total execution time reduces.
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

## Settlement & audit persistence incident Monday, 27 October 2025, log number 2:

### Context

Today I focused on diagnosing and fixing an issue where settlement instructions and audit records were not persisting across application restarts and therefore not visible to admin or middle-office users. The failure mode originated from SQL initialisation attempting to re-run non-idempotent seed statements (`src/main/resources/data.sql`) against an existing file-backed H2 database (`jdbc:h2:file:./data/tradingdb`) which caused primary-key violations and aborted the Spring ApplicationContext on startup.

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

## Frontend design and implementation 29/10/25

### Completed

Textarea with state (value, setValue).
DOM ref attached (textareaRef) and insertAtCursor implemented.
touched flag and onBlur are wired so errors show only after interaction.
A validator (isValid) enforces trimmed length (10–500) and forbids </>.
Inline validation message renders when invalid and touched.

Replaced the label line with:
<label htmlFor="settlement-instructions"> Settlement Instructions</label>
And replace the textarea line with:<textarea id="settlement-instructions" ref={textareaRef} value={value} onChange={handleChange} onBlur={() => setTouched(true)} />

## User Privileges 30/10/2025

### Business domain

- Business domain: trades have a business identifier (`trade_id`), metadata (trader, inputter, UTI), and optionally zero-or-more pieces of additional information (store settlement instructions in `additional_info`).
- Key rules I followed while implementing changes today:
  - Settlement instructions belong to a trade and must be persisted against the trade's business `trade_id` (AdditionalInfo.entity_id = trade.trade_id).
  - User references in trade payloads are numeric ids (backend expects Long), not usernames.
  - UX must be unambiguous: a single Save operation should persist both the trade and its settlement instructions.

---

## Security: Trade user not able to create a new trade

### Completed

- Ensured frontend requests include session credentials (axios withCredentials) so authenticated users can call protected endpoints during dev. This resolved earlier 403s observed when POSTing/PUTing trades.
- Verified that API helpers use explicit parameter types (reduces accidental implicit-any which can lead to malformed requests). Example: `createUser(user: User)`.

Code snippet:

```ts
// frontend/src/utils/api.ts
axios.defaults.withCredentials = true; // ensure session cookie is sent

export function createUser(user: User) {
  return axios.post("/users", user);
}
```

#### Learned

- 403 responses in dev were caused by missing cookies; enabling `withCredentials` and ensuring backend CORS allows credentials resolved the immediate authentication failures.
- Small TypeScript fixes (explicit parameter types) prevent malformed payloads that can cause authorization or validation errors server-side.

### Challenges

- Need to run an end-to-end test to confirm the server accepts the session cookie in all relevant endpoints (POST /trades, PUT /trades/{id}). Some environments still block credentials due to CORS config.

## Business data integrity: Settlement not persisting to AdditionalInfo (entity mapping)

### Completed

- Centralised settlement persistence in the parent modal via `saveSettlement(tradeId, text)` so settlement is saved only after the backend returns the authoritative trade business id.
- Wired `SingleTradeModal` to call `saveSettlement` after a successful trade POST/PUT to ensure AdditionalInfo.entity_id maps to the trade business `trade_id`.

Key code:

```ts
// TradeActionsModal.tsx
async function saveSettlement(tradeId: number, text: string) {
  await api.put(`/trades/${tradeId}/settlement-instructions`, { text });
}

// SingleTradeModal.tsx (after create/update)
if (saveSettlement) await saveSettlement(returned.tradeId, settlement ?? "");
```

#### Learned

- AdditionalInfo rows must reference the trade's business `trade_id` (not the DB surrogate id); waiting for the server response before persisting avoids orphan rows.
- Centralising persistence in the modal parent allows consistent snackbar/error handling and potential retry logic.

### Challenges

- The settlement PUT is reaching the network layer but I could not fully confirm DB insert in my environment—this requires running the frontend + backend locally and capturing server logs and SQL statements to confirm an upsert.

## UX: Adding settlement controls (textarea, templates, label and single-save UX)

### Completed

- Implemented an accessible, labelled settlement textarea with Clear and template insertion. Ensured the textarea exposes an explicit label to address accessibility and testing concerns.
- Removed the per-field Save button from the settlement editor to prevent user confusion; the main Save Trade button now persists settlement.

Code excerpt:

```tsx
<label htmlFor="settlement-text">Settlement instructions</label>
<textarea id="settlement-text" value={value} onChange={e => setValue(e.target.value)} />
<button type="button" onClick={() => setValue('')}>Clear</button>
```

#### Learned

- Adding an explicit label and removing duplicate Save controls significantly reduces user errors. Screen readers and automated tests rely on clear labels.
- If templates are provided via a dropdown, that dropdown must have an aria-label so automated accessibility checks and keyboard users can interact with it. Example: `<select aria-label="Settlement templates">`.

### Challenges

- Need to ensure every template control or dropdown has an accessible label the earlier UX issue reported as "Adding Settlement drop down without a label" is corrected by adding `aria-label` or a visible `<label>`.

## Data mapping: Trader / Inputter IDs and UTI handling (avoid 400 errors)

### Completed

- Reworked form fields to use `traderUserId` and `tradeInputterUserId` (numeric ids) and updated `staticStore.userValues` to supply `{ value, label }` so dropdowns pass id values.
- Coerced form values into numbers before building the DTO to match backend Long types.

Code excerpt:

```ts
// tradeFormFields.ts
{ key: 'traderUserId', label: 'Trader', type: 'dropdown', options: () => staticStore.userValues ?? [] }

// SingleTradeModal.tsx
const dto = { ...formValues, traderUserId: Number(formValues.traderUserId) || undefined };
```

#### Learned

- The backend validates types strictly; sending usernames or objects where Longs are expected leads to 400 Bad Request. Normalising dropdown values to id strings and coercing them to numbers is the safest approach.

### Challenges

- UTI handling remains unclear: some existing trades have UTIs while newly created ones do not. I need to inspect the backend trade create controller to determine whether UTI is server-generated or client-supplied.

## TypeScript, API contracts and documentation

### Completed

- Introduced `User` interface (`frontend/src/types/User.ts`) and updated API functions to use explicit types instead of `any`.
- Added `.eslintignore` to avoid linting generated files.

Code excerpt:

```ts
export interface User {
  id: number;
  username: string;
  displayName?: string;
}
export function createUser(user: User) {
  return axios.post("/users", user);
}
```

#### Learned

- Explicit types and small API fixes catch errors early and make the codebase easier to maintain.

### Challenges

- A few legacy call sites still used `any` or passed incomplete objects; these need a small follow-up pass to align types across the codebase.

# Frontend SettlementTextArea 30/10/2025

#### React & TypeScript Teachniques used which are mostly new to me and I had to research and learn

### Why use Typing Props:

- Typing props ensures initialValue is handled as a string, so your useState(initialValue) is typed and safe.
- Templates dropdown: typing each template as {value,label} makes mapping and inserting safe (no accidental undefined).
- Validation & tests: Type annotations help write tests (I know the shape) and catch type-related bugs early.
- Readability & future maintenance: other people (or future I) can see the expected inputs/outputs for the component.

### What is ?? and why I used it?

a ?? b is the nullish coalescing operator. It returns a unless a is null or undefined; otherwise it returns b.
Why that matters (example):

0 ?? 5 → 0 (keeps zero)
"" ?? "fallback" → "" (keeps empty string)
null ?? "fallback" → "fallback"
Contrast with ||:

a || b returns b when a is any falsy value (0, "", false, null, undefined).
So 0 || 5 → 5 (often wrong if 0 is a meaningful value).
In SettlementTxtArea:

const start = txtArea.selectionStart ?? txtArea.value.length;
If selectionStart is 0 (cursor at start), start will be 0 correct.
If selectionStart is null/undefined, it falls back to value.length.

### Use of Touched

- touched" is a simple boolean state that tracks whether the user has interacted with the input.]=
- Use it to control when to show validation messages and whether to let external updates overwrite the user's in-progress edits.

Why it helps:

- UX: Don’t show "This field is required" before the user tries to edit show validation only after they've touched/blurred the field.
- Safety when syncing props: If the parent updates initialValue, I usually want to update the textarea only when the user hasn't started typing. touched lets I avoid stomping their edits. e.g.
  ` setTouched(false); // treat as fresh content`

### Use of Controlled textarea:

- Keeps the visible text and your internal state in sync so the component is “controlled” (required for predictable validation, saving, tests).
- Validation & UI: because state updates on each keystroke, your validators/char-counter can read value and render live feedback or disable Save.
- Insert-at-cursor / templates: after inserting template text I also update value (same state path), so all code reads a single source of truth.
  Tests: easy to simulate user typing by calling onchange and asserting value changes.

### function handleChange

- Purpose: it reads the string from the textarea event and calls setValue(...) to update component state so the textarea can be used as a controlled component.
- Types: the parameter is typed as ChangeEvent<HTMLTextAreaElement>, which gives the handler a correctly typed event object for a <textarea>. That makes evt.currentTarget a strongly‑typed HTMLTextAreaElement and ensures TypeScript knows value is a string.
- currentTarget vs target: using evt.currentTarget.value is preferable here because currentTarget is typed as the element that the handler is bound to; evt.target can be less predictable and less well typed.
- Requirements: setValue must come from a React useState hook and ChangeEvent must be imported (or referenced as React.ChangeEvent). The handler must be wired to the textarea's onChange and the textarea should have its value prop set from state to avoid uncontrolled/controlled warnings.

### Use of text selection & Focus

`onst start = txtArea.selectionStart ?? txtArea.value.length;`

- The cursor or selection start index inside the settlement-instructions textarea.
  Trade example: a trader clicks into the instructions after “Reference: ” (cursor at position 45). start becomes 45 so an inserted template (e.g., “BENEFICIARY: …”) goes exactly after the Reference text.
- Why the fallback: if the browser can’t report selectionStart (rare), the code uses txtArea.value.length (append to the end). That prevents losing the template e.g., when the textarea isn’t fully mounted I still add the template at the end of the current settlement text.
- UX reason for ?? : preserves 0 (cursor at document start). If the cursor is at index 0 must treat 0 as valid ?? does that; || would mistakenly treat 0 as “missing” and append instead.

`const end = txtArea.selectionEnd ?? start;`

- Trade example: trader highlights the beneficiary account line and picks a “UBS Beneficiary” template. start/end enclose the highlighted range so the code replaces that entire selected text with the template (good for overwriting outdated account lines).
- If no selection (cursor only), end falls back to start so insertion replaces a zero-length selection (i.e., it just inserts at the cursor).
  Requirement fit:
- Insert-at-cursor: puts templates exactly where the trader wants (improves speed and accuracy for settlement info).
- Replace selection: lets trader select an old beneficiary block and replace it in one action (prevents duplicate/contradictory instructions).
- Safe fallback: if selection info is unavailable, still append the template instead of throwing avoids lost user action during edge cases (fast workflow, mounting timing).

`setValue((prevValue) => prevValue.slice(0, start) + text + prevValue.slice(end));`

- Uses the functional state updater to take the previous textarea string and produce a new string where the substring from index start to end is replaced by text. Implementation: prevValue.slice(0, start) keeps everything before the caret/selection, + text is the inserted template, + prevValue.slice(end) keeps everything after the selection.
- It reads the latest state (prevValue) safely even if other updates are queued important in React when multiple events/update cycles may race.
- Insert-at-cursor: inserts exactly where the trader placed the caret or replaces the highlighted selection (so a trader can replace an old beneficiary line with the UBS template in one action).
  Single source of truth: all edits go through component state so validation, save, and char-count logic read the same value.
  No duplicate instructions: replacing a selected block prevents leaving the old IBAN and adding a new one (reduces settlement errors).
  `window.requestAnimationFrame(() => { ... });`
- Schedules the inner DOM actions to run after the browser has painted the update caused by setValue. Because state updates are asynchronous and React re-renders, this ensures the textarea DOM reflects the new value before manipulating focus/selection.
  `txtArea.focus();`
- Returns keyboard focus to the textarea so the trader can continue typing immediately (good UX).
  Why it matters for settlement flow: after inserting a template the trader usually needs to fill placeholders (e.g., [NAME], [TRADE ID]); focus keeps workflow fluid and fast.

`txtArea.setSelectionRange(start + text.length, start + text.length);`

- Moves the caret to the position immediately after the inserted text (both start and end set to same index → no selection).
  Why that choice: places the insertion point so the trader can continue editing the inserted template (cursor sits right after it). If a selection was replaced, caret ends after the replacement; if inserted at caret, same result.
- Requirement linkage: helps quick post-insert edits (fill-in placeholders) and reduces clicks important for traders who need fast, accurate settlement instruction edits.

### defaultTemplates ready made array with settlements trader can edit

DefaultTemplates is a local fallback list of common settlement instruction blocks (examples) it does NOT automatically write into a trade. It’s only shown/inserted when the user chooses a template. The array exists to speed and standardize trader input; if I prefer traders always type from scratch I can remove it or replace it with an empty list or load templates from the server per-user.

const defaultTemplates = [ ... ]
A hard-coded array of objects: { value: string, label: string }.
Each entry is a ready-made instruction block (value) plus a short label for the UI.
Later: const templatesToUse = templates && templates.length ? templates : defaultTemplates;
If the parent passed templates props, use those; otherwise fall back to defaultTemplates.
Important: falling back to this array only makes template options available in the UI it does not change the trade unless the user inserts one.

#### Why have templates (benefits for trade settlement)

Speed: traders often reuse the same payment blocks (beneficiary, intermediary, charges). Templates let them insert standardized text quickly.
Accuracy & consistency: reduces manual typos and inconsistent instruction formats (helps settlement processing downstream).
UX: with insert-at-cursor, templates let a trader replace or augment specific lines (e.g., replace an outdated beneficiary) without retyping the whole block.

#### How can traders edit the default list

How editable it is depends on two things in this component:

Insertion behavior (replace vs append)
If a range is selected, the code replaces that range with the template:
prevValue.slice(0, start) + text + prevValue.slice(end)
That means the selected text is gone and the template is now in the document in its place editable like normal text.
If no selection, the template is inserted at the caret (or appended if selection info is unavailable).
Caret/focus handling (lets trader continue typing without extra clicks)
The code calls:
txtArea.focus();
txtArea.setSelectionRange(start + text.length, start + text.length);
That places the caret immediately after the inserted template and gives keyboard focus so the trader can type into the inserted template without clicking.
Important caveats for the current file state

All that works only if the textarea DOM node is attached to textareaRef (so txtArea isn't null). If textareaRef is not attached, the function falls back to appending the template and does not call focus/setSelectionRange, so the trader can still edit but must click into the field to place the caret.
The component also uses a touched flag and a syncing effect. If touched is used to prevent external updates from overwriting the field, the trader’s edits won’t be clobbered by parent initialValue updates while they’re typing.
UX / requirements mapping (trade settlement)

Fast accurate edits: replacing a highlighted beneficiary line with a standard “UBS Beneficiary” template prevents duplicate/conflicting instructions and lets the trader then fill placeholders (e.g., IBAN) immediately.
Low friction: focus + caret placement keeps the trader in flow no extra clicks required to edit after inserting a template.
Safety: because all edits go through component state, validation and the save flow will see the inserted-and-edited value before submission.

---

### Business Domain

- Business domain: trades have a business identifier (`trade_id`), metadata (trader, inputter, UTI), and optionally zero-or-more pieces of additional information (store settlement instructions in `additional_info`).
- Key rules I followed while implementing changes today:
  - Settlement instructions belong to a trade and must be persisted against the trade's business `trade_id` (AdditionalInfo.entity_id = trade.trade_id).
  - User references in trade payloads are numeric ids (backend expects Long), not usernames.
  - UX must be unambiguous: a single Save operation should persist both the trade and its settlement instructions.

---

## Security: Trade user not able to create a new trade

### Completed

- Ensured frontend requests include session credentials (axios withCredentials) so authenticated users can call protected endpoints during dev. This resolved earlier 403s observed when POSTing/PUTing trades.
- Verified that API helpers use explicit parameter types (reduces accidental implicit-any which can lead to malformed requests). Example: `createUser(user: User)`.

Code snippet:

```ts
// frontend/src/utils/api.ts
axios.defaults.withCredentials = true; // ensure session cookie is sent

export function createUser(user: User) {
  return axios.post("/users", user);
}
```

#### Learned

- 403 responses in dev were caused by missing cookies; enabling `withCredentials` and ensuring backend CORS allows credentials resolved the immediate authentication failures.
- Small TypeScript fixes (explicit parameter types) prevent malformed payloads that can cause authorization or validation errors server-side.

### Challenges

- Need to run an end-to-end test to confirm the server accepts the session cookie in all relevant endpoints (POST /trades, PUT /trades/{id}). Some environments still block credentials due to CORS config.

## Business data integrity: Settlement not persisting to AdditionalInfo (entity mapping)

### Completed

- Centralised settlement persistence in the parent modal via `saveSettlement(tradeId, text)` so settlement is saved only after the backend returns the authoritative trade business id.
- Wired `SingleTradeModal` to call `saveSettlement` after a successful trade POST/PUT to ensure AdditionalInfo.entity_id maps to the trade business `trade_id`.

Key code:

```ts
// TradeActionsModal.tsx
async function saveSettlement(tradeId: number, text: string) {
  await api.put(`/trades/${tradeId}/settlement-instructions`, { text });
}

// SingleTradeModal.tsx (after create/update)
if (saveSettlement) await saveSettlement(returned.tradeId, settlement ?? "");
```

#### Learned

- AdditionalInfo rows must reference the trade's business `trade_id` (not the DB surrogate id); waiting for the server response before persisting avoids orphan rows.
- Centralising persistence in the modal parent allows consistent snackbar/error handling and potential retry logic.

### Challenges

- The settlement PUT is reaching the network layer but I could not fully confirm DB insert in my environment—this requires running the frontend + backend locally and capturing server logs and SQL statements to confirm an upsert.

## UX: Adding settlement controls (textarea, templates, label and single-save UX)

### Completed

- Implemented an accessible, labelled settlement textarea with Clear and template insertion. Ensured the textarea exposes an explicit label to address accessibility and testing concerns.
- Removed the per-field Save button from the settlement editor to prevent user confusion; the main Save Trade button now persists settlement.

Code excerpt:

```tsx
<label htmlFor="settlement-text">Settlement instructions</label>
<textarea id="settlement-text" value={value} onChange={e => setValue(e.target.value)} />
<button type="button" onClick={() => setValue('')}>Clear</button>
```

#### Learned

- Adding an explicit label and removing duplicate Save controls significantly reduces user errors. Screen readers and automated tests rely on clear labels.
- If templates are provided via a dropdown, that dropdown must have an aria-label so automated accessibility checks and keyboard users can interact with it. Example: `<select aria-label="Settlement templates">`.

### Challenges

- Ensured every template control or dropdown has an accessible label the earlier UX issue reported as "Adding Settlement drop down without a label" is corrected by adding `aria-label` or a visible `<label>`.

### Placement / Visibility work

#### Completed

- Moved the settlement textarea and the templates dropdown from the full-width lower area into the trade modal's right column, directly under the trade header fields so they are visible without scrolling. This required adjusting `TradeActionsModal` markup and CSS grid classes.
- Updated `TradeActionsModal` layout to render the textarea within the right-hand column and ensure it receives focus when the modal opens (improves discoverability).
- Ensured the templates dropdown has an `aria-label` and is keyboard accessible.

Code excerpts (layout change, simplified):

```tsx
// TradeActionsModal.tsx (layout excerpt)
<div className="trade-modal-grid">
  <div className="left-column"> {/* trade inputs */} </div>
  <div className="right-column">
    {/* trade textboxes */}
    <SettlementTextArea initialValue={settlement} />
  </div>
</div>
```

#### Learned

- Placing the settlement editor next to the trade header fields reduces user friction: users naturally look in the same region for categorical trade metadata and related free-text fields.
- Small layout changes can have a large UX impact; verify across a few common screen sizes to avoid overflow/scroll issues inside the modal.

### Challenges

- Need to ensure CSS changes do not regress other modal content. A quick visual test across common resolutions.

## Data mapping: Trader / Inputter IDs and UTI handling (avoid 400 errors)

### Completed

- Reworked form fields to use `traderUserId` and `tradeInputterUserId` (numeric ids) and updated `staticStore.userValues` to supply `{ value, label }` so dropdowns pass id values.
- Coerced form values into numbers before building the DTO to match backend Long types.

Code excerpt:

```ts
// tradeFormFields.ts
{ key: 'traderUserId', label: 'Trader', type: 'dropdown', options: () => staticStore.userValues ?? [] }

// SingleTradeModal.tsx
const dto = { ...formValues, traderUserId: Number(formValues.traderUserId) || undefined };
```

#### Learned

- The backend validates types strictly; sending usernames or objects where Longs are expected leads to 400 Bad Request. Normalising dropdown values to id strings and coercing them to numbers is the safest approach.

### Challenges

- UTI handling remains unclear: some existing trades have UTIs while newly created ones do not. I need to inspect the backend trade create controller to determine whether UTI is server-generated or client-supplied.

## Developer Experience: TypeScript, API contracts and documentation

### Completed

- Introduced `User` interface (`frontend/src/types/User.ts`) and updated API functions to use explicit types instead of `any`.
- Added `.eslintignore` to avoid linting generated files.

Code excerpt:

```ts
export interface User {
  id: number;
  username: string;
  displayName?: string;
}
export function createUser(user: User) {
  return axios.post("/users", user);
}
```

#### Learned

- Explicit types and small API fixes catch errors early and make the codebase easier to maintain.

### Challenges

- A few legacy call sites still used `any` or passed incomplete objects; these need a small follow-up pass to align types across the codebase.

---

### 02/11/2025

## settlement-instructions editor & and wiring it into the trade save workflowCompleted

- Implemented a settlement-instructions editor in the trade modal and wired it into the trade save workflow. Settlement text supplied in the UI is now persisted to the backend as an AdditionalInfo record with entity_type = TRADE.
- Made settlement persistence non-blocking on the UI: the trade save does not wait for the settlement write; failed settlement writes surface a retry action so users can re-try the auxiliary save without losing the primary trade save.
- Added helpful UX features to the editor: templates, insert-at-cursor, validation (forbidden chars + length), a live character counter, a Clear action, and accessibility improvements for keyboard and screen-reader users.
- Hardened client-side DTO shaping to avoid 400s: coercion helpers ensure numeric IDs are sent where expected (or null), and an "Option A" quick-fix moves non-numeric identifier strings into their matching name fields before the POST/PUT.
- Ensured Axios requests include credentials/cookies (withCredentials) so session auth is preserved between frontend and backend calls.
- Normalised date/time formatting on the client to minute precision (YYYY-MM-DDTHH:MM) to reduce spurious validation/audit differences caused by seconds/milliseconds.
- Auto-generates a UTI when missing and falls back to the authenticated user for trader/inputter if those fields are blank.
- Backend: added id-first user resolution (try numeric id, then first-name, then loginId) which reduces lost user references when the frontend sends mixed formats.
- Backend: added structured @Valid error handling that returns a field -> message map (HTTP 400) to help client-side diagnostics.
- Backend: added logic to persist settlement instructions into the AdditionalInfo table during create and amend flows (update-or-create semantics).
- Rebuilt and restarted the backend to validate compilation and server startup.

### Learned

- Many of the 400/403 failures were caused by a simple mismatch between what the UI sent and what the backend expected: identifier fields sometimes contained user login strings rather than numeric IDs. Normalising input shapes early in the client prevents a large class of validation and permission issues.
- Reading values from a metadata store is not sufficient; metadata must be written explicitly when its parent entity is created or updated. The absence of a write-path was the reason settlement text was never persisted before today.
- Small UX decisions matter: making settlement saves non-blocking and adding a retry affordance preserves the primary business flow while giving users control when the supplementary save fails.
- Normalising date precision at the client edge avoids subtle issues in validation, comparison and audit trails on the server it is a low-risk, high-value change.
- Returning structured, field-level validation errors from the server massively improves the speed of debugging and the clarity of client-side error messaging.

### Challenges

- Historical data integrity: some persisted trades lacked owner references which complicated any plan to delete or clean records because multiple child tables reference trades. Referential integrity must be respected; children must be removed or migrated first, and adding cascade deletes in the schema is a risky change without controlled migrations and backups.
- Mixed-id formats from the client remain an ongoing risk. The short-term client-side heuristics and the backend id-first lookup reduce failures, but they are not a perfect substitute for ensuring UI controls always emit canonical numeric IDs.
- Balancing correctness and user experience: persisting settlement metadata synchronously ensures consistency, but it blocks on errors. I made a pragmatic choice to persist synchronously for correctness and also provide an asynchronous retry path in the UI; this is slightly more complex but keeps behaviour predictable for now.
- Type system friction: adjusting date formats and manipulating DTOs produced a few TypeScript typing issues which required careful, conservative fixes to preserve type safety while moving quickly.

## Files changed (key excerpts)

Below are short, focused snippets showing the important changes. They are intended to help reviewers jump to the right places.

### frontend/src/modal/SingleTradeModal.tsx

- Defensive coercion helper (convert non-numeric to null):

```ts
const toNumberOrNull = (v: unknown) => {
  if (v === undefined || v === null || v === "") return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
};
```

- Non-blocking settlement save called after trade save (background, retryable):

```ts
async function saveSettlementAsync(tradeId: string, text: string) {
  try {
    await props.saveSettlement(tradeId, text); // fire-and-forget from UI perspective
  } catch (e) {
    // store error state so snackbar can show Retry action
    setSettlementSaveError({ tradeId, text, error: e });
  }
}
```

- Fixing: move non-numeric trader id into name field instead of sending invalid id:

```ts
if (
  typeof editable.traderUserId === "string" &&
  toNumberOrNull(editable.traderUserId) === null
) {
  dto.traderUserName = dto.traderUserName ?? String(editable.traderUserId);
  dto.traderUserId = null;
}
```

- SQL injectionValidation for settlement text:

```ts
function validateSettlementText(text: string) {
  if (!text) return false;
  if (text.length < 10 || text.length > 500) return false;
  if (/[;'"<>]/.test(text)) return false; // forbid problematic chars
  return true;
}
```

### Normalise date formatting frontend/src/utils/dateUtils.ts

- Normalise date formatting to minute precision (used when sending to backend):

```ts
export function formatDateForBackend(
  input: Date | string | null
): string | null {
  if (!input) return null;
  const d = input instanceof Date ? input : new Date(input);
  const pad = (n: number) => String(n).padStart(2, "0");
  const year = d.getFullYear();
  const month = pad(d.getMonth() + 1);
  const day = pad(d.getDate());
  const hours = pad(d.getHours());
  const minutes = pad(d.getMinutes());
  return `${year}-${month}-${day}T${hours}:${minutes}`;
}
```

### frontend/src/utils/tradeUtils.ts

- convertEmptyStringsToNull now includes settlement and UTI fields to avoid accidentally sending empty strings:

```ts
const keysToNullIfEmpty = [
  "utiCode",
  "settlementInstructions",
  "traderUserId",
  "tradeInputterUserId",
  "traderUserName",
  "inputterUserName",
];
```

### persist settlement instructions into AdditionalInfo immediately after trade save:

backend/src/main/java/com/technicalchallenge/service/TradeService.java

```java
if (tradeDTO.getSettlementInstructions() != null && !tradeDTO.getSettlementInstructions().trim().isEmpty()) {
    AdditionalInfo settlementInfo = new AdditionalInfo();
    settlementInfo.setEntityType("TRADE");
    settlementInfo.setEntityId(savedTrade.getTradeId());
    settlementInfo.setFieldName("SETTLEMENT_INSTRUCTIONS");
    settlementInfo.setFieldValue(tradeDTO.getSettlementInstructions());
    settlementInfo.setFieldType("STRING");
    settlementInfo.setActive(true);
    settlementInfo.setCreatedDate(LocalDateTime.now());
    settlementInfo.setLastModifiedDate(LocalDateTime.now());
    settlementInfo.setVersion(1);
    additionalInfoRepository.save(settlementInfo);
}
```

- Amend: update-or-create semantics for settlement AdditionalInfo when a trade is amended:

```java
Optional<AdditionalInfo> existingInfoOpt = additionalInfoRepository.findActiveOne(
        "TRADE", savedTrade.getTradeId(), "SETTLEMENT_INSTRUCTIONS");

if (existingInfoOpt.isPresent()) {
    AdditionalInfo existingInfo = existingInfoOpt.get();
    existingInfo.setFieldValue(tradeDTO.getSettlementInstructions());
    existingInfo.setLastModifiedDate(LocalDateTime.now());
    existingInfo.setVersion(existingInfo.getVersion() == null ? 1 : existingInfo.getVersion() + 1);
    additionalInfoRepository.save(existingInfo);
} else {
    AdditionalInfo newInfo = new AdditionalInfo();
    newInfo.setEntityType("TRADE");
    newInfo.setEntityId(savedTrade.getTradeId());
    newInfo.setFieldName("SETTLEMENT_INSTRUCTIONS");
    newInfo.setFieldValue(tradeDTO.getSettlementInstructions());
    newInfo.setFieldType("STRING");
    newInfo.setActive(true);
    newInfo.setCreatedDate(LocalDateTime.now());
    newInfo.setLastModifiedDate(LocalDateTime.now());
    newInfo.setVersion(1);
    additionalInfoRepository.save(newInfo);
}
```

- User resolution: prefer numeric ID, then try first-name, then loginId fallbacks:

```java
if (tradeDTO.getTraderUserId() != null) {
    applicationUserRepository.findById(tradeDTO.getTraderUserId())
            .ifPresent(trade::setTraderUser);
} else if (tradeDTO.getTraderUserName() != null) {
    // try first-name then loginId fallbacks
    Optional<ApplicationUser> userOpt = applicationUserRepository.findByFirstName(firstName);
    if (!userOpt.isPresent()) {
        applicationUserRepository.findByLoginId(name).ifPresent(trade::setTraderUser);
    }
}
```

### backend/src/main/java/com/technicalchallenge/controller/ApiExceptionHandler.java

- Return field-level validation errors for @Valid failures (HTTP 400):

```java
Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(err -> err.getField(), err -> err.getDefaultMessage(),
                (a, b) -> a + "; " + b));

body.put("message", "Validation failed");
body.put("errors", fieldErrors);
```

### backend/src/main/java/com/technicalchallenge/repository/AdditionalInfoRepository.java

- Qquery used by the amend flow:

```java
Optional<AdditionalInfo> findActiveOne(String entityType, Long entityId, String fieldName);
```

## Learned

- End-to-end verification pending: Created a trade in the UI and provide either the POST payload / network trace or the new tradeId so I can check the DB row and confirm that:

  - trader_user_id and trade_inputter_user_id were persisted correctly,
  - an AdditionalInfo record with field_name = SETTLEMENT_INSTRUCTIONS was created for that trade.

- The frontend helper `convertEmptyStringsToNull` was broadened to include settlementInstructions and some id/name fields this is convenient but can cause required fields to be nulled if the UI accidentally sends empty strings.

- Deletion/cleanup: some historic trades lack owner references which prevents simple DELETE cascades. I prepared safe SQL recipes (delete child cashflows then legs then the trade) in the project notes to be run with backups and in small batches.

## How I verified

- Rebuilt backend with `mvn -DskipTests package` and confirmed server started (Tomcat on port 8080) and logs showed SQL/hibernate activity. Build produced "BUILD SUCCESS".
- Manually inspected modified frontend files and added basic guards & unit-safe type casts to resolve TypeScript issues introduced while changing date formatting.

---

# Global exception handler&making validation more consistent 03/11/2025

Today I focused on making validation more consistent and visible, tidying some integration tests, and improving how exceptions are returned to the client so front-end tools (Swagger, UI) get clear messages instead of stack traces.

### Task: Add / tidy Global exception handler

### Completed

- Created and then annotated `GlobalExceptionHandler` to return a consistent minimal JSON payload: `{ timestamp, status, message }` for errors.

### Learned

- Returning a single, predictable JSON shape helps the front end handle errors consistently.
- Mapping all `RuntimeException` to 400 is convenient but can hide server-side causes (for example, commit-time UnexpectedRollbackException). The handler should be specialised over time (e.g. add `MethodArgumentNotValidException` handlers) rather than broad.

### Challenges

- There is a trade between as a developer reading detailed stack traces for debugging and providing users a clean, safe message. I left the handler conservative (minimal info).

### Task: Investigate and document validation behaviour (DTOs, validators, engine)

### Completed

- Read and analysed the relevant DTOs and validators (`TradeDTO`, `TradeLegDTO`, `TradeLegValidator`, `TradeDateValidator`, `TradeValidationEngine`).
- Confirmed the current business rule: trades must have two legs, and each leg must have a maturity date defined and the two leg maturities must match exactly.
- Checked that cashflow generation still used the top-level trade maturity in some places an inconsistency to address.
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

### Task: Doc updates and error log consolidation

### Completed

- Wrote and appended detailed entries to `Development-Errors-and-fixes.md` and created a shorter `erors.md` synopsis listing problems and resolutions encountered during the day.

### Learned & Challenges

- Keeping a concise timeline and linking each change to its root cause (test failure, runtime error, or data mismatch) helps reviewers understand why a change was necessary.

## Noticed runtime error and trade majurity dates problem at frontend

- When calling the dashboard summary endpoint via Swagger, the client received a 400 with the message: "Transaction silently rolled back because it has been marked as rollback-only". This is the commit-time symptom (UnexpectedRollbackException) and not the original root cause. The original exception occurred earlier in the request and must be located in the server logs to fix the underlying problem.

- I did not modify `TradeDashboardService`. The above behaviour is caused by an exception earlier in the same transaction (for example a validator, a repository query, or a data mapping error). The global exception handler currently maps runtime exceptions to 400 and so the rollback message was returned to the client instead of the original cause.

## Detailed examples, test data and step-by-step actions

Below I add concrete examples from the codebase and the JSON payloads I used while applying and integrating validation rules into `TradeService`.

### Fixes and improvements I made

- Trade leg validation (logic found in `TradeLegValidator`):

  - Key checks enforced by the validator:

    - Ensured there are at least two legs:

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

    - Ensured the two maturities are identical:

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

  - Throwing `ResponseStatusException` inside a `@Transactional` service method will mark the transaction rollback-only; that leads to an `UnexpectedRollbackException` at commit time if something else catches and masks the original exception.

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

   - Fix: used `TransactionTemplate` to commit seed data, ensuring it is visible to MockMvc calls.

### Finding during debugging

- When a validation failed inside `TradeService.createTrade(...)` the code now throws a `ResponseStatusException` with a message that concatenates the validation errors. Example message: `Validation failed: Both legs must have a maturity date defined; Book not found`.

- Because the service method is `@Transactional`, that exception causes the transaction to be marked rollback-only. The later `UnexpectedRollbackException` can obscure the original validation message if the global exception handler is too broad; that's why I added comments in `GlobalExceptionHandler` recommending more specific handlers and suggested returning an `errors` array for field-level problems.

### ADDED: Propagation CLASS

is a small Spring enum used by @Transactional to tell Spring how the annotated method should behave relative to any existing transaction. Each value controls whether the method should join the caller’s transaction (REQUIRED), always runs in a new independent transaction (REQUIRES_NEW), run non‑transactionally and suspend any existing transaction (NOT_SUPPORTED), or one of several other semantics (e.g., SUPPORTS, MANDATORY, NEVER, NESTED). You “need” it whenever I care whether work should be atomic with the caller (default REQUIRED) or isolated/suspended; picking the right propagation avoids problems like the rollback-only UnexpectedRollbackException (e.g., marking dashboard reads NOT_SUPPORTED prevents inner failures from marking a transaction to roll back). It’s safe to use the enum is just configuration but the choices have real consistency implications: REQUIRES_NEW can produce independently committed side‑effects, NOT_SUPPORTED removes transactional guarantees for that call, and NESTED requires a transaction manager that supports savepoints. In short, use propagation deliberately: annotate specific methods (not whole classes) with the propagation that matches the business consistency.

### Backend routing investigation & docs fixes 04-11-2025

### completed

- Investigated a 400 Bad Request observed when calling `/api/trades/search` and `/api/trades/rsql`.
- Discovered that `TradeController` had `@GetMapping("/{id}")` which caused Spring to interpret the literal `search` segment as the `{id}` path variable (NumberFormatException -> 400).
- Updated developer HTTP examples in `docs/trades.http` to use the correct endpoints: `/api/dashboard/search` and `/api/dashboard/rsql`.

Code snippets / evidence

```java
// backend/src/main/java/com/technicalchallenge/controller/TradeController.java
@RequestMapping("/api/trades")
public class TradeController {
  @GetMapping("/{id}")
  public ResponseEntity<TradeDTO> getTradeById(@PathVariable Long id) { ... }
}

// backend/src/main/java/com/technicalchallenge/controller/TradeDashboardController.java
@RequestMapping("/api/dashboard")
public class TradeDashboardController {
  @GetMapping("/search")
  public ResponseEntity<List<TradeDTO>> searchTrades(...) { ... }
  @GetMapping("/rsql")
  public ResponseEntity<?> searchTradesRsql(@RequestParam String query) { ... }
}
```

DB / manual checks used

```sql
-- verify trades exist for bookId sample used in docs
SELECT id, trade_ref, book_id, trade_status

```

### completed

- Investigated a 400 Bad Request observed when calling `/api/trades/search` and `/api/trades/rsql`.
- Discovered that `TradeController` had `@GetMapping("/{id}")` which caused Spring to interpret the literal `search` segment as the `{id}` path variable (NumberFormatException -> 400).
- Updated developer HTTP examples in `docs/trades.http` to use the correct endpoints: `/api/dashboard/search` and `/api/dashboard/rsql`.

Code snippets / evidence

```java
// backend/src/main/java/com/technicalchallenge/controller/TradeController.java
@RequestMapping("/api/trades")
public class TradeController {
  @GetMapping("/{id}")
  public ResponseEntity<TradeDTO> getTradeById(@PathVariable Long id) { ... }
}

// backend/src/main/java/com/technicalchallenge/controller/TradeDashboardController.java
@RequestMapping("/api/dashboard")
public class TradeDashboardController {
  @GetMapping("/search")
  public ResponseEntity<List<TradeDTO>> searchTrades(...) { ... }
  @GetMapping("/rsql")
  public ResponseEntity<?> searchTradesRsql(@RequestParam String query) { ... }
}
```

DB / manual checks used

```sql
-- verify trades exist for bookId sample used in docs
SELECT id, trade_ref, book_id, trade_status
FROM trades
WHERE book_id = 1000
LIMIT 5;
```

### learned

- Literal path segments can be shadowed by a path-variable mapping on the same base route; prefer explicit mappings for reserved words such as `/search` or put search endpoints on a distinct base (dashboard) to avoid collisions.

### challenges

- Avoiding changes to public controller APIs without stakeholder sign-off; I fixed docs and left an optional compatibility mapping as a documented next step.

---

### Frontend lint/type/build fixes (Trade Dashboard, modals, pages)

### completed

- Resolved a set of frontend build-blocking ESLint/TypeScript errors so `pnpm --prefix frontend build` completes.
  - Fixed react/no-unescaped-entities errors by escaping quotes/apostrophes in JSX text nodes (used `&quot;` and `&apos;`).
  - Replaced explicit `any` usages in `TradeDashboard.tsx` with small local types (`WeeklyComparison`, `DashboardSummary`, `DailySummary`) to satisfy `@typescript-eslint/no-explicit-any`.
  - Removed unused helper functions from `HomeContent.tsx` to eliminate `no-unused-vars` warnings.
  - Fixed a JSX parsing issue where a `.map(...)` expression had been accidentally split outside the JSX expression (syntax error) restored a valid JSX expression.
  - Migrated `.eslintignore` ignore patterns into `frontend/eslint.config.js` `ignores` setting to remove runtime ESLint warning about `.eslintignore` deprecation.

Files changed (high level)

- `frontend/src/pages/TradeDashboard.tsx` added small types and adjusted callbacks.
- `frontend/src/modal/TradeActionsModal.tsx` escaped quotes in copy and added inline comments.
- `frontend/src/pages/TraderSales.tsx` escaped quotes/apostrophes and added comments.
- `frontend/src/components/HomeContent.tsx` removed unused nav helpers and added explanatory comment.
- `frontend/eslint.config.js` added `ignores` property mirroring `.eslintignore`.

Key code snippets

```ts
// frontend/src/pages/TradeDashboard.tsx (excerpt)
type WeeklyComparison = { tradeCount?: number };
type DashboardSummary = {
  weeklyComparisons?: WeeklyComparison[];
  notionalByCurrency?: Record<string, number | string>;
  // other optional fields used by the UI
};

// usage in reduce
const totalThisWeek = summary?.weeklyComparisons?.reduce(
  (a: number, b: WeeklyComparison) => a + (b.tradeCount || 0),
  0
);
```

Escaping example

```html
<p>Click &quot;Book New&quot; above to create a trade.</p>
```

ESLint config snippet

```js
// frontend/eslint.config.js (flat config)
export default [
  {
    ignores: [
      "node_modules/**",
      "dist/**",
      ".vite/**",
      "coverage/**",
      "playwright-report/**",
    ],
  },
  // ... existing rules
];
```

Verification / data used

- Executed `pnpm --prefix frontend build` and confirmed build completed; Vite produced the `dist/` bundle. The build run shows only non-blocking warnings for unused variables and a react-hooks suggestion.
- Tested interactive pages locally in the dev server and spot-checked the Trade Dashboard, TraderSales and Trade Actions modal to ensure copy still renders and behaviour unchanged.

Example API calls used while testing UI flow

```http
# Use dashboard search (correct endpoint) to drive UI data
GET http://localhost:8080/api/dashboard/search?bookId=1000&limit=10

# Fetch a trade used in UI tests
GET http://localhost:8080/api/trades/1001
```

### learned

- Escaping characters in JSX is the low-risk path when copy contains quotes/apostrophes; it's better to wrap strings in JavaScript variables or use templates for longer copy.
- Conservative, local TypeScript types are preferable to `any` inside UI components they remove lint friction while keeping code flexible.

### challenges

- Carefully editing copy-heavy files risked introducing parsing problems (I hit a split-`map` JSX issue which I fixed). Always run the linter/build after textual edits.

---

### Frontend Cashflow rate handling and UI presentation 05-11-2025

### completed

- Fixed incorrect use of demo fallback rates in cashflow calculations and presentation.
  - Previously, falsy checks like `if (!rate)` treated legitimate numeric `0` as missing and triggered a demo fallback.
  - Replaced permissive checks with explicit null/undefined checks so `0` is preserved as a valid rate.

Problem

```ts
// treats 0 as missing
if (!rate) {
  useDemoFallback = true;
}
```

Fix applied

```ts
// explicit null/undefined check
if (rate === null || rate === undefined) {
  useDemoFallback = true;
} else {
  displayedRate = Number(rate);
}
```

### verification / data used

- Opened Trade Actions modal for sample trade `T-1001` (sample payload available in `target/classes/sample-trade-post-payload.json`) and verified cashflow rows now display numeric values as expected.
- DB query used during manual validation:

```sql
SELECT leg_id, rate_source, rate_value
FROM cashflow_legs
WHERE trade_id = (
  SELECT id FROM trades WHERE trade_ref = 'T-1001'
);
```

### learned

- Be explicit when testing for missing numeric values. `0` is a valid number and should not trigger 'missing' logic. This is a common pitfall when migrating JS code from permissive checks to stricter validation.

### challenges

- The UI shows demo markers when `rate_source = 'demo'` the product decision is to show a visible marker and not silently apply demo values; aligning backend flags and UI display needed a careful UX decision and small UI copy update.

### Frontend pages, navigation and UI elements (complete list)

Below is a concise inventory of the frontend pages, top-level navigation pieces, common buttons/actions, and visual components (charts/tables/modals) observed in `frontend/src`. This is intended for future developers can quickly find the UI pieces touched during the debugging session.

#### Pages (frontend/src/pages)

- `App.tsx` / `AppRouter.tsx` application entry and route wiring.
- `Main.tsx` main landing page container used after login.
- `Admin.tsx` admin area page.
- `MiddleOffice.tsx` middle-office ops page.
- `Profile.tsx` user profile management.
- `SignIn.tsx`, `SignUp.tsx` auth pages for sign in/up.
- `Support.tsx` support UI.
- `TradeBooking.tsx` trade booking flow (create/edit trades).
- `TradeDashboard.tsx` dashboard view with charts, summary panels and top-level trade search (this is where chart components and dashboard summaries are rendered).
- `TraderSales.tsx` trader / sales view used to show trader-level metrics and lists.

#### Top-level components (frontend/src/components)

- `Navbar.tsx` / `Sidebar.tsx` / `Layout.tsx` primary navigation and page scaffolding; contains links to Dashboard, Booking, Trader Sales, Admin etc.
- `AGGridTable.tsx` grid/table wrapper (AG Grid) used for trade lists / blotters.
- `Button.tsx`, `Dropdown.tsx`, `Input.tsx`, `Label.tsx` small form/UI primitives used across pages.
- `HomeContent.tsx` homepage/micro-dashboard content (edited during lint fixes).
- `TradeDetails.tsx`, `TradeLegDetails.tsx` per-trade detail panels used inside modals or detail pages.
- `Snackbar.tsx` / `LoadingSpinner.tsx` UX helpers for notifications and loading states.

#### Modal components (frontend/src/modal)

- `TradeActionsModal.tsx` the modal for trade-level actions and inspection (where cashflow/currency/rate presentation occurs).
- `CashflowModal.tsx` dedicated cashflow view.
- `SingleTradeModal.tsx`, `TradeBlotterModal.tsx` single-trade and blotter modals used to view/edit trades.
- `UserActionsModal.tsx`, `AllUserView.tsx`, `SingleUserModal.tsx`, `StaticDataActionsModal.tsx` user and static-data related modals.

#### Charts and visualisations

- Dashboard uses charting components for quick summary and visualisation. The project uses Recharts (observed in TradeDashboard) to render common charts such as:
  - Pie charts (notional distribution by currency / instrument)
  - Bar charts (daily/weekly counts, volumes)
  - Line/area charts for trends (where used)
- AG Grid is used for tabular trade lists (blotters) and supports sorting/filtering/pagination.

#### Common buttons & actions

- "Book New" / "New Trade" starts trade booking flow (`TradeBooking.tsx`).
- "Search" / filter controls wired into Dashboard/AGGridTable search and backend `/api/dashboard/search` endpoint.
- "Export" / "CSV" / "Download" table export actions.
- Per-row actions: "Edit", "View", "Delete", "Copy" commonly exposed in table action columns and modals.

### Trade Dashboard components, elements and techniques used

This section describes the primary UI pieces on the Trade Dashboard page (`frontend/src/pages/TradeDashboard.tsx`), the components that compose them, and the front-end techniques and patterns used across the page.

1. Page purpose and high-level layout

- Purpose: surface high-level trade KPIs, visual summaries (charts), filter/search controls and a trade blotter so users can both explore aggregated metrics and inspect individual trades.
- Layout: composed using the global `Layout` (header/navbar/sidebar) with a page header (title + actions), a top row of KPI tiles, a mid section of charts, and a lower blotter table.

2. Filter and search controls

- Components: `Input.tsx`, `Dropdown.tsx`, date-range pickers (composed from `Input`/third-party), and an explicit Search button.
- Behaviour: filters update local state and trigger dashboard queries. Common UX patterns used:
  - Debouncing user input to reduce network calls.
  - Persisting filter state in local component state or `stores` so filters survive navigation.
  - Mapping UI-friendly names (e.g., book name) to backend parameters (bookId) before sending requests.

3. KPI tiles and summary panels

- Components: small presentational tiles (could be implemented inline or as small `Card` components) that show numeric KPIs (trade count, notional totals, active traders).
- Techniques:
  - Use `useMemo` for derived values (e.g., sum of an array) to avoid re-computation on unrelated renders.
  - Small sparklines or mini-charts included inside tiles using Recharts and `ResponsiveContainer` for compact visuals.

4. Charts and visualisations

- Library: Recharts is used for PieChart, BarChart and Line/Area charts (this is implemented in `TradeDashboard.tsx` alongside small helper components).
- Data handling:
  - Prefered aggregated data from the backend where possible (backend returns per-day counts or per-currency totals) to avoid heavy client-side computations.
  - When client-side aggregation is needed, `useMemo` and small reducers are used to calculate chart data from raw results.
- Performance:
  -To wrap chart configuration in `useMemo`.
  - To use `ResponsiveContainer` to make charts resize smoothly.

5. Table / Blotter

- Component: `AGGridTable.tsx` (an AG Grid wrapper) is used as the main trade table/blotter for performant rendering of many rows.
- Features: column definitions, sorting, client-side / server-side pagination, per-row action buttons (Edit/View/Delete), row selection.
- Integration pattern: table accepts `rowData` and `columnDefs` from the page; action buttons call handlers that open modals or navigate to editing routes.

6. Modals and details

- Key modals: `TradeActionsModal.tsx` (inspecting actions/cashflows), `CashflowModal.tsx`, `SingleTradeModal.tsx`.
- Techniques:
  - Controlled modal visibility via component state (isOpen, selectedTradeId).
  - Lazy-loading detailed payloads (call `/api/trades/{id}` only when requested) to save bandwidth.
  - Explicit handling for numeric fields (do not use coarse falsy checks like `if (!rate)`; use `rate === null || rate === undefined` to preserve numeric `0`).

7. Buttons, export and row actions

- Buttons implemented with `Button.tsx` for consistent styling and behavior.
- Export/CSV: either AG Grid's client-side CSV export or a backend export endpoint is used for larger datasets.

8. State management, data fetching and error handling

- Data fetching: page calls `/api/dashboard/search` for dashboard data and `/api/trades/{id}` for details.
- State: transient UI state in `useState`; session/user or cross-page state in `frontend/src/stores`.
- Error handling: errors map to `Snackbar.tsx` notifications; loading state uses `LoadingSpinner.tsx`.

9. Types, lint and code hygiene

- Types: local TypeScript types (e.g., `WeeklyComparison`, `DashboardSummary`) used to avoid `any` and satisfy lint rules.
- Lint rules enforced: react/no-unescaped-entities, @typescript-eslint/no-explicit-any, react-hooks/exhaustive-deps.

10. Styling and responsive layout

- Styling: Tailwind CSS utilities plus `index.css` for global resets.
- Responsive strategies: charts in `ResponsiveContainer`, grid/flex layout for panels, AG Grid responsive settings.

11. Performance considerations and optimisations

- Use AG Grid for large lists.
- Memoize computed chart data and handlers with `useMemo` and `useCallback`.
- Debounce filter inputs.

12. Developer notes and where changes were made

- Files I edited during fixes: `frontend/src/pages/TradeDashboard.tsx`, `frontend/src/modal/TradeActionsModal.tsx`, `frontend/src/pages/TraderSales.tsx`, and `frontend/src/components/HomeContent.tsx`. Those edits are small: escaping JSX characters, adding local types, adjusting conditional checks for numeric values, and removing unused functions.

## Frontend Settlemenet UI improvements & Modal wiring and persistence changes, 29 OCtober 2025

## Completed

1. Settlement editor (UI)

   - File: `frontend/src/modal/SettlementTextArea.tsx`
   - Implemented a controlled textarea component that supports:
     - an initial value prop, templates (quick-insert), and insert-at-cursor behaviour;
     - local touched state and inline validation (10–500 characters, forbids angle brackets);
     - a character counter, Save and Clear actions, and an isSaving indicator.
   - Small correctness fix (validation): the component now trims input first and applies both length and forbidden-character checks to the trimmed value. This prevents padding with whitespace from bypassing length validation.

2. Modal wiring and persistence

   - File: `frontend/src/modal/TradeActionsModal.tsx`
   - Wired `SettlementTextArea` into the trade modal. Behaviour added:
     - On trade load, the parent fetches the current settlement instructions via GET `/trades/{id}/settlement-instructions`.
     - The parent passes an `onSave` callback which builds an AdditionalInfoRequestDTO and PUTs it to `/trades/{id}/settlement-instructions`.
     - Success and failure are surfaced via the existing Snackbar UI; 404 for GET is treated as "no instructions" (empty editor).
   - Developer comments were added to the modal describing the business mapping (capture at booking, editable during amendments, server-side audit/ownership).

## I attempted frontend UI testing but could not finish

- Unit tests for `SettlementTextArea`: I added a Vitest + React Testing Library test (`frontend/src/__tests__/SettlementTextArea.test.tsx`) covering: initial value rendering, template insertion, validation and save flow. I that removed test file was later as I feel I do not have enough time to learn about Vtest. I wills till do this after deadline to practise and learn frontend testing.

## Files changed in this session

- frontend/src/modal/SettlementTextArea.tsx controlled settlement editor + validation fix
- frontend/src/modal/TradeActionsModal.tsx fetch + save wiring, comments
- frontend/.eslintignore and lint script change restrict lint to source files

### How I verified

- Code review of the changed files to ensure validation and save flows are using trimmed values and that axios error handling treats 404 as "not found" for GET.

# Non-standard Settlement-export, settlement ADMIN/MO deletion, Risk Net Exposure & Application events & listener 06-11-2025

## Summary

Today I implemented and stabilised the settlement-export and settlement-deletion flows, wired the real risk exposure value through to the dashboard, and fixed several authorization and front-end issues that blocked reliable CSV downloads. The key deliverables:

- Settlement CSV export: added `nonStandardOnly` and `mineOnly` behaviors, non-standard detection heuristic, consistent CSV boolean formatting, and frontend safeguards to avoid saving HTML as CSV.
- Deletion of settlement AdditionalInfo: added safe delete-by-id and delete-by-trade logic (soft-delete + audit), fixed MO authorization mismatch so Middle Office users can perform allowed deletes.
- Risk exposure: removed a hard-coded dashboard placeholder and wired the backend-provided risk value to the dashboard UI with a small formatter.
- Dev UX: added Vite dev proxy guidance and client-side checks to avoid index.html being downloaded as CSV.

## Task 1 Settlement non-standard exporting

Goal: allow users to export settlement instructions CSV with two useful filters: only non-standard settlement rows and only "my" trades for traders (but allow elevated roles to bypass).

Five steps taken:

1. Requirement & API surface (define)

- Decided endpoint: GET `/api/trades/exports/settlements`.
- Query parameters:
  - `nonStandardOnly` (boolean) return only settlement rows that match a "non-standard" heuristic.
  - `mineOnly` (boolean) when true, restrict to trades owned by the current authenticated trader. Elevated roles bypass this filter.

2. Backend: wire query params and principal extraction

- Extended `TradeController.exportSettlementCsv(nonStandardOnly, mineOnly)` to accept both params.
- Extracted current principal and authorities from SecurityContext:
  - `Authentication auth = SecurityContextHolder.getContext().getAuthentication();`
  - `principalName = auth.getName();`
  - iterated `auth.getAuthorities()` to set `hasElevatedRole` (explicit loop for clarity).
- Behavior: if `mineOnly && !hasElevatedRole` then filter trades in-memory where `trade.traderUser.loginId.equals(principalName)`.

3. Non-standard detection heuristic (calculate per-row)

- Implemented a small heuristic function used while preparing CSV rows. The heuristic is intentionally conservative and simple (helps ship quickly):
  - Example criteria used (pseudocode):
    - If the settlement field value length > 200 characters → non-standard.
    - If the field contains characters outside a safe charset (non-printable) → non-standard.
- The heuristic runs while building CSV rows and sets a boolean `nonStandard` per row.

4. CSV formatting & content-type contracts

- CSV writes the `nonStandard` column using textual lowercase booleans `"true"` or `"false"` so downstream tools get consistent textual booleans.
- Set `Content-Type: text/csv;charset=UTF-8` on the response and streamed the CSV through Spring MVC to return as an attachment.

5. Frontend integration & safety checks

- Updated `frontend/src/components/DownloadSettlementsButton.tsx`:
  - Defaulted the button's URL to `?nonStandardOnly=true&mineOnly=true` so traders get only flagged rows by default.
  - Added request header: `Accept: "text/csv, text/plain, */*"`.
  - After response received, inspect `Content-Type`. If it contains `text/html`, abort and show an error (this prevents saving dev-server index.html as CSV if the proxy is misconfigured).
- Added a Vite dev proxy (`vite.config.ts`) to forward `/api` to `http://localhost:8080` to prevent local dev server HTML being returned to API requests.

Representative code snippets (from the work done)

- Frontend: Accept header + content-type check (short excerpt)

```ts
// DownloadSettlementsButton.tsx (excerpt)
const res = await fetch(endpoint, {
  headers: { Accept: "text/csv, text/plain, */*" },
  credentials: "include",
});
const contentType = res.headers.get("Content-Type") ?? "";
if (contentType.includes("text/html")) {
  throw new Error(
    "Server returned HTML instead of CSV (check backend/dev-proxy)."
  );
}
```

- Backend: mineOnly & authority extraction (pseudocode)

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String principal = auth.getName();
boolean hasElevatedRole = false;
for (GrantedAuthority ga : auth.getAuthorities()) {
    String r = ga.getAuthority();
    if ("ROLE_ADMIN".equals(r) || "ROLE_MIDDLE_OFFICE".equals(r)) {
        hasElevatedRole = true; break;
    }
}
if (mineOnly && !hasElevatedRole) {
    trades = trades.stream()
          .filter(t -> principal.equals(t.getTraderUser().getLoginId()))
          .collect(Collectors.toList());
}
```

Validation steps

- Verified `GET /api/trades/exports/settlements?nonStandardOnly=true&mineOnly=true` returns only flagged rows for a trader user session.
- Observed Vite proxy `ECONNREFUSED` error when backend not running documented fix: run backend on `localhost:8080` and restart frontend.

---

## Task 2 Delete settlement AdditionalInfo safely and fix MO 403

Goal: provide safe deletions for both bulk-by-trade and single AdditionalInfo rows (used to clean duplicates while preserving audit). Fix 403 for a Middle Office user (Ashley).

What I implemented

- Delete-by-trade:

  - Endpoint: DELETE `/api/trades/{tradeId}/settlement-instructions` (uses business `tradeId` path param).
  - Implementation: `AdditionalInfoService.deleteByTradeId(tradeId, principal)` which soft-deactivates `AdditionalInfo` rows (set `active=false`, set `deactivated_date`) and writes an audit row to `additional_info_audit`.

- Delete-by-id:

  - Endpoint: DELETE `/api/trades/additional-info/{id}` (uses DB numeric id for targeted cleanup).
  - This is used when duplicates exist and I want to remove a single active row but keep audit.

- Soft-delete with audit:

  - Delete operations do not hard-delete. They:
    - Create an audit row with fields: changed_at, changed_by, field_name, old_value, new_value, trade_id.
    - Update additional_info entry to `active = false` and set `deactivated_date`.

- Fixing MO 403:

  - Symptom: user 'ashley' (ROLE_MIDDLE_OFFICE) received 403 attempting to delete settlement additional info.
  - Root cause: controller `@PreAuthorize` allowed Middle Office, but fallback service-level ownership checks were not including `ROLE_MIDDLE_OFFICE`, causing a mismatch and 403.
  - Fix: updated the service fallback checks (and `UserPrivilegeValidator`) to include `ROLE_MIDDLE_OFFICE` and `ROLE_ADMIN` so controller and service logic align.
  - Evidence in logs: Authentication showed `Authorities for user 'ashley': ROLE_MO,ROLE_MIDDLE_OFFICE` and subsequent DELETE successfully produced a 204 and inserted an audit row (see build/test logs).
    DB-finding and safe cleanup SQL (used to identify duplicates)

- To find duplicate active additional_info rows for the same field/trade:

```sql
SELECT entity_type, entity_id, field_name, COUNT(*) as cnt
FROM additional_info
WHERE active = TRUE
GROUP BY entity_type, entity_id, field_name
HAVING COUNT(*) > 1;
```

- Then list duplicated rows for manual review:

```sql
SELECT additional_info_id, entity_type, entity_id, field_name, field_value, created_date
FROM additional_info
WHERE entity_type = 'TRADE' AND entity_id = 'T-1002' AND field_name = 'settlementInstructions' AND active = TRUE
ORDER BY created_date DESC;
```

Operational validation

- After enabling delete-by-id and updating service checks:
  - Re-ran `mvn test` (successful).
  - Performed targeted DELETE via curl for `additional-info/{id}` and observed `204 NO_CONTENT`.
  - Confirmed `additional_info_audit` rows were created for each soft-delete.

---

## Task 3 Wire real Risk Net Exposure to the dashboard

Goal: replace a hard-coded placeholder in `TradeDashboard` with the real backend-provided risk exposure value.

### Completed:

- Removed the faux value `"$1,234,000"` and added a small frontend formatter to display the backend value when present, or `-` if the server did not provide a value.
- The dashboard consumes the backend summary object `summary` returned by `getDashboardSummary(loginId)` and now uses `summary.riskExposureSummary.delta` if available.

Code snippet added to `frontend/src/pages/TradeDashboard.tsx`:

```ts
// small helper added near top of file
function formatCurrency(value?: number | string | null) {
  if (value === undefined || value === null) return "-";
  const n = Number(value as any);
  if (Number.isNaN(n)) return String(value);
  return n.toLocaleString(undefined, { style: "currency", currency: "USD" });
}
...
<SummaryCard
  title="Risk Net Exposure"
  value={formatCurrency(summary?.riskExposureSummary?.delta)}
/>
```

Validation

- `getDashboardSummary` already returns an object that may include `riskExposureSummary.delta`. By wiring it directly and formatting, the dashboard now reflects the true backend risk number.
- Verified via browser: when `summary.riskExposureSummary.delta` was present, it displayed correctly; when missing, shows `-`.

---

## Completed

- Implemented export filters `nonStandardOnly` and `mineOnly` on `/api/trades/exports/settlements`.
- Implemented non-standard detection heuristic and wrote CSV `true`/`false` textual booleans.
- Frontend download button now sends Accept header and rejects `text/html` to avoid saving dev server HTML as CSV.
- Added delete endpoints (by trade, by additional_info id) that do soft-delete + audit.
- Fixed service-level authorization fallback to include `ROLE_MIDDLE_OFFICE` so Middle Office users (e.g., `ashley`) can delete where allowed.
- Replaced hard-coded risk exposure placeholder in dashboard with `summary.riskExposureSummary.delta` + `formatCurrency`.
- Ran local validation: `mvn clean test` and `pnpm build` completed successfully during development.

---

## Learned

- Controller-level `@PreAuthorize` guards must always be aligned with service-level checks; otherwise elevated roles may see 403 because of an inconsistent fallback. Centralizing privilege logic (via `UserPrivilegeValidator`) avoids this class of bug.
- Frontend downloads can silently save HTML if the dev server is misconfigured or the backend is down adding content-type checks prevents confusing results for users.
- Small, well-documented heuristics (e.g., non-standard detection) are preferable to complex logic for an initial shipping iteration. Make sure the heuristic is clearly documented and easily replaceable for future improvements.
- Soft-delete + audit is crucial for data safety and forensics. Avoid direct SQL deletes for cleanup until the app provides safe delete-by-id endpoints to preserve audit trails.

---

## Challenges (and how they were solved)

1. Duplicate `additional_info` rows broke queries

- Problem: JPA/queries assumed a single active `AdditionalInfo` row per (entity_type,entity_id,field_name). Duplicates caused errors and unexpected exports.
- Fix: added delete-by-id endpoint to allow safe soft-delete of specific rows; used SQL to find duplicates and then cleaned them via delete-by-id. Also documented the suggestion to add a unique index (if business rules allow) or to handle multi-row fallbacks in queries.

2. 403 for Middle Office (Ashley)

- Problem: controller allowed `ROLE_MIDDLE_OFFICE`, but service fallback check omitted that role, so MO user got 403.
- Fix: updated `UserPrivilegeValidator` and inline fallback checks to include `ROLE_MIDDLE_OFFICE` and `ROLE_ADMIN`. Verified via logs: authentication showed `Authorities for user 'ashley': ROLE_MO,ROLE_MIDDLE_OFFICE` and subsequent delete succeeded.

3. Frontend saved HTML as CSV

- Problem: Vite dev server returned index.html for API calls when the backend was not reachable; the download button saved that HTML and user opened it in Excel, causing confusion.
- Fixes:
  - Added content-type checks in `DownloadSettlementsButton.tsx`.
  - Added `server.proxy` config to `vite.config.ts` for dev to forward `/api` ➜ `http://localhost:8080`.
  - Documented the need to run the backend on `localhost:8080` during frontend dev.

4. Ensuring `mineOnly` respects elevated roles

- Problem: naive `mineOnly` filter would prevent Admin/MO from seeing full exports.
- Fix: detect elevated roles server-side and bypass the `mineOnly` filter for them (server-side is authoritative; frontend defaults to `mineOnly=true` but server enforces correct behavior).

5. Deciding non-standard heuristic

- Problem: "non-standard" is subjective and could be expensive to calculate if highly sophisticated.
- Fix: implemented a fast heuristic (length + character set) and documented it in code and docs this is a pragmatic starting point and can be improved later.

---

## Concrete artifacts & references

Files changed / touched (where to look)

- Backend:
  - `backend/src/main/java/.../controller/TradeController.java` export endpoint logic and `mineOnly` handling.
  - `backend/src/main/java/.../controller/TradeSettlementController.java` delete endpoints (by trade, by additionalInfo id).
  - `backend/src/main/java/.../service/AdditionalInfoService.java` soft-delete with audit and service-level privilege checks (fall-back).
  - `backend/src/main/java/.../security/UserPrivilegeValidator.java` central privilege changes (include ROLE_MIDDLE_OFFICE / ROLE_ADMIN).
- Frontend:
  - `frontend/src/components/DownloadSettlementsButton.tsx` Accept header and HTML detection, default query params `nonStandardOnly=true&mineOnly=true`.
  - `frontend/src/pages/TradeDashboard.tsx` added `formatCurrency` and replaced hard-coded exposure fallback.
  - `frontend/vite.config.ts` dev proxy for `/api` ➜ `http://localhost:8080`.

SQL used to find duplicates

- (see example above under "Delete settlement AdditionalInfo safely")

Auth evidence (from local logs)

- Example auth entry captured while reproducing delete flow:
  - `Authorities for user 'ashley': ROLE_MO,ROLE_MIDDLE_OFFICE`
- This informed the fix to accept `ROLE_MIDDLE_OFFICE` in the service fallback.

Build/test evidence

- Backend: `mvn clean test` reported successful build after fixes.
- Frontend: `pnpm build` completed successfully after edits.

---

## Task 4 Application events & listener (sprint work)

Goal: add a lightweight, test-friendly application event mechanism so settlement-instruction changes are recorded (audit) and announced (notifications) without coupling the write path to downstream consumers.

What I added and where to look

- Events (immutable DTOs) new classes under `backend/src/main/java/com/technicalchallenge/Events`:

  - `SettlementInstructionsUpdatedEvent.java` fields: `String tradeId`, `long tradeDbId`, `String changedBy`, `Instant timestamp`, `Map<String,Object> details`.
  - `TradeCancelledEvent.java` and `RiskExposureChangedEvent.java` sibling event DTOs for related domain signals.

- Listener `NotificationEventListener.java` (component with `@EventListener` handlers):

  - Logs incoming events and is the natural place to persist notifications or push SSE/WebSocket messages.
  - Current handlers are synchronous and annotated as ordinary `@EventListener` methods; comments were added explaining each logged field and suggesting future improvements (e.g. persist notifications or use `@TransactionalEventListener(AFTER_COMMIT)` / async).

- Service wiring `backend/src/main/java/.../service/AdditionalInfoService.java`:
  - New field: `private final ApplicationEventPublisher applicationEventPublisher;` injected by Spring.
  - Constructors:
    - `@Autowired` 6-arg constructor that accepts `ApplicationEventPublisher` (used by Spring DI).
    - Backwards-compatible 5-arg delegating constructor that calls the 6-arg constructor with a `null` publisher to avoid breaking existing tests that instantiate the service directly.
  - Publish sites (where events are emitted):
    - `upOrInsertTradeSettlementInstructions(Long tradeId, String settlementText, String changedBy)` after creating the audit row and saving the settlement text, the service publishes a `SettlementInstructionsUpdatedEvent` containing the business trade identifier, the DB numeric id (as `long`), who changed it, a timestamp (truncated to milliseconds for readability), and a small details map (old/new values).
    - `deleteSettlementInstructions(Long tradeId)` similar publish with `newValue == null` to indicate deletion.
  - Defensive behaviour:
    - Publish calls are wrapped in `try/catch` so that event delivery failures do not break the DB write or transaction.
    - Timestamps are generated via `Instant.now().truncatedTo(ChronoUnit.MILLIS)` to avoid noisy nanosecond values.
    - A small refactor replaced a terse ternary `tradeId != null ? tradeId.longValue() : 0L` with an explicit local `long` assignment (keeping existing comments) for clarity.

Why this satisfies the business requirement

- Business requirement: produce an audit trail and notify downstream consumers (notification UI, external systems) whenever settlement instructions change or are removed.

- How the changes map to that requirement:
  - Audit: the service still writes an `additional_info_audit` row for every change (unchanged behavior) this ensures the forensics requirement is satisfied.
  - Notification surface: publishing `SettlementInstructionsUpdatedEvent` means listeners (in-process or remote bridges) can observe changes and react (create user notifications, push events to websockets/SSE, or forward to message queues). The listener added (`NotificationEventListener`) is a simple, local example consumer and a place to implement the business notification rules.
  - Decoupling: event publishing is fire-and-forget (defensive try/catch) so the write path is not blocked by downstream consumers.

Files and methods to inspect first (quick pointers)

- `backend/src/main/java/com/technicalchallenge/service/AdditionalInfoService.java`
  - Methods: `upOrInsertTradeSettlementInstructions`, `deleteSettlementInstructions`, constructor(s), and the new `applicationEventPublisher` field.
- `backend/src/main/java/com/technicalchallenge/Events/SettlementInstructionsUpdatedEvent.java` event DTO contract and fields.
- `backend/src/main/java/com/technicalchallenge/Events/NotificationEventListener.java` example handler and notes about future persistence/async/transactional semantics.

To be completed:

- Replace `System.err` publish-failure logs in `AdditionalInfoService` with SLF4J to integrate with existing logging and correlate with request IDs.
- Provide a NoOp `ApplicationEventPublisher` or update unit tests to inject a mock publisher so tests can assert publishes (avoid silent null publishes via the 5-arg delegating constructor).
- Consider changing `tradeDbId` from primitive `long` to `Long` if `0L` as a sentinel is undesirable.
- For stronger delivery guarantees, move listeners that must not affect the transaction to `@TransactionalEventListener(AFTER_COMMIT)` or make them `@Async` and ensure proper error handling.

Validation done

- Confirmed publish calls execute in tests (suﬁre reports show System.err entries indicating attempted publishes when the publisher was null in some test contexts).
- Ran `mvn test` after conservative changes; build completed successfully. The try/catch behavior prevents event publish errors from failing DB writes.

### Application Notification result

I created a settlement on swagger post request and it worked.

Here is the notification from the console:
The audit row was inserted (Hibernate insert into additional_info_audit), then the listener logged the event:
2025-11-06T23:16:31.474Z INFO ... NotificationEventListener : SettlementInstructionsUpdatedEvent received for tradeId=10038 dbId=10038 by=ashley details={newValue=Settle via JPM New York, Account: 123456789, Further Credit: ABC Corp Trading Account, oldValue=Settle via JPM New York, Account: 123456789, Further Credit: ABC Corp Trading Account}

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

--
THE END
