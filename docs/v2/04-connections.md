# 04 — Connections

How the pieces join up. Read 03 first — table and column names are assumed here.

## Code to database

One row per domain object, readable left to right.

| Entity | Table | Repository | Service implementation | Controller | Base path |
|---|---|---|---|---|---|
| `Tenant` | `tenants` | `TenantRepo` | `TenantServiceImpl` | `TenantController` + `AuthenticationController` | `/api/v1/tenants`, `/api/v1/auth/register` |
| `User` | `users` | `UserRepo` | `UserServiceImpl` | `UserController` | `/api/v1/users` |
| `Category` | `categories` | `CategoryRepo` | `CategoryServiceImpl` | `CategoryController` | `/api/v1/categories` |
| `Product` | `products` | `ProductRepo` | `ProductServiceImpl` | `ProductController` | `/api/v1/products` |
| `StockMvt` | `stock_mvts` | `StockMvtRepo` | `StockMvtServiceImpl` | `StockMvtController` | `/api/v1/stock-mvts` |
| — | — | — | `ProvisioningServiceImpl` | — | called by `TenantServiceImpl` only |
| — | — | `UserRepo` | `AuthenticationServiceImpl` | `AuthenticationController` | `/api/v1/auth/login` |

Each row also has a `mapper/` class and a `dto/request` + `dto/response` pair. `UserServiceImpl`
is the only implementation that also acts as Spring Security's `UserDetailsService`
(`UserServiceImpl.java:165-168`).

Two entities have no controller of their own: `Tenant` is reached through two different
controllers, and `ProvisioningServiceImpl` is internal.

## Request to data

### A read: `GET /api/v1/categories?page=0&size=10`

| Step | Class | Produces |
|---|---|---|
| 1 | `TenantFilter:75` | header value → `TenantContext` |
| 2 | `JwtAuthenticationFilter:44` | **tenant from JWT claim overwrites it** ← the tenant-scoping step |
| 3 | `CategoryController:87` | calls `categoryService.findAll(0, 10)` |
| 4 | transaction advice | transaction opens (outermost advice) |
| 5 | `TenantHibernateFilter:93-94` | `tenantFilter` enabled on that transaction's session |
| 6 | `CategoryServiceImpl:80` | `categoryRepo.findAll(pageRequest)` |
| 7 | Hibernate + `Category:24` | `SELECT ... FROM categories WHERE tenant_id = ? AND deleted = false OFFSET ? LIMIT ?` |
| 8 | `CategoryMapper:22`, `PageResponse:24` | `PageResponse<CategoryResponse>` |

### A write: `POST /api/v1/categories`

| Step | Class | Produces |
|---|---|---|
| 1-2 | as above | tenant in `TenantContext` |
| 3 | `CategoryController:38` | `@Valid` runs; failures become 400 before the service is reached |
| 4-5 | as above | transaction open, filter enabled |
| 6 | `CategoryServiceImpl:38` → `:120-127` | duplicate-name check, tenant-scoped via explicit JPQL (`CategoryRepo:19`) |
| 7 | `CategoryMapper:18` | `tenantId` set from `TenantContext` ← the tenant-scoping step on write |
| 8 | `AbstractEntity:66-92` | `@PrePersist` fills `deleted`, `created_by`, and `tenant_id` if still unset |
| 9 | `CategoryServiceImpl:40` | `INSERT INTO categories (...)` |
| 10 | transaction commits | |

**Write scoping is belt-and-braces.** `CategoryMapper` sets `tenant_id` explicitly;
`@PrePersist` would have set it anyway. `ProductMapper` and `StockMvtMapper` set nothing and rely
on `@PrePersist` alone. Same outcome, two conventions.

## Configuration to behaviour

