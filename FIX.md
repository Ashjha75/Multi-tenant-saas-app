# FIX.md — Make the project compile and run anywhere

**Date:** 2026-10-10  
**Status:** every item below is applied and verified against a from-scratch `docker compose up`.
**Goal:** `git clone` → `docker compose up` works on any machine with Docker, and the same image runs
unchanged on a remote host / AWS ECS Fargate. No manual key copying, no "works on my laptop" steps.

Status legend: `[ ]` planned · `[x]` done & verified

---

## 1. BLOCKER — the project does not compile (407 errors, one cause)

**Symptom:** `./mvnw compile` fails with 407 `cannot find symbol` errors — every Lombok-generated
member in the project (`builder()`, `getXxx()`, `log`) is missing.

**Root cause:** `src/main/java/.../mapper/CategoryMapper.java:9`

```java
import static com.ashish.saas.multitanantsaasapp.dto.response.CategoryResponse.builder;
```

A **static import of a Lombok-generated method**. `javac` resolves static imports *before* annotation
processing produces that method. The import cannot be resolved, Lombok's processing round is aborted,
and **no** Lombok code is generated for **any** of the 77 source files. One bad import → 407 errors.

Ruled out by experiment (so we don't "fix" the wrong thing): Lombok version (1.18.30 and 1.18.46 behave
identically), Spring Boot 4.0.6 parent, maven-compiler-plugin 3.14.1, in-process vs forked javac,
spaces in the project path, JDK 21.0.12. A minimal project with the same pom compiles fine; delta-debugging
the 77 files reduced the failure to exactly `CategoryResponse.java` + `CategoryMapper.java`.

- [x] **Fix:** drop the static import, call `CategoryResponse.builder()` explicitly.
- [x] **Verify:** `./mvnw clean package` → BUILD SUCCESS (already confirmed on a scratch copy).

---

## 2. BLOCKER on any other machine — JWT keys are not in the repository

**Symptom (fresh clone):** app starts, then dies with
`IllegalStateException: Unable to initialize JWT keys` → `JWT key not found: classpath:certs/private_key.pem`.

**Root cause:** `application.yml` defaults the key paths to `classpath:certs/*.pem`, which resolve to
`src/main/resources/certs/*.pem`. Those files exist **only on this laptop** — `.gitignore` excludes
`src/main/resources/certs/*.pem` (correctly: private keys must not be committed). `git ls-files`
confirms no PEM is tracked. The repo's `certs/` directory is empty and, being empty, is not in git either.

Two further problems with the current setup:

- `.env.example` suggests `JWT_PRIVATE_KEY_PATH=certs/private_key.pem` — a **bare path**. The code resolves
  it through Spring's `ResourceLoader`, where a prefix-less path is *not* a filesystem path, so that value
  would not resolve inside the container. It needs an explicit `file:` prefix.
- `COPY src/ src/` in the Dockerfile bakes `src/main/resources/certs/*.pem` into the image whenever they
  exist locally — a private key shipped inside the image.

**Fix (three layers, so every environment works):**

- [x] `application.yml` default becomes `file:./certs/private_key.pem` — the same relative path works on the
      host (project root) and in the container (`WORKDIR /app`, keys mounted at `/app/certs`).
- [x] `docker-compose.yml` gains a one-shot `keygen` service that generates `certs/*.pem` on the host if they
      are missing, before the app starts. First `docker compose up` on a new machine just works.
- [x] The container entrypoint materialises `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` **environment variables**
      (raw PEM or base64) into `/app/certs/*.pem` when they are set. This is the **Fargate / Secrets Manager**
      path: inject the keys as secrets, no volume needed. All tasks then share one key pair, so tokens stay
      valid across tasks and restarts.
- [x] `.dockerignore` excludes `certs/` and `src/main/resources/certs/` so no key is ever baked into an image.
- [x] **Verify:** container boots with `JWT keys loaded successfully`; `POST /api/v1/auth/login` returns a token.

---

## 3. Port variable is named two different things

`application.yml` reads `${PORT:8080}`, while `.env.example` and `docker-compose.yml` use `SERVER_PORT`.
It currently works only by accident (compose falls back to `8080` and `.env` happens to set `PORT=8080`).
Change the host port in compose and nothing happens — a confusing failure later.

- [x] **Fix:** standardise on `SERVER_PORT` in `application.yml`, `.env`, `.env.example` and `docker-compose.yml`.

---

## 4. `docker-compose.yml` cleanups

