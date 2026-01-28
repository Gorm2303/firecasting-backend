# Firecasting backend docs

This folder is the backend-owned source of truth for:

- API transport contract (OpenAPI snapshot)
- DTO field meaning (data dictionary)
- Engine semantics and invariants

## Update workflows

- OpenAPI snapshot: see `contracts/openapi.md`
- Data dictionary: update the relevant markdown file under `data-dictionary/`

## High-signal entry points

- `data-dictionary/metrics-taxonomy.md`: metric grouping and where new metrics must be placed
- `data-dictionary/glossary.md`: tooltip-ready definitions for metrics shown in the UI
- `data-dictionary/resolutions-and-percentiles.md`: which resolutions exist, how monthly windows work, and percentile sets
- `invariants/timing-model.md`: timing conventions for compounding, taxes, fees, withdrawals
- `contracts/api-versioning.md`: how breaking changes are introduced safely

## Definitions

- **Contract**: what the backend sends/accepts over HTTP.
- **Data dictionary**: what each field/series/metric *means* (units, timing, derivation).
- **Invariant**: a rule that must hold across refactors.
