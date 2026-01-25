# OpenAPI contract

The backend publishes an OpenAPI document for the **Public API** group.

## Scope control

Only controllers/methods marked with `@PublicApi` are included in the public contract.
This prevents accidental exposure of internal endpoints.

## Snapshot + drift guard

The checked-in snapshot lives in:

- `firecasting/application/src/test/resources/openapi/openapi.yaml`

The drift guard test compares the runtime-generated spec against this snapshot.

### Update snapshot

From `firecasting-backend/firecasting`:

- `./mvnw.cmd -pl application -Dtest=dk.gormkrings.contract.OpenApiSnapshotTest -Dopenapi.snapshot.update=true test`

Review the diff in `openapi.yaml` as part of the workflow.

## Human-readable UI (dev stack)

When running the Docker dev stack, Swagger UI is served via Traefik at:

- `https://api.local.test/swagger-ui/`

The raw OpenAPI JSON for the public group is at:

- `https://api.local.test/v3/api-docs/public`
