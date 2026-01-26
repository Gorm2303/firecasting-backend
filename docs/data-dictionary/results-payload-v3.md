# Results payload (v3)

This documents the meaning of result payload fields used by the simulation UI.

See also:

- `glossary.md` (tooltip-ready definitions, provenance, storage semantics)
- `resolutions-and-percentiles.md` (what resolutions/percentiles exist)
- `docs/invariants/timing-model.md` (timing model)

## Examples

Concrete JSON examples live under `docs/examples/`:

- `examples/yearly-results.standard.json`
- `examples/yearly-results.with-monthly-window.json`
- `examples/deep-dive.representative-path.json`
- `examples/manipulation-trace.json`

## Scope

- IDs/labels used by the UI to render charts
- Units and timing assumptions
- Any derived series definitions

## Yearly summaries (authoritative)

The UI primarily consumes yearly summaries per `{phaseName, year}`.

Each yearly summary describes the distribution of **effective capital** across all simulated paths for that phase/year.

### Capital distribution fields

- `averageCapital`: average capital (robust average).
- `medianCapital`: median capital (p50).
- `minCapital`, `maxCapital`: extremes across runs.
- `stdDevCapital`: standard deviation.
- `cumulativeGrowthRate`: cumulative growth rate over the year.
- `quantile5`, `quantile25`, `quantile75`, `quantile95`: selected quantiles.
- `var`: Value-at-Risk at 5% (same value as `quantile5`).
- `cvar`: average of values at/below VaR.
- `negativeCapitalPercentage`: percent of runs that “failed” (capital <= 0 after the failure point).

### Percentiles grid

Some endpoints/exports include a percentiles grid:

- `percentiles`: 101 values for p0..p100 at 1% increments.
	- p0 is index 0, p100 is index 100.
	- The contract guarantees the subset p0..p100 at 5% steps at indices 0,5,10,…,100.

## Monthly view (derived)

The UI offers a monthly view/window by interpolating yearly summaries.

- This is intended for visualization and “windowing”, not as a strict monthly accounting ledger.


(Initial stub; fill as series IDs are consolidated.)
