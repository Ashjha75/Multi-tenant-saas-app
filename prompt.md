# Reverse-documentation prompt

Copy everything below the line into a fresh Claude Code session opened at the project root.

---

Reverse-document this existing Spring Boot multi-tenant SaaS codebase into `docs/v2/`.

I built it 2-3 months ago to learn Spring Boot and I have forgotten most of my own
decisions. The code is the only reliable source of truth. The existing `docs/*.md` files
(160 lines total) are vague and partly stale: read them for hints, trust them for nothing.

I need four things answered, in this order of importance:

1. What this project is and what it does
2. How the code is designed
3. How the database is designed
4. How all of those connect to each other

## Ground rules

1. **Code is truth.** Every factual claim must trace to a file you actually read. Cite as
   `path/to/File.java:42`.
2. **Do not invent.** If something is not determinable from the code, do not fill the gap
   with a plausible guess. Put it in the Open Questions doc instead.
3. **Mark rationale honestly.** When you state WHY something was built a certain way, tag it
   `[DOCUMENTED]` (stated in code, comments or docs) or `[INFERRED]` (your reading of the
   code). Never present an inference as a recorded decision.
4. **Separate IS from SHOULD.** 80% of the content describes what the system does today.
   Improvements go only in the clearly-marked final section of each doc. Never blend them
   into the descriptive sections.
5. **Be terse.** Strong headings, short paragraphs, tables over prose. Respect the length
   budgets. If you must cut, cut adjectives, not facts.

## NO DIAGRAMS — this is a hard rule

Do **not** output Mermaid, PlantUML, Graphviz, ASCII art, or any other drawn diagram. I draw
my own diagrams in Excalidraw.

Wherever a diagram would normally go, write a **Diagram Brief** instead: a plain-text spec I
can feed to my drawing tool. Format each one exactly like this:

    **Diagram Brief: <name>**
    Boxes:
      - <box name> — <one-line label>
      - ...
    Arrows:
      - <box A> -> <box B> : <label on the arrow>
      - ...
    Grouping: <which boxes sit inside which boundary, if any>
    Layout hint: <left-to-right, layered, top-down, etc.>

Keep each brief under 25 boxes. Prefer two focused briefs over one crowded one.

## Verified facts — treat as given, do not re-derive

- Spring Boot 4.0.6, Java 21, Maven.
- Security: spring-boot-starter-security, jjwt (api/impl/jackson), RSA keypair at
  `src/main/resources/certs/private_key.pem` and `public_key.pem`.
- Persistence: spring-boot-starter-data-jpa, PostgreSQL, H2 (test), Flyway split into
  `db/migration/common/` (V1-V5) and `db/migration/tenant/` (V1).
- Also present: springdoc-openapi, Lombok, validation, caching (`CacheConfig`), Docker Compose.
- Packages: config(11), entities(9), services(7) + services/impl(6), security(7),
  controllers(6), repositories(5), mapper(5), dto/request(5), dto/response(5), exception(4),
  auth, common. 77 Java files total.
- Test coverage: exactly 1 test file in `src/test`.

## Phase 1 — Read before writing

Read in this order. Do not write anything until this is done.

- `pom.xml`, `application.yml`, `application-dev.yaml`, `docker-compose.yml`, `Dockerfile`
- ALL of `config/` and `security/` — the architecture actually lives here
- Every Flyway migration in both `common/` and `tenant/`
- Entities, then controllers, then services/impl, then repositories and mappers
- `exception/`, `docs/*.md`, `FIX.md`, `README.md`

**Priority investigation — resolve this first, it is the heart of the system.**

This codebase appears to contain two multi-tenancy strategies at once:

- (a) schema/database-per-tenant via `MultiTenantConnectionProviderImpl`,
  `CurrentTenantIdentifierResolverImpl`, `TenantSchemaResolver`
- (b) row-level discriminator filtering via `TenantHibernateFilter`

and `docs/tenant-toggle.md` suggests a switch between them.

Determine from the code: which is active, how the toggle is configured, whether both can run
together, how `TenantContext` is populated and cleared per request, how `TenantFilter` orders
against `JwtAuthenticationFilter`, and what `TransactionAdviceOrderConfig` is guarding
against. If any of this is genuinely ambiguous, say so rather than smoothing it over.

This description is my hypothesis from file names, not a requirement. If the code says
otherwise, tell me plainly — a correction is worth more to me than agreement.

## Phase 2 — Write exactly these six files

### `docs/v2/README.md` (~30 lines)

Index of v2: one line on what each doc covers. State that `docs/v2/` supersedes the older
`docs/*.md`. Do not delete or edit the old files.

### `docs/v2/01-project-overview.md` (200-300 lines)

What this project IS, as it exists today.

- What the product is, in 3-4 sentences, no marketing tone
- The problem it solves, and the actors — derive real roles from `UserRole` and
  `SecurityConfig`, do not assume
- Domain glossary: Tenant, User, Product, Category, StockMvt, TypeMvt, TenantStatus. Define
  each from the entity and its actual usage, not from its name
