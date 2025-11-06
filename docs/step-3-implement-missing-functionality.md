# Step 3 Implement Missing Functionality (Enhancements 1–3)

---

Enhancement 1 Advanced Trade Search System

Business requirement

- Allow traders to quickly find trades by counterparty, book, trader, status and date ranges.
- Support paginated filtering and an RSQL endpoint for power users.
- Performance target: complex queries should respond < 2s for typical dataset sizes.

What I implemented (code references)

- Controller: `TradeDashboardController` endpoints added/exposed:
  - `GET /api/dashboard/search` → `searchTrades(...)`
  - `GET /api/dashboard/filter` → `filterTrades(...)` (paginated)
  - `GET /api/dashboard/rsql` → `searchTradesRsql(...)`
    (see `backend/src/main/java/com/technicalchallenge/controller/TradeDashboardController.java`)
- Service: `TradeDashboardService` core implementations:
  - `searchTrades(SearchCriteriaDTO)` builds a JPA `Specification` from flexible criteria and returns mapped `TradeDTO`s.
  - `filterTrades(SearchCriteriaDTO, int page, int size)` uses `PageRequest` and `tradeRepository.findAll(spec, pageable)` to fetch a `Page<Trade>` and maps to `Page<TradeDTO>`.
  - `searchTradesRsql(String query)` parses RSQL using `cz.jirutka.rsql.parser.RSQLParser` and a `TradeRsqlVisitor` to produce a `Specification<Trade>` (see `TradeDashboardService` in `backend/src/main/java/com/technicalchallenge/service/TradeDashboardService.java`).

Programming techniques used and why

- JPA Specifications
  - Technique: Dynamically build a `Specification<Trade>` from the search criteria (counterparty name, book id, trader loginId, status, date ranges).
  - Why: Specifications let us translate arbitrary combinations of criteria into SQL predicates executed by the database, which is far faster and more memory efficient than filtering in Java for large datasets.
  - Implementation note: I compared nested fields with `root.get("counterparty").get("name")` and `root.get("book").get("id")` to avoid Hibernate type mismatches.
- Pagination with PageRequest
  - Technique: Use `PageRequest.of(page,size)` and `tradeRepository.findAll(spec,pageable)` to let the DB return only the requested slice.
  - Why: Prevents loading high‑volume result sets into memory; required for UI responsiveness and the <2s target on reasonable data volumes.
- RSQL parsing
  - Technique: Use `RSQLParser` plus a `TradeRsqlVisitor` (AST → `Specification`) to allow expressive queries such as `counterparty.name==ABC` and complex boolean logic.
  - Why: RSQL gives power users an expressive query language without building a bespoke DSL. It maps well to `Specification` and still executes in the DB.
- Privilege short‑circuit + DB fallback for auth
  - Technique: `hasPrivilege(currentUser, "TRADE_VIEW")` first attempts a quick check against `SecurityContext` authorities and falls back to `UserPrivilegeService` DB lookup.
  - Why: Fast path avoids the DB for common authenticated role cases; DB fallback ensures authoritative privilege checks for fine-grained permissions.

Why I chose these approaches

- Simplicity + performance: JPA `Specification` + DB queries keep heavy lifting in the database where indexes and query planners are effective.
- Testability: `TradeDashboardService` is easily unit tested by mocking the `TradeRepository` and `UserPrivilegeService` and verifying that the right `Specification` is passed or the expected DTOs returned.
- UX and power: RSQL gives power users expressive queries while simple search/filter endpoints serve the common UI flows.

Alternatives considered

- Full text search engine (Elasticsearch)
  - Pros: Very powerful, fast text search and aggregations for very high volumes.
  - Cons: Introduces infra complexity; index maintenance for frequent writes; more to test and operate.
  - Decision: Defer to future phase if performance profiling shows DB approach insufficient.
- Build a bespoke query DSL
  - Pros: Tailored to domain; potentially safer than raw RSQL.
  - Cons: Reinventing parsing and evaluation; more work for marginal benefit.

Edge cases and tests

- RSQL syntax errors: caught and translated to HTTP 400 (handled in `searchTradesRsql` with parser exceptions -> ResponseStatusException).
- Date range boundaries: `startDate` and `endDate` are applied with >= and <= respectively.
- Large result sets: tested with pagination; `filterTrades` returns `count` and `content` in a response map to match frontend expectations.

How I validated

