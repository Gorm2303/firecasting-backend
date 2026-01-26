# Timing model

This document defines the timing conventions used by the simulation engine.
It exists to prevent accidental semantic drift when compounding, fees, taxes, and withdrawals change.

## Time base

- The engine advances in **epoch days**.
- A phase has a `startDate` and a `duration` measured in days.
- `day`, `month`, `year`, and `date` in CSV/live data are derived from the simulation start date + days elapsed.

## Events and order of operations

### Phase start

- The phase name is set on the live state.
- Deposit phases apply the **initial deposit** on phase start.

### Day end

- If return step is `DAILY` and the day is a trading day, return is applied.
- Passive phases compute passive flow at day end.

### Month end

- If return step is `MONTHLY`, the month’s return is applied at month end.
- The return model may perform a month-end update (`onMonthEnd`) for regime switching.
- Phase-specific month-end actions run after the return step:
  - Deposit phase applies the monthly deposit.
  - Withdraw phase applies withdrawal, then capital-gains tax (if applicable), then net earnings.

### Year start

- Tax exemption rules perform a yearly update.

### Year end

Year-end operations are applied in this order:

1. Notional tax (only when the overall tax rule is notional).
2. Inflation compounding (inflation multiplier is updated once per year).
3. Yearly fee deduction (absolute fee amount is deducted from capital).
4. Deposit phases then update (increase) their deposit schedule for the next year.

## Snapshot cadence

- For call-based engines, snapshots are taken at **year end** and again at **phase end**.
- A phase can end mid-year; in that case the final snapshot is *not* a year-end snapshot.

See also:
- `docs/data-dictionary/csv-export.md` (column semantics)
- `docs/data-dictionary/resolutions-and-percentiles.md` (what resolutions exist)
