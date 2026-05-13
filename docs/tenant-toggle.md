# Tenant Toggle (Filter vs Schema)

This project supports two tenant isolation modes using a simple toggle. The toggle is **only for routing** and **does not implement schema mode yet**.

## What we added
- A config flag `app.tenant.mode` with values:
  - `filter` (current, working)
  - `schema` (future, you will implement)
- A toggle on the Hibernate tenant filter so it runs **only** in `filter` mode.

## How to switch modes
Use `.env` or environment variables:

```
TENANT_MODE=filter
```

or

```
TENANT_MODE=schema
```

Default is `filter` if you do nothing.

## What happens in each mode
- **filter**: `TenantHibernateFilter` is active and applies the Hibernate `tenantFilter` on every service call.
- **schema**: `TenantHibernateFilter` is disabled. This prevents column-filter logic from running when you implement schema routing later.

## TODO (for schema mode)
- Implement schema-based tenant routing (MultiTenantConnectionProvider + CurrentTenantIdentifierResolver).
- Set schema per request using `TenantContext.getCurrentTenant()`.
- Add schema creation/migration logic for new tenants.
- Update tests to cover both modes.

