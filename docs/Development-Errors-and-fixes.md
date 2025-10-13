### Problem

I imported wrongly util.function.Predicate. Not using NotNull to the toPredicate parameters.

### Solution

Ensuring the correct Predicate type is used in the Specification:
Used jakarta.persistence.criteria.Predicate in the toPredicate method, not java.util.function.Predicate.
Adding the required @NonNull annotation to the parameters of the toPredicate method:
Imported org.springframework.lang.NonNull and annotate Root<Trade> root, CriteriaQuery<?> query, and CriteriaBuilder criteriaBuilder in the anonymous inner class.

### Problem

[ERROR] /C:/Users/saksa/cbfacademy/trade-capture-system/backend/src/main/java/com/technicalchallenge/service/TradeRsqlVisitor.java:[22,39] cannot find symbol  
[ERROR] symbol: class AbstractRSQLVisitor

### Solution

I checked pom file and the dependency is there mvn clean install then
`mvn dependency:tree 
`
to confirm the RSQL dependency is present. I can see the rsql dependency is there [INFO] +- cz.jirutka.rsql:rsql-parser:jar:2.1.0:compile

I found out that the class AbstractRSQLVisitor does not exist in the rsql-parser library version using (2.1.0). I changed to RSQLVisitor class.
2025-10-09T15:00:00 | TradeRsqlVisitor.java | The type Path is not generic; it cannot be parameterized with arguments <?>

### Problem

I imported java.nio.file.Path instead of jakarta.persistence.criteria.Path. The JPA Criteria API Path type is not generic and should not be parameterized. This caused compilation errors when building dynamic predicates for RSQL.

### Solution

Removed the import for java.nio.file.Path and replaced it with jakarta.persistence.criteria.Path. Used Path without generics in the Specification implementation.

2025-10-09T15:01:00 | TradeRsqlVisitor.java | Predicate is a raw type. References to generic type Predicate<T> should be parameterized

### Problem

I used java.util.function.Predicate instead of jakarta.persistence.criteria.Predicate in the Specification implementation. The JPA Criteria API Predicate should not be parameterized and must be imported from jakarta.persistence.criteria. Using the wrong Predicate type caused type mismatch and raw type warnings.

### Solution

Deleted import java.util.function.Predicate and used jakarta.persistence.criteria.Predicate everywhere in the Specification and toPredicate method. This resolved the raw type warning and ensured correct JPA Criteria usage.
2025-10-09T15:10:00 | TradeRsqlVisitor.java | missing return statement in toPredicate method

### Problem

In the anonymous inner class implementing Specification<Trade>, the toPredicate method contains multiple conditional branches, each returning a Predicate for a recognized operator (e.g., ==, =in=, etc.). However, if none of the conditions are met (for example, an unsupported operator or an unexpected code path), the method does not return anything, resulting in a compilation error: "missing return statement".

This error occurs even if there are multiple return statements within the method, because Java requires that every possible execution path must return a value. If all branches are conditional, the compiler cannot guarantee that a return will always happen.

### Solution

Added a final unconditional return statement at the end of the toPredicate method, `return null;`. This ensures that if none of the conditions are met, the method still returns a value, satisfying the compiler and preventing the error. This is especially important in dynamic query code, where not all operators or cases may be handled explicitly.

### Problem

I am using the endpoint:
`GET http://localhost:8080/api/trades/rsql?query=counterparty.name==BIGBANK;tradeStatus.tradeStatus==LIVE`
but it keeps returning an empty result, even though I have data in the H2 database that should match.

### Investigation

- I wrote SQL SELECT statements directly against the H2 database to confirm that records exist with `counterparty.name = 'BIGBANK'` and `tradeStatus.tradeStatus = 'LIVE'`.
- I checked that foreign key relationships between Trade, Counterparty, and TradeStatus are correct and present in the database schema.
- I checked field names in Trade, TradeStatus and CounterParty entities.
- I verified that the RSQL query matches the actual field names and values in the database.
- I tried both uppercase and lowercase values in the RSQL query, and also implemented case-insensitive matching in the RSQL visitor using `criteriaBuilder.lower()` and `value.toLowerCase()` for string comparisons.
- I confirmed that the code uses the correct JPA Criteria API types (Predicate, Path) and that all required annotations (@NonNull) are present.
- I checked that the RSQLVisitor implementation correctly navigates nested fields and supports all major operators (==, !=, =in=, =out=, etc.).
- I added debug output to print the parsed RSQL nodes and query execution results.

### Changes Made

