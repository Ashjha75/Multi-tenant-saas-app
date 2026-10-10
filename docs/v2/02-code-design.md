# 02 — Code Design

How the code is structured and how the pieces fit. Vocabulary follows the deep-module glossary:
**module** (anything with an interface and an implementation), **interface** (everything a caller
must know, not just the signature), **seam** (where an interface lives and behaviour can be
swapped without editing in place), **adapter** (a thing filling a slot at a seam), **depth**
(behaviour per unit of interface a caller must learn).

## Layering

Six layers. The rule is: a layer may call the layer below it and may not call upward.

| Layer | Holds | May call |
|---|---|---|
| **Edge** | Servlet filters, Spring Security chain | Nothing in the app except `TenantContext` and `JwtTokenService` |
| **Controller** | `controllers/`, `auth/AuthenticationController` | Service interfaces only |
| **Service interface** | `services/*.java`, `auth/service/AuthenticationService` | — (this is the seam) |
| **Service implementation** | `services/impl/`, `auth/service/AuthenticationServiceImpl` | Repositories, mappers, other service interfaces |
| **Repository** | `repositories/` | JPA only |
| **Entity** | `entities/` | — |

Supporting modules sit outside the stack: `dto/`, `mapper/`, `exception/`, `common/`, `config/`,
`security/`.

**Observed violations.** Two, both real:

1. **`entities/AbstractEntity` calls into `config/TenantContext`** (`AbstractEntity.java:3,81`).
   The bottom layer reaches up to request-scoped state. A `@PrePersist` hook reads a ThreadLocal
   to fill `tenant_id`. This makes entities unusable outside a web request — any batch job or
   test that persists an entity must first populate a ThreadLocal.
2. **`mapper/CategoryMapper` calls `TenantContext`** (`CategoryMapper.java:18`), but
   `ProductMapper` and `StockMvtMapper` do not — they let the `@PrePersist` hook do it. The same
   job is done in two different places depending on the entity. Neither is wrong on its own;
   having both is the problem.

## Module map

| Module | Responsibility | Depends on | Depended on by |
|---|---|---|---|
| `config/` (11 files) | Tenant resolution, Hibernate wiring, caching, auditing, OpenAPI, bean definitions | Hibernate, Spring, `exception/` | Everything |
| `security/` (7) | JWT issue and verify, filter chain, auth error responses | `config/TenantContext`, `config/TenantSchemaResolver`, `exception/` | `auth/`, edge |
| `controllers/` (6) | HTTP mapping, validation trigger, OpenAPI annotations | `services/`, `dto/`, `common/` | — |
| `auth/` (4) | Login and tenant registration endpoints | `security/`, `services/TenantService` | — |
| `services/` (7) | The seam between controllers and business logic | `dto/`, `common/` | `controllers/`, `auth/` |
| `services/impl/` (6) | Business rules, tenant ownership checks, provisioning | `repositories/`, `mapper/`, `entities/`, `config/TenantContext` | via interfaces |
| `repositories/` (5) | Spring Data JPA interfaces | `entities/` | `services/impl/` |
| `entities/` (9) | JPA mappings, audit fields, tenant filter definition | `config/TenantContext` ⚠️ | `repositories/`, `mapper/` |
| `mapper/` (5) | Entity ↔ DTO conversion | `entities/`, `dto/` | `services/impl/` |
| `dto/` (10) | Request and response shapes, Bean Validation constraints | — | `controllers/`, `mapper/` |
| `exception/` (4) | `AppException`, `ApiError`, the `@RestControllerAdvice` | Spring Web | Everything |
| `common/` (1) | `PageResponse<T>` | Spring Data `Page` | `controllers/`, `services/` |

**Diagram Brief: Package dependency map**
Boxes:
  - Edge filters — TenantFilter, Spring Security chain, JwtAuthenticationFilter
  - controllers — 6 REST controllers
  - auth — AuthenticationController + AuthenticationServiceImpl
  - services — 7 interfaces (the seam)
  - services/impl — 6 implementations
  - repositories — 5 Spring Data interfaces
  - entities — 9 JPA classes
  - mapper — 5 converters
  - dto — request and response records
  - config — tenant + Hibernate + cache wiring
  - security — JWT and filter-chain classes
  - exception — error contract
  - common — PageResponse
  - TenantContext — ThreadLocal holding tenant id and schema