- Unit tests: mock `tradeRepository` and assert `findAll(spec,pageable)` called; verify mapping to `TradeDTO`.
- Integration tests: API calls to `/api/dashboard/rsql` and `/api/dashboard/filter` assert response schema (`content`, `count`) and role-based access.

---

Enhancement 2 Comprehensive Trade Validation Engine

Business requirement

- Prevent invalid trades from entering the system: enforce date rules, cross-leg checks, floating/index rules and user privilege enforcement.

What I implemented (code references)

- Central validation entry point: `TradeValidationEngine.validateTradeBusinessRules(TradeDTO)` (see `backend/src/main/java/com/technicalchallenge/validation/TradeValidationEngine.java`).
- Field/leg validators: `TradeLegValidator`, `TradeDateValidator`, `SettlementInstructionValidator` (these are orchestrated by `TradeValidationEngine`).
- Service use: `TradeService.createTrade(...)` calls `tradeValidationEngine.validateTradeBusinessRules(tradeDTO)` and throws 400 on failures.
- Privilege checks: `UserPrivilegeValidator` and service-side ownership checks are used in `TradeService` and `AdditionalInfoService` to enforce operation-level permissions.

Programming techniques used and why

- Centralised validation engine
  - Technique: A single orchestration class (`TradeValidationEngine`) runs a suite of specific validators and returns a `TradeValidationResult` containing errors.
  - Why: Consolidates business-rule logic in one testable place and avoids scattering rule checks across controllers and services. It makes the codebase easier to maintain as rules grow.
- Small, focused validators
  - Technique: Implement `TradeDateValidator`, `TradeLegValidator`, `SettlementInstructionValidator` each with a narrow responsibility (SRP).
  - Why: Easier to unit test each rule, clearer failure messages and straightforward composition.
- Constructor injection and test-friendly no‑arg constructor
  - Technique: `TradeValidationEngine` accepts validators via constructor injection but provides a no‑arg constructor for lightweight tests.
  - Why: Keeps production wiring via Spring while allowing unit tests to instantiate the engine without requiring DB wiring.
- Fail-fast and precise errors
  - Technique: On validation failure the engine yields specific messages; callers (services/controllers) convert these into `400 Bad Request` with the composed message.
  - Why: Provides helpful feedback to users and QA—easier to triage which rule failed.

Key business rules implemented

- Date rules (in `TradeDateValidator` / orchestrated by engine):
  - Maturity date >= start date and start date >= trade date
  - Trade date not more than 30 days in the past
- User privilege enforcement (service + `UserPrivilegeValidator`):
  - Enforce role semantics (TRADER, SALES, MIDDLE_OFFICE, SUPPORT) at service layer and controller layer (`@PreAuthorize` where applicable)
- Cross-leg rules (in `TradeLegValidator`):
  - Both legs must have identical maturity dates
  - Legs must have opposite pay/receive flags
  - Floating legs must have an index specified
  - Fixed legs must have a valid rate
- Entity status checks: ensure referenced `User`, `Book`, `Counterparty` exist and are active (implementation via `EntityStatusValidationEngine` where available; otherwise via repository lookups)

Why I chose these approaches

- Centralised, composable validators make it easy to add or change rules without touching controller/service code.
- Narrow validators are straightforward to unit test individually and collectively.
- Using validators inside the service (rather than in controller) ensures server‑side enforcement even if a buggy frontend bypasses client validation.

Alternatives considered

- Declarative rule engines (Drools, Easy Rules)
  - Pros: Good for extremely dynamic rules and business-authoring UIs.
  - Cons: Extra complexity and fewer developers familiar with rule engines; heavy for this scope.
  - Decision: Use simple Java validators now; a rule engine can be introduced later if business requires non-developer rule edits.
- Bean Validation (JSR 380) for cross-field rules
  - Pros: Built-in and standard for field-level constraints.
  - Cons: Less suited to complex multi-leg or entity-status checks which require repository access.

Edge cases and tests

- Missing leg rates: test both floating and fixed legs; floating legs require index, fixed require a numeric rate.
- Date edge boundaries: create tests for tradeDate exactly 30 days ago, start==tradeDate and maturity==start.
- Privilege permutations: unit tests for TRADER, SALES, MIDDLE_OFFICE and SUPPORT for allowed/disallowed operations.

How I validated

- Unit tests for each validator class (happy path + 1–2 negative cases per rule).
- Integration tests invoking `POST /api/trades` and asserting HTTP 400 with the expected validation message for failing inputs.

