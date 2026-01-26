# Metrics taxonomy

This file keeps the metric surface coherent over time.

See also:

- `glossary.md` for tooltip-ready definitions, provenance, and storage semantics.

When you add a new metric (CSV column, JSON field, or derived series), you must:

1. Add a Data Dictionary entry describing **meaning**, **unit**, **timing**, and **phase applicability**.
2. Place the metric in exactly one of the groups below with a short description.
3. Add a glossary entry marking **Provenance** (INPUT/MARKET/INTERNAL) and **Storage** (STORED/COMPUTED).

## Capital (state)

- `capital`: Portfolio value at the snapshot.
- `inflation`: Cumulative inflation multiplier (compounded at year-end).

## Flows (deltas and cumulative totals)

- `deposit`: Deposit applied at this step.
- `deposited`: Cumulative deposits.
- `passive`: Cumulative passive flow.
- `return`: Return applied at this step.
- `returned`: Cumulative returns (net of notional tax when applicable).
- `withdraw`: Withdrawal applied at this step.
- `withdrawn`: Cumulative withdrawals.
- `tax`: Tax applied at this step.
- `taxed`: Cumulative taxes.
- `fee`: Fee applied at this step.
- `fees`: Cumulative fees.
- `net`: Net earnings applied at this step.
- `nettotal`: Cumulative net earnings.

## Risk / distribution (cross-run aggregates)

These fields exist on the **yearly summaries** returned by the API.

- `averageCapital`: Robust average capital for the year.
- `medianCapital`: Median capital for the year.
- `minCapital`: Minimum observed capital for the year.
- `maxCapital`: Maximum observed capital for the year.
- `stdDevCapital`: Standard deviation of capital for the year.
- `cumulativeGrowthRate`: Cumulative growth rate over the year (dimensionless ratio).
- `quantile5`: 5th percentile of capital for the year.
- `quantile25`: 25th percentile of capital for the year.
- `quantile75`: 75th percentile of capital for the year.
- `quantile95`: 95th percentile of capital for the year.
- `var`: Value-at-Risk (VaR) at 5% (equal to `quantile5`).
- `cvar`: Conditional VaR (average of values at/below VaR).
- `negativeCapitalPercentage`: Share of runs that failed (capital <= 0 after the failure point).
- `percentiles`: 101-point grid: p0..p100 at 1% increments (so p0,p5,…,p100 are included as a subset).

## Accounting integrity

- `y-return`: Yearly return delta between consecutive snapshots.
- `y-withdraw`: Yearly withdrawal delta between consecutive snapshots.
- `y-tax`: Yearly tax delta between consecutive snapshots.
- `y-net`: Yearly net delta between consecutive snapshots.

## Deep-dive trace / indexing

- `phase`: Phase name at snapshot time.
- `day`: Days since simulation start (0-based).
- `month`: Months since simulation start (0-based).
- `year`: Years since simulation start (0-based).
- `date`: ISO date label for the snapshot.
