# 05 — Open Questions

Things the code could not tell me. These are intent-level: only you know the answer. Each one
names the file that raised it and why it matters.

Grouped by topic. This is the backlog to work through.

## Product scope

1. **Is this an inventory system, or only a movement log?** Nothing sums `StockMvt.quantity` into
   an on-hand figure anywhere in `services/impl/`. Was that the next feature, or is the ledger the
   whole product? *Matters: it decides whether the data model is finished or half-built.*

2. **What is `alert_threshold` for?** Stored on every product (`Product.java:36`), required by
   validation, never read by any code. Low-stock alerts? A UI hint? *Matters: if alerts are
   planned, they need the stock calculation from Q1 first.*

3. **Should `StockMvt` ever be editable or deletable?** `StockMvtController` exposes `PUT` and
   `DELETE`. A stock ledger is normally append-only. *Matters: if it is a ledger, two endpoints
   should be removed, not fixed.*

4. **Who is the intended end user of the API?** The Postman collection has no UI client and the
   repo has no frontend. Is a UI planned, or is this a backend-only portfolio project? *Matters:
   it changes how much the error contract and CORS config need to be hardened.*

## Multi-tenancy strategy

5. **Do you still want schema-per-tenant?** `docs/flow.md:3` says a schema is allocated
   automatically, and provisioning does create one — but `filter` mode never reads it. Finish
   schema mode, or delete the scaffolding? *Matters: it is the largest fork in the project, and
   today both halves are half-present.*

6. **If schema mode stays, what happens to the `tenant_id` columns?** They would become redundant
   once rows are isolated by schema, but `AbstractEntity` requires `tenant_id` on insert and
   throws without it (`AbstractEntity.java:83-86`). *Matters: the two strategies have
   contradictory requirements on the same column.*

7. **Should every tenant approval keep creating a Postgres schema?** `ProvisioningServiceImpl`
   creates and migrates `tenant_<code>` on every approval and nothing queries it. *Matters: it is
   wasted work today and misleads the next reader into thinking schema mode is live.*

8. **Is `X-Tenant-ID` meant to stay required now that the JWT is authoritative?**
   `JwtAuthenticationFilter.java:44` overwrites whatever the header said. The header only causes
   400s and tags logs. *Matters: it is a required header on every call that changes no behaviour.*

## Domain model

9. **Should `tenant_id` mean the same thing on every table?** It holds `company_code` on `tenants`
   and a UUID everywhere else (`AbstractEntity.java:73-77` vs `TenantServiceImpl.java:185`).
   *Matters: this is the single most confusing thing in the schema.*

10. **Which of the two tenant columns on `users` is the real one?** `tenant_id` is what the app
    reads; `"tenant id"` holds the foreign key and no entity maps it
    (`V1__init_Common_tables.sql:65,89-91` vs `User.java:25-27`). *Matters: the column the app
    trusts has no referential integrity.*

11. **Should `username` and `email` be unique per tenant instead of globally?** Today two
    companies cannot both have a user called `admin` (`V1:85`). Login depends on global
    uniqueness (`TenantHibernateFilter.java:43-48`). *Matters: real multi-tenant SaaS normally
    scopes usernames; changing it means reworking login.*

12. **Are `ROLE_USER` and `ROLE_SALES_OPERATOR` planned, or leftovers?** Defined in
    `UserRole.java:8-9`, assigned by no code path, referenced in no `@PreAuthorize`. *Matters:
    either wire them up or delete them and the CHECK constraint entries.*

13. **Is a user allowed to belong to more than one tenant?** The model says no (one `tenant_id`
    column), but it is worth confirming before anything is built on that assumption. *Matters:
    changing it later is a schema migration plus an auth rework.*

## Authorization

14. **What should the role rules on the business endpoints be?** `CategoryController`,
    `ProductController` and `StockMvtController` have no `@PreAuthorize` at all. Any authenticated
    user can delete any product in their tenant. *Matters: the roles exist and are unused — this
    is the biggest security gap found.*

15. **Who is allowed to approve or suspend a tenant?** `TenantController` gates only `/pending`
    (line 119). Approve, activate, deactivate, suspend and list-all are open to any authenticated
    caller. *Matters: a company admin can currently approve their own tenant.*

