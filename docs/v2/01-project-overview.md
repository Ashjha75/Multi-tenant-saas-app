# 01 — Project Overview

What this project is today. Every claim cites the file it came from.

## What it is

A multi-tenant SaaS backend for **stock and inventory management**. One running instance serves
many companies. Each company is a *tenant*. A tenant registers, a platform admin approves it,
and the tenant's users then manage their own products, categories and stock movements.

It is a REST API only. There is no UI in this repo. The JWT issuer name is `stock-saas-app`
(`security/JwtTokenService.java:78`), which is the clearest statement of intent in the codebase.

## The problem it solves

One deployment, many companies, no company able to see another's data. Isolation is enforced by
a `tenant_id` column on every business row plus a Hibernate filter that adds
`tenant_id = :tenantId AND deleted = false` to every query
(`entities/AbstractEntity.java:29-33`).

## Actors

Roles come from `entities/UserRole.java` and the `@PreAuthorize` annotations. Five roles are
defined; only three are ever referenced in an authorization rule.

| Role | Used in a rule? | Where |
|---|---|---|
| `ROLE_PLATFORM_ADMIN` | Yes | `TenantController.java:119`, `UserController.java:30,144` |
| `ROLE_COMPANY_ADMIN` | Yes | `UserController.java:42,55,72,86,100,115,130,144` |
| `ROLE_ADMINISTRATOR` | Yes | `UserController.java:55,72,144`, `TenantController.java:119` |
| `ROLE_USER` | No | defined only, `UserRole.java:8` |
| `ROLE_SALES_OPERATOR` | No | defined only, `UserRole.java:9` |

Two practical actors exist in the running system:

- **Platform admin** — the seeded `admin` user, `ROLE_PLATFORM_ADMIN`
  (`db/migration/common/V3__seed_demo_data.sql:56`). Approves tenants.
- **Company admin** — created automatically when a tenant is approved, always
  `ROLE_COMPANY_ADMIN` (`services/impl/TenantServiceImpl.java:184`). There is no code path that
  creates `ROLE_USER` or `ROLE_SALES_OPERATOR`.

## Domain glossary

Defined from the entity and how the code actually uses it, not from the name.

| Term | What it actually is |
|---|---|
| **Tenant** | A customer company. Row in `tenants`. Carries its own admin's credentials as plain columns (`admin_username`, `admin_password`) until approval copies them into a `User`. `entities/Tenant.java` |
| **`tenant_id`** | **Ambiguous — means two different things.** On a `Tenant` row it is the lowercased `company_code` (`AbstractEntity.java:73-77`). On every other row it is the tenant's UUID `tenants.id` (`TenantServiceImpl.java:185`). See the warning below. |
| **User** | A person who logs in. Belongs to exactly one tenant. Implements Spring Security's `UserDetails` directly (`entities/User.java:23`), so the entity is also the security principal. `username` is unique **across all tenants**, not per tenant (`V1__init_Common_tables.sql:85`). |
| **Category** | A grouping label for products. Name only, plus description. Unique by name *within a tenant* (`CategoryRepo.java:19`). `entities/Category.java` |
| **Product** | A stock item. Has `reference` (a SKU), `price`, and `alert_threshold` — a low-stock trigger value that **nothing in the code reads**. Belongs to one Category. `entities/Product.java` |
| **StockMvt** | One stock movement: a quantity going in or out on a date, against one Product. The ledger entry. `entities/StockMvt.java` |
| **TypeMvt** | `IN` or `OUT`. The direction of a StockMvt. `entities/TypeMvt.java` |
| **TenantStatus** | `PENDING`, `ACTIVE`, `SUSPENDED`, `INACTIVE`. `entities/TenantStatus.java` |

> **Warning — `tenant_id` is overloaded.** A `Tenant` row's `tenant_id` holds a company code
> (`acme-corp`); a `User`, `Category`, `Product` or `StockMvt` row's `tenant_id` holds a UUID.
> The Hibernate filter compares `tenant_id` against whatever is in `TenantContext`, which is the
> UUID from the JWT (`security/JwtAuthenticationFilter.java:40-44`). So the filter works for
> business rows and would never match a `Tenant` row. `Tenant` is deliberately left out of the
> filter for that reason. This is an open question, see 05.

**There is no stock-level calculation anywhere.** No code sums `StockMvt` quantities into an
on-hand figure, and nothing compares anything to `alert_threshold`. The system records movements;
it does not yet compute inventory. `[INFERRED]` from the absence of any such method in
`services/impl/`.

## Capabilities

Auth column: "Public" = in `SecurityConfig.PUBLIC_URLS:28-47`. "Yes" = any valid JWT.

