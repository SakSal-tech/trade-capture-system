# Trade Capture System — API README

## Quick summary

- Backend: Java 21, Spring Boot 3 (Spring 6). Key changes:

  - `AdditionalInfoService` now publishes immutable events when settlement instructions are created/updated/deleted using Spring's ApplicationEventPublisher.
  - Immutable event DTOs added under `com.technicalchallenge.Events` (e.g. `SettlementInstructionsUpdatedEvent`).
  - `NotificationEventListener` receives events via `@EventListener` (currently logs; intended place to persist notifications or push via SSE/WebSocket).
  - Controller-level authorization for settlement endpoints expanded to include `MIDDLE_OFFICE` and `ADMIN` where business rules require it.
  - Defensive constructor changes to `AdditionalInfoService` (backwards-compatible constructor and guarded publish calls) to keep unit tests and manual instantiation stable.

- Validation: `SettlementInstructionValidator` field-level rules tightened (length, allowed characters, escaped quotes, semicolon ban); validation orchestration in `TradeValidationEngine`.

- Security: `UserPrivilegeValidator` centralises per-trade canView/canEdit logic; `DatabaseUserDetailsService` maps DB roles/privileges to Spring authorities; `SecurityConfig` enables method-level security.

- Frontend: small TypeScript/ESLint fixes in `frontend/src/pages/TradeDashboard.tsx` (removed explicit any, removed unused navigation helpers, fixed hook dependencies and disabled buttons during loading) to resolve pnpm build issues.

## Table of contents

- Quick start (run & test)
- Key files to inspect (start here)
- How to reproduce / debug common problems
- How to test events and listeners
- Frontend build notes
- Contributing & next steps

## Quick start (run & test)

Prerequisites:

- Java 21
- Maven 3.8+
- Node 18+ / pnpm

Backend (from repo root):

```bash
cd backend
mvn clean test
mvn -DskipTests=false verify    # runs integration/unit tests
```

Run the backend locally (dev):

```bash
cd backend
mvn spring-boot:run
```

Frontend (from repo root):

```bash
cd frontend
pnpm install
pnpm build      # production build (runs TypeScript/ESLint checks)
pnpm dev        # local dev server with HMR
```

If you prefer to run frontend while backend runs locally, start backend on default port and run `pnpm dev` from `frontend/`.

## Key files to inspect (start here)

- Backend eventing

  - `backend/src/main/java/com/technicalchallenge/service/AdditionalInfoService.java` — CRUD for additional info; now publishes settlement events. Inspect constructor wiring if tests manually instantiate the service.
  - `backend/src/main/java/com/technicalchallenge/Events/SettlementInstructionsUpdatedEvent.java` — immutable DTO used as event payload.
  - `backend/src/main/java/com/technicalchallenge/Events/NotificationEventListener.java` — example listener; extend here to persist notifications or send SSE/WebSocket messages.

- Security & privileges

  - `backend/src/main/java/com/technicalchallenge/config/SecurityConfig.java` — PasswordEncoder, DaoAuthenticationProvider, SecurityFilterChain, and `@EnableMethodSecurity`.
  - `backend/src/main/java/com/technicalchallenge/security/DatabaseUserDetailsService.java` — maps ApplicationUser + DB privileges to GrantedAuthority set.
  - `backend/src/main/java/com/technicalchallenge/security/UserPrivilegeValidator.java` — component used by services for per-trade access checks (canViewTrade / canEditTrade).
  - `backend/src/test/java/com/technicalchallenge/config/TestSecurityConfig.java` — test-only security setup used in integration tests.

- Validation

  - `backend/src/main/java/com/technicalchallenge/validation/SettlementInstructionValidator.java` — field-level settlement text rules (length, allowed characters, escaped quotes, semicolon ban).
  - `backend/src/main/java/com/technicalchallenge/validation/TradeValidationEngine.java` — orchestrates validators and exposes `validateSettlementInstructions` used by service endpoints.

- Frontend
  - `frontend/src/pages/TradeDashboard.tsx` — small TypeScript/ESLint fixes applied to remove `any`, unused hooks, and ensure hook dependency correctness.