Arrows:
  - Edge filters -> controllers : request passes through
  - Edge filters -> TenantContext : writes tenant id
  - controllers -> services : calls interface
  - controllers -> dto : binds and validates
  - auth -> services : calls TenantService
  - auth -> security : issues token
  - services/impl -> services : implements
  - services/impl -> repositories : queries
  - services/impl -> mapper : converts
  - services/impl -> TenantContext : reads tenant id
  - repositories -> entities : maps
  - mapper -> entities : converts
  - mapper -> dto : converts
  - entities -> TenantContext : reads tenant id in @PrePersist (LAYERING VIOLATION)
  - config -> TenantContext : populates and clears
  - security -> TenantContext : overwrites with JWT claim
Grouping: controllers, auth inside "HTTP layer"; services, services/impl inside "business layer"; repositories, entities inside "persistence layer"; config, security, exception, common inside "cross-cutting"
Layout hint: top-down, HTTP at top, persistence at bottom, cross-cutting to the right

## Request lifecycle

One authenticated, tenant-scoped request: `GET /api/v1/categories?page=0&size=10` with an
`Authorization: Bearer` header and an `X-Tenant-ID` header.

| # | What happens | Where |
|---|---|---|
| 1 | `TenantFilter` runs first — `@Order(HIGHEST_PRECEDENCE)`, so it is outside the Spring Security chain. | `config/TenantFilter.java:22` |
| 2 | Path is checked against the exempt list (auth, health, swagger, `/error`). `/api/v1/categories` is not exempt. | `TenantFilter.java:44,90-102` |
| 3 | `X-Tenant-ID` is read and lowercased. Missing → **400**, no further processing. | `TenantFilter.java:49-60,104-110` |
| 4 | Format checked against `^[a-z0-9_-]{2,50}$`. Bad format → **400**. | `TenantFilter.java:63-72` |
| 5 | Header value written to `TenantContext` and to the SLF4J MDC. | `TenantFilter.java:75-76` |
| 6 | Spring Security chain starts. `JwtAuthenticationFilter` sits before `UsernamePasswordAuthenticationFilter`. | `security/SecurityConfig.java:79` |
| 7 | Bearer token extracted and verified with the RSA **public** key. Invalid → exception caught, chain continues unauthenticated. | `JwtAuthenticationFilter.java:37-38,57-59` |
| 8 | **`TenantContext` is overwritten** with the `tenant_id` claim from the token. The header value from step 5 is discarded. | `JwtAuthenticationFilter.java:40-47` |
| 9 | Schema name resolved and cached, then stored in `TenantContext`. Nothing reads it in `filter` mode. | `JwtAuthenticationFilter.java:45-46` |
| 10 | `Authentication` placed in the `SecurityContext`. Principal is the **user-id String**, not the `User` entity. | `JwtAuthenticationFilter.java:50-52` |
| 11 | Authorization rules evaluated. `/api/v1/categories` only needs `authenticated()`. No token → **401** via `RestAuthenticationEntryPoint`. | `SecurityConfig.java:57-67,76` |
| 12 | `CategoryController.getAllCategory` runs and calls the service interface. | `controllers/CategoryController.java:87` |
| 13 | Transaction advice opens a transaction — it is the **outermost** advice, pinned there deliberately. | `config/TransactionAdviceOrderConfig.java` |
| 14 | `TenantHibernateFilter` advice runs **inside** that transaction. Checks: method not exempt, tenant present, transaction active. Any failure throws. | `config/TenantHibernateFilter.java:60-91` |
| 15 | Hibernate `tenantFilter` enabled on the transaction's session with the tenant id as parameter. | `TenantHibernateFilter.java:93-94` |
| 16 | `CategoryServiceImpl.findAll` calls `categoryRepo.findAll(pageable)`. | `services/impl/CategoryServiceImpl.java:78-83` |
| 17 | Hibernate appends `WHERE tenant_id = ? AND deleted = false` to the SQL, because `@Filter` is on the concrete entity. | `entities/Category.java:24` |
| 18 | Rows mapped to `CategoryResponse`, wrapped in `PageResponse`. | `CategoryServiceImpl.java:81-82`, `common/PageResponse.java:24` |
| 19 | Transaction commits. Advice unwinds. | — |
| 20 | `JwtAuthenticationFilter`'s `finally` clears `TenantContext`. | `JwtAuthenticationFilter.java:60-62` |
| 21 | `TenantFilter`'s `finally` clears `TenantContext` and the MDC again. | `TenantFilter.java:78-81` |

