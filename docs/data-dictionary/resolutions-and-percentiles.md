# Resolutions and percentiles

This document answers: “what time resolutions exist?” and “what percentile set do we get?”

## Resolutions

### Yearly (native)

The backend’s primary aggregated resolution is **yearly**.

- Yearly summaries are computed per `{phaseName, year}` across all simulated paths.
- These are the values the UI should treat as authoritative.

### Monthly (visualization window)

The UI supports a monthly window for interpretation/visualization.

- Monthly points are derived by **linear interpolation** between yearly summaries.
- They are meant for charting convenience and should not be interpreted as a fully simulated monthly accounting ledger.

### Snapshot / trace (engine checkpoints)

The engine also produces per-run snapshots used for CSV exports and deep-dive tracing.

- Snapshots are captured at **year end** and **phase end**.
- A phase can end mid-year; the final snapshot will reflect that end date.

## Percentiles

Yearly summaries expose:

- `quantile5`, `quantile25`, `medianCapital` (p50), `quantile75`, `quantile95`
- `var` (VaR at 5%, equal to `quantile5`)
- `cvar` (average of values at/below VaR)

Some outputs also include a percentiles grid:

- `percentiles`: 101 values representing p0..p100 in **1% increments**.
- Contractually, clients should assume that the subset p0..p100 in **5% steps** is always present (indices 0,5,10,…,100).
	- If/when the contract is narrowed to only 5% steps, clients should treat any intermediate 1% points as optional.

Metric summaries also expose a fixed percentile set:

- `p5`, `p10`, `p25`, `p50`, `p75`, `p90`, `p95`
	- This is intended as a compact, stable set for UI charts and tooltips.
