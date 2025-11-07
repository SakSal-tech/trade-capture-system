# Introduction

This document gives a concise, developer-focused overview of the project layout and the important files you should inspect when working on the Trade Capture System. It complements the course README and is intended as a quick on-ramp for new contributors or reviewers.

This repo is a full-stack sample application with two main parts:

- backend — Java 21 / Spring Boot (Maven)
- frontend — React + TypeScript (Vite + pnpm)

Below you'll find package/ folder structure, the key features implemented in the project, security notes, and links to documentation and code locations.

---

# Packages and Code file structure

Top-level folders

- `backend/` — Java Spring Boot application

  - `pom.xml` — Maven project descriptor
  - `src/main/java/...` — application source code
    - `com/technicalchallenge/service/AdditionalInfoService.java` — settlement additional-info CRUD, audit rows and event publishing
    - `com/technicalchallenge/Events/*` — application event DTOs (e.g. `SettlementInstructionsUpdatedEvent`)
    - `com/technicalchallenge/service/TradeService.java` — core trade business logic
    - `com/technicalchallenge/service/TradeDashboardService.java` — backend dashboard aggregates
    - `com/technicalchallenge/controller/TradeSettlementController.java` — REST endpoints for settlement instructions (authorization via `@PreAuthorize`)
    - `com/technicalchallenge/security/UserPrivilegeValidator` — fine-grained per-trade checks used by services/controllers
  - `src/main/resources/` — Spring configuration and SQL fixtures (H2)

- `frontend/` — React + TypeScript UI

  - `package.json` / `pnpm-lock.yaml` — npm/pnpm manifest and lock
  - `src/pages/TradeDashboard.tsx` — dashboard page (charts, summary cards, recent trades)
  - `src/components/DownloadSettlementsButton.tsx` — CSV export button for settlements
  - `src/modal/SettlementTextArea.tsx` — modal used to view/edit settlement instructions
  - `src/stores/userStore.ts` — simple user store used by the UI
  - `src/utils/api.ts` — API helper methods used by the UI to call backend endpoints

- `docs/` — additional project documentation and developer notes

  - `candidate-checklist.md`, `design.md`, `detailed-project-plan.md`, and many more developer-oriented docs

- `data/`, `target/`, and other support files appear in the repo root and build output directories.

Notes and patterns

- Events: the backend uses Spring Application Events to decouple additional-info persisting from notification logic. Events live under `com.technicalchallenge.Events`.
- Audits: settlement changes create entries under `additional_info_audit` (see `data.sql` in `target/classes` for example fixtures used in tests).
- Tests: backend tests run with Maven Surefire (see `mvn -DskipTests=false test`).

---

# Example package tree (from screenshot)

Below is a monospace package/file tree for this project showing the real `src/main/java/com/technicalchallenge` package layout and the key frontend folders. This is more complete than the screenshot example and should help you find the files you changed or added.