## How to reproduce / debug common problems

- Getting 403s (AccessDenied):

  1. Check controller `@PreAuthorize` annotations first (they run before service checks). Example: settlement endpoints were updated to include MIDDLE_OFFICE and ADMIN.
  2. Inspect `UserPrivilegeValidator.java` — service-level ownership/role fallback logic (ownerless trades and elevated roles logic are here).
  3. Inspect `DatabaseUserDetailsService.java` to confirm how the DB user profile maps to authorities (ROLE\_... and raw privilege strings like `TRADE_VIEW`).

- Tests failing with Null ApplicationEventPublisher: this happened after adding publisher to `AdditionalInfoService`.

  - Solution: unit tests that manually instantiate `AdditionalInfoService` should either supply a mock/NoOp `ApplicationEventPublisher` or use the delegating constructor that accepts fewer args (a backwards-compatible constructor was kept). Alternatively, update tests to @MockBean the publisher or to construct the service via Spring test context.

- Frontend pnpm build fails with ESLint/TS errors:
  - Remove explicit `any` casts or add proper types.
  - Remove unused imports/variables or prefix them with `_` if intentionally unused.
  - Ensure effect dependencies arrays include all referenced variables (or justify with eslint-disable-next-line and a precise comment).

## How to test events and listeners

- Unit test for producer (`AdditionalInfoService`):

  - Inject a mocked ApplicationEventPublisher (Mockito) and verify `publishEvent(...)` is called with an instance of `SettlementInstructionsUpdatedEvent` when performing an upsert/delete operation.

- Integration test (Spring context):

  - Use `@SpringBootTest` and register a Test `ApplicationListener` or `@SpyBean` for `NotificationEventListener` to assert the listener receives the event when real service method runs and DB writes succeed.

- Manual runtime test:
  1. Start backend (`mvn spring-boot:run`).
  2. Use curl/Postman to call the settlement instruction endpoint (PUT/POST/DELETE) and then check logs for `NotificationEventListener` INFO messages showing event payload.

## Frontend build notes

- ESLint (`@typescript-eslint`) rules are enforced during `pnpm build`. Common fixes applied in `TradeDashboard.tsx`:
  - Remove `any` casts and use narrow types (number | string | null).
  - Remove unused navigation helpers and functions.
  - Ensure hook dependency arrays include referenced values.
  - Use `loading` state to disable buttons while actions complete to silence unused-var warnings and avoid race conditions.

If `pnpm build` still fails, run:

```bash
cd frontend
pnpm install
pnpm build --reporter silent
```

and paste the first 40 lines of the error output here — I'll fix the offending lines.

## Contributing & next steps

- If you add more event types, follow the existing pattern:

  1. Create an immutable event DTO under `com.technicalchallenge.Events`.
  2. Publish via `ApplicationEventPublisher` from services after successful DB transaction (consider @TransactionalEventListener if you need to publish only after commit).
  3. Add or extend `NotificationEventListener` to persist notifications or broadcast via SSE/WebSocket.

- Tests:

  - Prefer small unit tests with Mockito for service logic and event publishing verification.
  - Use integration tests annotated with `@SpringBootTest` to validate listener wiring and end-to-end flows.

- Suggested small follow-ups:
  - Replace System.err prints with SLF4J logging where publish errors are caught.
  - Add a NoOp ApplicationEventPublisher implementation for lightweight unit tests, or update tests to inject mocks.
  - Persist notifications via JPA and add an SSE/WebSocket push endpoint for clients.

## Validation package — file structure

The core validation logic lives in the `com.technicalchallenge.validation` package. Below is an ASCII diagram of the package and the primary validator classes to inspect.

```
com.technicalchallenge.validation
├── TradeValidationEngine.java        # orchestrates validators, exposes validateSettlementInstructions()
├── TradeValidationResult.java        # accumulator for validation errors / messages
├── SettlementInstructionValidator.java # free-text rules for settlement instructions
├── TradeDateValidator.java           # trade date/start/maturity checks
├── TradeLegValidator.java            # validates legs, rates, floating-index
├── EntityStatusValidationEngine.java # adapter that calls repository-backed EntityStatusValidator
└── EntityStatusValidator.java        # DB-backed checks for referenced entities (books, indices, etc.)
```

