# Step 3 – Enhancement2_Validation_Engine

**Scope:** End‑to‑end validation of trade data and user permissions across services and controllers.  
**Code Areas:** `TradeValidationEngine`, `TradeDateValidator`, `TradeLegValidator`, `EntityStatusValidator`, `EntityStatusValidationEngine`, `UserPrivilegeValidator`, `UserPrivilegeValidationEngine`, `TradeValidationResult`, selected guards in `TradeService` and controllers.

## 1. What I Implemented and Why

I built a **layered validation architecture** that keeps rules **cohesive and testable**, and makes failures readable for the UI. It covers dates, cross‑leg consistency, entity status, and user privilege enforcement.

- **Central orchestration:** `TradeValidationEngine.validateTradeBusinessRules(TradeDTO)` gives me one place to run every rule for create/amend flows.
- **Date rules:** `TradeDateValidator` ensures logical sequences and recency thresholds.
- **Leg rules:** `TradeLegValidator` validates maturity alignment, pay/receive flags, and rate/index requirements.
- **Entity status rules:** `EntityStatusValidator` verifies referenced entities exist and are active.
- **Privilege rules:** `UserPrivilegeValidator` and the engine wrapper enforce role‑based permissions for operations like create, amend, cancel, and terminate.
- **State:** I aggregate messages in `TradeValidationResult` so a single request can surface **all issues at once**.

### Before

- Scattered checks; some fields validated, others not.
- Invalid trades could pass into downstream steps settlement issues and operational risk.

### After

- A **single orchestration point** (`TradeValidationEngine`) coordinates:
  - Date rules (`TradeDateValidator`)
  - Leg consistency & leg-type rules (`TradeLegValidator`)
  - Entity existence & status checks (`EntityStatusValidationEngine`/`EntityStatusValidator`)
  - User privilege enforcement (`UserPrivilegeValidationEngine`/`UserPrivilegeValidator`)
- Errors aggregated in **`TradeValidationResult`** with `{ valid, errors[] }`.

**Outcome**: Repeatable, testable, _explainable_ validation with clear messages.

This structure means I can explain a failure to a trader in one response rather than sending them through a whack‑a‑mole loop.

## 2. TradeValidationEngine: The Conductor

### 2.1 Where I run it

- In `TradeService.createTrade(...)` and `TradeService.amendTrade(...)` before any persistence, I call `tradeValidationEngine.validateTradeBusinessRules(tradeDTO)`.
- If invalid, I throw `ResponseStatusException(HttpStatus.BAD_REQUEST, "...")` with concatenated rule messages.

### 2.2 Why a central engine

One call site, one decision. This avoids scattered rule checks that become inconsistent over time. It also keeps unit testing simple: I can pass a crafted `TradeDTO` and assert the output `TradeValidationResult` directly.
Orchestration: `TradeValidationEngine`

**Responsibilities**:

- Run date validations first .
- If legs exist, run leg validators .
- Optionally run repository-backed entity status validation (book/counterparty/user).
- Surface human-readable messages in `TradeValidationResult`.

**Why centralisation matters**:

- Controllers and services have a **single place** to call.
- Consistent error semantics across REST endpoints.
- Easy to add stages later (e.g., credit/risk checks) without changing controllers.

## 3. Date Validation Rules (TradeDateValidator)

`TradeValidationResult` consolidates outcomes:

- `boolean valid` (default: true)
- `List<String> errors` (messages appended via `setError(msg)`)

**Design choice**: never throw inside validators; **collect** all problems for the client to fix in one shot. The service/controller can decide when to convert into HTTP 400.

### 3.1 Rules I enforce

- Trade date must be present.
- If the trade includes legs, both `tradeStartDate` and `tradeMaturityDate` are required.
- `tradeMaturityDate` cannot be before `tradeStartDate`.
- `tradeStartDate` cannot be before `tradeDate`.
- `tradeDate` cannot be more than 30 days in the past.

### 3.2 Implementation techniques

- I guard for nulls first to prevent NPEs, then compare with `isBefore(...)` once both dates exist.
- I calculate recency with `ChronoUnit.DAYS.between(tradeDate, LocalDate.now())`.

### 3.3 Alternatives considered

- Bean Validation annotations alone. Useful, but they don’t express cross‑field logic like “start after trade date”. I keep field annotations for presence, but cross‑field rules live here.

## 4. Cross‑Leg Rules (TradeLegValidator)

### 4.1 Rules I enforce

