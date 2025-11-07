# Step 3 – Developer_Reflection

**Author:** Me  
**Date:** 2025-11-06  
**Scope:** What I learned building Step 3 and how I would guide another engineer through the decisions.

---

## 1. What Was Hard

- Getting **RSQL right** required me to think in terms of ASTs and controlled property paths. I found it helpful to picture each comparison node as a predicate that I could compose into a single `Specification`.
- Balancing **method security** with **service‑level ownership**. I kept both because future entry points and tests won’t always traverse the controllers.
- Designing error messages that are both **short** and **actionable**. The `TradeValidationResult` made it easier to present the whole story at once.

---

## 2. Programming Techniques That Paid Off

- **Specification composition**: It let me extend search without exploding repository methods.
- **Visitor for RSQL**: Separating parse from translation made debugging straightforward.
- **DTOs that mirror the UI**: By modelling `TradeSummaryDTO` and `DailySummaryDTO` around how the frontend thinks, I reduced glue code and made evolution safe.
- **Defensive null handling**: Validators never assume presence; they check, report, and move on. This avoided noisy stacks and kept tests green.

---

## 3. Alternatives I Weighed

- QueryDSL vs Specifications. I stayed with Specifications to keep the dependency surface small and align with existing code.
- Caching dashboard summaries. I postponed it because correctness and clarity were more critical than micro‑optimising first.
- Centralising everything into annotations. Useful, but I still wanted explicit service checks to survive refactors and CLI tools.

---

## 4. How This Reflects Trading Domain Practice

- Date and leg rules mirror real booking constraints: legs share maturities, fixed legs carry explicit rates, floating legs must reference an index.
- Role separation is realistic: traders can action their own trades, middle office can amend and review, support can view only.

---

## 5. What I Would Do Next

- Add end‑to‑end tests for RSQL with tricky nesting to guard against regressions.
- Introduce response caching for `/summary` and `/daily-summary` during peak hours.
- Integrate a proper risk service for Greeks instead of the placeholder deltas.

---

## 6. Pointers for New Contributors

- When adding a filter, think in terms of **entity paths** and null‑safe comparisons.
- When adding a rule, add a **unit test** on the validator first, wire it into `TradeValidationEngine`, then cover the service path.
- Keep controller annotations explicit; they are the best documentation of who’s allowed to do what.
