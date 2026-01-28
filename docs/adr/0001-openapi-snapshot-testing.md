# ADR 0001: OpenAPI snapshot testing

## Status

Accepted.

## Context

We want:

- A stable transport contract for frontend integration
- Reviewable diffs when the contract changes
- A guard that is fast and does not require DB connectivity

## Decision

- Use springdoc-openapi to generate OpenAPI at runtime.
- Restrict the contract to endpoints explicitly marked `@PublicApi`.
- Store snapshot as YAML for review friendliness.
- Compare canonical JSON in tests.

## Consequences

- Adding a new public endpoint requires marking it `@PublicApi` and updating the snapshot.
- Accidental internal endpoints are not included unless explicitly marked.
