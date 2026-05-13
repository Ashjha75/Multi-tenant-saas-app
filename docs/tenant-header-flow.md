# Header Tenant Flow (X-Tenant-ID)

This explains how tenant id flows through the request when using the header approach.

## Flow (simple)
1. Client sends `X-Tenant-ID` header with every API request.
2. `TenantFilter` reads the header and validates it.
3. If the header is missing or invalid, the request is rejected with 400.
4. If valid, the tenant id is stored in `TenantContext` (ThreadLocal).
5. In `filter` mode, `TenantHibernateFilter` applies the Hibernate `tenantFilter` using the tenant id.
6. All queries are scoped to the current tenant automatically.
7. At the end of the request, the tenant context is cleared.

## Where to look in code
- `config/TenantFilter.java` — reads and validates `X-Tenant-ID`.
- `config/TenantContext.java` — stores the tenant id per request.
- `config/TenantHibernateFilter.java` — applies Hibernate filter in `filter` mode.

## TODO (later improvements)
- Allow tenant resolution from JWT or subdomain.
- Centralize header name in config.
- Add tests for missing/invalid header behavior.

