# Step 3 – Enhancement1_Advanced_Trade_Search

**Scope:** Advanced search and filtering for trades, including multi-criteria endpoints, pagination, and RSQL support.  
**Code Areas:** `TradeDashboardController`, `TradeDashboardService`, `TradeRsqlVisitor` (via RSQL library), `SearchCriteriaDTO`, `TradeMapper`, `TradeRepository`, Spring Security context usage.

## 1. What I Implemented and Why

I implemented **three complementary search capabilities**, each aimed at a different user persona and workload:

### 1.1 Multi‑criteria search (`GET /api/dashboard/search`)

- **Why:** UBS Traders asked for fast, obvious filters (counterparty, book). This is the most approachable entry point and avoids exposing RSQL to everyone.
  The desk asked for **multi-criteria search, pagination, and power-user RSQL**, with fast responses. The implementation directly targets the pain points:

- Traders no longer scroll through full lists they submit _precise filters_ (counterparty/book/trader/status/date range).
- High-volume result sets don’t overwhelm the UI _pagination_ keeps responses small and quick.
- Power users get **RSQL** for complex, ad-hoc reporting (month-end, combined clauses, etc.).

- **How:** I introduced `SearchCriteriaDTO` and built a **JPA `Specification<Trade>`** dynamically in `TradeDashboardService.searchTrades(...)`. I map DTO fields to actual entity paths (for example `root.get("counterparty").get("name")`) so Hibernate builds proper joins and avoids type‑mismatch errors.
- **Security:** I require the caller to have viewing capability by calling `hasPrivilege(currentUser, "TRADE_VIEW")` before touching the database.
- **Alternatives I considered:**
  - Hardcoded repository methods like `findByCounterpartyAndBook(...)`. I rejected this because it **doesn't scale**; every new combination would need a new method.
  - QueryDSL. It is excellent for type safety, but I kept to Spring Data Specifications because the rest of the project already uses Spring Data with minimal extra dependencies.

### 1.2 Paginated filter (`GET /api/dashboard/filter`)

- **Why:** The desk often pulls **high‑volume** blotter views; pagination preserves memory and guarantees predictable response times.
- **How:** I reuse the same `Specification<Trade>` building approach but return a `Page<TradeDTO>`. The controller returns a JSON object with `count` and `content` so the frontend can render tables without guessing totals.
- **Performance techniques used:**
  - `PageRequest.of(page,size)` ensures **SQL LIMIT/OFFSET** is used.
  - Mapping to DTOs is batched; I then do a **single enrichment pass** for ancillary fields to avoid N+1 lookups.
- **Alternatives:**
  - Cursor‑based pagination. Useful at very high cardinality, but OFFSET is acceptable for the current data profile and simpler for the UI today.

### 1.3 RSQL power search (`GET /api/dashboard/rsql?query=...`)

- **Why:** Power users requested **ad hoc, complex filters**. RSQL lets them compose queries like:  
  `tradeDate=ge=2025-01-01;tradeDate=le=2025-12-31;(counterparty.name==ABC,counterparty.name==XYZ)`
- **How:** I parse queries with `RSQLParser` and feed the AST through a `TradeRsqlVisitor` to build a `Specification<Trade>`.
- **Robustness:** I catch `RSQLParserException` and `IllegalArgumentException` and translate them to **HTTP 400** with a clear `"Invalid RSQL query"` message, so a typo never creates a 500.
- **Alternatives:** Building a custom DSL or exposing raw JPQL would be brittle and unsafe. RSQL is a **well‑known, battle‑tested** syntax with a clear AST model.

---

## 2. How the Search Pipeline Works (End‑to‑End)

1. **Controller receives inputs.**  
   `TradeDashboardController.searchTrades(...)`, `filterTrades(...)`, or `searchTradesRsql(...)` accept query params.
2. **Privilege guard.**  
   `TradeDashboardService.hasPrivilege(currentUser,"TRADE_VIEW")` short‑circuits for common authorities and then consults the DB via `UserPrivilegeService.findPrivilegesByUserLoginIdAndPrivilegeName(...)` for authoritative checks.
3. **Specification assembly.**
   - For simple filters, I compose a `Specification<Trade>` by **chaining predicates** with `spec = spec.and(...)`.
   - For RSQL, I parse to an AST and call `.accept(new TradeRsqlVisitor())` to obtain a `Specification<Trade>`.
4. **Repository query.**  
   `tradeRepository.findAll(spec[, pageable])` executes a single SQL query.
5. **Mapping and enrichment.**  
   I map `Trade` to `TradeDTO` via `TradeMapper`. When I need extra display values, I run a **single enrichment step** over the resulting list to keep database round‑trips low.
6. **Response shaping.**  
   Controllers return either a raw `List<TradeDTO>` or a `{ count, content }` wrapper for tables. Errors from parsing or validation are converted to friendly status codes using Spring’s `ResponseStatusException` or controller‑level checks.

---

## 3. Programming Techniques I Used

### 3.1 Dynamic Specifications

