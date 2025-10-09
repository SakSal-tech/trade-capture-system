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

I found out that the class AbstractRSQLVisitor does not exist in the rsql-parser library version you are using (2.1.0). I changed to RSQLVisitor class.
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