- Capability table: one row per endpoint group — capability, endpoint, auth required, role
  required, source file
- Tenant lifecycle: provisioning via `ProvisioningServiceImpl` and `TenantServiceImpl`,
  including what happens to schemas and migrations when a tenant is created
- Auth model: registration, login, token issue and verify, what the RSA keys are for
- Non-functional behaviour visible in code: validation, caching, error contract, health,
  OpenAPI
- Final section only — **Gaps and Improvement Candidates**: options with one-line rationale
  each. The 1-test situation belongs here. No roadmap, no estimates.

### `docs/v2/02-code-design.md` (300-450 lines)

How the code is structured and how it fits together.

- **Layering**: the layers in this codebase, what belongs in each, and the rule for what may
  call what
- **Module map**: a table of every package — responsibility, key classes, what it depends on,
  what depends on it. Call out any layering violations you actually observe
- *Diagram Brief: Package dependency map*
- **Request lifecycle**: a numbered walkthrough of ONE authenticated tenant-scoped request,
  end to end, naming each class in order with `file:line`. Include where the tenant is
  resolved, where it is bound to the thread, where it is cleared, and how that interacts with
  transaction boundaries
- *Diagram Brief: Request lifecycle*
- **Multi-tenancy mechanism**: the authoritative explanation of the two strategies and the
  toggle. Include failure modes visible in code — missing, unknown, or inactive tenant header
- *Diagram Brief: Tenant resolution path*
- **Security design**: filter ordering, JWT lifecycle with RSA signing, what
  `RestAuthenticationEntryPoint` vs `RestAccessDeniedHandler` each handle, public vs protected
  route matchers
- **Error handling contract**: how `GlobalExceptionHandler` maps exceptions to responses, and
  the response shape clients can rely on
- **Key class notes**: short notes only for the genuinely non-obvious classes — the connection
  provider, the tenant resolver, the filter chain ordering. Skip CRUD classes entirely
- **Decisions and rationale**: table of notable choices, each rationale tagged
  `[DOCUMENTED]` or `[INFERRED]`
- Final section only — **Design Risks and Improvement Candidates**

### `docs/v2/03-database-design.md` (200-300 lines)

How the data is modelled and stored.

- **Storage topology**: what lives in the common schema vs a tenant schema, and why. Make the
  isolation boundary unmistakable
- **Entity reference**: one subsection per entity — table name, columns with types,
  nullability, constraints, defaults. Source each from the Flyway migration, not the Java
  annotation, and note any place where the two disagree
- **Relationships**: a table of every relationship — from, to, cardinality, owning side, fetch
  type, cascade, orphan removal
- *Diagram Brief: Entity relationship map*
- **`AbstractEntity` and auditing**: the inherited columns and how `JpaAuditingConfig`
  populates them
- **Enums**: how `TenantStatus`, `UserRole` and `TypeMvt` are persisted, and what
  `V2__add_enum_checks.sql` enforces
- **Migration strategy**: a table of every migration file — what it does, which schema it
  targets, when it runs. Explain how a new tenant's schema gets migrated at provisioning time
- **Indexes, keys and constraints** actually present, and the query patterns they serve
- Final section only — **Data Model Risks and Improvement Candidates**

### `docs/v2/04-connections.md` (150-250 lines)

How the pieces join up. This is the doc that answers "how is it all connected".

- **Code to database**: entity → table → repository → service → controller, as one table.
  One row per domain object, readable left to right
- **Request to data**: for one read and one write, trace the path from HTTP in to SQL out,
  naming the tenant-scoping step
- **Configuration to behaviour**: a table mapping each meaningful property in
  `application.yml` to the class that reads it and the behaviour it changes
- **Runtime wiring**: which beans are created where, and the ordering constraints that matter
  (filters, transaction advice, connection provider)
- *Diagram Brief: End-to-end system map*
- **External touchpoints**: Postgres, Docker Compose services, OpenAPI surface, health endpoint
- Final section only — **Integration Risks and Improvement Candidates**

### `docs/v2/05-open-questions.md` (60-120 lines)

Everything you could NOT determine from the code — the intent-level questions only I can
answer. Group by topic. One line each, with the file that prompted it and why it matters.
This becomes the backlog I work through with `/grill-me`, so make the questions sharp and
specific, not generic.

## Skills to use

Invoke these explicitly as you work. Do not skip them.

- **`codebase-design`** — use its deep-module vocabulary precisely for the layering, module
  map and seam analysis in `02-code-design.md`. Do not fall back on loose words like
  "component" or "service".
- **`domain-modeling`** — use for the domain glossary in `01-project-overview.md` and the
  Decisions and Rationale table in `02-code-design.md`.
- **`engineering:system-design`** — use to frame the system map and connection analysis in
  `04-connections.md`.
- **`engineering:architecture`** — use to shape each row of the Decisions and Rationale table
  as an ADR-style entry: decision, context, consequences.

## Working method

Work one file at a time. Show me each file before moving to the next, and wait for my
go-ahead.
