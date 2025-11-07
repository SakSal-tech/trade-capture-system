# Detailed Project Plan: Trading Application Technical Challenge

## Step 1: Project Setup (Backend & Frontend)

**Objective:** Set up backend (Spring Boot) and frontend (React) locally, verify connectivity and basic functionality.

### Backend

- **Key Files/Classes:**
  - `BackendApplication.java`, `pom.xml`, `application.properties`, `data.sql`
- **Endpoints:**
  - `/actuator/health` (health check)
  - `/h2-console` (database console)
  - `/swagger-ui/index.html` (API docs)
- **Tasks:**
  - Install JDK 17+, Maven
  - Build and run backend (`mvn spring-boot:run`)
  - Verify endpoints above

### Frontend

- **Key Files/Classes:**
  - `main.tsx`, `package.json`, `index.html`
- **Tasks:**
  - Install Node.js 18+, npm
  - Install dependencies (`npm install`)
  - Run frontend (`npm start`)
  - Verify UI loads and connects to backend

## Step 2: Fix Failing Test Cases (Backend)

**Objective:** Debug and fix backend test failures, document fixes.

### Backend

- **Key Files/Classes:**
  - Test classes: `TradeServiceTest.java`, `TradeLegServiceTest.java`, `UserServiceTest.java`, etc.
  - Main classes: `TradeService.java`, `TradeController.java`, etc.
- **Tasks:**
  - Run all tests (`mvn test`)
  - Investigate failures, fix code or test logic
  - Document each fix in `docs/test-fixes-template.md`
  - Use proper commit messages (`docs/git-commit-standards.md`)

## Step 3: Implement Missing Functionality (Backend & Frontend)

**Objective:** Add advanced search, validation, and dashboard features.

### Enhancement 1: Advanced Trade Search System

#### Backend

- **Controller:** `TradeController.java`
  - Endpoints:
    - `@GetMapping("/search")` → `searchTrades(SearchCriteriaDTO criteria)`
    - `@GetMapping("/filter")` → `filterTrades(FilterCriteriaDTO criteria, Pageable pageable)`
    - `@GetMapping("/rsql")` → `rsqlSearch(@RequestParam String query)`
- **Service:** `TradeService.java`
  - Methods:
    - `List<TradeDTO> searchTrades(SearchCriteriaDTO criteria)`
    - `Page<TradeDTO> filterTrades(FilterCriteriaDTO criteria, Pageable pageable)`
    - `List<TradeDTO> rsqlSearch(String rsqlQuery)`
- **Repository:** `TradeRepository.java`
  - Methods:
    - `List<Trade> findAll(Specification<Trade> spec)`
    - `Page<Trade> findAll(Specification<Trade> spec, Pageable pageable)`
- **DTOs:**
  - `SearchCriteriaDTO` (new)
  - `FilterCriteriaDTO` (new)
  - `TradeDTO` (update fields if needed)
- **Utility:**
  - `TradeSpecificationBuilder.java` (new):
    - `build(SearchCriteriaDTO criteria)`
    - `fromRSQL(String rsqlQuery)`
- **Error Handling:**
  - `GlobalExceptionHandler.java` (update if needed)

#### Frontend

- **UI Components:**
  - `AGGridTable.tsx`, `TradeBlotterModal.tsx`: Add search/filter UI
- **API Integration:**
  - `api.ts`: Add API methods for search/filter/rsql
- **State Management:**
  - `tradeStore.ts`: Add actions/selectors for search/filter results

#### Summary Table

| Layer          | Class/File                             | Method(s) to Add/Update                |
| -------------- | -------------------------------------- | -------------------------------------- |
| Controller     | TradeController.java                   | searchTrades, filterTrades, rsqlSearch |
| Service        | TradeService.java                      | searchTrades, filterTrades, rsqlSearch |
| Repository     | TradeRepository.java                   | findAll(Spec), findAll(Spec, Pageable) |
| DTOs           | SearchCriteriaDTO.java                 | (new class)                            |
|                | FilterCriteriaDTO.java                 | (new class)                            |
|                | TradeDTO.java                          | (update fields if needed)              |
| Utility        | TradeSpecificationBuilder.java         | build, fromRSQL                        |
| Error Handling | GlobalExceptionHandler.java            | (update if needed)                     |
| Frontend       | AGGridTable.tsx, TradeBlotterModal.tsx | (update UI)                            |
|                | api.ts                                 | searchTrades, filterTrades, rsqlSearch |
|                | tradeStore.ts                          | (update actions/selectors)             |
| Testing        | TradeServiceTest.java                  | (add tests)                            |
|                | TradeControllerTest.java               | (add tests)                            |
|                | AGGridTable.test.tsx                   | (add tests)                            |

