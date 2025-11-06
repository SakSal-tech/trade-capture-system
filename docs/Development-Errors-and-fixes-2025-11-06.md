## Errors and test failures (since 06-Nov-2025 10:00 UK)

---

### 1) Syntax / stray-brace error in TradeDashboardService

### Problems

- Tests and compilation failed locally with a Java syntax error originating from `TradeDashboardService` (a stray or mismatched brace). The compiler stack trace pointed to that class and prevented the test suite from starting.

### Root cause

- During refactoring to extract settlement enrichment logic the file ended up with a mismatched brace (likely from a cut-and-paste or an incomplete edit). This produced a compile-time syntax error and prevented any further test execution.

### Solution

- Manually corrected the brace placement in `TradeDashboardService.java` so the class compiles. Verified compilation by running targeted tests that depend on the service.

Files changed:

- backend/src/main/java/com/technicalchallenge/service/TradeDashboardService.java

Commands used to verify:

```bash
mvn -Dtest=com.technicalchallenge.service.TradeDashboardServiceAuthTest test
```

### Impact

- Immediate: prevented the build/test runner from compiling the project, blocking all further automated testing and local development runs until fixed.
- After fix: compilation restored and the test harness could start up; allowed following fixes to proceed.

---

### 2) Large number of ApplicationContext startup errors after constructor change ("missing bean")

### Problems

- After adding `AdditionalInfoRepository` as a constructor parameter to `TradeDashboardService` (and similar wiring changes), many tests failed to start: the initial full test run showed ~74 ApplicationContext startup errors. These failures manifested as Spring unable to create beans for slices/tests that didn't declare a repository bean.

### Root cause

- Constructor injection was changed to include a new required dependency (`AdditionalInfoRepository`). Several Spring test slices (controller-level tests, @WebMvcTest or similar) did not provide that repository bean in their slice context. Spring failed to resolve the dependency when creating the service bean, causing ApplicationContext startup failures across many tests.

### Solution

- Three-pronged mitigation applied:
  1. Added a wiring compatibility measure to service class by annotating the primary constructor with `@Autowired` so Spring selects the intended constructor explicitly (reduces ambiguity in auto-wiring during tests). This was a safe, non-behavioral change to make Spring's constructor selection deterministic.
  2. For controller/test slices that failed to start because they don't load repository beans, added `@MockBean AdditionalInfoRepository` to those test classes. This injects a mock repository into the test ApplicationContext so the service bean can be created without changing production behavior.
  3. Verified the fixes by running targeted controller tests and iterating until the contexts started successfully.

Files changed (test-side only):

- backend/src/test/java/com/technicalchallenge/controller/BookControllerTest.java (added `@MockBean AdditionalInfoRepository`)
- backend/src/test/java/com/technicalchallenge/controller/UserControllerTest.java (added `@MockBean AdditionalInfoRepository`)
- backend/src/test/java/com/technicalchallenge/controller/TradeLegControllerTest.java (added `@MockBean AdditionalInfoRepository`)
- backend/src/test/java/com/technicalchallenge/controller/CounterpartyControllerTest.java (added `@MockBean AdditionalInfoRepository`)
- backend/src/test/java/com/technicalchallenge/controller/CashflowControllerTest.java (added `@MockBean AdditionalInfoRepository`)
- backend/src/test/java/com/technicalchallenge/controller/TradeControllerTest.java (added `@MockBean AdditionalInfoRepository`)

Commands used to verify:

```bash
mvn -Dtest=BookControllerTest,UserControllerTest,TradeLegControllerTest,CounterpartyControllerTest,CashflowControllerTest test
```

### Impact

- Immediate: Large number of tests failed to start; CI / local dev runs reporting dozens of ApplicationContext errors.
- After fix: Controller slice tests started successfully when provided a `@MockBean` for the missing repository; this is a test-context-only wiring change and does not alter production behavior.