- A trade needs at least two legs.
- Both legs must have identical `tradeMaturityDate`.
- Pay/receive flags must be present on both legs and must be **opposite**.
- For floating legs: an **index** must be specified (`indexId` or `indexName`).
- For fixed legs: a **valid rate** must be present (positive, reasonable precision).

### 4.2 Implementation techniques

- I extract the first two legs and validate symmetrical constraints.
- I wrote `validateFloatingLegIndex(TradeLegDTO)` as a standalone helper to make the rule easy to unit test.
- For fixed legs, I treat the rate as a `BigDecimal` and reject excessive scale to avoid accidental fat‑finger entries.
- I return booleans for focused helpers, but rule failures flow into a shared `TradeValidationResult` inside the engine loop.

### 4.3 Alternatives considered

- Validating legs only when both are present. I chose to fail fast on count and then be explicit about which leg rules failed.
- Pushing rate/index rules to persistence. I prefer surfacing meaningful messages at the **API boundary**.

## 5. Entity Status Validation (EntityStatusValidator)

### 5.1 What I checked

- **Book:** must exist (by id or name) and be `active`.
- **Counterparty:** must exist and be `active`.
- **Trader user:** must exist and be `active` (by id or login/first name, to support historical payloads).

### 5.2 Implementation techniques

- I inject repositories and look up by **whichever identifier** the DTO provides, preferring IDs for reliability.
- I record a **specific** message for “not found” and for “not active” to help the desk act quickly.
- If a repository is missing in production, that indicates mis‑wiring; I throw on construction. This keeps behaviour strict in real environments and explicit in tests.

### 5.3 Alternatives considered

- Allowing “soft missing” lookups during tests. I chose explicit constructor checks and proper mocks for clarity.

## 6. Privilege Validation (UserPrivilegeValidator + Engine)

### 6.1 Rules I enforced

- **TRADER:** create, amend, terminate, cancel.
- **SALES:** create, amend.
- **MIDDLE_OFFICE:** amend, view.
- **SUPPORT:** view.

### 6.2 Implementation techniques

- I normalise inputs to uppercase and trim to avoid brittle comparisons.
- I use a `switch` on `userType` and then assert the `action` is allowed, writing a human message like “SUPPORT cannot AMEND trades” when not allowed.
- I also provide convenience methods `canViewTrade(Trade, Authentication)` and `canEditTrade(Trade, Authentication)` where I combine role checks with ownership (`ownerLogin.equalsIgnoreCase(authentication.name)`).

### 6.3 Alternatives considered

- Centralising everything into Spring Expression Language in `@PreAuthorize`. I do use `@PreAuthorize`, but the **service‑layer guard** remains necessary because not all entry points are HTTP, and unit tests may bypass controllers.

## 7. ValidationResult and Error Messaging (TradeValidationResult)

- I store a boolean `valid` and a `List<String>` of errors.
- Validators **append** messages instead of raising exceptions; the engine reports them together. This improves UX because traders get a single, actionable report.

## 8. Defensive Checks in Services

- `TradeService.getTradeById(...)` enforces a **server‑side ownership** check for TRADERs without elevated privileges, throwing `AccessDeniedException` early.
- `TradeService.cancelTrade(...)` checks edit rights using `UserPrivilegeValidator` or an inline fallback if the validator is not yet wired (keeps older tests working).

## 9. How This Meets the Business Requirement

- Invalid trades are **stopped before** they persist, with explainable messages.
- Rules reflect standard industry practice for OTC legs and lifecycle operations.
- Ownership and roles are respected consistently across controllers and services.

## 10. How to Extend Safely

- Add a new rule by creating a focused validator method and wiring it in `TradeValidationEngine`.
- Keep messages short and specific.
- Write a unit test per rule so regressions are visible.

## 11. Key Classes and Methods

- `TradeValidationEngine.validateTradeBusinessRules(TradeDTO)`
- `TradeDateValidator.validate(...)`
- `TradeLegValidator.validateTradeLeg(...)`, `.validateTradeLegPayReceive(...)`, `.validateFloatingLegIndex(...)`, `.validateLegRate(...)`
- `EntityStatusValidator.validate(...)` and `EntityStatusValidationEngine.validate(...)`
- `UserPrivilegeValidator.validateUserPrivilege(...)`, `canViewTrade(...)`, `canEditTrade(...)`
- `TradeValidationResult`

### More explanations

Date Validation Rules `TradeDateValidator`

**Presence**

- `tradeDate` **required** (always).
- When legs are present: `tradeStartDate` and `tradeMaturityDate` required.

**Ordering**

