# Trade Capture System -- Enterprise Trading Platform (Portfolio Project)

This repository contains an **enterprise-style Trade Capture System**
delivered as part of a banking software engineering programme in
collaboration with UBS and Code Black Females.\
My role on this project was to **stabilise, correct, extend, and
production‑harden the system end‑to‑end** across **backend, validation,
security, calculations, eventing, and frontend UX**.

This project demonstrates my ability to work productively in a **large
legacy codebase**, diagnose and fix **critical business defects**, and
deliver **production-grade full‑stack features**.

This project extends beyond application development to include **cloud architecture and deployment design**.
Alongside building and stabilising the system, I documented an **Azure cloud architecture** applying concepts learned from **Azure AZ-900 and AWS cloud fundamentals**, translating theory into hands-on architectural decisions.

## Tech Stack

**Backend** - Java 21, Spring Boot 3 (Spring 6) - Spring Security,
JPA/Hibernate - SQL (H2 for development) - RSQL, Specifications,
Pagination - ApplicationEventPublisher

**Frontend** - React, TypeScript - pnpm, ESLint - AG Grid

**Testing** - JUnit 5, Mockito - Spring Boot integration tests -
Dedicated security test configuration

## My Core Backend Contributions

### 1. Critical Financial Cashflow Calculation Fix

- Investigated a defect where **fixed‑leg cashflows were calculated
  \~100× too large**.
- Root causes:
  - Percentage rate not converted to decimal.
  - Incorrect use of `double` for monetary values.
- Implemented:
  - Correct percentage conversion.
  - **BigDecimal‑based monetary calculations**.
- Verified through **unit and integration tests**.
- Restored financial correctness for notional‑based quarterly
  cashflows.

### 2. Enterprise Business Validation Engine

Implemented a full, centralised validation framework including:

- Trade date, start date, and maturity sequencing.
- Leg consistency rules:
  - Opposite pay/receive.
  - Mandatory floating index.
  - Fixed‑rate validation.
  - Identical maturities across legs.
- Entity status checks:
  - Book, counterparty, index.
- Settlement instruction validation:
  - Length bounds (10--500).
  - Allowed character set.
  - Escaped quotes.
  - Semicolon blocking for export safety.

All orchestration is handled through centralised `TradeValidationEngine` with
structured validation results.

### 3. Settlement Instructions with Event Publishing

- Extended `AdditionalInfoService` to **publish immutable application
  events** on create, update, and delete.
- Implemented:
  - `SettlementInstructionsUpdatedEvent`
  - `NotificationEventListener`
- Designed for future:
  - WebSocket / SSE notifications
  - Operational audit pipelines
- Preserved backwards compatibility with existing unit tests.

### 4. Security & Role‑Based Access Control

- Centralised per‑trade privilege logic in `UserPrivilegeValidator`.
- Implemented DB‑backed authentication via
  `DatabaseUserDetailsService`.
- Mapped roles and fine‑grained privileges to Spring Authorities.
- Enabled **method‑level security**.
- Expanded access for settlement workflows for:
  - TRADER
  - SALES
  - MIDDLE_OFFICE
  - ADMIN
- Added dedicated **test security configuration** for integration
  tests.

### 5. Advanced Trade Search with RSQL

- Implemented:
  - Spring Specifications
  - Pagination
  - RSQL query parsing
- Enabled complex operational queries for:
  - Trade surveillance
  - Settlements
  - Reporting and exports

### 6. Test Stabilisation & Reliability

- Diagnosed and fixed **multiple failing unit and integration tests**.
- Introduced:
  - Defensive construction around event publishers.
  - Mock‑based strategies for Spring event testing.
- Improved overall backend stability and regression safety.

## My Core Frontend Contributions

### 1. Trade Dashboard (New Central Feature)

- Implemented dashboard navigation and routing.
- Integrated daily and weekly trade summaries.
- Connected frontend views to backend aggregation services.
- Enabled CSV export directly from the dashboard.