- **What:** I build a predicate tree using `Specification.where(null)` and successive `and` clauses.
- **Why:** It keeps the code **composable** and easy to extend for new filters without changing repository interfaces.
- **Example:**
  ```java
  if (criteria.getCounterparty() != null) {{
      spec = spec.and((root, q, cb) -> cb.equal(root.get("counterparty").get("name"), criteria.getCounterparty()));
  }}
  ```
- **Edge case I handled:** Using `root.get("counterparty")` directly causes a type mismatch; I always dereference `name` or `id` to compare like‑for‑like.

### 3.2 AST‑Driven Querying with RSQL

- **What:** RSQL produces an **AST** (`Node`) which I visit to convert each comparison or logical node to a `Specification<Trade>` and then **compose**.
- **Why:** Treating the query as a tree gives me a clean separation: parsing concerns stay in the RSQL layer; **translation** lives in my visitor.
- **Advantages:** I can **whitelist** operators and map dotted paths like `tradeStatus.tradeStatus` safely to entity paths.
- **Failure handling:** I detect parser errors immediately and return 400 to keep UX predictable for power users.

### 3.3 Pagination and DTO Mapping

- **What:** `Page<Trade>` to `Page<TradeDTO>` with `PageRequest` and batch conversion.
- **Why:** Prevents memory spikes and gives the UI total counts for virtual scrolling.
- **Tweak:** I format aggregations on the server when convenient so the client doesn’t re‑calculate repeatedly.

### 3.4 Security Context Short‑Circuit

- **What:** Before DB work, I check the caller’s authorities for `TRADE_VIEW` or role equivalents in `hasPrivilege(...)`.
- **Why:** Most requests are made by properly‑granted users; this prevents unnecessary DB hits and keeps latency low.

---

## 4. Meeting the Business Requirements

- **Multiple criteria:** Counterparty, book, trader, status, and date range are all supported through DTO‑to‑Specification mapping.
- **RSQL:** Complex filters including `ge`, `le`, nested AND/OR are parsed and executed.
- **Performance:** Pagination, short‑circuit privilege checks, and one enrichment pass keep responses within target timings.
- **Error handling:** Bad queries give 400 with human‑readable messages, never 500s.

---

## 5. Alternatives and Trade‑offs

- **QueryDSL:** Strong typing, but adds framework complexity. Spring Data Specifications were sufficient and consistent with current code.
- **Elasticsearch:** Great for free‑text and enormous datasets, but operationally heavier and out of scope for this stage.
- **Stored procedures:** Fast for fixed patterns but not flexible for ad‑hoc trading filters.

## 7. Key Classes and Methods (for quick reference)

- `TradeDashboardController.searchTrades`, `.filterTrades`, `.searchTradesRsql`
- `TradeDashboardService.searchTrades`, `.filterTrades`, `.getTradesByBook`, `.getTradesByTrader`
- `TradeDashboardService.hasPrivilege`, `.resolveCurrentTraderId`
- `TradeRsqlVisitor` (visitor that turns RSQL AST into `Specification<Trade>`)
- `SearchCriteriaDTO` (transport for filters)
- `TradeRepository` (Spring Data JPA)
- `TradeMapper` (entity ↔ DTO)

## High-Level Architecture

```
Controller (TradeDashboardController)
        └── Service (TradeDashboardService)
                ├── JPA Specifications (Criteria API)
                ├── RSQL Parsing → RSQL AST → Specification
                ├── Security guards (privilege + ownership)
                └── Repository (TradeRepository)
                        └── DB
```

## What I built (controllers & endpoints)

### Endpoints

- `GET /api/dashboard/search` I multi-criteria search (simple filters)
- `GET /api/dashboard/filter` I **paginated** filtering (table views)
- `GET /api/dashboard/rsql` I **RSQL** power-user search

> **Note:** Search is intentionally hosted under the _dashboard_ domain (rather than CRUD `/api/trades`) to reflect that these are **read/report** scenarios distinct from create/amend flows.

### Security (high level for these endpoints)

```java
@PreAuthorize("(hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')) or hasAuthority('TRADE_VIEW')")
```

- Works for **role-based** users (mapped from user profile to ROLE\_\*)
- Works for **privilege-based** users (`TRADE_VIEW` authority)

Inside the service layer I do an **additional DB-backed privilege check** for `TRADE_VIEW` (deny-by-default). See Security doc for details.

---

## Programming techniques and why I used them

### 1) **Spring Data JPA Specifications** for dynamic filters

**How it’s implemented**

- In `TradeDashboardService.searchTrades(...)` and `.filterTrades(...)` I build a `Specification<Trade>` incrementally:
  ```java
  Specification<Trade> spec = Specification.where(null);
  if (criteria.getCounterparty() != null) {
      spec = spec.and((root, query, cb) ->
          cb.equal(root.get("counterparty").get("name"), criteria.getCounterparty()));
  }
  // ... similar for book, trader, status, date range
  ```
- I only add predicates for fields actually provided by the caller.
- I navigate **nested attributes** (e.g., `root.get("counterparty").get("name")`) to avoid type mismatches and allow the DB to use FK indexes.

**Why this technique**

