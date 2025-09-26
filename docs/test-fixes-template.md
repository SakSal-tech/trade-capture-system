FrontEnd is not running on port 3000

saksa@Work-Sak MINGW64 ~/cbfacademy/trade-capture-system/frontend (main)
$ npm run dev

> frontend@0.0.0 dev
> vite

VITE v6.3.6 ready in 612 ms

➜ Local: http://localhost:5173/
➜ Network: use --host to expose
➜ press h + enter to show help

Problem was the default viteport is 5173. I changed the port to 3000 inside the package.json
scripts": {
"dev": "vite --port 3000",
"build": "pnpm lint && vite build",
"lint": "eslint .",
"preview": "vite preview",
"test": "vitest"
},

saksa@Work-Sak MINGW64 ~/cbfacademy/trade-capture-system/frontend (main)
$ npm run dev

> frontend@0.0.0 dev
> vite --port 3000

I tried to login but there is no email field in database. I had to add email field and insert some emails. The log_in id serves as te email which can confuse the users, so I should either change the database and authentication or change the form.
![original application_user table](image.png)
![alter table added email field](image-1.png)
UPDATE application_user SET email='simon@example.com' WHERE login_id='simon';
UPDATE application_user SET email='ashley@example.com' WHERE login_id='ashley';
Update application_user Set email='joey@example.com' WHERE login_id='joey';
UPDATE application_user SET email='stuart@example.com' WHERE login_id='stuart';

# Fix Failing Test Cases

[ERROR] Failures:
[ERROR] TradeControllerTest.testCreateTrade:138 Status expected:<200> but was:<201>
[ERROR] TradeControllerTest.testCreateTradeValidationFailure_MissingBook:175 Status expected:<400> but was:<201>
[ERROR] TradeControllerTest.testCreateTradeValidationFailure_MissingTradeDate:158 Response content expected:<Trade date is required> but was:<>
[ERROR] TradeControllerTest.testDeleteTrade:223 Status expected:<204> but was:<200>
[ERROR] TradeControllerTest.testUpdateTrade:194 No value at JSON path "$.tradeId"
[ERROR] TradeControllerTest.testUpdateTradeIdMismatch:209 Status expected:<400> but was:<200>
[ERROR] TradeLegControllerTest.testCreateTradeLegValidationFailure_NegativeNotional:166 Response content expected:<Notional must be positive> but was:<>
[ERROR] TradeServiceTest.testCashflowGeneration_MonthlySchedule:181 expected: <1> but was: <12>
[ERROR] TradeServiceTest.testCreateTrade_InvalidDates_ShouldFail:99 expected: <Wrong error message> but was: <Start date cannot be before trade date>
[ERROR] Errors:
[ERROR] BookServiceTest.testFindBookById:29 » NullPointer
[ERROR] BookServiceTest.testFindBookByNonExistentId:58 » NullPointer
[ERROR] BookServiceTest.testSaveBook:42 » NullPointer Cannot invoke "com.technicalchallenge.mapper.BookMapper.toEntity(com.technicalchallenge.dto.BookDTO)" because "<local3>.bookMapper" is null
[ERROR] TradeServiceTest.testAmendTrade_Success:148 » NullPointer Cannot invoke "java.lang.Integer.intValue()" because the return value of "com.technicalchallenge.model.Trade.getVersion()" is null
[ERROR] TradeServiceTest.testCreateTrade_Success:80 » Runtime Book not found or not set
[INFO]
[ERROR] Tests run: 61, Failures: 9, Errors: 5, Skipped: 0

Filter only failed tests, run: mvn clean test | grep '\[ERROR\]'

## Test Failure 1

### Problem Description: What was failing and why

[ERROR] com.technicalchallenge.service.TradeServiceTest.testCashflowGeneration_MonthlySchedule -- Time elapsed: 0.007 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <1> but was: <12>
assertEquals(1, 12); // This was checking if 1 =12! which will always fail
test method did not call the method under test.
The required fields for the monthly schedule was not set up.
The assertion does not verify the number of cashflows

### Root Cause Analysis: The underlying issue causing the test failure

1. assertEquals(1, 12); // This was checking if 1 =12! which will always fail.It was not checking cash flow logic
2. Missing the call of the method under test:generateCashflows method to test the cashflow generation logic.
3. Test was incomplete also as only TradeLeg is created by setting up test data by assigning value to the notional field of the TradeLeg object, I need to set all fields that generateCashflows uses, otherwise it may not work or may use defaults.

### Solution Implemented: How you fixed the issue and why this approach was chosen

1. created a TradeLeg and Added athe other Other required fields: schedule, rate
2. Called to the generateCashflows(leg, startDate, maturityDate) with appropriate dates. When I tried to call the method generateCashflows, it was not visible as it was defined as private. I had to change it to public.
3. Changed the assertion so the returned cashflows match the expected monthly schedule
4. Tested the correct number of cashflows are generated and if their values/dates are correct. Used Mockito to verify that cashflowRepository.save() was called 12 times matching the expected number of monthly cashflows for 1 year period.
5. I changed from AssertEquals to verify method, as I do not know the number of generated cashflows, I could get them from a list or create a method that returns a list of chashflows repository, I needed to count them by using list size. This is a long process to capture saved chashflows
   Tested the dates and values.