### Enhancement 2: Comprehensive Trade Validation Engine

#### Backend

- **Service:** `TradeService.java`
  - Methods:
    - `ValidationResult validateTradeBusinessRules(TradeDTO tradeDTO)`
    - `boolean validateUserPrivileges(String userId, String operation, TradeDTO tradeDTO)`
    - `ValidationResult validateTradeLegConsistency(List<TradeLegDTO> legs)`
- **Controller:**
  - Use validation methods before trade creation/amendment
- **Business Rules:**
  - Date validation, user privilege enforcement, cross-leg rules, entity status validation

#### Frontend

- **UI Components:**
  - Trade booking forms: Add validation feedback
- **API Integration:**
  - Show error messages for invalid trades

#### Summary Table

| Layer | Class/File | Method(s) to Add/Update |
| - | | - |
| Service | TradeService.java | validateTradeBusinessRules, validateUserPrivileges, validateTradeLegConsistency |
| Controller | TradeController.java | (invoke validation before create/amend) |
| DTOs | TradeDTO.java, TradeLegDTO.java | (update if needed) |
| Frontend | TradeBookingModal.tsx, TradeForm.tsx | (add validation UI) |
| | api.ts | (handle validation errors) |
| Testing | TradeServiceTest.java | (add tests for validation) |
| | TradeControllerTest.java | (add tests for validation) |

### Enhancement 3: Trader Dashboard and Blotter System

#### Backend

- **Controller:** `TradeController.java`
  - Endpoints:
    - `@GetMapping("/my-trades")` → `getMyTrades()`
    - `@GetMapping("/book/{id}/trades")` → `getBookTrades(Long id)`
    - `@GetMapping("/summary")` → `getTradeSummary()`
    - `@GetMapping("/daily-summary")` → `getDailySummary()`
- **Service:** `TradeService.java`
  - Methods:
    - `List<TradeDTO> getMyTrades(String userId)`
    - `List<TradeDTO> getBookTrades(Long bookId)`
    - `TradeSummaryDTO getTradeSummary()`
    - `DailySummaryDTO getDailySummary()`
- **DTOs:**
  - `TradeSummaryDTO` (new)
  - `DailySummaryDTO` (new)

#### Frontend

- **UI Components:**
  - `TraderSales.tsx`, `Main.tsx`, `AGGridTable.tsx`: Add dashboard views
- **API Integration:**
  - `api.ts`: Add API methods for dashboard endpoints
- **State Management:**
  - `tradeStore.ts`: Add actions/selectors for dashboard data

#### Summary Table

| Layer | Class/File | Method(s) to Add/Update |
| - | | |
| Controller | TradeController.java | getMyTrades, getBookTrades, getTradeSummary, getDailySummary |
| Service | TradeService.java | getMyTrades, getBookTrades, getTradeSummary, getDailySummary |
| DTOs | TradeSummaryDTO.java | (new class) |
| | DailySummaryDTO.java | (new class) |
| Frontend | TraderSales.tsx, Main.tsx, AGGridTable.tsx | (add dashboard UI) |
| | api.ts | (dashboard API methods) |
| | tradeStore.ts | (dashboard state/actions) |
| Testing | TradeServiceTest.java | (add tests for dashboard) |
| | TradeControllerTest.java | (add tests for dashboard) |
| | TraderSales.test.tsx | (add tests for dashboard UI) |

## Step 4: Bug Investigation and Fix (Backend)

**Objective:** Fix cashflow calculation bug in `TradeService.java`.

#### Backend

- **Files/Classes:**
  - `TradeService.java`: Method `calculateCashflowValue`
  - `CashflowService.java`, `CashflowControllerTest.java`