---

Enhancement 3 Trader Dashboard and Blotter System

Business requirement

- Provide personalised views & summary statistics: `/my-trades`, `/book/{id}/trades`, `/summary`, `/daily-summary` and DTOs `TradeSummaryDTO` and `DailySummaryDTO`.

What I implemented (code references)

- Controller: `TradeDashboardController` exposes:
  - `GET /api/dashboard/my-trades` → `getMyTrades(traderId)`
  - `GET /api/dashboard/book/{bookId}/trades` → `getTradesByBook(bookId)`
  - `GET /api/dashboard/summary` → `getTradeSummary(traderId)`
  - `GET /api/dashboard/daily-summary` → `getDailySummary(traderId)`
- Service: `TradeDashboardService` implements aggregation logic:
  - `getTradeSummary(String traderId)` builds `TradeSummaryDTO`: tradesByStatus, notionalByCurrency, tradesByTypeAndCounterparty, riskExposureSummary (naive delta), weekly comparisons.
  - `getDailySummary(String traderId)` builds `DailySummaryDTO` with today/yesterday comparisons, user performance metrics and book activity summaries.
    (see `backend/src/main/java/com/technicalchallenge/service/TradeDashboardService.java`)

Programming techniques used and why

- DTO aggregation in service layer
  - Technique: fetch domain `TradeDTO`s for the trader (via `fetchTradesForTraderWithoutPrivilegeCheck`) then perform in‑memory aggregations using Java streams and `BigDecimal` for numeric accuracy.
  - Why: Aggregations are domain-specific and easier to express in Java when datasets per trader are modest. `BigDecimal` avoids rounding errors when summing notionals.
- Defensive authorization and short-circuits
  - Technique: capture `Authentication` at the start of `getTradeSummary` and compute `canViewOthers` to prevent accidental leaks; enforce privilege checks early to avoid expensive aggregations for unauthorised callers.
  - Why: Performance and security deny early prevents wasted work and prevents accidental data exposure.
- Weekly / daily comparisons
  - Technique: compute per-day buckets for the last 7 days using `LocalDate` and stream filters. Compose `DailySummaryDTO.DailyComparisonSummary` entries.
  - Why: Simple and explicit; easier to unit test and to tune later for DB‑side aggregation if performance requires.

Why I chose these approaches

- Readability and auditability: implementing aggregation in Java makes the logic explicit and easy to explain in a retrospective valuable for the hand‑in and debugging.
- Correctness: using `BigDecimal` ensures numeric correctness for financial notional sums.
- Flexibility: building DTOs in service layer gives the frontend a single shape to render without additional client-side joins.

Alternatives considered

- Push aggregation into the database (SQL GROUP BY / materialised views)
  - Pros: potentially much faster for very large datasets and can use DB indexes and parallel execution.
  - Cons: increases SQL complexity, more migration scripts, and less flexibility for business logic changes. If profiling shows slow queries, I recommend moving heavy aggregates into DB views or precomputed tables.
- Use OLAP / cube or analytics DB
  - Pros: best for large-scale analytics and historical comparisons.
  - Cons: heavy infra; out of scope for initial enhancement.

Edge cases and tests

- Empty datasets: return empty DTOs with zero counts rather than throwing errors (the service catches exceptions and returns empty DTOs for resilience).
- Currency conversion: the current implementation sums notionals per currency; if risk requires cross‑currency aggregation, add FX conversion service in a follow‑up.

How I validated

- Unit tests: service-level tests for `getTradeSummary` and `getDailySummary` against small synthetic trade lists covering multiple currencies and statuses.
- Integration tests: controller-level tests asserting role-based access and the presence/shape of `TradeSummaryDTO` and `DailySummaryDTO` JSON keys.

---

Cross-cutting concerns and final notes

- Security and ownership: I kept consistent server‑side ownership checks in both `TradeService` and `TradeDashboardService`, and controller methods have `@PreAuthorize` annotations. This defence‑in‑depth ensures that a client cannot access another trader’s data by manipulating request parameters or calling unprotected endpoints.
- Test-first incremental approach: for each enhancement I added unit tests for core logic and small integration tests for the controller contract. This keeps risk low and makes it easy to review behaviour changes.
- Performance and observability: the chosen DB‑centric search approach should be monitored in staging; add metrics for query latency and counts and consider DB indexing for frequent criteria (tradeDate, counterparty.name, book.id, trader loginId).