Notes and rationale:

- Adding `@MockBean` is the least intrusive way to restore Spring test slices. It keeps the production code unchanged except for the `@Autowired` annotation used to make constructor selection explicit. If you prefer, an alternative is to revert the constructor change and introduce a setter or optional dependency — but that would require a design decision and touches production code more broadly. The approach used preserves domain behavior.

---

### 3) Malformed / corrupted test file: TradeDashboardServiceAuthTest.java

### Problems

- A test file (`TradeDashboardServiceAuthTest.java`) was malformed (corrupted contents) which caused compilation/test harness failures for that test class.

### Root cause

- During earlier refactors (edits and multiple patches), that test file became malformed — likely due to an incomplete patch or accidental paste that left invalid Java syntax. The JUnit run reported compile errors for that file.

### Solution

- Repaired the file by restoring a valid Java test class structure and re-adding necessary mocks (including `@Mock AdditionalInfoRepository`) and wiring the mock into the service under test within the `@BeforeEach` setup.

Files changed:

- backend/src/test/java/com.technicalchallenge.service/TradeDashboardServiceAuthTest.java (fixed corrupted content, restored valid test class and mocks)

Commands used to verify:

```bash
mvn -Dtest=com.technicalchallenge.service.TradeDashboardServiceAuthTest test
```

### Impact

- Immediate: The malformed test prevented the individual test from compiling/running, and could have contributed to noisy build output.
- After fix: the test compiled and ran successfully, reducing unrelated noise and enabling focused debugging.

---

### 4) UnfinishedStubbingException in TradeControllerTest (nested mock invocation)

### Problems

- While running `TradeControllerTest`, an `UnfinishedStubbingException` occurred when using Mockito `when(...)` with an argument that indirectly called another mock. This is the familiar "invoking a mock inside a when(...)" anti-pattern that causes nested/mock scheduling issues.

### Root cause

- Test code used `when(mockA.someMethod(mockB.otherMethod(...)))` or otherwise called into another mocked object's method inside the `when(...)` argument. Mockito treats this as unfinished stubbing because the inner mock call is evaluated at stubbing time.

### Solution

- Reworked the test stubbing to avoid invoking other mocks while building the `when(...)` call. The stable approach was to create concrete return objects (e.g., a real `TradeDTO` instance) and use that concrete instance in `when(...).thenReturn(...)`. This removed nested mock calls and avoided `UnfinishedStubbingException`.

Files changed (test-only):

- backend/src/test/java/com/technicalchallenge/controller/TradeControllerTest.java (adjusted stubbing to return concrete `TradeDTO` and avoid nested mock calls)

Example verification command:

```bash
mvn -Dtest=com.technicalchallenge.controller.TradeControllerTest test
```

### Impact

- Immediate: caused a test run to abort with an exception, blocking verification until the test was fixed.
- After fix: the test suite progressed; the `TradeControllerTest` executed cleanly.

---

### 5) TradeControllerTest failures after controller started delegating to TradeService.getTradeDtoById(...)

### Problems

- After the controller was changed to delegate to a new service method `tradeService.getTradeDtoById(...)` (which enriches a single trade DTO), `TradeControllerTest` failed because the test's existing stubs expected the old service methods and did not stub this new method.

### Root cause

- The controller change introduced a new interaction point (a service method) that the slice test was not aware of. Tests continued to stub the old service behavior and therefore did not provide a return value for the new call — causing test failures.

### Solution

- Updated `TradeControllerTest` to stub `tradeService.getTradeDtoById(...)` and return a concrete `TradeDTO` instance. Also added `@MockBean AdditionalInfoRepository` to the controller test context (see item 2) so the overall ApplicationContext could create the service bean.

Files changed:

- backend/src/test/java/com.technicalchallenge.controller/TradeControllerTest.java

Commands used to verify:

```bash
mvn -Dtest=com.technicalchallenge.controller.TradeControllerTest test
```