```
src/
└── main/
    ├── java/
    │   └── com/
    │       └── technicalchallenge/
    │           ├── BackendApplication.java
    │           ├── config/
    │           │   ├── SecurityConfig.java
    │           │   ├── WebConfig.java
    │           │   ├── OpenApiConfig.java
    │           │   └── ModelMapperConfig.java
    │           ├── controller/
    │           │   ├── TradeController.java
    │           │   ├── TradeDashboardController.java
    │           │   ├── TradeSettlementController.java
    │           │   ├── TradeLegController.java
    │           │   ├── TradeStatusController.java
    │           │   ├── BookController.java
    │           │   ├── UserController.java
    │           │   ├── DownloadController.java (CSV export endpoints)
    │           │   └── ApiExceptionHandler.java
    │           ├── service/
    │           │   ├── TradeService.java
    │           │   ├── TradeDashboardService.java
    │           │   ├── AdditionalInfoService.java
    │           │   ├── TradeLegService.java
    │           │   ├── ApplicationUserService.java
    │           │   ├── UserPrivilegeService.java
    │           │   └── TradeRsqlVisitor.java
    │           ├── repository/
    │           │   ├── TradeRepository.java
    │           │   ├── AdditionalInfoRepository.java
    │           │   ├── AdditionalInfoAuditRepository.java
    │           │   ├── UserPrivilegeRepository.java
    │           │   └── (many other JPA repositories: BookRepository, IndexRepository, etc.)
    │           ├── model/
    │           │   ├── Trade.java
    │           │   ├── TradeLeg.java
    │           │   ├── AdditionalInfo.java
    │           │   ├── AdditionalInfoAudit.java
    │           │   ├── ApplicationUser.java
    │           │   └── auxiliary domain models (Book, Desk, SubDesk, PayRec...)
    │           ├── dto/
    │           │   ├── TradeDTO.java
    │           │   ├── TradeLegDTO.java
    │           │   ├── AdditionalInfoDTO.java
    │           │   └── summary DTOs (DailySummaryDTO, TradeSummaryDTO...)
    │           ├── mapper/
    │           │   ├── TradeMapper.java
    │           │   ├── AdditionalInfoMapper.java
    │           │   └── other MapStruct mappers
    │           ├── Events/
    │           │   ├── SettlementInstructionsUpdatedEvent.java
    │           │   ├── TradeCancelledEvent.java
    │           │   ├── RiskExposureChangedEvent.java
    │           │   └── NotificationEventListener.java
    │           ├── security/
    │           │   ├── DatabaseUserDetailsService.java
    │           │   ├── UserPrivilegeValidator.java
    │           │   └── (security helpers / test configs under src/test)
    │           ├── validation/
    │           │   ├── TradeValidationEngine.java
    │           │   ├── TradeValidationResult.java
    │           │   ├── SettlementInstructionValidator.java
    │           │   ├── TradeDateValidator.java
    │           │   ├── TradeLegValidator.java
    │           │   ├── EntityStatusValidator.java
    │           │   └── EntityStatusValidationEngine.java
    │           ├── exception/
    │           │   └── GlobalExceptionHandler.java
    │           └── util/ (mappers, converters, helpers)
    └── resources/
        ├── application.properties
        ├── data.sql (test fixtures)
        └── static/

frontend/
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── AppRouter.tsx
    ├── pages/
    │   ├── TradeDashboard.tsx
    │   ├── TradeDetails.tsx
    │   └── other pages...
    ├── components/
    │   ├── DownloadSettlementsButton.tsx
    │   ├── SummaryCard.tsx
    │   └── charts/ (Recharts components)
    ├── modal/
    │   └── SettlementTextArea.tsx
    ├── stores/
    │   └── userStore.ts
    ├── utils/
    │   └── api.ts
    └── assets/

docs/
├── PROJECT-FILE-STRUCTURE.md
├── TRADING-API-README.md
├── Step5-nonstandard-settlement.md
├── Searches-RSQL-SettlementExport-Endpoints.http
├── Steps-3-5-DeveloperLog-And-Explanations.md
└── diagrams/
    ├── backend-uml-class-diagram.drawio.xml
    ├── TradeRsqlVisitor-Overview.drawio
    └── other diagrams (*.png, *.drawio)

```

Notes:

- This tree is intentionally focused on the parts you changed or added (events, validation, security, settlement UI and exports). It is not an exhaustive byte-for-byte listing of every file in the repo, but it reflects the canonical package layout and the most relevant files for development and review.
- To generate a live tree from your workspace use `tree -a -I target,node_modules -L 4` at the repo root (or `Get-ChildItem -Recurse` on PowerShell).

# Features I implemented

This project includes the following implemented features (high level):

- Settlement instruction capture and audit

  - When settlement instructions are created/updated/deleted via the TradeSettlementController, the change is stored and an audit row is written.
  - The `AdditionalInfoService` publishes a `SettlementInstructionsUpdatedEvent` (and other events) so listeners can create notifications without coupling to the service logic.

- Application events and notification listener

  - Event DTOs added under `backend/src/main/java/com/technicalchallenge/Events/` (e.g. `SettlementInstructionsUpdatedEvent`).
  - `NotificationEventListener` component receives events via `@EventListener` - currently logs event receipt and is the recommended place to persist notifications or broadcast via SSE/WebSockets.

- Dashboard UI

  - `TradeDashboard.tsx` provides a simple summary row, charts (Recharts), and a recent trades table. It fetches weekly and daily summaries from the backend.
  - Export button (`DownloadSettlementsButton`) allows users to download settlements CSVs.

- Non-standard settlements export

  - A CSV export path and related frontend button were added to allow ops to download settlement data.

- Controller privilege adjustment

  - The `TradeSettlementController` `@PreAuthorize` on put/create for settlement instructions was expanded to include `MIDDLE_OFFICE` and `ADMIN` roles (so middle-office users can create/update settlement instructions when intended).

- Defensive changes to preserve test compatibility
  - `AdditionalInfoService` constructor was adjusted to keep a delegating constructor for unit tests; publishing is wrapped in try/catch to avoid breaking writes if no publisher is present in test instantiation.

---

# Security and user privileges

Roles used (examples found in the codebase and tests):

- `ROLE_TRADER` (TRADER)
- `ROLE_SALES` (SALES)
- `ROLE_MIDDLE_OFFICE` (MIDDLE_OFFICE / MO)
- `ROLE_ADMIN` (ADMIN)

Notes:

- Controller-level security uses Spring EL `@PreAuthorize` checks on controller methods. For example, create/update settlement endpoints use `hasAnyRole('TRADER','SALES','MIDDLE_OFFICE','ADMIN')` after the change.
- Per-trade fine-grained checks (e.g. whether a user may modify a specific trade) are implemented in `UserPrivilegeValidator` and used inside services — these run after the controller-level role checks.
- If you get a 403 for a user that appears to have the right role, check the controller `@PreAuthorize` first (it runs before service validators), then check any `UserPrivilegeValidator` logic.

## Security & Validation files (exact paths)

Below are the primary security and validation Java classes you'll want to inspect. They are the canonical places to check authentication/authorization wiring and the trade/settlement validation rules.

Security (auth / privileges)

- `backend/src/main/java/com/technicalchallenge/config/SecurityConfig.java`
  - Configures PasswordEncoder, DaoAuthenticationProvider, and the global SecurityFilterChain. Method-level security is enabled here with `@EnableMethodSecurity`.
- `backend/src/main/java/com/technicalchallenge/security/DatabaseUserDetailsService.java`
  - Loads ApplicationUser from the database and maps user profile and DB privileges into Spring Security authorities (ROLE\_ and privilege authorities).
- `backend/src/main/java/com/technicalchallenge/security/UserPrivilegeValidator.java`
  - Component (`@Component("securityUserPrivilegeValidator")`) that centralises per-trade ownership and edit/view checks used by services.

Test / test-time security

- `backend/src/test/java/com/technicalchallenge/config/TestSecurityConfig.java`
  - Test-only security config that provides an in-memory UserDetailsService and a lightweight SecurityFilterChain for integration tests.

Validation (trade and settlement rules)

- `backend/src/main/java/com/technicalchallenge/validation/TradeValidationEngine.java`
  - Orchestrates trade-level validators and exposes `validateSettlementInstructions` to validate settlement text.
- `backend/src/main/java/com/technicalchallenge/validation/SettlementInstructionValidator.java`
  - Field-level validator for settlement instruction free-text (length, allowed chars, escaped quotes, semicolon ban, etc.).
- `backend/src/main/java/com/technicalchallenge/validation/TradeDateValidator.java`
  - Validates required dates and logical relationships (tradeDate, start/maturity, recency rules).
- `backend/src/main/java/com/technicalchallenge/validation/TradeLegValidator.java`
  - Validates trade legs, rates and floating-leg index rules.
- `backend/src/main/java/com/technicalchallenge/validation/EntityStatusValidationEngine.java`
  - Adapter that delegates to `EntityStatusValidator` for repository-backed entity checks (injected bean used in integration runs).
- `backend/src/main/java/com/technicalchallenge/validation/EntityStatusValidator.java`
  - Repository-backed validation of referenced entities (books, counterparties, indices etc.).

Start here (quick guide)

- To debug a user 403 or privilege decision: open `UserPrivilegeValidator.java` (service-level rules) then `SecurityConfig.java` and `DatabaseUserDetailsService.java` (how roles/privileges are mapped). Those three files explain most 403s.
- To debug settlement validation failures: open `SettlementInstructionValidator.java` then `TradeValidationEngine.java` to see how the validator is wired into the service layer.

---

# Docs structure with links

Important docs in this repository (click the path to open in the project):

- Developer & feature docs

  - `docs/design.md` — design notes and architecture overview
  - `docs/detailed-project-plan.md` — project plan and milestones
  - `docs/Step 3 - Technical_Architecture_Overview .md` — technical architecture (review for eventing decisions)
  - `docs/Step4_Cashflow_BugInvestigation_and_Fix.md` — investigation notes for a cashflow bug
  - `docs/Step5-nonstandard-settlement.md` — non-standard settlement feature details
  - `docs/Step3-Enhancement2_Validation_Engine.md` — validation engine notes

- New developer entry points

  - Backend: `backend/src/main/java/com/technicalchallenge/service/AdditionalInfoService.java`
  - Events: `backend/src/main/java/com/technicalchallenge/Events/SettlementInstructionsUpdatedEvent.java`
  - Listener: `backend/src/main/java/com/technicalchallenge/Events/NotificationEventListener.java`
  - Controller: `backend/src/main/java/com/technicalchallenge/controller/TradeSettlementController.java`

- Frontend UI

  - Dashboard: `frontend/src/pages/TradeDashboard.tsx`
  - CSV export: `frontend/src/components/DownloadSettlementsButton.tsx`
  - Settlement modal: `frontend/src/modal/SettlementTextArea.tsx`

- Run & build notes
  - Backend (Maven): from `backend/` run `mvn clean test` to run unit/integration tests.
  - Frontend (pnpm): from `frontend/` run `pnpm build` (or `pnpm dev` for local dev) to build the UI. Lint rules are enforced during the Vite build and may fail if `@typescript-eslint` rules detect `any` or unused variables.

---