- [x] `version: '3.8'` is obsolete and warns on every command with Compose v5 → remove it.
- [x] The `app` service has **no healthcheck**, so nothing (compose, a load balancer, a deploy script) can wait
      for readiness → add one hitting `/api/v1/health`.
- [x] `certs/` is mounted read-only but empty → fixed by the `keygen` service in item 2.
- [x] Add `SPRING_DOCKER_COMPOSE_ENABLED=false` for the containerised app (see item 5).
- [x] Set `TZ` on the app container so Java and Postgres agree on the timezone.
- [x] Name the built image (`saas-app:local`) so compose builds it once and reuses it for `keygen`.

## 5. `Dockerfile` hardening

- [x] `ENTRYPOINT ["sh", "-c", "java ... -jar app.jar"]` makes the JVM a child of `sh`, so it never receives
      SIGTERM → containers are SIGKILLed after the stop timeout. On ECS this means slow, unclean task stops.
      **Fix:** entrypoint script ending in `exec java ...` (PID 1 = JVM).
- [x] Add `openssl` to the runtime image (~1 MB) so the entrypoint can handle keys.
- [x] Add a `HEALTHCHECK` so plain `docker run` reports health too.
- [x] `spring-boot-docker-compose` is a runtime dependency: outside a dev workspace it is inert, but we disable
      it explicitly in the container to remove any doubt.

## 6. `pom.xml` — remove hardcoded Lombok version

The working tree pins `lombok.version` to `1.18.30` (added while debugging the build). That overrides the
version Spring Boot 4.0.6 manages (`1.18.46`) and will break on a newer JDK on a build machine.

- [x] **Fix:** delete the `lombok.version` property and the explicit `<version>` on the dependency; let the
      Spring Boot parent manage it, and set `annotationProcessorPathsUseDepMgmt` so the annotation-processor
      path picks up the same managed version.

## 7. `.env` / `.env.example` drift

`.env` is missing every `JWT_*` variable and uses `PORT`; `.env.example` has `JWT_*` with unresolvable bare
paths and `SERVER_PORT`. Anyone copying `.env.example` to `.env` on a new machine gets a broken app.

- [x] **Fix:** make both files list the same variables, with container-correct values and comments explaining
      which ones change for a host run vs compose vs Fargate.

---

## Observations — noted, not changed here

These are real but do not block compiling or running; they need a product decision, so they stay as-is for now.

1. **`"tenant id"` column with a space.** `V1__init_Common_tables.sql` creates `users."tenant id"` (and an FK on
   it), while the `User` entity maps its `@ManyToOne` to `tenant_id` (read-only, `insertable=false updatable=false`)
   — the column AbstractEntity already owns. The seeded demo row fills both. It works, but there are two tenant
   columns on `users` with overlapping meaning. Worth a migration to consolidate later.
2. **`TENANT_MODE=schema`** is the default in `.env`, while `docs/tenant-toggle.md` says schema mode is not
   implemented yet (`MultiTenantConnectionProviderImpl` and `CurrentTenantIdentifierResolverImpl` exist but the
   doc predates them). Behaviour will be confirmed when the app boots; `filter` mode is the documented working path.
3. **Private key in `src/main/resources/certs/`** on this laptop stays where it is — gitignored and now excluded
   from images. Nothing reads it after item 2.

---

## Verification checklist (run after all fixes)

- [x] `./mvnw clean package` → BUILD SUCCESS
- [x] `docker compose build` → image `saas-app:local`
- [x] `docker compose up -d` → `saas-db` healthy, `saas-app` healthy
- [x] Flyway applies `V1`, `V2`, `V3` on an empty database
- [x] `GET /api/v1/health` → `{"status":"UP","database":{"status":"UP",...}}`
- [x] `POST /api/v1/auth/login` with the seeded `admin` user → JWT returned
- [x] One authenticated call from the Postman collection with `X-Tenant-ID`
- [x] `docker compose down -v && docker compose up -d` from scratch → still healthy (proves no laptop state)

---

# Part 2 — runtime fixes (found by actually calling the API)

Part 1 got the project to compile and the containers to boot. Running the Postman collection
against it then surfaced six further defects, all of which made the running app unusable or unsafe.

## 8. The seeded admin password matched nothing — login was impossible

`V3__seed_demo_data.sql` seeds the hash `$2a$10$phJ7Vjqf...` for both `tenants.admin_password` and
`users.password`. That hash does not verify against `SecurePassword123` (the value the Postman
collection documents as `PLATFORM_PASSWORD`), nor any other obvious candidate. On a fresh clone
nobody could obtain a token, so every authenticated endpoint was unreachable.