Inspect `SettlementInstructionValidator.java` first for settlement-specific rules (length, allowed characters, escaped quotes, semicolons). Then open `TradeValidationEngine.java` to see how validators are wired into services.

## Files changed / created (frontend & backend highlights)

I've updated and added several frontend components and pages related to settlement editing and the dashboard. Key frontend files:

- `frontend/src/modal/SettlementTextArea.tsx` — new modal component used to view and edit settlement instructions (rich text area with validation hook).
- `frontend/src/pages/TradeDashboard.tsx` — dashboard page (charts, summaries, recent trades); TypeScript/ESLint fixes applied and UI wired to summary endpoints.
- `frontend/src/components/DownloadSettlementsButton.tsx` — CSV export button for settlements (nonstandard export flows supported).
- Other new/updated frontend files: `frontend/src/stores/userStore.ts`, `frontend/src/utils/api.ts`, `frontend/src/components/*` (see project for full list).

Backend highlights related to the dashboard and settlements:

- `backend/src/main/java/com/technicalchallenge/service/TradeDashboardService.java` — backend aggregates and endpoints used by the dashboard views (daily/weekly summaries).
- `backend/src/main/java/com/technicalchallenge/service/AdditionalInfoService.java` — publishes settlement events and writes audit rows.
- `backend/src/main/java/com/technicalchallenge/controller/TradeSettlementController.java` — settlement instruction endpoints; `@PreAuthorize` expanded to include `MIDDLE_OFFICE` and `ADMIN` where appropriate.

API testing and query files

- `docs/Searches-RSQL-SettlementExport-Endpoints.http` — HTTP/collection file containing example queries, RSQL search examples, and nonstandard settlement export requests. Use this to reproduce the RSQL-based searches and CSV export flows.
- `docs/Steps-3-5-DeveloperLog-And-Explanations.md` — detailed developer log covering steps 3-5 and the design/implementation decisions for eventing, validation, and security. Read this for step-by-step explanations.
- `docs/Step5-nonstandard-settlement.md` — feature notes and API examples for exporting nonstandard settlements.

Developer artifacts you should review

- `docs/test-fixes-template.md` — sample templates and notes on how I fixed the test failures that came with the original project.
- `docs/Development-Errors-and-fixes.md` — chronological error log and fixes applied during development.
- `docs/` diagrams folder — contains UML diagrams, flowcharts, and draw.io sources: `docs/diagrams/*.drawio`, `docs/diagrams/*.png`.

## Developer logs & supporting artifacts (review these)

Please review the project-level developer artifacts for full context, investigations, and fixes I applied during implementation. They contain step-by-step developer logs, test-fix examples, and diagrams (UML / flowcharts).

- [Steps 3-5 Developer Log & Explanations](docs/Steps-3-5-DeveloperLog-And-Explanations.md) — Detailed, chronological developer notebook covering Steps 3→5: design decisions, implementation notes, and rationale for the eventing, validation and security changes.
- [Development Errors & Fixes](docs/Development-Errors-and-fixes.md) — Chronological error log with stack traces, debugging notes and the fixes applied while developing and stabilising the project.
- [Test Fixes Template](docs/test-fixes-template.md) — Concrete templates and examples demonstrating how failing tests were diagnosed and fixed (useful when adding new tests or updating fixtures).
- [RSQL & Export HTTP Examples](docs/Searches-RSQL-SettlementExport-Endpoints.http) — Executable HTTP snippets (compatible with many editors/Postman) that demonstrate RSQL search queries, settlement export requests, and CSV export flows.
- [Nonstandard Settlement Export Spec](docs/Step5-nonstandard-settlement.md) — Feature notes, API examples and filter patterns used for exporting nonstandard settlements (includes example payloads and CSV column mapping).
- [Diagrams (UML / Flowcharts)](docs/diagrams/) — Folder containing UML class diagrams, sequence diagrams and flowcharts (`*.drawio`, `*.png`) illustrating event flows, validation flow and system components. Open the `*.drawio` sources to edit diagrams.
- [Project File Structure](docs/PROJECT-FILE-STRUCTURE.md) — The repository package/file tree and key files overview (this file) to help new contributors quickly find relevant code.
- [Design Notes & Architecture](docs/design.md) — High-level architecture, design tradeoffs and component interactions.
- [Detailed Project Plan](docs/detailed-project-plan.md) — Project milestones, scope and tracking for the delivered features.
- [Technical Architecture Overview (Step 3)](docs/Step 3 - Technical_Architecture_Overview .md) — Technical architecture notes and diagrams referenced during the Step 3 enhancements.
- [Security Architecture (Step 3)](docs/Step 3 - Security_Architecture .md) — Security design notes, privilege mapping and test configuration guidance.
- [Validation Engine Notes (Step 3)](docs/Step 3 - Enhancement2_Validation_Engine.md) — Details about the validation engine, field-level validators and entity status checks.

