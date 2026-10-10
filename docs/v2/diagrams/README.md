# Diagrams

Drawn from the Diagram Briefs in `docs/v2/`. Each one is a `.excalidraw` source file plus a
rendered `.png`.

| Diagram | Shows | Brief lives in |
|---|---|---|
| [01-request-lifecycle](01-request-lifecycle.png) | One authenticated request from client to SQL, with the two error exits | `02-code-design.md` |
| [02-tenant-resolution](02-tenant-resolution.png) | Where the tenant comes from, and why the JWT beats the header | `02-code-design.md` |
| [03-entity-relationships](03-entity-relationships.png) | The five entities, split into cross-tenant and tenant-scoped | `03-database-design.md` |
| [04-package-map](04-package-map.png) | The layers, call direction, and the one layering violation | `02-code-design.md` |
| [05-system-map](05-system-map.png) | The three containers, both schemas, and the key pair | `04-connections.md` |

## Editing

Open a `.excalidraw` file at [excalidraw.com](https://excalidraw.com) or in the VS Code Excalidraw
extension. After editing, re-render the PNG:

```bash
npx -y mcp-excalidraw-server render docs/v2/diagrams/01-request-lifecycle.excalidraw \
  --scale 2 --out docs/v2/diagrams/01-request-lifecycle.png
```

## Colour key

Consistent across all five:

| Colour | Means |
|---|---|
| Blue | Edge and HTTP layer — filters, controllers |
| Yellow / amber | Security, tenant context, JWT keys, Flyway |
| Purple | Transaction and AOP layer |
| Green | Business logic, persistence, the `public` schema |
| Grey, dashed | Present in the code but **not used at runtime** |
| Red | Error exits, and things that are wrong or surprising |