### Impact

- Immediate: caused controller tests to fail until updated.
- After fix: the controller test runs green and accurately tests controller-to-service delegation.

---

### 6) Missing `@MockBean` in other controller test slices (propagating failures)

### Problems

- Several controller-level tests (slices) failed to start because the new required repository bean was not present in their test ApplicationContext.

### Root cause

- When production constructors require additional beans, test slices must supply them (via `@MockBean` or configuration). The tests in those slices had not been updated to provide `AdditionalInfoRepository` and so Spring could not construct the service graph.

### Solution

- Added `@MockBean AdditionalInfoRepository` to the affected controller test classes (BookControllerTest, UserControllerTest, TradeLegControllerTest, CounterpartyControllerTest, CashflowControllerTest, TradeControllerTest). After that the ApplicationContext for each slice could start and run tests.

Files changed (test-only):

- backend/src/test/java/com/technicalchallenge/controller/BookControllerTest.java
- backend/src/test/java/com/technicalchallenge/controller/UserControllerTest.java
- backend/src/test/java/com/technicalchallenge/controller/TradeLegControllerTest.java
- backend/src/test/java/com/technicalchallenge/controller/CounterpartyControllerTest.java
- backend/src/test/java/com/technicalchallenge/controller/CashflowControllerTest.java
- backend/src/test/java/com/technicalchallenge/controller/TradeControllerTest.java

Verification command used iteratively:

```bash
mvn -Dtest=BookControllerTest,UserControllerTest,TradeLegControllerTest,CounterpartyControllerTest,CashflowControllerTest,TradeControllerTest test
```

### Impact

- Immediate: without the mocks, many tests could not start and the overall test run appeared to fail with dozens of ApplicationContext errors.
- After fix: the controller slice tests started and executed as expected; build noise reduced significantly.

Note: these `@MockBean` additions only affect test contexts; they do not change runtime application wiring in production.

---

### 7) Interrupted / cancelled test run

### Problems

- A previously-started `mvn -Dtest=TradeControllerTest test` run was interrupted/cancelled during a troubleshooting session. This left the immediate verification incomplete.

### Root cause

- Manual interruption (tool/terminal cancel) during iterative debugging.

### Solution

- Re-ran the specific test thereafter to completion. The rerun succeeded: `TradeControllerTest` finished with 7 tests, 0 failures.

Command re-run:

```bash
mvn -Dtest=TradeControllerTest test
```

### Impact

- Minimal — this was an operational interruption; no code change was needed. The later re-run completed successfully and confirmed the fix.

---

## Summary of current state (end of day 06-Nov-2025, since 10:00 UK)

- All problems described above were investigated and addressed with targeted, low-risk fixes, primarily in test code and test configuration. No production business logic, validation, or privilege checks were changed.
- Compiler/syntax error was fixed first so the codebase could be compiled.
- The majority of failures stemmed from making a constructor require an extra repository bean; the safe mitigation was to provide test-context mocks (`@MockBean AdditionalInfoRepository`) for controller slices and make Spring's constructor selection explicit via `@Autowired` on the intended constructor.
- Nested mock stubbing issues were resolved by returning concrete DTO objects in Mockito stubs instead of calling mocks inside `when(...)`.

## Recommended follow-ups

1. Run the full backend test suite once more to confirm no remaining failing tests:

```bash
mvn -f backend/pom.xml clean test
```

2. If CI still reports intermittent ApplicationContext failures, search for remaining `@WebMvcTest` or slice tests that depend indirectly on `TradeDashboardService` and add `@MockBean AdditionalInfoRepository` there as needed.
3. If you prefer not to add test-only `@MockBean`s widely, consider an alternate production-friendly approach: make `AdditionalInfoRepository` an optional dependency, or introduce a thin factory/service that can be mocked more easily. That is a design decision and should be reviewed because it touches production constructors.