- [x] **Fix:** repair migration `V4__fix_demo_admin_password.sql` setting a hash that verifies
      against `SecurePassword123`. Done as a new migration rather than an edit to `V3`, so existing
      databases do not fail Flyway checksum validation.

## 9. Every authentication failure returned 500, and unexpected errors were logged nowhere

`GlobalExceptionHandler` had no `@ExceptionHandler` for Spring Security's `AuthenticationException`,
so `BadCredentialsException` fell through to the catch-all `Exception` handler and surfaced as
`500 Unexpected error`. That same catch-all discarded the exception without logging it, so the
container logs contained no trace of any 500 — the original login failure was invisible.

- [x] **Fix:** added an `AuthenticationException` handler returning `401` with a specific message
      (bad credentials / disabled / locked), and made the catch-all `log.error(..., ex)` before
      returning. Every fix below was found because of that one log line.

## 10. `TENANT_MODE=schema` disabled the only implemented isolation mode

`docs/tenant-toggle.md` states plainly that the toggle *"does not implement schema mode yet"* and
that the default is `filter`. But `application.yml`, `.env` and `.env.example` all set `schema`.
Both `TenantFilter` and `TenantHibernateFilter` are `@ConditionalOnProperty(havingValue = "filter")`,
so selecting `schema` switched them **off** and switched nothing on: no `X-Tenant-ID` enforcement,
no tenant predicate, and no schema routing either (nothing creates tenant schemas or runs the
tenant migration).

- [x] **Fix:** default is `filter` in `application.yml`, `.env` and `.env.example`, with a comment
      at each site explaining why `schema` is not selectable yet.
- [x] `MultiTenantConnectionProviderImpl` and `CurrentTenantIdentifierResolverImpl` are now
      `@ConditionalOnProperty(havingValue = "schema")`. They were plain `@Component`s, so they
      registered a Hibernate `MultiTenantConnectionProvider` even in filter mode.
- [x] `CurrentTenantIdentifierResolverImpl.customize()` registered itself under
      `MULTI_TENANT_CONNECTION_PROVIDER` instead of `MULTI_TENANT_IDENTIFIER_RESOLVER` — a
      copy-paste from the sibling class. It fought the real provider for the same key.

## 11. The business tables were never created

`categories`, `products` and `stock_mvts` exist only in `db/migration/tenant/V1__init_For_tenat_DB.sql`.
`spring.flyway.locations` points only at `db/migration/common`, and nothing else runs the `tenant`
location, so those tables were never created in any database. Every `/categories`, `/products` and
`/stock-mvts` call failed with `relation "categories" does not exist`.

- [x] **Fix:** `V5__init_tenant_business_tables.sql` creates them in the shared schema, which is
      where filter mode expects them, plus a `tenant_id` index on each (every query filters on it).
      The `tenant/` location is kept as the template for a future schema-per-tenant mode.

## 12. Tenant isolation was not actually in effect — cross-tenant data was returned

This is the serious one. Two independent defects meant the Hibernate `tenantFilter` never reached
a single query, so **any authenticated user saw every tenant's rows**:

1. `@Filter(name = "tenantFilter")` was declared on `AbstractEntity`, a `@MappedSuperclass`.
   Hibernate does not inherit `@Filter` from a mapped superclass — it must be on each concrete
   `@Entity`. `session.enableFilter(...)` therefore matched nothing.
2. Only `CategoryServiceImpl` carried `@Transactional`. `TenantHibernateFilter` enables the filter
   on `entityManager.unwrap(Session.class)`; with no active transaction that is a throwaway session
   discarded immediately, so `Product`, `StockMvt`, `User` and `Tenant` queries ran bare.
   Additionally the aspect and the transaction advisor both sat at `LOWEST_PRECEDENCE`, leaving
   their relative order undefined — the aspect ran outside the transaction even for `Category`.

- [x] **Fix:** `@Filter(name = "tenantFilter")` moved onto `Category`, `Product`, `StockMvt`, `User`.
      `Tenant` is deliberately excluded: it is a platform-level resource listed by platform admins.
- [x] **Fix:** `@Transactional` added to `ProductServiceImpl`, `StockMvtServiceImpl`,
      `UserServiceImpl`, `TenantServiceImpl`.
- [x] **Fix:** `TransactionAdviceOrderConfig` pins the transaction advisor to `HIGHEST_PRECEDENCE`
      so it is the outer advice and the tenant aspect always runs inside the transaction.