### 2. Full Settlement Instructions UI Integration

- Implemented a dedicated **SettlementTextArea modal**.
- Integrated settlement editing into:
  - Trade booking
  - Trade amendment flows
- Real‑time frontend validation mirroring backend rules.
- Non‑standard settlement detection.
- Audit‑safe save flow.
- CSV export compatibility.

### 3. Enterprise Navigation Redesign

- Added **vertical sidebar navigation**.
- Separated:
  - Global navigation (top bar)
  - Context‑specific navigation (sidebar)
- Improved discoverability of:
  - Dashboard
  - History
  - Admin tools

### 4. Snackbar (Toast) System Redesign

- Moved to top‑left to avoid modal collisions.
- Increased visibility duration.
- Added retry support.
- Added accessibility support using `aria-live`.
- Prevented hidden critical errors.

### 5. Robust CSV Export for Non‑Standard Settlements

- Implemented:
  - Content‑type validation to prevent HTML downloads.
  - Automatic filename extraction.
  - Loading state and duplicate‑click prevention.
- Eliminated corrupted exports and silent failures.

### 6. Trade Form UX Improvements

- Added contextual maturity‑date helper text.
- Clarified trade‑leg behaviour.
- Improved field rendering consistency across forms.

### 7. Frontend Test Coverage

- Added and stabilised tests for:
  - Navbar
  - Protected routing
  - AG Grid table
  - Loading spinner

## What This Project Demonstrates

- Enterprise **Java backend engineering**.
- Financial **business‑rule enforcement**.
- **Correct monetary computation** using BigDecimal.
- Secure **role‑based access control**.
- Event‑driven architecture foundations.
- Advanced search via **RSQL**.
- Real‑world **full‑stack integration**.
- Production‑grade **trading UX improvements**.
- Diagnostic debugging and test stabilisation in a legacy system.

## Cloud Architecture & Azure Design

As part of extending this project beyond application development, I designed and documented a **theoretical Azure cloud architecture** aligned with enterprise and regulated financial environments.

The focus was on applying **Azure Fundamentals (AZ-900)** concepts in practice, including compute selection, container strategy, CI/CD, monitoring, security, cost awareness, and governance.

### Key artefacts

- **Azure Cloud Architecture Design**

  - [`docs/Azure-Cloud-Architecture-Design-Documentation.md`](docs/Azure-Cloud-Architecture-Design-Documentation.md)

- **Architecture Diagrams**

  - End-to-end Azure architecture:
    - [`docs/diagrams/Azure Architecture.drawio.png`](docs/diagrams/Azure-Architecture.drawio.png)
  - Editable source:
    - [`docs/diagrams/Azure end-to-end architecture.drawio`](docs/diagrams/Azure-end-to-end-architecture.png)

- **Supporting Design & Learning Notes**
  - [`docs/design.md`](docs/design.md)
  - [`docs/docker-Explanations.md`](docs/docker-Explanations.md)
  - [`docs/LearningLinksUsed.md`](docs/LearningLinksUsed.md)

This work demonstrates how cloud fundamentals can be applied to a real system through **architecture reasoning and documentation**, without requiring live cloud deployment.

## Running the System

### Backend

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
pnpm install
pnpm dev
```

## Deep Technical Documentation

All detailed engineering artefacts, debugging logs, diagrams, and
architectural notes live under `/docs`:

- `docs/Steps-3-5-DeveloperLog-And-Explanations.md`
- `docs/Development-Errors-and-fixes.md`
- `docs/Searches-RSQL-SettlementExport-Endpoints.http`
- `docs/diagrams/`
- `docs/design.md`

These files document the **actual investigations, fixes, and design
decisions** applied during development.

## Status

This project is complete as a **full enterprise delivery simulation**
and is maintained as part of my professional portfolio.
# trigger deploy
