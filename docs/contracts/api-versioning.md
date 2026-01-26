# API versioning policy

This API is primarily consumed by the Firecasting frontend and is **not intended as a public API**.
Even so, the frontend depends on stable response formats, so we treat the JSON payload shapes as a versioned contract.

## Principles

- **Breaking changes require a new major version** (either a new versioned endpoint prefix, or a versioned payload type).
- **Non-breaking changes** (adding new optional fields) can ship within the same major version.
- The OpenAPI snapshot is generated from **versioned endpoints only**.

## Endpoint versioning

We use *versioned endpoint prefixes*.

- Simulation API contract: `/api/simulation/v3/**`
- Form schema contract: `/api/forms/v1/**`

For convenience during development, the backend may also expose an **unversioned alias** (e.g. `/api/simulation/**`) that maps to the current major version.
Clients that require stability should always use the versioned prefix.

## When a breaking change is necessary

Examples of breaking changes:

- Renaming or removing a field
- Changing a field type (e.g. number → string)
- Changing a field’s meaning (semantic change) in a way that breaks the UI

Process:

1. Introduce a new endpoint prefix (e.g. `/api/simulation/v4/**`).
2. Keep the previous version (e.g. v3) available during a migration window.
3. Document the differences (see “Version diffs” below).
4. Update the frontend to target the new version.
5. Deprecate and eventually remove the old version.

## Version diffs

When introducing a new major version, add a section here describing:

- Which endpoints changed
- Which fields were added/removed/renamed
- Any changed semantics (timing, units, derivation)

### v3 (current)

- `v3` is the current stable contract used by the UI.

### v4 (reserved)

- Reserved for the next breaking change.