- Updated the RSQL visitor to use case-insensitive string comparisons for `==` and `!=` operators.
- Implemented the `=out=` operator for "not in" queries.
- Fixed all type and annotation issues in the Specification and Predicate usage.
- Ensured all field navigation in the visitor uses the correct path and type.
- Added debug logging to trace query parsing and result counts.

### Current Status

Despite all these changes and checks, the endpoint still returns empty results. The SQL queries show that matching data exists, and the foreign key relationships are correct. The RSQL visitor appears to be building the correct Specification, but the repository query does not return any matches.

### Next Steps

- Further debug the Specification logic and check for any subtle mapping issues between entity fields and DTOs.
- Consider adding more detailed logging to the repository and service layers to trace the exact query being executed.
- Review the H2 database contents and schema for any unexpected data or mapping issues.
  2025-10-07T12:00:00 | Line 179 | TradeController.java | The method searchTradesRsql(SearchCriteriaDTO) in the type TradeService is not applicable for the arguments (String)
  2025-10-07T12:00:00 | Line 703 | TradeService.java | The return type is incompatible with Specification<Trade>.toPredicate(Root<Trade>, CriteriaQuery<?>, CriteriaBuilder)
  2025-10-07T12:00:00 | Line 705 | TradeService.java | Type mismatch: cannot convert from jakarta.persistence.criteria.Predicate to java.util.function.Predicate
  2025-10-09T17:00:00 | RSQL Search | Final fix and successful output

### Problem

RSQL search was returning empty results even though matching data existed in the database. The Specification chaining and entity mappings were suspected.

### Final Changes Made

- Corrected the chaining of Specifications in the RSQL visitor for both AND and OR nodes:
  - Used `result = result.and(spec);` for AND logic
  - Used `result = result.or(spec);` for OR logic
- Verified and fixed all entity field mappings, especially the `TradeStatus` relationship in the `Trade` entity.
- Ensured the RSQL visitor uses case-insensitive string comparisons for `==` and `!=` operators.
- Double-checked all imports and type usages (Predicate, Path, @NonNull).
- Manually tested the endpoint and confirmed that the output now matches the expected results.

### Outcome

After these final changes, the RSQL endpoint now returns the correct output and matches records as expected. The Specification logic and entity mappings are working, and the search functionality is robust and reliable.

### Problem

`[ERROR] Compilation failure:
[ERROR] /C:/Users/saksa/cbfacademy/trade-capture-system/backend/src/main/java/com/technicalchallenge/service/TradeService.java:[36,34] cannot find symbol
  symbol:   class Operator
  location: package cz.jirutka.rsql.parser.ast
`

RSQL parser did not recognize the wildcard/like operator (`=like=`) in queries such as `counterparty.name=like=*bank*`. This caused errors like `Unknown operator: =like=` and prevented wildcard search functionality.

### Solution

- Registered a custom RSQL operator for wildcard/like queries by creating a new `ComparisonOperator` with the value `=like=`.
- Added the custom operator to the set of default RSQL operators using a `HashSet`:

```java
Set<ComparisonOperator> operators = new HashSet<>(RSQLOperators.defaultOperators());
ComparisonOperator LIKE = new ComparisonOperator("=like=");
operators.add(LIKE);
RSQLParser parser = new RSQLParser(operators);
```

- Used a `Set` (specifically a `HashSet`) to ensure each operator is unique and to provide fast lookup and insertion. This is important for operator registration, so user does not accidentally register the same operator multiple times and the parser can efficiently check available operators.
- Created a new `RSQLParser` instance with the updated set of operators so it recognizes the new `=like=` operator.
- Updated the RSQL visitor logic to handle the `=like=` operator and perform SQL `LIKE` queries using wildcards.
- Added validation and handling for the `=like=` operator in the RSQL visitor class, ensuring case-insensitive wildcard matching. Example:

```java
// Handle case-insensitive LIKE operator (=like=)
    // Convert RSQL-style *wildcards* into SQL-style %wildcards%
    String pattern = values.get(0)
        .replace('*', '%')
        .toLowerCase(); // Make the search case-insensitive

    // Apply LOWER() to both sides (field + search text)
    return criteriaBuilder.like(
        criteriaBuilder.lower(path.as(String.class)),
        pattern);
}
```

- Verified the fix by running queries such as `counterparty.name=like=*bank*` and confirming correct results are returned.

### Outcome

2025-10-09T20:30:00 | TradeService.java, TradeRsqlVisitor.java | RSQL wildcard/like operator fix

### Problem

### Investigation

- I temporarily changed the assertion in my test from `assertNotNull(spec, ...)` to `assertNull(spec, ...)` to force a failure.
- Alternatively, I could have changed the visitor code to return `null` for the tested method.
- When I ran the test, it failed as expected, confirming that the test is working and will catch problems if the visitor does not return a valid Specification.

