# Testing Strategy and Implementation

## Overview

This document explains the unit and integration tests I created for the trade capture system. The tests verify that individual components work correctly in isolation and that different modules work together as expected. I focused on critical business logic, security features, and data validation to ensure reliability and catch issues early in the development process.

## Unit Testing Approach

### Purpose

Unit tests verify that individual functions or components work exactly as intended in isolation. I used these tests to validate business logic, edge cases, and error handling without depending on external systems like databases or REST APIs.

### Testing Structure

I used the Arrange-Act-Assert pattern to keep tests readable and consistent:

- **Arrange**: Set up test data and mock dependencies
- **Act**: Execute the method under test
- **Assert**: Verify the expected outcome

### Key Unit Tests Created

#### Cashflow Calculation Tests

**File**: `CashflowServiceTest.java`

**What I tested**: The cashflow calculation logic for fixed-rate interest rate swaps, particularly the rate scaling and precision handling.

**Background**: I discovered a critical bug where fixed-leg cashflows were calculated at approximately 100 times the correct value. For a £10,000,000 notional at 3.5% with quarterly payments, the system calculated around £875,000 per quarter instead of the correct £87,500.

**Root causes identified**:

- Rates expressed as percentages (3.5) were used as decimals instead of being converted to 0.035
- Monetary arithmetic used `double` in places, introducing rounding and precision errors

**Test implementation**: I created tests that verify the canonical case using `ArgumentCaptor` to capture what actually gets saved to the repository:

```java
@Test
void testGenerateQuarterlyCashflow() {
    // Arrange: £10m notional, 3.5% rate, quarterly payments
    TradeLeg leg = new TradeLeg();
    leg.setNotional(BigDecimal.valueOf(10_000_000));
    leg.setRate(3.5); // percentage
    leg.setCalculationPeriodSchedule("3M");

    ArgumentCaptor<Cashflow> captor = ArgumentCaptor.forClass(Cashflow.class);

    // Act
    tradeService.generateCashflows(leg, startDate, maturityDate);

    // Assert: verify £87,500.00 per quarter
    verify(cashflowRepository, atLeastOnce()).save(captor.capture());
    assertEquals(new BigDecimal("87500.00"), captor.getValue().getPaymentValue());
}
```

**Edge cases tested**:

- Zero rates and notional amounts
- Negative payment values (should throw `IllegalArgumentException`)
- Missing value dates (should throw `IllegalArgumentException`)
- Different schedule formats ("3M", "Quarterly", "Monthly")

**Benefits**: These tests caught the rate scaling bug immediately and gave me confidence when refactoring the calculation logic to use `BigDecimal` instead of `double` for monetary arithmetic.

#### User Privilege Validation Tests

**File**: `UserPrivilegeValidatorTest.java`

**What I tested**: Role-based access control logic that determines which trade actions (CREATE, AMEND, TERMINATE, CANCEL) different user types can perform.

**Test implementation**: I created a helper method to reduce code duplication and make tests more maintainable:

```java
private void assertTraderCanPerformAction(UserProfile trader, TradeDTO tradeDTO, String action) {
    tradeDTO.setAction(action);
    TradeValidationResult result = new TradeValidationResult();
    UserPrivilegeValidator validator = new UserPrivilegeValidator();
    validator.validateUserPrivilege(trader, tradeDTO, result);
    assertTrue(result.isValid(), "TRADER should be able to " + action + " trades");
}

@Test
void traderCanPerformAllTradeActions() {
    UserProfile trader = new UserProfile();
    trader.setUserType("TRADER");
    TradeDTO tradeDTO = new TradeDTO();

    assertTraderCanPerformAction(trader, tradeDTO, "CREATE");
    assertTraderCanPerformAction(trader, tradeDTO, "AMEND");
    assertTraderCanPerformAction(trader, tradeDTO, "TERMINATE");
    assertTraderCanPerformAction(trader, tradeDTO, "CANCEL");
}
```

**What this validates**:

- TRADER role has full permissions for all trade lifecycle actions
- SALES role can only CREATE and AMEND trades
- MIDDLE_OFFICE role can AMEND and VIEW trades
- SUPPORT role can only VIEW trades

**Benefits**: These tests ensure the privilege system follows business rules and prevents unauthorised trade modifications.

#### Settlement Instruction Validation Tests

**File**: `SettlementInstructionValidatorTest.java`

**What I tested**: Field-level validation for free-text settlement instructions, which enforces length limits, character restrictions, and proper escaping to prevent injection attacks.

**Business rules validated**:

- Minimum length: 10 characters
- Maximum length: 500 characters
- Forbidden characters: semicolons (risk of CSV injection)
- Quote handling: unescaped quotes rejected, escaped quotes (`\"`) allowed

**Test examples**:

```java
@Test
void validInstructions_pass() {
    TradeValidationResult result = new TradeValidationResult();
    String text = "Please settle by bank transfer \\\"ASAP\\\" with reference 12345.";
    validator.validate(text, result);
    assertTrue(result.isValid());
}

@Test
void semicolonForbidden_fails() {
    TradeValidationResult result = new TradeValidationResult();
    validator.validate("Payment; send now", result);
    assertFalse(result.isValid());
    assertTrue(result.getErrors().get(0).toLowerCase().contains("semicolon"));
}

@Test
void unescapedQuote_fails() {
    TradeValidationResult result = new TradeValidationResult();
    validator.validate("Client said \"urgent\"", result);
    assertFalse(result.isValid());
    assertTrue(result.getErrors().get(0).toLowerCase().contains("unescaped quote"));
}
```

**Benefits**: These tests protect against malformed data and security vulnerabilities in user-provided settlement instructions.

#### Authentication Service Tests

**File**: `TradeDashboardServiceAuthTest.java`

**What I tested**: Authentication and authorisation logic for the dashboard service, focusing on privilege-checking behaviour and the deny-by-default security approach.

**Test scenarios**:

1. SecurityContext contains `ROLE_TRADER` authority - access granted
2. SecurityContext contains `TRADE_VIEW` authority - access granted
3. No SecurityContext but database privilege present - access granted
4. No authentication and no privilege - access denied

**Implementation approach**: I used Mockito to isolate the service layer and mock the database privilege lookups:

```java
@Test
void whenSecurityContextHasRoleTrader_thenHasPrivilegePermits() {
    TestingAuthenticationToken auth = new TestingAuthenticationToken("alice", null, "ROLE_TRADER");
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertDoesNotThrow(() -> dashboardService.searchTrades(new SearchCriteriaDTO()));
}

@Test
void whenNoSecurityContext_butDbPrivilegePresent_thenPermits() {
    SecurityContextHolder.clearContext();
    when(userPrivilegeService.findPrivilegesByUserLoginIdAndPrivilegeName(anyString(), anyString()))
        .thenReturn(List.of(new UserPrivilege()));

    assertDoesNotThrow(() -> dashboardService.searchTrades(new SearchCriteriaDTO()));
}
```

**Benefits**: These focused unit tests validate the security layer without spinning up the entire application context, making them fast and reliable.

#### Additional Info Service Tests

**File**: `AdditionalInfoServiceTest.java`

**What I tested**: The service layer that handles creation and validation of additional trade information, particularly settlement instructions.

**Key test cases**:

1. Invalid settlement instructions throw `IllegalArgumentException`
2. Valid non-settlement fields save successfully and return DTO
3. Validation engine is called for settlement instruction fields
4. Repository is not called when validation fails

**Example**:

```java
@Test
void createSettlement_invalid_throws() {
    request.setFieldName("SETTLEMENT_INSTRUCTIONS");
    request.setFieldValue("bad; value");

    TradeValidationResult bad = new TradeValidationResult();
    bad.setError("Semicolons are not allowed in settlement instructions.");

    when(tradeValidationEngine.validateSettlementInstructions(anyString())).thenReturn(bad);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> additionalInfoService.createAdditionalInfo(request));

    assertTrue(ex.getMessage().contains("Semicolons"));
    verify(tradeValidationEngine).validateSettlementInstructions(anyString());
    verifyNoInteractions(additionalInfoRepository);
}
```

**Benefits**: These tests ensure the service layer correctly enforces validation rules and prevents invalid data from reaching the database.

## Integration Testing Approach

### Purpose

Integration tests check that different modules or services work together correctly. I focused on testing the full request-response cycle, including API endpoints, service layer, validation engine, and database interactions.

### Testing Strategy

I used Spring Boot's `@SpringBootTest` with `MockMvc` to simulate real HTTP requests and verify the entire stack works correctly. Tests run against an in-memory H2 database to ensure database queries execute properly without requiring PostgreSQL.

### Key Integration Tests Created

#### User Privilege Integration Tests

**File**: `UserPrivilegeIntegrationTest.java`

**What I tested**: End-to-end privilege enforcement from HTTP request through to database queries, ensuring that role-based access control works correctly across the entire application.