- **Tasks:**
  - Review and fix percentage and precision bugs (use `BigDecimal`)
  - Document root cause and fix in `docs/test-fixes-template.md`
  - Add/Update unit and integration tests

#### Summary Table

| Layer         | Class/File                                            | Method(s) to Add/Update |
| ------------- | ----------------------------------------------------- | ----------------------- |
| Service       | TradeService.java                                     | calculateCashflowValue  |
| Testing       | CashflowServiceTest.java, CashflowControllerTest.java | (add/fix tests)         |
| Documentation | test-fixes-template.md                                | (document bug and fix)  |

## Step 5: Full-Stack Feature Implementation (Backend & Frontend)

**Objective:** Integrate settlement instructions into trade booking and management.

#### Backend

- **Controller:** `TradeController.java`
  - Endpoints:
    - `@GetMapping("/search/settlement-instructions")` → `searchBySettlementInstructions(String instructions)`
    - `@PutMapping("/{id}/settlement-instructions")` → `updateSettlementInstructions(Long id, SettlementInstructionsUpdateDTO request)`
- **Service:** `TradeService.java`
  - Methods:
    - `List<TradeDTO> searchBySettlementInstructions(String instructions)`
    - `void updateSettlementInstructions(Long id, SettlementInstructionsUpdateDTO request)`
- **Entity:**
  - Extend `Trade` or use `AdditionalInfo` for instructions
- **DTO:**
  - `SettlementInstructionsUpdateDTO` (new)

#### Frontend

- **UI Components:**
  - `SingleTradeModal.tsx`, `TradeBlotterModal.tsx`: Add/edit instructions field
- **API Integration:**
  - `api.ts`: Add API methods for instructions
- **State Management:**
  - `tradeStore.ts`: Add state/actions for instructions

#### Summary Table

| Layer | Class/File | Method(s) to Add/Update |
| - | -- | |
| Controller | TradeController.java | searchBySettlementInstructions, updateSettlementInstructions |
| Service | TradeService.java | searchBySettlementInstructions, updateSettlementInstructions |
| Entity | Trade.java, AdditionalInfo.java | (add instructions field or logic) |
| DTO | SettlementInstructionsUpdateDTO.java | (new class) |
| Frontend | SingleTradeModal.tsx, TradeBlotterModal.tsx | (add/edit UI) |
| | api.ts | (instructions API methods) |
| | tradeStore.ts | (instructions state/actions) |
| Testing | TradeServiceTest.java, TradeControllerTest.java | (add tests) |
| | SingleTradeModal.test.tsx, TradeBlotterModal.test.tsx | (add tests) |

## Step 6: Application Containerization (DevOps)

**Objective:** Dockerize backend and frontend, set up Docker Compose.

#### Backend

- **Files:** `backend/Dockerfile`

#### Frontend

- **Files:** `frontend/Dockerfile`

#### Compose

- **Files:** `docker-compose.yml` (root)
- **Tasks:**
  - Write multi-stage Dockerfiles for backend/frontend
  - Create Docker Compose file to run both services
  - Test containers locally

#### Summary Table

| Layer    | File                | Task/Method       |
| -------- | ------------------- | ----------------- |
| Backend  | backend/Dockerfile  | Multi-stage build |
| Frontend | frontend/Dockerfile | Multi-stage build |
| DevOps   | docker-compose.yml  | Compose setup     |

## Step 7: Azure Cloud Architecture Design (Documentation)

**Objective:** Document Azure architecture for cloud deployment.

#### Documentation

- **Files:** `docs/design.md`, `docs/functionality.md`
- **Tasks:**
  - Draw architecture diagrams (App Service, AKS, etc.)
  - Document service selection, deployment strategy, monitoring, cost, and governance
  - Justify design choices for scalability, security, compliance

#### Summary Table

| Layer | File | Task/Method |
| - | | - |
| Documentation | design.md, functionality.md | Architecture diagrams, service selection, deployment strategy, monitoring, cost, governance |

## Testing & Documentation

- **Backend:** All new features and fixes must have unit/integration tests (`src/test/java/...`)
- **Frontend:** Add/Update component tests (`*.test.tsx`)
- **Documentation:** Update `docs/test-fixes-template.md`, `docs/design.md`, and other guides as progress