Open these files when you need the full reproduction steps, error traces, and the design notes that explain why changes were made.

### Frontend User Guide

This short guide describes how to use the UI pages and forms, the business validation rules the frontend enforces, and how user privileges affect available actions. It includes two quick flows: one for Developers (how to extend and test the UI) and one for Traders (how to use the app day-to-day).

For Traders (day-to-day)

- Dashboard (`/` or Trade Dashboard): shows daily and weekly summaries, charts, and a recent trades table. Use the filters at the top to restrict by date, book or trader.
- Search & RSQL: use the Search box or import `docs/Searches-RSQL-SettlementExport-Endpoints.http` into Postman to run advanced RSQL queries (for ops/reporting). RSQL lets you combine field comparisons (e.g., `tradeDate>2025-01-01;book==FX*`).
- View trade details: click a trade row to open details. The settlement area shows the current settlement instructions and audit history.
- Edit settlement instructions: click the Edit button to open the `SettlementTextArea` modal. Business validation rules enforced on save:
  - Length: 10–500 characters.
  - Semicolons (`;`) are forbidden.
  - Unescaped quotes are rejected; use `\"` for double quotes if needed.
  - Allowed characters: letters, numbers, spaces and common punctuation (see backend `SettlementInstructionValidator` for exact pattern).
- Save: after validation passes the UI calls the backend settlement endpoint. If the save succeeds, an audit row is written and an event is published (notification may appear depending on listener implementation).
- Exporting: use the CSV export button on the dashboard (`DownloadSettlementsButton`) to export standard and nonstandard settlement files. For complex filters use the RSQL export endpoints documented in `docs/Step5-nonstandard-settlement.md`.
- Privileges: your available actions depend on your role and trade ownership. Typical rules:
  - TRADER: can create and edit own trades and settlement instructions.
  - SALES: limited edit/create depending on business rules.
  - MIDDLE_OFFICE / ADMIN: allowed to create/update settlement instructions for operational workflows. If you get a 403, first check that your account has the required role and that the trade owner matches (see `UserPrivilegeValidator`).

For Developers (extending/testing UI)

- Run & dev: from `frontend/` use `pnpm dev` for fast HMR; `pnpm build` runs the TypeScript and ESLint checks used by CI.
- Components to extend:
  - `frontend/src/modal/SettlementTextArea.tsx` — modal and validation hook; add fields or change validation messages here.
  - `frontend/src/pages/TradeDashboard.tsx` — main dashboard page; add charts and summary cards or change API calls to new endpoints.
  - `frontend/src/components/DownloadSettlementsButton.tsx` — export logic; adjust CSV columns or filters here.
- API layer: `frontend/src/utils/api.ts` centralises fetch calls. Use typed DTOs for responses and add client-side validation that mirrors backend rules.
- Tests: add unit tests for components with React Testing Library and mock API calls; include integration tests for common flows. Fix ESLint/TS issues by following project lint rules—avoid `any`, add explicit hook dependency arrays, and remove unused imports.
- Event testing: mock the backend responses for settlement save and assert that the modal closes, audit entries refresh, and download buttons work.