**Why the tenant is cleared twice.** Both filters own a `finally` block. `JwtAuthenticationFilter`
is nested inside `TenantFilter`, so its clear runs first and `TenantFilter`'s clear is a no-op.
Harmless, but it means the tenant is gone the moment the security chain unwinds — nothing after
the Spring Security filter chain can read it. `[INFERRED]`

**Diagram Brief: Request lifecycle**
Boxes:
  - Client — sends Bearer token + X-Tenant-ID
  - TenantFilter — validates header, sets TenantContext
  - SecurityFilterChain — Spring Security entry
  - JwtAuthenticationFilter — verifies RS256 token, overwrites TenantContext from claim
  - AuthorizationFilter — permitAll vs authenticated vs @PreAuthorize
  - CategoryController — maps HTTP to service call
  - Transaction advice — opens transaction, outermost
  - TenantHibernateFilter — enables Hibernate tenantFilter inside the transaction
  - CategoryServiceImpl — business logic
  - CategoryRepo — Spring Data query
  - Hibernate — appends tenant_id predicate
  - PostgreSQL — public schema
Arrows:
  - Client -> TenantFilter : HTTP request
  - TenantFilter -> SecurityFilterChain : header valid
  - TenantFilter -> Client : 400 if header missing or malformed
  - SecurityFilterChain -> JwtAuthenticationFilter : chain
  - JwtAuthenticationFilter -> AuthorizationFilter : SecurityContext populated
  - AuthorizationFilter -> Client : 401 or 403
  - AuthorizationFilter -> CategoryController : allowed
  - CategoryController -> Transaction advice : service call is proxied
  - Transaction advice -> TenantHibernateFilter : inside transaction
  - TenantHibernateFilter -> CategoryServiceImpl : filter enabled on session
  - CategoryServiceImpl -> CategoryRepo : findAll
  - CategoryRepo -> Hibernate : criteria query
  - Hibernate -> PostgreSQL : SELECT with tenant_id predicate
Grouping: TenantFilter outside the security chain; JwtAuthenticationFilter and AuthorizationFilter inside SecurityFilterChain; TenantHibernateFilter and CategoryServiceImpl inside the transaction
Layout hint: left-to-right, with the two error exits dropping downward

## Multi-tenancy mechanism

This is the heart of the system and the place the old docs are least reliable.

### Two strategies exist in the source; one runs

| Strategy | Classes | Status |
|---|---|---|
| **(b) Row-level filtering** | `TenantFilter`, `TenantHibernateFilter`, `AbstractEntity`'s `@FilterDef`, `@Filter` on each entity | **Active** |
| **(a) Schema-per-tenant** | `MultiTenantConnectionProviderImpl`, `CurrentTenantIdentifierResolverImpl`, `TenantSchemaResolver`, `db/migration/tenant/` | **Inert** |

### The toggle

One property, `app.tenant.mode`, default `filter` (`application.yml:55`). It is not a runtime
switch — it decides which beans are created at startup.

| Class | Condition | File |
|---|---|---|
| `TenantFilter` | `mode = filter` (or absent) | `TenantFilter.java:23` |
| `TenantHibernateFilter` | `mode = filter` (or absent) | `TenantHibernateFilter.java:28` |
| `MultiTenantConnectionProviderImpl` | `mode = schema` | `MultiTenantConnectionProviderImpl.java:24` |
| `CurrentTenantIdentifierResolverImpl` | `mode = schema` | `CurrentTenantIdentifierResolverImpl.java:19` |

**They cannot run together.** The conditions are mutually exclusive, so no configuration produces
both. This answers the hypothesis in the brief directly: the codebase holds two strategies but is
not running two strategies.

**`schema` mode does not work.** Selecting it switches the `filter` beans off and switches on two
classes that nothing drives: no header is enforced, no tenant predicate is added, and no code sets
a schema that `MultiTenantConnectionProviderImpl` could route to. The result is an application
with **no isolation at all**. `docs/tenant-toggle.md` states this plainly.