- Specifications are **composable** (perfect for optional filters).
- Keeps SQL in the repository layer with type safety.
- Reusable across endpoints (search/filter/daily summary, etc.).

**Alternatives I considered**

- **CriteriaBuilder** directly: more verbose; Specifications are a small, composable wrapper on top of it.
- **QueryDSL**: very powerful with generated Q-types, but adds a generator step and extra onboarding for the team.
- **Native SQL** / **@Query** strings: fastest to write but brittle (stringly typed), harder to refactor, and not composable for optional filters.
- **Elasticsearch**: great for free text and analytics, but operational overhead and eventual consistency didn’t fit the sprint.

**How this meets the requirement**

- Supports **multi-criteria** search without branching controller code.
- Efficient for **high volume** because the DB can use indexes on joined columns.

---

### 2) **Pagination** with `PageRequest` (performance & UX)

**How it’s implemented**

- `filterTrades(criteria, page, size)` uses:
  ```java
  Pageable pageable = PageRequest.of(page, size);
  Page<Trade> tradePage = tradeRepository.findAll(spec, pageable);
  ```
- The controller wraps a compact response: `{ "count": <total>, "content": [...] }`

**Why this technique**

- Prevents large payloads and **speeds up responses** substantially.
- Aligns with UI data tables (page numbers, sizes).

**Alternatives**

- Offset-based pagination with custom SQL: similar, but I already get it via Spring Data.
- Cursor-based pagination: better for massive datasets + stable ordering, but requires token logic; not needed yet.

**How this meets the requirement**

- **Fast response times** by shrinking result sets and using indexes.
- Natural fit for “blotter” views the traders use all day.

---

### 3) **RSQL** for power users (AND/OR, ranges, nesting)

**How it’s implemented**

- Controller: `/api/dashboard/rsql?query=...`
- Service:
  ```java
  Set<ComparisonOperator> ops = new HashSet<>(RSQLOperators.defaultOperators());
  ops.add(new ComparisonOperator("=like=")); // extra operator
  Node root = new RSQLParser(ops).parse(query);
  Specification<Trade> spec = root.accept(new TradeRsqlVisitor());
  ```
- I **catch parser errors** and return **400 Bad Request** with a clear message:
  ```java
  } catch (RSQLParserException | IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid RSQL query");
  }
  ```

**Why this technique**

- RSQL turns free-form string queries into a syntax tree I I convert this **AST** into a JPA `Specification`.
- It aligns with our **Specification**-based search (composable predicates).
- Power users can write filters like:
  - `counterparty.name==ABC`
  - `(counterparty.name==ABC,counterparty.name==XYZ);tradeStatus.tradeStatus==NEW`
  - `tradeDate=ge=2025-01-01;tradeDate=le=2025-12-31`

**Tree traversal mental model**

- Think of the RSQL expression as a **tree**:
  - Nodes are **AND** (;) and **OR** (,) logical branches.
  - Leaves are **comparisons** (`field OP value`).
- `TradeRsqlVisitor` recursively descends the tree and **combines `Specification`s** with `.and(...)` or `.or(...)`.
- This yields a single DB query with the exact combined predicate set.

**Alternatives**

- Build our own DSL: time-consuming and risky.
- Use plain query parameters for everything: quickly becomes unreadable for complex compositions and ranges.
- GraphQL: powerful for client-driven queries but heavy for this stage and introduces a new stack.

**How this meets the requirement**

- Enables **complex searches** without changing backend code.
- Serves **month-end reporting** and analyst power users immediately.

---

## Performance techniques used

- **Predicate pushes** to the DB via JPA I the DB handles filtering, not the app.
- **Pagination** I reduces IO and serialization.
- **Exact field comparisons** on nested attributes I helps the optimizer pick indexes.
- **Defensive null/blank checks** I skip building predicates that degrade plans.

---

## Error handling & messages

- Invalid RSQL I **400** with `"Invalid RSQL query"`
- Access without privilege I **403** (`AccessDeniedException`)
- Everything else I handled by `GlobalExceptionHandler` with structured JSON (`timestamp`, `status`, `message`).

---

## Example requests that traders will use

### Simple search by counterparty

```
GET /api/dashboard/search?counterparty=ABC_BANK
```

### Paginated filter by counterparty (page 2, size 25)

```
GET /api/dashboard/filter?counterparty=ABC_BANK&page=2&size=25
```

### RSQL: NEW trades for ABC or XYZ

```
GET /api/dashboard/rsql?query=(counterparty.name==ABC,counterparty.name==XYZ);tradeStatus.tradeStatus==NEW
```

### RSQL: Date range for 2025 month-end

```
GET /api/dashboard/rsql?query=tradeDate=ge=2025-01-01;tradeDate=le=2025-12-31
```

---

## Testing strategy (what I actually covered)

- **Unit**: Visitor parsing (simple comparisons, nesting, invalid syntax).
- **Integration**: endpoint security (TRADER/MO/SUPPORT/TRADE_VIEW), pagination shapes, 400 on invalid queries.
- **Performance**: smoke testing big pages (size 100/200) I verify < 2s under H2; rely on DB indexes in prod.
