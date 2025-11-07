# Step 3 – Technical_Architecture_Overview

## 1. Layering and Responsibilities

- **Controllers:** Accept requests, apply `@PreAuthorize`, translate parameters to DTOs, and delegate to services.
- **Services:** Orchestrate business logic, validation, and persistence. Apply a second layer of authorisation checks where ownership matters.
- **Repositories:** Spring Data JPA repositories that accept `Specification<Trade>` and return entities (paged or list).
- **Mappers:** `TradeMapper` converts between `Trade` and `TradeDTO` so controllers never deal with entities.
- **Validators:** Standalone classes that enforce one area of rules and report via `TradeValidationResult`.
- **Security:** User loading with `DatabaseUserDetailsService`, policy at controllers with `@PreAuthorize`, and ownership checks in services.
- **DTOs:** Describe responses explicitly (`TradeSummaryDTO`, `DailySummaryDTO`) to keep the frontend stable.

## 2. Search and Filtering Flow

- From `TradeDashboardController`, I route to `TradeDashboardService` which builds a `Specification<Trade>` either from a simple criteria DTO or a **visited RSQL AST**.
- The repository executes a single SQL query. For pageable endpoints I return a `Page<TradeDTO>`; for simple search I return `List<TradeDTO>`.
- I perform **one enrichment pass** after mapping to DTOs when extra fields are required for display.

## 3. Validation Flow

- `TradeService.createTrade(...)` and `.amendTrade(...)` call `TradeValidationEngine.validateTradeBusinessRules(...)`.
- Inside the engine I invoke `TradeDateValidator`, `TradeLegValidator`, and `EntityStatusValidationEngine/Validator` in sequence, collecting messages.
- If any rule fails I raise `ResponseStatusException(HttpStatus.BAD_REQUEST, ...)` so the controller returns a **clean JSON error** through the global exception handler.

## 4. Security Flow

- Authentication is performed by Spring Security using `DaoAuthenticationProvider` bound to my DB‑backed `DatabaseUserDetailsService`.
- Method security enforces per‑endpoint policy. Services reinforce **ownership** checks to prevent misuse outside the web layer.

## 5. Performance Considerations

- Pagination for heavy lists.
- Short‑circuit privilege checks.
- Avoid N+1 by batching mapping and enrichment.
- Accurate comparisons on nested attributes (`root.get("counterparty").get("name")`) so JPA builds sensible joins.

## 6. Extensibility

- Add new filters by composing another `Specification` predicate.
- Add new validations by creating a small validator and wiring it into the engine.
- Add new dashboard metrics by extending DTOs and reusing the existing aggregation helpers.