### How `TenantContext` is populated and cleared

`TenantContext` holds two `ThreadLocal<String>` values — tenant id and schema name
(`TenantContext.java:4-5`).

| Event | Tenant id source | Where |
|---|---|---|
| Written (1st) | `X-Tenant-ID` header, lowercased | `TenantFilter.java:75` |
| **Overwritten** | `tenant_id` claim from the verified JWT | `JwtAuthenticationFilter.java:44` |
| Read by the aspect | → Hibernate filter parameter | `TenantHibernateFilter.java:62,94` |
| Read on write | → `tenant_id` column on insert | `AbstractEntity.java:81` |
| Read by services | → ownership checks and scoped queries | `CategoryServiceImpl.java:107,121` |
| Cleared | inner `finally` | `JwtAuthenticationFilter.java:61` |
| Cleared again | outer `finally` | `TenantFilter.java:79` |

**The JWT wins.** The header is required but not trusted. A caller sending another tenant's
`X-Tenant-ID` still gets only their own rows, because step 8 of the lifecycle replaces the header
value before any query runs. The header's real job is to fail fast and to tag logs. `[INFERRED]`
— no comment records this intent, but the ordering makes it unambiguous.

### Three conditions the filter needs — all failed at some point

Enabling a Hibernate filter is easy to get silently wrong. All three must hold:

1. **`@Filter` must be on each concrete `@Entity`.** Hibernate ignores it on a
   `@MappedSuperclass`. It is now on `Category`, `Product`, `StockMvt` and `User`; `AbstractEntity`
   carries only the global `@FilterDef` and a comment saying why (`AbstractEntity.java:34-36`).
2. **The service module must be `@Transactional`.** The filter is enabled on
   `entityManager.unwrap(Session.class)`; with no transaction that session is discarded before the
   query runs.
3. **Transaction advice must be the outer advice.** Otherwise the aspect and the transaction
   interceptor both sit at `LOWEST_PRECEDENCE` and their order is undefined.

**What `TransactionAdviceOrderConfig` guards against** — exactly condition 3. It sets
`@EnableTransactionManagement(order = HIGHEST_PRECEDENCE)` so the transaction advisor wraps
everything, guaranteeing `TenantHibernateFilter` runs inside a live transaction.

**The guard.** Condition 2 now fails loudly: the aspect throws if no transaction is active
(`TenantHibernateFilter.java:85-91`). Condition 1 has no such guard — adding an entity and
forgetting `@Filter` is still silent. That asymmetry is worth knowing.

### Failure modes

| Situation | Result | Where |
|---|---|---|
| No `X-Tenant-ID` on a protected path | 400, plain JSON `{"error": ...}` — **not** the `ApiError` shape | `TenantFilter.java:51-60` |
| Malformed `X-Tenant-ID` | 400, same non-standard shape | `TenantFilter.java:63-72` |
| No tenant in context at a service call | `IllegalStateException` → 500 "Unexpected error" | `TenantHibernateFilter.java:71-76` |
| No transaction at a service call | `IllegalStateException` → 500 | `TenantHibernateFilter.java:85-91` |
| Insert with no tenant anywhere | `IllegalStateException` from `@PrePersist` | `AbstractEntity.java:83-86` |
| Row belongs to another tenant | **404**, not 403 — deliberately, so existence is not leaked | `CategoryServiceImpl.java:106-113` |
| Unknown tenant id in `TenantSchemaResolver` | Falls back to `public`, logs a warning | `TenantSchemaResolver.java:62-72` |

Two of these are inconsistent with the documented error contract: the 400s from `TenantFilter`
bypass `GlobalExceptionHandler` entirely and the tenant-violation 500s expose nothing useful.

**Diagram Brief: Tenant resolution path**
Boxes:
  - Request — carries X-Tenant-ID and Bearer token
  - TenantFilter — validate header format
  - Reject 400 — missing or malformed header
  - TenantContext — ThreadLocal, tenant id + schema
  - JwtAuthenticationFilter — verify signature, read tenant_id claim
  - TenantSchemaResolver — tenant id to schema name, cached
  - tenantSchemas cache — ConcurrentMapCacheManager, no eviction
  - TenantHibernateFilter — aspect before every services.impl method
  - Guard: tenant present — else IllegalStateException
  - Guard: transaction active — else IllegalStateException
  - Hibernate session — filter enabled with tenant id
  - SQL — WHERE tenant_id = ? AND deleted = false
