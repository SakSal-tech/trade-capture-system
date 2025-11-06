# Errors log — 2025-11-06 (since 10:00 UK)

- Date: 2025-11-06
- Time window: from 10:00 (UK) onward
- Location: backend tests and controller slices

Summary (one-line per issue)

1. TradeDashboardService syntax error — stray/mismatched brace prevented compilation.
2. ApplicationContext startup failures (~74 initially) after adding `AdditionalInfoRepository` to a constructor (missing bean in test slices).
3. Malformed test file `TradeDashboardServiceAuthTest.java` — corrupted contents fixed.
4. `UnfinishedStubbingException` in `TradeControllerTest` due to nested mock invocation in `when(...)` — fixed by returning concrete DTOs.
5. `TradeControllerTest` failures after controller now delegates to `tradeService.getTradeDtoById(...)` — updated test stubs accordingly.
6. Several controller slice tests missing `@MockBean AdditionalInfoRepository` — added mocks to restore ApplicationContext.
7. Interrupted/cancelled `TradeControllerTest` run — re-ran and confirmed success (7 tests, 0 failures).

Quick links

- Detailed writeup (appendix): `docs/Development-Errors-and-fixes-2025-11-06.md`
- Main development error log: `docs/Development-Errors-and-fixes.md` (you previously asked to append; I created the dated appendix file instead due to patching issues)

Verification commands used during debugging (examples run locally):

```bash
# run a single test class
mvn -Dtest=com.technicalchallenge.controller.TradeControllerTest test

# run multiple controller slice tests together
mvn -Dtest=BookControllerTest,UserControllerTest,TradeLegControllerTest,CounterpartyControllerTest,CashflowControllerTest test

# run the full backend test suite (recommended next step)
mvn -f backend/pom.xml clean test
```

Notes

- All fixes were test-context-only or wiring fixes (e.g., `@MockBean` additions and a constructor `@Autowired` annotation) or syntax repairs. No production validation/privilege/business logic was changed.