- **Maturity >= Start >= Trade**.
- Implemented with null-safe checks to avoid NPEs.

**Recency**

- `tradeDate` cannot be **> 30 days in the past** (risk/control requirement).

**Design**: Null-safe, composable, and precise messages:

- `"tradeStartDate is required when trade legs are present"`,
- `"Maturity date cannot be before start date"`, etc.

## Leg Validation `TradeLegValidator`

### Structural Rules

- A trade must have **exactly two legs** (service guard) and at least two legs (validator guard).
- Both legs must provide **maturity dates**.
- Both legs must have **opposite pay/receive flags** (`PAY` vs `RECEIVE`).

### Latent Issues Fixed

- Duplicate retrieval of legs simplified.
- Correct equality check of maturity dates (avoid contradictory before/equal checks).
- Null-safe flag comparison before `.equals(...)`.

### Floating/Fixed Constraints

- **Floating legs**: must specify an **index** (either `indexId` or `indexName`).
- **Fixed legs**: must provide a **valid rate** (present, > 0, <= 100, with ≤ 4 decimal places).
- Other leg types: either `null` or positive rate depending on business rule; your validator defaults to **reject negative**.

## Entity Status Validation `EntityStatusValidationEngine`

**Purpose**: ensure all referenced entities **exist** and are **active** before accepting the trade.

**Checks**:

- **Book**: by `bookId` or `bookName` must exist and be `active=true`.
- **Counterparty**: by `counterpartyId` or `counterpartyName` must exist and be active.
- **Trader user**: by id or name/login record must exist and be active.

**Design choices**:

- Constructor **requires** repositories (fail-fast).
- **Null-safe** and **resilient** messaging.
- Explicit `"... reference is required"` messages if missing.

## User Privilege Enforcement

### Validation Engine (`UserPrivilegeValidationEngine`)

- Coordinates privilege checks using injected `UserPrivilegeValidator`.
- Returns `TradeValidationResult` (consistent API with other validators).

### Rules (`UserPrivilegeValidator`)

Also supports granular pseudo-roles like `TRADE_VIEW`, `TRADE_EDIT`, `TRADE_DELETE`, etc.

**Implementation details**:

- **Normalized** (`trim().toUpperCase()`) inputs avoid brittle comparisons.
- Guarded nulls for `userType` and `action` with clear errors.
- Switch-case for clarity and easy extension.

### Service-Layer Defenses

- `TradeService.cancelTrade(...)` enforces editor permissions **even if** the controller annotation is misconfigured.
- Ownership semantics: owner or elevated roles can edit; ownerless trades permit TRADER (for legacy data/fixtures).

## How It All Fits in Trade Lifecycle

**Create Trade** (`TradeService.createTrade`)

- Optional auto-generate tradeId.
- Run **central validation**; convert failures to `400 BAD_REQUEST`.
- Populate reference data by name/ID (book, counterparty, users, types).
- Save trade + legs + cashflows (cashflows can calculate payment values for both fixed and floating when rate supplied).

**Amend Trade**

- Deactivate existing row; create new version (null-safe version increment).
- Re-run validation.
- Update or insert settlement info (if present – staged for later focus).

**Terminate / Cancel**

- Enforce **server-side permissions**.
- Update status; maintain audit fields (timestamps).

## Error Handling Strategy

- Validators **collect** messages; services translate to exceptions with aggregated text for REST.
- `GlobalExceptionHandler` standardizes JSON shape: `timestamp`, `status`, `message` (keeps UI simple).
- Avoid leaking stack traces to clients; include details in logs

## Testing Guidance

**Unit tests**:

- Date ordering and recency edge cases.
- Pay/receive opposites; maturity equality; floating index presence; fixed rate bounds/precision.
- Entity existence + active flags (mock repos).
- Privilege permutations per role/action.
- Aggregation of multiple errors in a single pass.

**Integration tests**:

- Full create/amend/cancel/terminate flows with invalid inputs 400/403.
- Repository-backed existence/active checks.
- Service-layer permission enforcement (owner vs non-owner).

## Design Rationale & Trade-offs

- Centralization eases maintenance and scales with new rules.
- Error **aggregation** improves UX (fix multiple issues per submission).
- Privilege checks at **multiple layers** (annotations + service) = defense-in-depth.
- Null-safety and precise messages reduce support load.

## Summary

This enhancement transforms validation from ad-hoc checks into a **coherent, auditable control system**. It blocks invalid trades upfront, enforces permissions consistently, and leaves a clean paper trail for risk and operations.