Arrows:
  - Request -> TenantFilter : X-Tenant-ID
  - TenantFilter -> Reject 400 : missing or bad format
  - TenantFilter -> TenantContext : write header value
  - TenantFilter -> JwtAuthenticationFilter : continue chain
  - JwtAuthenticationFilter -> TenantContext : OVERWRITE with JWT claim
  - JwtAuthenticationFilter -> TenantSchemaResolver : resolve schema
  - TenantSchemaResolver -> tenantSchemas cache : read or populate
  - TenantSchemaResolver -> TenantContext : write schema name (unused in filter mode)
  - TenantContext -> TenantHibernateFilter : read tenant id
  - TenantHibernateFilter -> Guard: tenant present : check
  - TenantHibernateFilter -> Guard: transaction active : check
  - Guard: transaction active -> Hibernate session : enable filter
  - Hibernate session -> SQL : append predicate
Grouping: the two Guard boxes inside "TenantHibernateFilter"; TenantSchemaResolver and tenantSchemas cache inside "schema-mode groundwork, unused in filter mode"
Layout hint: left-to-right, with Reject 400 dropping below

## Security design

### Filter order

| Position | Filter | Why there |
|---|---|---|
| Outermost | `TenantFilter` | `@Order(HIGHEST_PRECEDENCE)`. Runs before security so a missing header is rejected without touching the token. |
| Inside security chain | `JwtAuthenticationFilter` | `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)` (`SecurityConfig.java:79`) |
| Last | Spring's authorization filter | Evaluates `permitAll` / `authenticated` / `@PreAuthorize` |

A consequence of this order: **a missing tenant header returns 400 even when the caller has no
token at all.** Authentication never gets a chance to return 401 first.

### JWT lifecycle

| Step | Detail | Where |
|---|---|---|
| Keys loaded | `@PostConstruct`, via `ResourceLoader` so `file:` and `classpath:` both work. Failure is fatal at startup. | `JwtTokenService.java:38-60` |
| Signed | RS256 with the private key. | `JwtTokenService.java:79` |
| Verified | Public key, on every request. | `JwtTokenService.java:87-90` |
| Errors | Expired / malformed / unsupported / bad signature / empty each map to a distinct message. | `JwtTokenService.java:94-108` |

**A bug in that error mapping.** `JwtTokenService` throws
`exception.UnauthorizedException` (`JwtTokenService.java:3`), but `GlobalExceptionHandler` imports
and handles `security.UnauthorizedException` (`GlobalExceptionHandler.java:3,112`). Two classes,
same name, different packages. The specific messages are therefore never returned to a client. In
practice it does not surface, because `JwtAuthenticationFilter` catches every exception and lets
the chain continue unauthenticated (`JwtAuthenticationFilter.java:57-59`), producing a generic
401. The duplicate class is dead weight either way.

### Entry point vs access denied handler

| Handler | Fires when | Status | File |
|---|---|---|---|
| `RestAuthenticationEntryPoint` | Caller is **not authenticated** — no token, bad token, expired | 401, message `"Unauthorized"` | `security/RestAuthenticationEntryPoint.java:30-42` |
| `RestAccessDeniedHandler` | Caller **is** authenticated but lacks the role | 403, message `"Forbidden"` | `security/RestAccessDeniedHandler.java:30-42` |

Both write the same `ApiError` shape as `GlobalExceptionHandler`, so clients see one contract.
Neither includes a reason — just the word.

### Public vs protected

`PUBLIC_URLS` (`SecurityConfig.java:28-47`) covers health (both paths), `/error`,
`/api/v1/auth/**`, and the OpenAPI and Swagger surface. Everything else is `authenticated()`.
Preflight `OPTIONS` is open on every path (`SecurityConfig.java:63-64`).

`TenantFilter.isTenantExempt` (`TenantFilter.java:90-102`) maintains a **second, separate** list
covering the same ground. The two lists are kept in sync by hand.

## Error handling contract

`GlobalExceptionHandler` is a `@RestControllerAdvice` mapping exceptions to one JSON shape.

