2025-10-07T12:00:00 | Line 179 | TradeController.java | The method searchTradesRsql(SearchCriteriaDTO) in the type TradeService is not applicable for the arguments (String)
2025-10-07T12:00:00 | Line 703 | TradeService.java | The return type is incompatible with Specification<Trade>.toPredicate(Root<Trade>, CriteriaQuery<?>, CriteriaBuilder)
2025-10-07T12:00:00 | Line 705 | TradeService.java | Type mismatch: cannot convert from jakarta.persistence.criteria.Predicate to java.util.function.Predicate

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