| Capability | Endpoint | Auth | Role required | Source |
|---|---|---|---|---|
| Health check | `GET /health`, `GET /api/v1/health` | Public | — | `controllers/HealthController.java:28` |
| Log in | `POST /api/v1/auth/login` | Public | — | `auth/AuthenticationController.java:57` |
| Register a tenant | `POST /api/v1/auth/register` | Public | — | `auth/AuthenticationController.java:81` |
| Log out | `POST /api/v1/auth/logout` | Public | — | `auth/AuthenticationController.java:100` |
| Register a tenant (dup) | `POST /api/v1/tenants` | Yes | **none** | `controllers/TenantController.java:30` |
| Approve a tenant | `POST /api/v1/tenants/{id}/approve` | Yes | **none** | `TenantController.java:41` |
| Activate / deactivate / suspend | `POST /api/v1/tenants/{id}/{action}` | Yes | **none** | `TenantController.java:55,70,85` |
| List all tenants | `GET /api/v1/tenants` | Yes | **none** | `TenantController.java:100` |
| List pending tenants | `GET /api/v1/tenants/pending` | Yes | `PLATFORM_ADMIN` or `ADMINISTRATOR` | `TenantController.java:119` |
| Create user | `POST /api/v1/users` | Yes | `COMPANY_ADMIN` | `UserController.java:42` |
| Update / read one user | `PUT`/`GET /api/v1/users/{id}` | Yes | `COMPANY_ADMIN` or `ADMINISTRATOR` | `UserController.java:55,72` |
| List / delete / enable / disable user | `GET`/`DELETE`/`POST .../{enable,disable}` | Yes | `COMPANY_ADMIN` | `UserController.java:86,100,115,130` |
| List roles | `GET /api/v1/users/roles` | Yes | `PLATFORM_ADMIN`, `COMPANY_ADMIN` or `ADMINISTRATOR` | `UserController.java:144` |
| Category CRUD | `/api/v1/categories` | Yes | **none** | `controllers/CategoryController.java` |
| Product CRUD | `/api/v1/products` | Yes | **none** | `controllers/ProductController.java` |
| Stock movement CRUD | `/api/v1/stock-mvts` | Yes | **none** | `controllers/StockMvtController.java` |

Every non-public endpoint also needs an `X-Tenant-ID` header or it is rejected with 400 before
authentication runs (`config/TenantFilter.java:51-60`).

Three facts from this table that are easy to miss:

1. **The business controllers have no role checks.** `CategoryController`, `ProductController`
   and `StockMvtController` carry no `@PreAuthorize` at class or method level. Any authenticated
   user of any role can create, read, update and delete.
2. **Most of `TenantController` has no role check either.** Only `/pending` is gated. Approving
   or suspending a tenant is open to any authenticated caller.
3. **`UserController`'s class-level rule is dead.** `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`
   on line 30 is overridden by the method-level annotation on every single method. Spring
   applies the most specific one. The effect is that a `PLATFORM_ADMIN` can call almost nothing
   in this controller.

## Tenant lifecycle

| Step | What happens | Where |
|---|---|---|
| 1. Register | Company code and email checked for duplicates, admin password BCrypt-hashed, row saved with status `PENDING`. | `TenantServiceImpl.java:43-67` |
| 2. Approve | Status set to `ACTIVE`, then provisioning runs, then the admin `User` is created. | `TenantServiceImpl.java:70-94` |
| 3. Provision | Creates a Postgres schema `tenant_<company_code>`, runs Flyway's `db/migration/tenant` into it, seeds defaults. | `services/impl/ProvisioningServiceImpl.java` |
| 4. Admin user | A `User` is built from the tenant's `admin_*` columns with role `ROLE_COMPANY_ADMIN` and `tenantId = tenant.getId()`. | `TenantServiceImpl.java:152-194` |
| 5. Failure | Any exception rolls the status back and returns 500. | `TenantServiceImpl.java:83-93` |

**The provisioned schema is never used.** Step 3 creates `tenant_<code>` and migrates it, but the
application runs in `filter` mode and always queries the shared `public` schema. The schemas are
created and then ignored. `[INFERRED]` — nothing reads `TenantContext.getCurrentSchema()` except
`CurrentTenantIdentifierResolverImpl`, which is not instantiated in `filter` mode. Details in 02.

Two state-machine oddities, stated as the code reads:

- `activateTenant` requires the current status to be `PENDING` (`TenantServiceImpl.java:102`), but
  `approveTenant` has already set it to `ACTIVE`. So activate always fails after approve.
