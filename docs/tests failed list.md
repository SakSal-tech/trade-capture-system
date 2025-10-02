# Test Failures & Errors List

## com.technicalchallenge.controller.TradeControllerTest

- **testCreateTrade**

  # Status expected:<200> but was:<201>

- **testCreateTradeValidationFailure_MissingBook**

  # Status expected:<400> but was:<201>

- **testCreateTradeValidationFailure_MissingTradeDate**

  # Response content expected:<Trade date is required> but was:<>

- **testDeleteTrade**

  # Status expected:<204> but was:<200>

- **testUpdateTrade**

  # No value at JSON path "$.tradeId"

- **testUpdateTradeIdMismatch**
  # Status expected:<400> but was:<200>

## com.technicalchallenge.controller.TradeLegControllerTest

- **testCreateTradeLegValidationFailure_NegativeNotional**
  # Response content expected:<Notional must be positive> but was:<>

## com.technicalchallenge.service.BookServiceTest

- **testFindBookById**

  # NullPointerException

  # java.lang.NullPointerException at java.util.Objects.requireNonNull

- **testFindBookByNonExistentId**

  # NullPointerException

  # java.lang.NullPointerException at java.util.Objects.requireNonNull

- **testSaveBook**
  # NullPointerException: Cannot invoke "com.technicalchallenge.mapper.BookMapper.toEntity(com.technicalchallenge.dto.BookDTO)" because "bookMapper" is null

## com.technicalchallenge.service.TradeServiceTest

- **testCashflowGeneration_MonthlySchedule**

  # expected:<1> but was:<12>

- **testCreateTrade_InvalidDates_ShouldFail**

  # expected:<Wrong error message> but was:<Start date cannot be before trade date>

- **testAmendTrade_Success**

  # NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because the return value of "Trade.getVersion()" is null

- **testCreateTrade_Success**
  # RuntimeException: Book not found or not set

## Total

- **TradeControllerTest** 6 failures
- **TradeLegControllerTest** 1 failure
- **BookServiceTest** → 3 errors
- **TradeServiceTest** → 2 failures, 2 errors

# Total: 61 tests run → 9 failures, 5 errors, 0 skipped

## TradeControllerTest tests failed:

testCreateTrade
testCreateTradeValidationFailure_MissingBook
testDeleteTrade
testUpdateTrade
testUpdateTradeIdMismatch

### BookServiceTest

testFindBookById
testFindBookByNonExistentId
testSaveBook
testAmendTrade_Success
testCreateTrade_Success

### Failing Tests Overview

**BookServiceTest**

- testCreateBook_InvalidCostCenter_ShouldFail → NullPointerException (mapper dependency missing)

**TradeServiceTest**

- testAmendTrade_Success → NullPointerException (leg was null)
- testCreateTrade_Success → RuntimeException: Trade status not found or not set
- testCreateTrade_InvalidDates_ShouldFail → RuntimeException (date validation)
- testCreateTrade_InvalidLegCount_ShouldFail → RuntimeException (leg count validation)

**TradeLegServiceTest**

- testAmendTrade_Success → NullPointerException (leg.getLegId() called on null)

**TradeControllerTest**

- testCreateTrade_ShouldReturnBadRequest → Response content mismatch ("Trade date is required")