### Solution

- After confirming the test can fail, I restored the correct assertion (`assertNotNull(spec, ...)`) and code.
- This process proved that my test is not a false positive and will correctly report errors if the code breaks in the future.

### Outcome

Deliberately testing for failure is a useful technique to validate new tests. It is not meant to be left in the codebase, but helps ensure that the test suite is reliable and meaningful. My TradeRsqlVisitor test now provides confidence that the visitor logic is being checked properly.

### Problem

`org.junit.jupiter.api.AssertionFailedError: Expected exception to be thrown, but nothing was thrown.
    at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:55)
    at com.technicalchallenge.service.TradeRsqlVisitorTest.testInvalidOperatorThrowsException(TradeRsqlVisitorTest.java:123)`

Unit tests for TradeRsqlVisitor did not throw an exception for unsupported RSQL operators (e.g., =invalid=), causing tests to pass when they should fail. This could allow invalid queries to be silently ignored, reducing robustness and making bugs harder to detect.

### Root Cause

The visitor logic only threw exceptions for unsupported operators in the toPredicate method, not in the visit(ComparisonNode) method. As a result, invalid operators could be processed without error until later, or even return null, which is unsafe.

### Solution

Added a check for supported operators at the start of visit(ComparisonNode) in TradeRsqlVisitor.
Now throws IllegalArgumentException immediately for unsupported operators, ensuring invalid queries are rejected early.
Replaced any return null in toPredicate with an exception for safety and clarity.
Updated unit tests to expect exceptions for invalid operators and verify correct error handling.

### Outcome

Tests now fail as expected for unsupported operators, and the codebase is more robust against invalid RSQL queries. Exception handling is consistent and safe, and documentation is updated for future reference.

### Test failed

2025-10-09T21:00:00 | TradeRsqlVisitor.java, TradeRsqlVisitorTest.java | Invalid operator exception handling fix
2025-10-10T22:20:00 | TradeRsqlVisitorTest.java | Type mismatch and Mockito generics errors in testVisitComparisonNode_unexpectedValueType

### Problem 1: Type Mismatch Conversion Error

The test method for RSQL visitor failed when a query value did not match the expected Java type of the model field. For example, passing a string value 'NotANumber' for a numeric field (e.g., tradeId of type Long) caused a conversion error:

```java
Object typedValue = convertValue(fieldType, values.get(0));
```

This resulted in:

```
IllegalArgumentException: Invalid value 'NotANumber' for type Long
```

### Solution

Added robust type conversion and error handling in the utility method:

```java
public static Object convertValue(Class<?> type, String value) {
    try {
        if (type == String.class) return value;
        if (type == Integer.class || type == int.class) return Integer.parseInt(value);
        if (type == Long.class || type == long.class) return Long.parseLong(value);
        // ...other types
        throw new IllegalArgumentException("Unsupported type: " + type.getSimpleName());
    } catch (Exception e) {
        throw new IllegalArgumentException("Invalid value '" + value + "' for type " + type.getSimpleName());
    }
}
```

This ensures that invalid values for a field type throw a clear exception, and the test now passes by expecting this error.

### Problem 2: Mockito Type Safety and Generics Error

Mockito stubbing in the test method failed due to type safety and generics mismatch when mocking JPA Criteria API Path and its return types:

```java
Mockito.when(root.get("tradeId")).thenReturn(path); // Type mismatch error
Mockito.when(path.getJavaType()).thenReturn(Long.class); // Generics error
```

Resulted in:

```
The method thenReturn(Path<Object>) is not applicable for the arguments (Path<Long>)
Type safety: Unchecked cast from Path to Path<Long>
```

### Solution

Resolved by using raw type casting and method-level suppression for unchecked warnings:

```java
@SuppressWarnings({"unchecked", "rawtypes"})
void testVisitComparisonNode_unexpectedValueType() {
    Path rawPath = (Path) path;
    Class rawClass = (Class) Long.class;
    Mockito.when(root.get(Mockito.eq("tradeId"))).thenReturn(rawPath);
    Mockito.when(path.getJavaType()).thenReturn(rawClass);
    // ...rest of test
}
```

This allows the test to compile and run, while still verifying the correct exception is thrown for type mismatches.

### Progress

- Identified and documented type conversion and mocking errors.
- Improved error handling in conversion utility.
- Refactored test code for type safety and generics compatibility.
- All tests now pass, confirming robust handling of edge cases in RSQL visitor logic.