16. **Was `UserController`'s class-level `hasRole('PLATFORM_ADMIN')` meant to be the floor?** Every
    method overrides it with a different role, so a platform admin can call almost nothing
    (`UserController.java:30` vs `:42,55,72,86`). *Matters: the intended permission matrix cannot
    be recovered from the code.*

17. **Should `ROLE_COMPANY_ADMIN` be able to act outside its own tenant?** The tenant filter scopes
    data, but nothing checks that the tenant in the token matches the tenant being administered on
    `/api/v1/tenants/{id}/*`. *Matters: data is scoped; tenant *management* is not.*

## Tenant lifecycle

18. **What is the intended state machine?** As written, `activateTenant` requires `PENDING`
    (`TenantServiceImpl.java:102`) but `approveTenant` has already set `ACTIVE` (`:76`), so
    activate always fails after approve. And `suspendTenant` throws if the tenant **is** `ACTIVE`
    (`:128`), so only a non-active tenant can be suspended. *Matters: three of five lifecycle
    endpoints cannot be used in the obvious order. I could not tell which condition is wrong.*

19. **Why does `POST /api/v1/auth/register` duplicate `POST /api/v1/tenants`?** Both call
    `tenantService.registerTenant` (`AuthenticationController.java:85`,
    `TenantController.java:36`), but one is public and one requires a token. *Matters: two doors
    to one room, with different locks.*

20. **Should a tenant's `admin_password` stay on the tenant row after approval?** It is copied to
    the `users` row (`TenantServiceImpl.java:183`) and both hashes then live on independently.
    *Matters: a stale credential copy with no owner.*

## Data model details

21. **Are deletes meant to be soft?** There is a `deleted` column, the filter predicate includes
    `deleted = false`, and the comments say "soft-delete" — but the soft-delete line is commented
    out and the code calls `repo.delete()` (`CategoryServiceImpl.java:94-97`,
    `ProductServiceImpl.java:92-95`). *Matters: the whole audit design assumes soft deletes.*

22. **Should `products.reference` be unique per tenant?** It is globally unique
    (`V5:51`) but the duplicate check is tenant-scoped (`ProductServiceImpl.java:98-101`), so
    tenant B gets an unexplainable 409. *Matters: a plain bug, but the right fix depends on
    whether SKUs are meant to be global.*

23. **How long should a category description be?** The DTO allows 500 characters
    (`CategoryRequest.java:25`), the column holds 255 (`V5:29`). *Matters: one of the two is
    wrong and the mismatch produces a 500.*

24. **Was `V2__add_enum_checks.sql` meant to add anything?** `V1` already created equivalent CHECK
    constraints under different names, so `V2`'s `IF NOT EXISTS` guard never fires and both exist.
    *Matters: a no-op migration that looks load-bearing.*

25. **Is `UserRequest.tenantId` supposed to be client-supplied?** Its `@Schema` says "auto-injected from
    context if not provided" (`UserRequest.java:49-50`), and `UserMapper.toEntity` never reads it
    (`UserMapper.java:11-23`) — `@PrePersist` fills the column instead. *Matters: a request field
    that is documented, accepted, and silently ignored. Delete it, or make it meaningful.*

26. **`UserRequest.role` is annotated `@NotBlank` but typed `UserRole`** (`UserRequest.java:45-47`).
    `@NotBlank` only applies to character sequences, so Bean Validation raises
    `UnexpectedTypeException` on this constraint. *Matters: this suggests `POST /api/v1/users` has
    never been exercised. Was the field once a `String`?*

## Operations

27. **Is there meant to be a `prod` profile?** `dev` is the default and `application-dev.yaml` is
    empty, so production would run dev defaults including `show-sql: true`. *Matters: SQL logging
    and permissive CORS would ship.*

28. **Where will this run?** `Dockerfile` and the entrypoint support an ECS/Fargate secrets path
    (`JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` env vars), but compose uses host files. *Matters: it
    decides which key-distribution path to keep and which to delete.*

29. **Should the app run as more than one instance?** The `tenantSchemas` cache is per-JVM and
    never evicted (`CacheConfig.java:13`), which is fine for one node and wrong for several.
    *Matters: affects whether a shared cache is needed before scaling out.*

30. **What should `traceId` contain?** `ApiError` carries the field and
    `RestAuthenticationEntryPoint.java:46` reads the MDC key `traceId`, which nothing ever sets, so
    it is always `null`. *Matters: either wire up request ids or drop the field from the contract.*