6. Doing all of the above the test still failed as the cashflowRepository.save(...) only called3 times as in service class generateCashflows method, calls getSchedule() from Schedule class and parseSceduleand that the default Schedule is '3M' quarterly. I had to sett the schedule string to "Monthly" by passing this to setSchedule for test to generate monthly cashflows.
7. Then I discovered another problem When I fixed everything in that method, I encountered another problem that it was generating 11 months instead 12 LocalDate
   -> at com.technicalchallenge.service.TradeServiceTest.testCashflowGeneration_MonthlySchedule(TradeServiceTest.java:197)
   But was 11 times:
   The code generated cashflows for each month from January to December, but the end date is exclusive, it will only create 11 cashflows. I had to change maturity date instead of 31 December to 1 JAN 2026.LocalDate maturityDate = LocalDate.of(2026, 1, 1);

### Verification: How you confirmed the fix works

I ran the tests again mvn targeting only this test mvn -Dtest=TradeServiceTest#testCashflowGeneration_MonthlySchedule test
and it passed and correctly shows the cashflow generation for a monthly schedule

## Test Failure 2

### Problem Description: What was failing and why

[ERROR] com.technicalchallenge.controller.TradeLegControllerTest.testCreateTradeLegValidationFailure_NegativeNotional -- Time elapsed: 0.017 s <<< FAILURE!
java.lang.AssertionError: Response content expected:<Notional must be positive> but was:<>

When TradeLegDTO POST request is made with a negative notional, the API should return a 400 bad request and display message "Notional must be positive", but the response is empty.

### Root Cause Analysis: The underlying issue causing the test failure

Since this class is testing the TradeLegController, I checked the reason why empty string is printed in the controller. I also followed the setNotation method and checked the TradeLeg model, dto, mapper and service class.

#### Root cause assumption 1:

Firstly, I though that there is that there is no validation in the controller but I found that in the createTradeLeg method does validate the notional in the controller. If the notional is negative or zero, it will return a bad request with the message. I checked all other classes if there was any logic or validation that adjusts the Notional value. I found annoatations @NotNull and @Positive which should get Spring’s bean validation kicked in.The controller has @Valid which is making the annotations in the DTO to work.

#### Root cause assumption2:

The test was expecting "Notional must be positive" but by default Spring returns JSON format not a plain string but the test expects a string andExpect(content().string("Notional must be positive")): This asserts that the response body contains the expected error message. I thought that Spring Boot response may be wrapped in quotes, or returned as JSON, or with extra whitespace and Jackson may serialize the string as "Notional must be positive" with quotes. I changed the test method to expect "\"Notional must be positive\"" backslash is just for escaping quotes with quotes to deal with a possible mismatch between the expected and actual response format due to Spring’s JSON message conversion.
I tested the method and still was not working mvn -Dtest=TradeLegControllerTest#testCreateTradeLegValidationFailure_NegativeNotional test.
still fail and showed [ERROR] TradeLegControllerTest.testCreateTradeLegValidationFailure_NegativeNotional:168 Response content expected:<"Notional must be positive"> but was:<>

#### Actual Root cause:

I used Java debugger and the debug output shown that the request is mapped to TradeLegController#createTradeLeg(TradeLegDTO). The TradeLegDTO is deserialized with a negative notional. Spring’s bean validation triggers before the manual validation code in the controller. Spring’s default behavior for validation errors is to return a 400 status with an empty body. The exception MethodArgumentNotValidException is thrown because the @Positive annotation on notional fails. The error message is "Notional must be positive", but the response body is empty and the content type is null.

Why the test fails:
When a negative notional is sent in the test, Spring’s validation detects that it violates the @Positive constraint and throws a MethodArgumentNotValidException before your controller logic runs.The test failed because Spring’s default validation handling returned a 400 with an empty body, instead of your expected error message.
The test expects the error message in the response body.
Spring returns an empty body for validation errors by default.

### Solution

After researching why the exception handling in the TradeLegController is
not invoked, I came across this StackOverflow post:https://stackoverflow.com/questions/66371164/spring-boot-exceptionhandler-for-methodargumentnotvalidexception-in-restcontroll?
I had to read about the topic more to understand the @RestControllerAdvice
public class ValidationExceptionHandler {
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException ex) {
String message = ex.getBindingResult().getFieldError().getDefaultMessage();
return ResponseEntity.badRequest().body(message);
}
} https://www.baeldung.com/global-error-handler-in-a-spring-rest-api?

https://medium.com/javaguides/spring-boot-restcontrolleradvice-annotation-complete-guide-with-examples-5254a4f6f62d

I added a custom exception handler to return the error message in the response body for validation errors, to ensure that when validation fails, the error message (e.g., "Notional must be positive") is returned in the response body, and the test will pass.
After added a ValidationExceptionHandler class with @RestControllerAdvice and an @ExceptionHandler(MethodArgumentNotValidException.class) method. This intercepted validation exceptions, extracted the message (e.g., “Notional must be positive”), and returned it in the response body. This fixed the issue by making the response include the validation error message, so the test passed.