// Utility Method Explanation
The `convertValue` utility method is designed to convert a string value from an RSQL query into the correct Java type expected by the model field. This ensures type safety when building JPA predicates. The method checks the field's type (e.g., `Long`, `Integer`, `String`) and parses the value accordingly. If the value cannot be converted, it throws an `IllegalArgumentException`.

Usage in the RSQL visitor:

- In the Specification's `toPredicate` method, after determining the field type using JPA Criteria API (`path.getJavaType()`), the code calls `convertValue(fieldType, values.get(0))` to convert the query value to the correct type before building the predicate.
- This guarantees that comparisons (e.g., `tradeId == 123`) are performed with the correct types, preventing runtime errors and ensuring robust query handling.

Example usage:

```java
Class<?> fieldType = path.getJavaType();
Object typedValue = convertValue(fieldType, values.get(0));
```

This pattern is used for all supported operators, so every query value is validated and converted before being used in a database comparison.

### Problem

Test failure in testSearchTrades_FilterByCounterparty - AssertionFailedError: expected: <BigBank> but was: <null>

### Cause:

The mock for tradeMapper.toDto(any(Trade.class)) returned a new TradeDTO with no fields set, so counterpartyName was null in the result.

### Solution:

Updated the stubbing to return a TradeDTO with counterpartyName set to "BigBank":

### Problem

The method findAll(Example<Trade>) is ambiguous for the type TradeRepository

### Cause:

Mockito stubbing for findAll(any()) was ambiguous because TradeRepository inherits multiple overloaded findAll methods from its parent interfaces.

### Solution:

Made the stubbing explicit for the Specification overload:
`when(tradeRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(trade));`

Problem
Test failure in testFilterTrades_FilterByCounterparty - NullPointerException: Cannot invoke "org.springframework.data.domain.Page.getContent()" because "tradePage" is null
Method:
testFilterTrades_FilterByCounterparty
Class:
TradeServiceTest

### Root Cause:

The test called tradeService.filterTrades(criteria, 0, 10).getContent(), but the repository/service mock did not return a valid Page<Trade>, resulting in a null value and a NullPointerException when .getContent() was called.
getContent() is a method on the Spring Data Page object. It returns the actual list of items (e.g., List<TradeDTO>) for the current page, extracted from the paginated result. For example, if the query returns a page containing 10 trades, getContent() will return a List<TradeDTO> with those 10 trades.
In the test, the repository/service mock did not return a valid Page object—so the result of filterTrades(...) was null. When called .getContent() on this null value, it caused a NullPointerException. The fix is to ensure the mock returns a real (non-null) Page object, so getContent() can safely return the expected list of results.

### Solution:

Mock the repository to return a non-null Page object using Mockito:

### Problem test failing TradeTradeValidationServiceTest

[ERROR] TradeValidationServiceTest.failWhenMajurityBeforeStartDate:34 expected: <true> but was: <false>
[INFO] Finished at: 2025-10-11T22:52:09+01:00

### Cause

Forgot to validate that the maturity date is not before the trade date. The test expected the validation to fail when maturity date is before trade date, but the code only checked against start date.

### Solution

Added an OR condition to the validation logic to check both maturity date against start date and start date against trade date:

```java
if ((trade.getTradeMaturityDate().isBefore(trade.getTradeStartDate())
     || (trade.getTradeStartDate().isBefore(trade.getTradeDate())))) {
    // fail validation
}
```

This ensures that the test passes and both business rules are enforced.

# Error 1

2025-10-13T09:15:00 | TradeLegValidatorTest.java | NullPointerException when validating pay/receive flags

## Cause

Called .equals() on a null pay/receive flag in TradeLegDTO, resulting in a NullPointerException during validation.

## Solution

Added explicit null checks before calling .equals() on pay/receive flags in TradeLegValidator:

```java
if (leg1.getPayReceiveFlag() != null && leg2.getPayReceiveFlag() != null) {
    // compare flags
}
```

### Problem

2025-10-13T09:20:00 | TradeLegValidator.java | Incorrect validation logic for opposite pay/receive flags

### Cause

Validation logic did not correctly check for opposite pay/receive flags, causing tests to fail for valid legs.

### Solution

Refactored validation to use:
if (!leg1.getPayReceiveFlag().equals(leg2.getPayReceiveFlag())) {
// valid: flags are opposite
}

Error 3
2025-10-13T09:25:00 | TradeLegValidatorTest.java | Test failure due to missing error message assertion

### Cause

Test did not assert that the correct error message was returned when pay/receive flags were not opposite, resulting in false positives.

### Solution

Added assertion for error message in TradeLegValidatorTest:

`assertTrue(result.getErrorMessages().contains("Legs must have opposite pay/receive flags"));`