| Property | Env override | Read by | Changes |
|---|---|---|---|
| `spring.datasource.url` | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_TZ` | Spring Boot → HikariCP | Which database, and the session time zone |
| `spring.datasource.username` / `password` | `DB_USER`, `DB_PASSWORD` | HikariCP | Credentials |
| `spring.datasource.hikari.maximum-pool-size: 10` | — | HikariCP | Concurrency ceiling. Provisioning needs 2 connections at once |
| `spring.jpa.hibernate.ddl-auto: none` | — | Hibernate | Hibernate never touches the schema; Flyway owns it |
| `spring.jpa.show-sql: true` | — | Hibernate | Every statement logged — **on by default, including in the container** |
| `spring.jpa.open-in-view: false` | — | Spring Boot | No session outside a service method; lazy access after return fails |
| `spring.flyway.locations` | — | Flyway | `db/migration/common` only. `tenant/` is *not* in this list |
| `spring.flyway.schemas: public` | — | Flyway | Startup migrations target `public` |
| `server.port` | `SERVER_PORT` | Tomcat, `OpenApiConfig:23` | Listen port, and the server URL in the OpenAPI spec |
| **`app.tenant.mode`** | **`TENANT_MODE`** | `@ConditionalOnProperty` in 4 classes | **Which multi-tenancy classes exist at all.** See table below |
| `app.jwt.private-key-path` | `JWT_PRIVATE_KEY_PATH` | `JwtProperties` → `JwtTokenService:46` | Signing key. `file:` vs `classpath:` prefix decides the lookup |
| `app.jwt.public-key-path` | `JWT_PUBLIC_KEY_PATH` | `JwtProperties` → `JwtTokenService:48` | Verification key |
| `app.jwt.access-token-expiration` | `JWT_ACCESS_TOKEN_EXPIRATION` | `JwtTokenService:70` | Token lifetime, default 86400000 ms = 24h |
| `springdoc.api-docs.path: /api-docs` | — | springdoc | Spec URL. **Must match `SecurityConfig.PUBLIC_URLS`** or the spec 401s |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | Spring | Default `dev`; `application-dev.yaml` is empty |

### What `app.tenant.mode` switches

| Value | Beans created | Beans skipped | Isolation |
|---|---|---|---|
| `filter` (default) | `TenantFilter`, `TenantHibernateFilter` | `MultiTenantConnectionProviderImpl`, `CurrentTenantIdentifierResolverImpl` | `tenant_id` predicate on every query |
| `schema` | the two Hibernate classes | the two filter classes | **none — see 02** |
| absent | same as `filter` (`matchIfMissing = true`) | — | as `filter` |

This is the single most consequential property in the project. It is not a runtime switch: the
beans are chosen at startup.

## Runtime wiring

### Beans and where they come from

| Bean | Defined in | Notes |
|---|---|---|
| `PasswordEncoder` (BCrypt) | `config/BeansConfig.java:14` | Used by `TenantServiceImpl` and `UserServiceImpl` |
| `AuthenticationManager` | `config/BeansConfig.java:19` | From `AuthenticationConfiguration`; wraps `UserServiceImpl` as `UserDetailsService` |
| `CacheManager` | `config/CacheConfig.java:12` | `ConcurrentMapCacheManager("tenantSchemas")` — in-memory, single node, no eviction |
| `AuditorAware<String>` | `config/JpaAuditingConfig.java:18` | Fills `created_by` / `updated_by` |
| `OpenAPI` | `config/OpenApiConfig.java:27` | Declares `X-Tenant-ID` and bearer schemes |
| `SecurityFilterChain` | `security/SecurityConfig.java:50` | Route rules, session policy, exception handlers |
| `CorsConfigurationSource` | `security/SecurityConfig.java:90` | All origins, all methods, credentials allowed |
| `JwtProperties` | `security/JwtProperties.java:10` | `@ConfigurationProperties("app.jwt")` |
| `TenantFilter` | `config/TenantFilter.java:21` | `@Component` + `@Order(HIGHEST_PRECEDENCE)`, conditional |
| `TenantHibernateFilter` | `config/TenantHibernateFilter.java:26` | `@Aspect` + `@Order(LOWEST_PRECEDENCE)`, conditional |
| transaction advisor order | `config/TransactionAdviceOrderConfig.java` | `@EnableTransactionManagement(order = HIGHEST_PRECEDENCE)` |

### Ordering constraints that actually matter

| # | Constraint | Enforced by | If broken |
|---|---|---|---|
| 1 | `TenantFilter` before the Spring Security chain | `@Order(HIGHEST_PRECEDENCE)` | Header check would run after authentication; 401 instead of 400 |
| 2 | `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter` | `addFilterBefore` (`SecurityConfig:79`) | Token never read, everything 401 |
| 3 | `JwtAuthenticationFilter` overwrites `TenantContext` **after** `TenantFilter` sets it | filter nesting | Header would win over the token — spoofable |
| 4 | Transaction advice **outside** `TenantHibernateFilter` | `TransactionAdviceOrderConfig` | Filter enabled on a discarded session → silent cross-tenant reads |
| 5 | `TenantHibernateFilter` before the service method body | `@Before` advice | Query runs unfiltered |
| 6 | Flyway before Hibernate validates / app serves traffic | Spring Boot auto-config | Missing tables at runtime |
| 7 | JWT keys loaded before first request | `@PostConstruct` (`JwtTokenService:38`) | Startup fails fast — intended |
| 8 | `keygen` container completes before `app` starts | `depends_on: service_completed_successfully` | App exits: "Unable to initialize JWT keys" |
| 9 | `postgres` healthy before `app` starts | `depends_on: service_healthy` | Flyway fails on connect |

Constraints 3 and 4 are the two that are invisible in normal reading and silently produce wrong
data rather than an error.

**Diagram Brief: End-to-end system map**
Boxes:
  - Client — Postman, browser, or service
  - saas-app container — Spring Boot, port 8080
  - TenantFilter — X-Tenant-ID gate
  - Security chain — JWT verify and authorize
  - Controllers — 6 REST controllers
  - Services — business logic, transactional
  - TenantHibernateFilter — adds tenant predicate
  - Repositories — Spring Data JPA
  - Hibernate — ORM, filter applied here
  - HikariCP — pool, max 10
  - saas-db container — PostgreSQL 17.5, port 5432
  - public schema — tenants, users, categories, products, stock_mvts
  - tenant_<code> schemas — created on approval, never queried
  - saas-keygen container — one-shot RSA keypair generator
  - certs volume — private_key.pem, public_key.pem mounted read-only
  - Flyway startup — runs db/migration/common into public
  - Flyway provisioning — runs db/migration/tenant into a tenant schema
  - Swagger UI — /swagger-ui, spec at /api-docs
Arrows:
  - Client -> saas-app container : HTTPS/HTTP with Bearer + X-Tenant-ID
  - saas-app container -> TenantFilter : request enters
  - TenantFilter -> Security chain : header ok
  - Security chain -> Controllers : authenticated and authorized
  - Controllers -> Services : interface call
  - Services -> TenantHibernateFilter : proxied, inside transaction
  - TenantHibernateFilter -> Repositories : filter enabled
  - Repositories -> Hibernate : query
  - Hibernate -> HikariCP : connection
  - HikariCP -> saas-db container : SQL
  - saas-db container -> public schema : all reads and writes
  - saas-db container -> tenant_<code> schemas : created, then unused
  - saas-keygen container -> certs volume : writes keypair once
  - certs volume -> saas-app container : mounted read-only at /app/certs
  - Flyway startup -> public schema : V1 to V5 at boot
  - Services -> Flyway provisioning : on tenant approval
  - Flyway provisioning -> tenant_<code> schemas : tenant V1
  - Client -> Swagger UI : browse API
Grouping: TenantFilter, Security chain, Controllers, Services, TenantHibernateFilter, Repositories, Hibernate, HikariCP inside "saas-app container"; public schema and tenant_<code> schemas inside "saas-db container"
Layout hint: left-to-right, containers as boundaries, keygen and certs below

## External touchpoints

### Docker Compose services

| Service | Container | Image | Role |
|---|---|---|---|
| `keygen` | `saas-keygen` | `saas-app:local` | One-shot. Generates `certs/*.pem` on the host if absent, then exits. Runs as root to write the mount |
| `postgres` | `saas-db` | `postgres:17.5` | Database. Healthcheck `pg_isready`. Data in the `postgres_data` volume |
| `app` | `saas-app` | `saas-app:local` | The application. Healthcheck hits `/api/v1/health` |

`app` waits for `postgres: service_healthy` **and** `keygen: service_completed_successfully`.
Keys live on the host in `certs/` (gitignored) and are mounted read-only at `/app/certs`, so they
survive rebuilds and every container shares one pair — tokens stay valid across restarts.

Container-only overrides in `docker-compose.yml`: `DB_HOST=postgres`, `SERVER_PORT=8080`,
`JWT_*_PATH=file:/app/certs/*.pem`, `SPRING_DOCKER_COMPOSE_ENABLED=false`.

### PostgreSQL

Reached over JDBC with the session time zone pinned by `DB_TZ` (default `Asia/Kolkata`), set both
on the JDBC URL and as `TZ`/`PGTZ` on the database container so Java and Postgres agree.

### OpenAPI surface

| Path | Serves |
|---|---|
| `/api-docs` | JSON spec |
| `/api-docs.yaml` | YAML spec |
| `/api-docs/swagger-config` | Swagger UI bootstrap |
| `/swagger-ui/index.html` | Swagger UI |

All public. `/v3/api-docs` is **not** a valid path here — `springdoc.api-docs.path` is
`/api-docs` — and now returns 404.

### Health endpoint

`GET /health` and `GET /api/v1/health` return the same body: status, timestamp, uptime, a live
database probe, and a build version that reads `"dev"` unless `BuildProperties` is on the
classpath (`HealthController.java:57`). It is not Spring Actuator — Actuator is not a dependency, so
`/actuator/health` does not exist.

---

# Integration Risks and Improvement Candidates

Suggestions only.

| # | Risk | Why it matters |
|---|---|---|
| 1 | **`show-sql: true` in all profiles.** | Every statement, with parameters' shape, is logged in production. Noise and a mild disclosure risk. |
| 2 | **The cache is per-JVM.** `ConcurrentMapCacheManager` with no eviction. | Run two instances and they hold independent, unbounded, never-refreshed tenant→schema maps. |
| 3 | **Hikari pool is 10 and provisioning holds two connections.** | Concurrent approvals plus normal traffic could starve the pool. |
| 4 | **`springdoc.api-docs.path` and `SecurityConfig.PUBLIC_URLS` must agree by hand.** | They already drifted once — the spec returned 401. |
| 5 | **No readiness/liveness split.** One health endpoint that opens a DB connection serves both. | A slow database makes the container look dead and get killed. |
| 6 | **CORS allows every origin with credentials.** | Fine locally; the code comment already flags it for production. |
| 7 | **`application-dev.yaml` is empty and `dev` is the default profile.** | No prod profile exists, so production would run dev defaults. |
| 8 | **Keys are host files mounted into the container.** | Works for compose and a VPS. For ECS/Fargate the entrypoint's `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY` env path is the one to use. |
