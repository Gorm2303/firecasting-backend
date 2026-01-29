# ADR 0002: Results storage volume and viability

Date: 2026-01-26

## Context

We want to expose richer results than the existing yearly capital distribution summaries.

The new standard v3 results payload adds percentile summaries for additional metrics (flows + selected state values) at multiple scopes:

- YEARLY per {phaseName, year}
- PHASE_TOTAL per phase
- OVERALL_TOTAL for the whole run

There is also interest in “storing more than ~14 stats per year”, including:

- additional percentiles
- more metrics
- potentially arrays per step (e.g., daily values)

## Decision

1) Persist compact percentile summaries and yearly distributions as the default.

- Store per-run aggregated outputs that are O(years × metrics × percentiles), not O(paths × steps × metrics).
- Keep percentiles to a stable, UI-oriented set (currently p5/p10/p25/p50/p75/p90/p95) unless a concrete use case requires more.

2) Do not persist per-path per-step arrays by default.

- If detailed traces are needed, they should be:
  - opt-in,
  - bounded (short windows), and/or
  - stored in a separate “artifact” store (compressed files) rather than in primary relational tables.

## Reasoning (order-of-magnitude sizing)

These are rough estimates to guide design.

### Aggregated percentile summaries (current direction)

Assume:

- years ≈ 30
- phases ≈ 3 (total phase-years ≈ 30, because phases partition the horizon)
- metrics ≈ 8 (5 flow totals + capital + inflation + one extra)
- percentiles ≈ 7

Row count per run (approx):

- YEARLY: 30 years × 8 metrics = 240 rows
- PHASE_TOTAL: 3 phases × 8 metrics = 24 rows
- OVERALL_TOTAL: 8 rows
- Total ≈ 272 rows

Even with generous per-row overhead, this is typically on the order of **tens of KB per run**, i.e. very manageable.

### Per-path per-step arrays (the expensive option)

Assume:

- paths ≈ 1000
- daily steps over 30 years ≈ 10,950
- metrics ≈ 10

Values per metric ≈ 1000 × 10,950 ≈ 10.95 million doubles.

Raw size per metric ≈ 10.95M × 8 bytes ≈ 87.6 MB.

For 10 metrics, raw size ≈ **~876 MB per simulation run** (before overhead).

This scales poorly in storage, IO, and query cost, and is not viable as the default persistence model.

## Consequences

- The default API stays “small and fast” while supporting richer UI charts (percentile series by year and totals).
- If/when more detail is needed, we can introduce an explicit, opt-in trace export (likely compressed) without destabilizing the core contract.
- Adding more percentiles or metrics is feasible, but should be guided by concrete UI/analysis needs.
