# docs/v2 — reverse-documented from the code

This set was written by reading the source, not the old docs. Every factual claim cites the
file it came from as `path/File.java:line`. Where the code did not say *why*, the rationale is
tagged `[INFERRED]` rather than presented as a recorded decision.

**`docs/v2/` supersedes the older `docs/*.md`.** Those files are kept, not edited or deleted —
several are stale and a few are contradicted by the code. Where they conflict, v2 wins, and the
conflict is called out explicitly in the relevant v2 doc.

## The documents

| File | Covers |
|---|---|
| [01-project-overview.md](01-project-overview.md) | What the product is, who the actors are, the domain glossary, the capability/endpoint table, tenant lifecycle, and the auth model |
| [02-code-design.md](02-code-design.md) | Layering, the module map, one request end-to-end, the multi-tenancy mechanism, security design, and the error contract |
| [03-database-design.md](03-database-design.md) | Storage topology, every table and column sourced from Flyway, relationships, auditing, enums, and the migration strategy |
| [04-connections.md](04-connections.md) | How code, configuration, runtime wiring and the database join up — the "how is it all connected" doc |
| [05-open-questions.md](05-open-questions.md) | What the code could not tell me. Intent-level questions only the author can answer |

## Conventions used throughout

- **`[DOCUMENTED]`** — the reason is stated in code, a comment, or a doc file.
- **`[INFERRED]`** — my reading of the code. Treat as a hypothesis to confirm.
- **Diagram Brief** — a plain-text box/arrow spec in place of a drawn diagram, meant to be fed
  to a drawing tool. There are no Mermaid or ASCII diagrams anywhere in v2, by request.
- Each document describes the system **as it is today**. Suggestions are confined to a single
  clearly-marked final section per file and never mixed into the descriptive text.

## Reading order

Read 01 for orientation, then 02. Read 03 before 04 — the connections doc assumes the table
and column names from the database doc. 05 stands alone and is the backlog to work through.