- `suspendTenant` throws if the tenant **is** `ACTIVE` (`TenantServiceImpl.java:128`), meaning
  only a non-active tenant can be suspended.

Both are listed in 05 rather than called bugs here, because the intended rule is not recorded.

## Auth model

| Stage | Behaviour | Where |
|---|---|---|
| Registration | Public. Creates a `PENDING` tenant. No user and no login yet. | `AuthenticationController.java:81` |
| Login | `username` + `password`. Looked up across all tenants, since `username` is globally unique. | `auth/service/AuthenticationServiceImpl.java:26-32` |
| Token issue | RS256 JWT. Claims: `sub` = user id, `tenant_id`, `role`, `iss` = `stock-saas-app`. Default life 24h (`JWT_ACCESS_TOKEN_EXPIRATION`, `application.yml:66`). | `JwtTokenService.java:62-81` |
| Token verify | Public key verifies the signature on every request. Tenant and role are read from claims. | `JwtAuthenticationFilter.java:37-52` |
| Logout | Returns 200 and does nothing. Tokens are stateless and not revocable. | `AuthenticationController.java:100` |

**The RSA keys** are an asymmetric pair: the private key signs tokens, the public key verifies
them. Paths come from `app.jwt.*` and default to `file:./certs/private_key.pem` and
`file:./certs/public_key.pem` (`application.yml:64-65`), resolved through Spring's
`ResourceLoader` (`JwtTokenService.java:163-180`). Using a keypair rather than a shared secret
means a verifier never needs the signing key `[INFERRED]`.

> Note: `src/main/resources/certs/*.pem` also exists on disk but is **not** what the app loads.
> The runtime path is the project-root `certs/`, which is gitignored and generated by the
> `keygen` service in `docker-compose.yml`.

## Other behaviour visible in the code

| Concern | What exists | Where |
|---|---|---|
| Validation | Bean Validation on every request DTO — `@NotBlank`, `@Size`, `@Email`, `@Positive`. Failures become 400 with a per-field `details` map. | `dto/request/*.java`, `GlobalExceptionHandler.java:49-57` |
| Error contract | One JSON shape for every error: `timestamp`, `status`, `error`, `message`, `path`, `traceId`, `details`. | `exception/ApiError.java` |
| Caching | One in-memory cache, `tenantSchemas`, holding tenant-id → schema-name. Never evicted. | `config/CacheConfig.java:13`, `config/TenantSchemaResolver.java:19-22` |
| Health | Opens a real DB connection and reports the product name and URL. Not Spring Actuator — Actuator is not a dependency. | `HealthController.java:63-74` |
| OpenAPI | Swagger UI enabled; spec at `/api-docs`. Declares both `X-Tenant-ID` and bearer auth. | `config/OpenApiConfig.java`, `application.yml:78-79` |
| Logging | Tenant id is put in the SLF4J MDC for the request, so log lines carry it. | `TenantFilter.java:76` |
| Transactions | `open-in-view: false`, so lazy loading outside a service method fails fast. | `application.yml:35` |

---

# Gaps and Improvement Candidates

Suggestions only. Nothing above this line is a recommendation.

| # | Gap | Why it matters |
|---|---|---|
| 1 | **Business controllers have no authorization.** Any authenticated user can delete any product in their tenant. | Role data exists and is unused. This is the largest gap in the project. |
| 2 | **`TenantController` lets any authenticated user approve or suspend tenants.** | A company admin could approve their own tenant, or suspend a competitor's. |
| 3 | **`UserController`'s class-level rule is shadowed by every method rule.** | A platform admin cannot administer users, which is probably the opposite of the intent. |
| 4 | **One test file, `contextLoads` only.** Tenant isolation, the filter chain and the lifecycle state machine have no tests. The isolation rules are subtle and fail *silently* — exactly the case tests are for. | Highest-value place to add tests. |
| 5 | **No stock calculation.** `alert_threshold` is stored and never read; no on-hand quantity is derived. | The core inventory feature appears unfinished. |
| 6 | **Schema provisioning runs but produces nothing used.** Every approval creates a dead Postgres schema. | Wasted work and a misleading signal to a future reader. |
| 7 | **`ROLE_USER` and `ROLE_SALES_OPERATOR` are unreachable.** No code path assigns them. | Either wire them up or remove them. |
| 8 | **Logout is a no-op and tokens live 24h.** No revocation list. | A leaked token is valid for a day. |
| 9 | **`tenant_id` means two different things.** See the glossary warning. | Guarantees confusion for the next reader; blocks a clean schema-mode port. |
| 10 | **CORS allows every origin with credentials enabled** (`SecurityConfig.java:94-103`). | The comment already says to restrict it in production. |