| Exception | Status | Message |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | `"Validation failed"` + per-field `details` |
| `BindException` | 400 | `"Binding failed"` + `details` |
| `ConstraintViolationException` | 400 | `"Constraint violation"` + `details` |
| `HttpMessageNotReadableException` | 400 | `"Malformed JSON request"` |
| Missing param / type mismatch / missing path var | 400 | exception message |
| `HttpMediaTypeNotSupportedException` | 415 | exception message |
| `HttpRequestMethodNotSupportedException` | 405 | exception message |
| `AuthenticationException` | 401 | `"Invalid username or password"` / `"Account is disabled"` / `"Account is locked"` / `"Authentication failed"` |
| `AccessDeniedException` | 403 | exception message |
| `security.UnauthorizedException` | 401 | exception message |
| `NoResourceFoundException` | 404 | `"No endpoint METHOD /path"` |
| `AppException` | its own status | its own message and code |
| `DataIntegrityViolationException`, `DuplicateKeyException` | 409 | `"Data integrity violation"` |
| `TransactionSystemException` | 500 | `"Transaction failed"` |
| `ResponseStatusException`, `ErrorResponseException` | its own | its own |
| anything else | 500 | `"Unexpected error"`, full stack trace logged |

Response shape clients can rely on (`exception/ApiError.java:7-13`):

```
{ "timestamp", "status", "error", "message", "path", "traceId", "details" }
```

`traceId` reads the SLF4J MDC key `traceId`. **Nothing ever sets that key**, so it is always
`null`. `tenantId` is set in the MDC instead (`TenantFilter.java:76`).

**The one gap**: `TenantFilter`'s two 400s are written straight to the response and never reach
this handler, so they use `{"error": "..."}` instead of the `ApiError` shape.

## Key class notes

Only the non-obvious ones. CRUD classes are skipped.

**`TenantHibernateFilter`** (`config/`) — a deep module behind a tiny interface: one `@Before`
advice, no public methods. Callers learn nothing and get tenant scoping on every query for free.
That is real **leverage**: the alternative is a `tenantId` parameter threaded through all 5
repositories and 6 implementations. The cost of the depth is that the mechanism is invisible at
the call site — nothing in `CategoryServiceImpl.findAll` hints that the SQL will be rewritten.

**`TransactionAdviceOrderConfig`** (`config/`) — an empty class whose whole content is one
annotation. It exists only to make advice order deterministic. Delete it and tenant filtering
breaks silently and intermittently. A good example of a module whose implementation is nearly
nothing but whose removal is not free.

**`MultiTenantConnectionProviderImpl`** (`config/`) — implements Hibernate's
`MultiTenantConnectionProvider`. Sets `search_path` per connection and resets it to `public` on
release (`:44-71`). Validates the schema name against `^[a-z0-9_]+$` before interpolating it into
SQL (`:95-99`) — the string is concatenated, so the validation is the only thing preventing SQL
injection through a tenant name. Not instantiated in `filter` mode.

**`CurrentTenantIdentifierResolverImpl`** (`config/`) — resolves the current tenant for Hibernate,
defaulting to `public`. Registers itself under `MULTI_TENANT_IDENTIFIER_RESOLVER`
(`:41`). Not instantiated in `filter` mode.

**`TenantSchemaResolver`** (`config/`) — maps a tenant id to `tenant_<company_code>` via a direct
`JdbcTemplate` query, cached in `tenantSchemas`. Called on **every authenticated request**
(`JwtAuthenticationFilter.java:45`) even though `filter` mode never uses the result. The cache is
what keeps that from being a database hit per request. Entries are never evicted, so renaming a
company code would serve a stale schema name until restart.

**`User`** (`entities/`) — both a JPA entity and Spring Security's `UserDetails`
(`User.java:23`). Convenient, but it means the persistence model and the security model cannot
change independently, and the password hash travels with every entity load.

**`AbstractEntity`** (`entities/`) — carries the `@FilterDef`, the audit columns, and a
`@PrePersist` hook that injects `tenant_id` from `TenantContext` and throws if it cannot
(`:66-92`). The throw is the last line of defence against an untenanted write.

## Decisions and rationale

ADR-style: decision, context, consequence. Rationale tagged by source.

