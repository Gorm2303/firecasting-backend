# Glossary (UI tooltips)

This glossary is intended for UI tooltips.
Each entry is a short definition (1–2 sentences) aligned with the Data Dictionary.

Each metric is also tagged with:

- **Provenance**:
	- **INPUT**: derived from user inputs/config and engine rules
	- **MARKET**: derived from a market dataset (e.g. DJIA) via data-driven return models
	- **INTERNAL**: internal diagnostic/trace values not meant for end-user interpretation
- **Storage**:
	- **STORED**: persisted in snapshots/results payloads
	- **COMPUTED**: computed on demand (or at export time) from other stored values

## Snapshot / trace / CSV

- `phase`: Phase name at snapshot time. Provenance: INTERNAL. Storage: STORED.
- `day`: Days since simulation start (0-based). Provenance: INTERNAL. Storage: STORED.
- `month`: Months since simulation start (0-based). Provenance: INTERNAL. Storage: STORED.
- `year`: Years since simulation start (0-based). Provenance: INTERNAL. Storage: STORED.
- `date`: ISO date label for the snapshot. Provenance: INTERNAL. Storage: STORED.

## Capital and flows

- `capital`: Portfolio value at the snapshot after all operations up to that time. Provenance: INPUT. Storage: STORED.
- `deposited`: Cumulative deposits applied so far. Provenance: INPUT. Storage: STORED.
- `deposit`: Deposit amount applied in the latest deposit event (often month-end). Provenance: INPUT. Storage: STORED.
- `passive`: Cumulative passive flow applied so far. Provenance: INPUT. Storage: STORED.
- `returned`: Cumulative returns applied so far (net of notional tax when applicable). Provenance: MARKET (when using data-driven returns) or INPUT (when using synthetic returns). Storage: STORED.
- `return`: Return applied in the latest return event (daily or month-end depending on configuration). Provenance: MARKET (when using data-driven returns) or INPUT (when using synthetic returns). Storage: STORED.
- `withdrawn`: Cumulative withdrawals applied so far. Provenance: INPUT. Storage: STORED.
- `withdraw`: Withdrawal amount applied in the latest withdrawal event (often month-end). Provenance: INPUT. Storage: STORED.
- `taxed`: Cumulative taxes applied so far. Provenance: INPUT. Storage: STORED.
- `tax`: Tax amount applied in the latest tax event (month-end capital-gains tax and/or year-end notional tax). Provenance: INPUT. Storage: STORED.
- `fees`: Cumulative fees applied so far. Provenance: INPUT. Storage: STORED.
- `fee`: Fee amount applied in the latest fee event (typically year-end). Provenance: INPUT. Storage: STORED.
- `inflation`: Cumulative inflation multiplier (compounded at year-end when enabled). Provenance: INPUT. Storage: STORED.
- `nettotal`: Cumulative net earnings recorded so far. Provenance: INPUT. Storage: STORED.
- `net`: Net earnings recorded in the latest net-earnings event (after tax). Provenance: INPUT. Storage: STORED.

## Accounting integrity (derived deltas)

- `y-return`: Yearly return delta between consecutive snapshots. Provenance: INTERNAL. Storage: COMPUTED.
- `y-withdraw`: Yearly withdrawal delta between consecutive snapshots. Provenance: INTERNAL. Storage: COMPUTED.
- `y-tax`: Yearly tax delta between consecutive snapshots. Provenance: INTERNAL. Storage: COMPUTED.
- `y-net`: Yearly net delta between consecutive snapshots. Provenance: INTERNAL. Storage: COMPUTED.

## Yearly summaries (cross-run aggregates)

- `averageCapital`: Average capital across runs for a given phase/year. Provenance: INTERNAL. Storage: COMPUTED.
- `medianCapital`: Median (p50) capital across runs for a given phase/year. Provenance: INTERNAL. Storage: COMPUTED.
- `minCapital`: Minimum observed capital across runs for a given phase/year. Provenance: INTERNAL. Storage: COMPUTED.
- `maxCapital`: Maximum observed capital across runs for a given phase/year. Provenance: INTERNAL. Storage: COMPUTED.
- `stdDevCapital`: Standard deviation of capital across runs for a given phase/year. Provenance: INTERNAL. Storage: COMPUTED.
- `cumulativeGrowthRate`: Cumulative growth rate over the year (dimensionless ratio) across runs. Provenance: INTERNAL. Storage: COMPUTED.
- `quantile5`: 5th percentile capital across runs for a given phase/year. Provenance: INTERNAL. Storage: COMPUTED.
- `quantile25`: 25th percentile capital across runs for a given phase/year. Provenance: INTERNAL. Storage: COMPUTED.
- `quantile75`: 75th percentile capital across runs for a given phase/year. Provenance: INTERNAL. Storage: COMPUTED.
- `quantile95`: 95th percentile capital across runs for a given phase/year. Provenance: INTERNAL. Storage: COMPUTED.
- `var`: Value-at-Risk (VaR) at 5% (equal to `quantile5`). Provenance: INTERNAL. Storage: COMPUTED.
- `cvar`: Conditional VaR (average of values at/below VaR). Provenance: INTERNAL. Storage: COMPUTED.
- `negativeCapitalPercentage`: Share of runs that failed (capital <= 0 after the failure point). Provenance: INTERNAL. Storage: COMPUTED.