- [x] **Guard:** the aspect now throws if `TransactionSynchronizationManager.isActualTransactionActive()`
      is false. A future service added without `@Transactional` fails loudly instead of leaking.

Verified with two real tenants: `acme-corp` sees 1 category / 1 product / 1 stock movement,
`orbit-commerce` sees 1 category / 0 / 0, and tenant 2 presenting tenant 1's `X-Tenant-ID` header
still sees only its own rows — `JwtAuthenticationFilter` overrides the header with the tenant claim
from the signed token, so the header cannot be used to cross tenants.

## 13. The tenant header deadlocked login

With `filter` mode correctly on, `TenantFilter` demanded `X-Tenant-ID` on every path except health
and swagger — including `POST /api/v1/auth/login`, which is where you *get* the token that tells
you your tenant, and `POST /api/v1/auth/register`, which is what creates a tenant. Separately,
`TenantHibernateFilter`'s hard-fail fired on `UserServiceImpl.loadUserByUsername`, called by Spring
Security during login when no tenant can possibly be in context.

- [x] **Fix:** `TenantFilter` exempts the paths in `SecurityConfig.PUBLIC_URLS` (auth, health,
      swagger, api-docs, webjars).
- [x] **Fix:** `TenantHibernateFilter` exempts `loadUserByUsername` and `registerTenant`, with the
      reasoning recorded in the code. `users.username` carries a global UNIQUE constraint, so the
      unfiltered lookup resolves to at most one row and the tenant is taken from that row.
- [x] **Hardening:** authentication now uses `findByUsernameAndDeletedFalse`. With the filter off
      for that one call the `deleted = false` half of the filter condition was also absent, so a
      soft-deleted user could still log in.

## 14. Tenant approval self-deadlocked

Making `TenantServiceImpl` `@Transactional` (item 12) exposed a latent bug in
`ProvisioningServiceImpl.provisionTenant`: it issues `CREATE SCHEMA` on the JdbcTemplate
connection and then hands a *second* connection to Flyway. Inside the caller's transaction the
first connection held the uncommitted DDL and Flyway's connection blocked on it forever. The
request hung until the client timed out, and `pg_stat_activity` showed one connection
`idle in transaction` on `CREATE SCHEMA` with two more waiting on `Lock / transactionid`.

- [x] **Fix:** `provisionTenant` is `@Transactional(propagation = NOT_SUPPORTED)`, suspending the
      caller's transaction so each statement auto-commits — the behaviour it had before item 12.
- [x] It is also exempt from the tenant aspect: it runs raw DDL and Flyway on their own
      connections, touches no Hibernate session, and by design has no transaction for the
      aspect's new "must be in a transaction" guard to find.

Note that in `filter` mode these per-tenant schemas are created but never read: the app queries the
shared schema. The provisioning step is schema-mode groundwork that currently runs on every tenant
approval. Harmless, but worth revisiting when schema mode is finished.

## Notes

- `GET /api/v1/users` returns `403` for the seeded `admin`. That is correct, not a bug:
  `UserController` requires `COMPANY_ADMIN`/`ADMINISTRATOR` for the list operation and the demo
  admin is `PLATFORM_ADMIN`.
- The Postman collection's `TENANT_ID` variable was the placeholder `public`, which matches no
  seeded row. It is now the demo tenant's id.
- Observation 1 from Part 1 (the `users."tenant id"` column with a space) is still open and still
  needs a product decision; it does not block anything.

## Part 2 verification (all run against a from-scratch `docker compose up`)

- [x] `./mvnw clean package` → BUILD SUCCESS
- [x] `docker compose down -v` then `up -d --build` with no `certs/` and no volume → keygen
      generates the key pair, db healthy, app healthy
- [x] Flyway applies V1–V5 on the empty database
- [x] `GET /api/v1/health` → `{"status":"UP","database":{"status":"UP",...}}`
- [x] `POST /api/v1/auth/login` (`admin` / `SecurePassword123`) → JWT
- [x] Wrong password → `401 Invalid username or password` (was `500`)
- [x] Missing `X-Tenant-ID` on a tenant route → `400` with the explanatory message
- [x] No token → `401`
- [x] Category, Product and StockMvt create + list, authenticated, with `X-Tenant-ID`
- [x] `POST /api/v1/auth/register` → tenant `PENDING`; approve as platform admin → `ACTIVE` with
      its admin user created
- [x] Second tenant logs in and sees only its own rows; header spoofing changes nothing
