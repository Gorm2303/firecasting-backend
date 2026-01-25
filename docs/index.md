# Firecasting backend docs

This folder is the backend-owned source of truth for:

- API transport contract (OpenAPI snapshot)
- DTO field meaning (data dictionary)
- Engine semantics and invariants

## Update workflows

- OpenAPI snapshot: see `contracts/openapi.md`
- Data dictionary: update the relevant markdown file under `data-dictionary/`

## Definitions

- **Contract**: what the backend sends/accepts over HTTP.
- **Data dictionary**: what each field/series/metric *means* (units, timing, derivation).
- **Invariant**: a rule that must hold across refactors.
