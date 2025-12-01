# Step 3 – Security_Architecture

**Scope:** How I secured controller entry points, service operations, and database‑backed user privileges; how I mapped user profiles to Spring authorities; and how I enforced ownership semantics.

## 1. Goals

- Express **who can call what** clearly at the controller layer.
- Enforce **ownership and elevated roles** at the service layer to avoid gaps.
- Load users and privileges from the database in a way that is **predictable and testable**.

## 2. User Loading and Authority Mapping

### 2.1 DatabaseUserDetailsService

- I load `ApplicationUser` by `loginId` and ensure the user is `active`.
- From `userProfile.userType` I build a role like `ROLE_TRADER`, and I also add **normalised** roles for hybrids to keep checks robust.
- I fetch the user’s privileges via `UserPrivilegeService.findPrivilegesByUserLoginId(...)`, map `Privilege.name` to authorities, and add **aliases** where historical names differ (for example `READ_TRADE` implies `TRADE_VIEW`).

**Why this design:** It centralises the mapping between **domain profile** and **security model** in one place, so controllers and services can just inspect authorities. All conversion from database user data (roles, privileges) to Spring Security authorities happens in one class (DatabaseUserDetailsService).

**Alternatives:** Using only roles would make fine‑grained checks difficult. Using only privileges would make common `@PreAuthorize("hasRole('TRADER')")` brittle. I keep both.

## 3. HTTP Security Configuration

### 3.1 SecurityConfig

- I enable `@EnableMethodSecurity` so controller annotations are active.
- For dev and testing I keep requests generally open and rely on method security, disable CSRF for API tooling, and register a `DaoAuthenticationProvider` that binds `DatabaseUserDetailsService` to a `PasswordEncoder`.
- I enable HTTP Basic for easy testing from Swagger and curl and disable the default form login to avoid browser redirects.

**Why:** The app is primarily an API. **Method security** is the most precise way to express per‑endpoint policy without fighting global path rules during development.

### 3.2 TestSecurityConfig

- For integration tests, I define an in‑memory user and a simple `SecurityFilterChain` that permits H2 and Swagger endpoints and requires authentication elsewhere, with CSRF disabled for the H2 console.
- I use `BCryptPasswordEncoder` even in tests so there is no mismatch when I flip between environments.

**Why:** Tests need a predictable environment that is isolated from the developer’s database and credentials.

## 4. Controller‑Level Policy with `@PreAuthorize`

Examples from `TradeDashboardController` and `TradeController`:

- `@PreAuthorize("(hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')) or hasAuthority('TRADE_VIEW')")` for read endpoints.
- `@PreAuthorize("hasAnyRole('TRADER','SALES')")` for create.
- Narrow endpoints like `/my-trades` also compare the requested `traderId` to `authentication.name` so traders can only fetch their **own** data.

**Why this matters:** Authorisation is **visible and auditable** in the controller signatures and stays close to the business use case.

## 5. Service‑Level Guards and Ownership

I repeat key checks in the service to prevent bypass through non‑HTTP entry points or mistaken reuse:

- `TradeService.getTradeById(...)` and `cancelTrade(...)` check `canViewTrade(...)` or `canEditTrade(...)` against the current `Authentication`. I allow elevated roles (`ROLE_MIDDLE_OFFICE`, `ROLE_SUPERUSER`) to access others’ trades, while normal traders are limited to their **own**.
- Where a validator bean is not available in test contexts, I apply a **safe fallback** owner check so behaviour remains consistent.

**Why duplicate protection:** Defence in depth. If a future controller forgets an annotation, the service still won’t leak data.

## 6. DB‑Backed Privilege Checks in `hasPrivilege(...)`

In `TradeDashboardService` I added a robust helper:

- First it **short‑circuits** when the current `Authentication` already has the required authority or a role treated as equivalent for view rights.
- If that doesn’t match, it queries the DB using `UserPrivilegeService.findPrivilegesByUserLoginIdAndPrivilegeName(user, privilege)` for an authoritative decision.
- Errors during lookup **deny by default** and are logged.

**Why:** It balances speed (short‑circuit) and correctness (DB check), and gives me a single place to evolve the definition of “who can view dashboards”.

## 7. CORS and Web Config

`WebConfig` defines a single CORS policy for `/api/**` so I don’t have to copy configuration into each controller. I allow local dev origins and common methods and headers.

## 8. Alternatives Considered

- Resource‑server JWT with external identity provider: excellent for SSO, but beyond this step and would add moving parts.
- URL‑based ant matcher rules only: workable but coarse for this API; method security is clearer.
