# Tenant Toggle (Filter vs Schema)

This project supports two tenant isolation modes behind one flag. Only **`filter`** is implemented.
`schema` is a placeholder for future work — selecting it leaves the app with **no isolation at all**.

## The flag

```
TENANT_MODE=filter   # default, and the only working value
```

`app.tenant.mode` in `application.yml`, overridable by the `TENANT_MODE` environment variable
(`.env` for compose).

## What each mode does

### `filter` (implemented)

All tenants share one schema and are separated by the `tenant_id` column.

- `TenantFilter` (servlet) requires an `X-Tenant-ID` header and puts it in `TenantContext`.
  It skips the public paths in `SecurityConfig.PUBLIC_URLS` — login and registration happen
  before a tenant is known, so requiring the header there would be a deadlock.
- `JwtAuthenticationFilter` then **overwrites** the context with the `tenant_id` claim from the
  signed JWT. The token is authoritative: a caller cannot reach another tenant by changing the
  header. The header is still required, and is what tenant-less public calls are checked against.
- `TenantHibernateFilter` (AOP, `@Before` every `services.impl` method) enables the Hibernate
  `tenantFilter`, which appends `tenant_id = :tenantId AND deleted = false` to every query.

Three things must all hold for that last step to work, and each one failed silently at some point:

1. `@Filter(name = "tenantFilter")` must be on each concrete `@Entity`. Hibernate ignores it on a
   `@MappedSuperclass`, so putting it only on `AbstractEntity` does nothing.
2. The service must be `@Transactional`. The filter is enabled on
   `entityManager.unwrap(Session.class)`; with no transaction that session is thrown away before
   the query runs. The aspect now throws if no transaction is active rather than returning
   unfiltered rows.
3. The transaction advisor must be the outer advice, which `TransactionAdviceOrderConfig` pins
   via `@EnableTransactionManagement(order = HIGHEST_PRECEDENCE)`. Otherwise the aspect and the
   transaction interceptor both sit at `LOWEST_PRECEDENCE` and their order is undefined.

**Adding a new tenant-scoped entity?** Annotate it with `@Filter(name = "tenantFilter")` and make
its service `@Transactional`. Forgetting the first is silent; forgetting the second now throws.

### `schema` (NOT implemented — do not use)

Intended as schema-per-tenant. The scaffolding exists — `MultiTenantConnectionProviderImpl`,
`CurrentTenantIdentifierResolverImpl`, `TenantSchemaResolver`, and
`db/migration/tenant/V1__init_For_tenat_DB.sql` as the per-tenant table template — but the mode is
not wired up:

- Nothing creates a tenant schema, and nothing runs the `db/migration/tenant` location.
  `spring.flyway.locations` points only at `db/migration/common`.
- `TenantContext.setCurrentSchema(...)` is only ever set from `JwtAuthenticationFilter`; there is
  no provisioning step that would make the resolved schema exist.
- Selecting `schema` turns **off** `TenantFilter` and `TenantHibernateFilter` (both are
  `@ConditionalOnProperty(havingValue = "filter")`) and turns nothing on. The result is no header
  enforcement and no tenant predicate — every tenant sees every row.

The two Hibernate classes are now `@ConditionalOnProperty(havingValue = "schema")` so they stay
inert in filter mode instead of registering a `MultiTenantConnectionProvider` that nothing uses.

## TODO to finish schema mode

- Provision a schema per tenant on approval (`TenantServiceImpl.approveTenant` already calls a
  `provisioningService`), and run `db/migration/tenant` against it with a programmatic Flyway.
- Set the schema per request and verify `MultiTenantConnectionProviderImpl` sets `search_path`.
- Decide what happens to `tenant_id` columns and the `tenantFilter` once rows are schema-isolated.
- Cover both modes in tests.
