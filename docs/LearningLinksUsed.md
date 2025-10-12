# Learning Links

## Documentaions

### Paging and Sorting

https://docs.spring.io/spring-data/rest/reference/paging-and-sorting.html

### Interface Pageable

https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/domain/Pageable.html

### Specification

https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/domain/Specification.html

https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html

#### Key words:

    Root: the root entity (e.g., Trade)

    Query: the criteria query object being built.The actual SQL-like query object

    cb: the criteria builder, The actual SQL-like query object which provides methods like equal, like, between, and, or, etc.

## REST Query Language with RSQL

### RSQL / FIQL parser

https://github.com/jirutka/rsql-parser/blob/master/README.adoc

### To convert RSQL into Spring Data JPA Specification and QueryDSL Predicate

https://github.com/perplexhub/rsql-jpa-specification

### REST Query Language with RSQL

https://www.baeldung.com/rest-api-search-language-rsql-fiql

https://www.here.com/docs/bundle/data-client-library-developer-guide-java-scala/page/client/rsql.html

### What is Visitor Pattern design

https://www.baeldung.com/java-visitor-pattern

https://en.wikipedia.org/wiki/Visitor_pattern?ref=aboullaite.me

### Visitor class for RSQL Parsing

https://github.com/tennaito/rsql-jpa

### How to create a visitor class

https://www.youtube.com/watch?v=UQP5XqMqtqQ
https://www.youtube.com/watch?v=SK50E5CAzr0

https://www.youtube.com/watch?v=oqx62PVSM1U

### RSQL Injection

https://jayaramyalla.medium.com/unlocking-the-power-and-risks-of-rsql-a-deep-dive-into-its-real-world-use-and-security-e219b552928e

## Videos

### JPA Specifications

https://www.youtube.com/watch?v=AWBSWlM0JmQ&t=42s

https://www.youtube.com/watch?v=Zcs7tnK_7ec

### Spring Data JPA -Criteria Queries - The Full Guide

https://www.youtube.com/watch?v=qpSasUow1XI

### Spring Data JPA Pagination

https://www.youtube.com/watch?v=oq-c3D67WqM

Filtering API Responses for users

https://www.youtube.com/watch?v=d-Bljxm--EQ

## Java Reflection

- https://docs.oracle.com/javase/tutorial/reflect/index.html
- https://www.baeldung.com/java-reflection
- https://www.geeksforgeeks.org/reflection-in-java/
- https://www.tutorialspoint.com/java/java_reflection.htm

# ValidationResult and ValidationEngine – Learning Resources

## 1. Core Concepts

### The Result Pattern

**Link:** [The Result Pattern: Simplifying Error Handling in Your Code](https://medium.com/%40dev-hancock/the-result-pattern-simplifying-error-handling-in-your-code-fc31bb50a244?utm_source=chatgpt.com)  
**What you’ll learn:** How to use a `Result` or `ValidationResult` object instead of throwing exceptions, simplifying error handling and making business logic cleaner.

### Chain of Responsibility Pattern

**Link:** [Validation using the Chain of Responsibility Pattern](https://levelup.gitconnected.com/validation-using-the-chain-of-responsibility-pattern-236a6ded7078?utm_source=chatgpt.com)  
**What you’ll learn:** How to structure your validation engine so each validator performs a single responsibility in sequence.

### Rule Engine and Validation Engine Design

**Link:** [Basic Rules Engine Design Pattern](https://tenmilesquare.com/resources/software-development/basic-rules-engine-design-pattern/?utm_source=chatgpt.com)  
**What you’ll learn:** How to build a reusable “rules engine” that executes business rules dynamically — similar to your `TradeValidationEngine`.

### Strategy Pattern for Validation

**Link:** [Simplifying Validations Using the Strategy (Enum) Pattern](https://dzone.com/articles/validations-using-enum?utm_source=chatgpt.com)  
**What you’ll learn:** Using enum or strategy-based approaches for handling multiple validation rules (cleaner alternative to long `if-else` or `switch` blocks).

### Sequential Validation Design

**Link:** [Design Patterns for Dependent Sequential Validation (StackOverflow)](https://stackoverflow.com/questions/65233648/design-patterns-for-dependent-sequential-validation?utm_source=chatgpt.com)  
**What you’ll learn:** How to handle validations that depend on the outcome of previous validations (for example, only run leg validation if date validation passes).

## 2. XMLUnit ValidationResult (Inspiration Source)

### XMLUnit ValidationResult Javadoc

**Link:** [XMLUnit ValidationResult Class (javadoc.io)](https://javadoc.io/doc/org.xmlunit/xmlunit-core/2.6.1/index-all.html?utm_source=chatgpt.com)  
**What you’ll learn:** The structure of XMLUnit’s `ValidationResult` — includes a boolean `isValid()` and a list of problems (similar to your `TradeValidationResult`).

### XMLUnit User Guide

**Link:** [XMLUnit User Guide](https://xmlunit.sourceforge.net/userguide/html/ar01s04.html?utm_source=chatgpt.com)  
**What you’ll learn:** How `Validator` and `ValidationResult` work together to validate XML documents. This mirrors how your `TradeValidationEngine` orchestrates validators.

### Introduction to XMLUnit (Baeldung)

**Link:** [Introduction to XMLUnit 2](https://www.baeldung.com/xmlunit2?utm_source=chatgpt.com)  
**What you’ll learn:** Example of XML validation using `ValidationResult`, showing how errors are collected and reported.

## 3. Design Patterns and Techniques to Explore

| Pattern                  | Description                                                                                                   |
| ------------------------ | ------------------------------------------------------------------------------------------------------------- |
| Result / Either Pattern  | Represent success or failure using a structured result object, similar to `TradeValidationResult`.            |
| Chain of Responsibility  | Pass a `ValidationResult` through multiple validators like `TradeDateValidator` and `UserPrivilegeValidator`. |
| Strategy Pattern         | Implement each validation rule type (date, user privilege, legs, entity) as a separate strategy class.        |
| Fail-Fast vs Collect-All | Decide whether to stop on the first error or collect all validation messages.                                 |
| Composite Pattern        | Combine multiple validation rules into one logical engine.                                                    |

## 4. Optional Deep Dive: Open Source Rule Engines

If you want to explore professional-grade validation and rule systems:

- [Drools (Business Rules Management System)](https://www.drools.org/)
- [Easy Rules – Lightweight Java Rules Engine](https://github.com/j-easy/easy-rules)

## Notes

Last updated: October 2025