**Test setup**: I used `@WithMockUser` annotations to simulate authenticated users with different roles and privileges:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserPrivilegeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Create test data: books, counterparties, users, trades
        // Use dedicated transaction to ensure data is committed
        // and visible to MockMvc requests
    }
}
```

**Test scenarios**:

1. Authenticated TRADER can create trades
2. Authenticated MIDDLE_OFFICE can view but not create trades
3. Anonymous users receive 401 Unauthorized
4. Users without required privileges receive 403 Forbidden
5. Daily summary endpoint enforces trader-specific data access

**Benefits**: These tests validate that the security configuration, controller annotations (`@PreAuthorize`), and database-backed privilege system work together correctly. They caught issues where permissions were checked at the wrong layer or where the SecurityContext was not properly configured.

#### Trade Validation Integration Tests

**File**: `TradeValidationIntegrationTest.java`

**What I tested**: The complete validation engine flow, ensuring that all validators (date, leg, entity status, user privilege, settlement instruction) work together to reject invalid trades.

**Test approach**: I created trades with various invalid states and verified that the validation engine:

- Collects multiple errors in a single pass
- Returns all validation failures to the user
- Prevents invalid trades from being persisted
- Provides clear, actionable error messages

**Benefits**: These tests ensure the validation engine components integrate correctly and that users see all problems at once rather than fixing one error at a time.

#### Additional Info Integration Tests

**File**: `AdditionalInfoIntegrationTest.java`

**What I tested**: Settlement instruction persistence from frontend UI through to the database, including audit logging and ownership enforcement.

**Test focus**:

- POST requests with valid settlement instructions save to database
- Invalid instructions return 400 Bad Request with validation errors
- Audit records are created with correct username and timestamp
- Ownership rules prevent users from modifying other users' instructions

**Database verification**: Tests query the database directly after API calls to verify that data was persisted correctly:

```java
@Test
void createSettlementInstruction_validData_persists() {
    String json = "{\"entityType\":\"TRADE\",\"entityId\":1," +
                  "\"fieldName\":\"SETTLEMENT_INSTRUCTIONS\"," +
                  "\"fieldValue\":\"Valid settlement text here\"}";

    mockMvc.perform(post("/api/additional-info")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
            .with(csrf()))
        .andExpect(status().isCreated());

    // Verify database state
    List<AdditionalInfo> saved = additionalInfoRepository.findAll();
    assertEquals(1, saved.size());
    assertEquals("SETTLEMENT_INSTRUCTIONS", saved.get(0).getFieldName());
}
```

**Benefits**: These tests ensure the entire flow from HTTP request to database persistence works correctly, catching issues with transaction management, entity mapping, and constraint violations.

## Test-Driven Development Experience

### Challenges Faced

I found writing tests before implementing the logic more challenging than writing logic first and then adding tests. However, I came to understand why TDD is valuable. Writing tests first forced me to think about:

- Expected behaviour before implementation details
- Edge cases and error handling upfront
- Clear method signatures and dependencies
- How the code would be used by callers

### TDD Phases Applied

I practised the Red-Green-Refactor cycle:

1. **Red**: Write a failing test that describes the desired behaviour
2. **Green**: Write the minimum code needed to make the test pass
3. **Refactor**: Clean up the implementation while keeping tests green

**Example from date validation**:

1. Wrote `failWhenMaturityBeforeStartDate()` test - it failed (RED)
2. Added validation logic to check maturity > start date - test passed (GREEN)
3. Refactored validation logic to use clear variable names and extracted helper methods (REFACTOR)

### Benefits Realised

The tests helped me:

- Catch issues early during development
- Refactor with confidence knowing tests would catch regressions
- Document expected behaviour through executable examples
- Move faster by avoiding manual testing cycles
- Ensure edge cases were handled correctly

## Testing Coverage Summary

### Unit Tests Created

- `CashflowServiceTest` - Cashflow calculation logic and bug fix verification
- `UserPrivilegeValidatorTest` - Role-based access control logic
- `SettlementInstructionValidatorTest` - Field validation and injection prevention
- `TradeDashboardServiceAuthTest` - Authentication and authorisation logic
- `AdditionalInfoServiceTest` - Settlement instruction service layer validation

### Integration Tests Created

- `UserPrivilegeIntegrationTest` - End-to-end privilege enforcement
- `TradeValidationIntegrationTest` - Complete validation engine flow
- `AdditionalInfoIntegrationTest` - Settlement instruction persistence and audit

### Key Improvements Made

The most significant improvement was fixing the cashflow calculation bug. I discovered that:

1. **Percentage handling bug**: Rates expressed as percentages (3.5) were used as decimals without conversion
2. **Precision bug**: Monetary arithmetic used `double`, causing rounding errors

I rewrote the calculation logic to:

- Normalise rates by dividing by 100 when > 1
- Use `BigDecimal` for all monetary arithmetic
- Set consistent scale and rounding mode
- Add defensive guards and logging

The unit tests locked this behaviour in place: £10m at 3.5% quarterly must equal £87,500.00 per quarter.

## Lessons Learned

### What Worked Well

- Using `ArgumentCaptor` to verify exact values saved to repositories
- Creating helper methods to reduce test code duplication
- Testing one behaviour per test method
- Using descriptive test names that explain expected behaviour
- Running tests frequently during development

### What I Would Do Differently

- Write more tests upfront for complex business logic
- Use parameterised tests for similar test cases with different inputs
- Add more integration tests for error scenarios
- Document test fixtures more clearly
- Consider using test containers for database integration tests

## Conclusion

The unit and integration tests I created significantly improved the reliability of the trade capture system. The tests helped me catch a critical cashflow calculation bug, enforce security rules correctly, and validate user inputs thoroughly. Writing tests upfront using TDD was challenging but valuable, as it forced me to think about edge cases and error handling before writing implementation code. The test suite now provides confidence when refactoring and serves as executable documentation of expected system behaviour.
