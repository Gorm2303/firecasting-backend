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
- `examples/standard-results-v3.json`
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

## Standard results payload (v3)

The backend exposes a single “standard results” response that bundles the pieces the UI typically needs.

Endpoint:

- `GET /api/simulation/v3/results/{simulationId}`

Fields:

- `simulationId`: the run ID.
- `yearlySummaries`: the same yearly summaries described above.
- `metricSummaries`: percentile summaries for supported metrics, for different scopes.

### Metric summaries

Each `metricSummaries[]` item is a `MetricSummary`:

- `metric`: metric id (string).
- `scope`: one of:
	- `YEARLY` – values per `{phaseName, year}`
	- `PHASE_TOTAL` – totals per phase across its full duration
	- `OVERALL_TOTAL` – totals across all phases
- `phaseName`:
	- Present for `YEARLY` and `PHASE_TOTAL`.
	- Omitted/null for `OVERALL_TOTAL`.
- `year`:
	- Present for `YEARLY`.
	- Omitted/null for `PHASE_TOTAL` and `OVERALL_TOTAL`.
- `p5`, `p10`, `p25`, `p50`, `p75`, `p90`, `p95`: the selected percentiles across simulated paths.

### Metric semantics (what each number means)

Unless otherwise stated, values are computed across paths by aggregating the engine’s snapshots and then taking percentiles.

Supported metrics (current set):

- Flow totals (deltas): `deposit`, `withdraw`, `tax`, `fee`, `return`
	- `YEARLY`: the change over the given year within the given phase.
	- `PHASE_TOTAL`: the sum over all years in the phase.
	- `OVERALL_TOTAL`: the sum over all phases.
- State values: `capital`, `inflation`
	- `YEARLY`: end-of-year value for the given phase/year.
	- `PHASE_TOTAL`: end-of-phase value.
	- `OVERALL_TOTAL`: end-of-simulation value.

## Monthly view (derived)

The UI offers a monthly view/window by interpolating yearly summaries.

- This is intended for visualization and “windowing”, not as a strict monthly accounting ledger.


(Initial stub; fill as series IDs are consolidated.)