| # | Decision | Context | Consequence | Tag |
|---|---|---|---|---|
| 1 | Row-level filtering over schema-per-tenant | Schema mode needs per-tenant migration and connection routing; filtering needs one predicate | One schema, simple ops; isolation depends on a predicate being correctly applied every time | `[DOCUMENTED]` — `docs/tenant-toggle.md` |
| 2 | Tenant comes from the JWT, not the header | The header is client-controlled and therefore not trustworthy | Header spoofing is impossible; the header becomes a fail-fast check and a log tag | `[INFERRED]` — from the overwrite at `JwtAuthenticationFilter.java:44` |
| 3 | RS256 keypair, not HMAC | Signing and verifying are separate capabilities | A verifier never needs the signing key; key distribution is a deployment concern | `[INFERRED]` |
| 4 | Enable the filter by AOP, not per repository | 11 call sites across 6 implementations would each need the tenant | One place to change; invisible at the call site | `[INFERRED]` |
| 5 | Fail closed when no tenant is in context | A query without a tenant returns every tenant's rows | Any un-tenanted service call is a 500, never a silent leak | `[DOCUMENTED]` — comment at `TenantHibernateFilter.java:70` |
| 6 | 404, not 403, on cross-tenant access | 403 confirms the row exists | Enumeration is prevented; debugging is harder | `[DOCUMENTED]` — `CategoryServiceImpl.java:104-105,111` |
| 7 | Transaction advice pinned to `HIGHEST_PRECEDENCE` | Advice order was undefined, so filtering was intermittent | Deterministic ordering; a global change made for one aspect's benefit | `[DOCUMENTED]` — `TransactionAdviceOrderConfig` javadoc |
| 8 | `User` implements `UserDetails` | Avoids a separate principal type and a mapping step | Persistence and security models are coupled | `[INFERRED]` |
| 9 | `username` globally unique, not per tenant | Login happens before a tenant is known | Login needs no tenant; two companies cannot both have an `admin` | `[DOCUMENTED]` — `TenantHibernateFilter.java:43-48` |
| 10 | Service interfaces separate from implementations | A seam between controllers and logic | Swappable, mockable; but only one adapter exists per seam, so it is a hypothetical seam, not a real one | `[INFERRED]` |

---

# Design Risks and Improvement Candidates

Suggestions only.

| # | Risk | Why it matters |
|---|---|---|
| 1 | **Nothing guards a forgotten `@Filter`.** Add an entity without it and that table leaks, silently. The missing-transaction case throws, this one does not. | Highest-severity design gap. A startup check that every `AbstractEntity` subclass declares the filter would close it. |
| 2 | **Two exempt-path lists** — `SecurityConfig.PUBLIC_URLS` and `TenantFilter.isTenantExempt` — hand-synced. | They have already drifted once. One shared constant removes the class of bug. |
| 3 | **`TenantFilter` 400s bypass the error contract.** Clients get a different JSON shape from one filter. | Breaks the single-contract promise clients code against. |
| 4 | **Two `UnauthorizedException` classes**; the one that is thrown is not the one that is handled. | Dead code that looks live. Delete one. |
| 5 | **`traceId` is always `null`** — nothing populates the MDC key. | Either set it in a filter or drop the field. |
| 6 | **Entities depend on a ThreadLocal.** `AbstractEntity` cannot be persisted outside a web request. | Blocks batch jobs and makes persistence tests need a fake request. |
| 7 | **`TenantSchemaResolver` runs on every request for an unused result.** | Pure overhead in `filter` mode; gate it on `mode = schema`. |
| 8 | **Delete is a hard delete, despite the comments.** `CategoryServiceImpl.java:94-97` and `ProductServiceImpl.java:92-95` both say "soft-delete", have the soft-delete line commented out, call `repo.delete()`, then log "Soft-deleted". | The `deleted` column, the filter predicate and the audit trail all assume soft deletes. Comments and code say opposite things. |
| 9 | **Tenant id injection happens in two places** — `CategoryMapper` explicitly, `@PrePersist` for everything else. | Pick one. |
| 10 | **Service seams have one adapter each.** Six interfaces exist for six implementations with no second adapter and no tests using them. | By the two-adapters rule these are hypothetical seams. They cost indirection and buy nothing yet — worth keeping only if tests will use them. |
