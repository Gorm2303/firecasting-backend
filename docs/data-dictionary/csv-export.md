# CSV export

This file is the source of truth for what each exported CSV column means.

See also:

- `docs/data-dictionary/glossary.md` (tooltip-ready definitions, provenance, storage semantics)
- `docs/invariants/timing-model.md` (timing conventions and order of operations)
- `docs/data-dictionary/metrics-taxonomy.md` (metric grouping)

## Snapshot cadence

CSV rows are produced from **engine snapshots**. A run typically records snapshots at:

- **year end** (after year-end operations), and
- **phase end** (the final state of the phase, even if mid-year)

This means some rows represent year-end state and others represent a mid-year phase boundary.

## Column dictionary

Add one entry per column, matching the exact header name.

For every entry, document:

- Meaning
- Unit
- Timing convention
- Phase applicability

- `phase`: Phase name at snapshot time.
	- Unit: string.
	- Timing: snapshot-time label.
	- Applies: all phases.

- `day`: Days since simulation start.
	- Unit: days (integer, 0-based).
	- Timing: snapshot-time label.
	- Applies: all phases.

- `month`: Months since simulation start.
	- Unit: months (integer, 0-based).
	- Timing: snapshot-time label.
	- Applies: all phases.

- `year`: Years since simulation start.
	- Unit: years (integer, 0-based).
	- Timing: snapshot-time label.
	- Applies: all phases.

- `date`: Date label for the snapshot.
	- Unit: ISO date string (`YYYY-MM-DD`).
	- Timing: snapshot-time label.
	- Applies: all phases.

- `capital`: Portfolio value after all operations up to the snapshot.
	- Unit: currency (same unit as deposits/withdrawals).
	- Timing: value *at snapshot time*.
	- Applies: all phases.

- `deposited`: Cumulative deposits applied.
	- Unit: currency.
	- Timing: cumulative total at snapshot time.
	- Applies: deposit phases primarily; may stay constant in other phases.

- `deposit`: Deposit applied in the latest deposit event.
	- Unit: currency.
	- Timing: month-end deposit amount (deposit phases apply deposit at month end).
	- Applies: deposit phases; typically 0 in other phases.

- `passive`: Cumulative passive flow.
	- Unit: currency.
	- Timing: cumulative total at snapshot time.
	- Applies: passive phases primarily.

- `returned`: Cumulative returns applied (net of notional tax if notional tax is enabled).
	- Unit: currency.
	- Timing: cumulative total at snapshot time.
	- Applies: all phases with return enabled.

- `return`: Return applied in the latest return event.
	- Unit: currency.
	- Timing: depends on return step:
		- daily returns apply at day end (trading days)
		- monthly returns apply at month end
	- Applies: all phases with return enabled.

- `withdrawn`: Cumulative withdrawals applied.
	- Unit: currency.
	- Timing: cumulative total at snapshot time.
	- Applies: withdraw phases primarily.

- `withdraw`: Withdrawal applied in the latest withdrawal event.
	- Unit: currency.
	- Timing: month-end withdrawal amount.
	- Applies: withdraw phases; typically 0 in other phases.

- `taxed`: Cumulative taxes applied.
	- Unit: currency.
	- Timing: cumulative total at snapshot time.
	- Applies: withdraw phases (capital gains tax) and/or year end (notional tax).

- `tax`: Tax applied in the latest tax event.
	- Unit: currency.
	- Timing:
		- capital gains tax is applied at month end during withdraw phases
		- notional tax (if enabled) is applied at year end
	- Applies: withdraw phases and/or year end.

- `fees`: Cumulative fees applied.
	- Unit: currency.
	- Timing: cumulative total at snapshot time.
	- Applies: any phase with a yearly fee configured.

- `fee`: Fee applied in the latest fee event.
	- Unit: currency.
	- Timing: year-end fee amount.
	- Applies: any phase with a yearly fee configured.

- `inflation`: Cumulative inflation multiplier.
	- Unit: multiplier (1.0 means no inflation applied).
	- Timing: compounded at year end.
	- Applies: all phases (if inflation is enabled).

- `nettotal`: Cumulative net earnings.
	- Unit: currency.
	- Timing: cumulative total at snapshot time.
	- Applies: withdraw phases (net earnings are recorded when withdrawals occur).

- `net`: Net earnings applied in the latest net-earnings event.
	- Unit: currency.
	- Timing: month-end net earnings in withdraw phases (after tax).
	- Applies: withdraw phases.

- `y-return`: Yearly return delta (difference from previous snapshot).
	- Unit: currency.
	- Timing: snapshot-to-snapshot delta; meaningful when snapshots are at year boundaries.
	- Applies: all phases.

- `y-withdraw`: Yearly withdrawal delta (difference from previous snapshot).
	- Unit: currency.
	- Timing: snapshot-to-snapshot delta; meaningful when snapshots are at year boundaries.
	- Applies: all phases.

- `y-tax`: Yearly tax delta (difference from previous snapshot).
	- Unit: currency.
	- Timing: snapshot-to-snapshot delta; meaningful when snapshots are at year boundaries.
	- Applies: all phases.

- `y-net`: Yearly net delta (difference from previous snapshot).
	- Unit: currency.
	- Timing: snapshot-to-snapshot delta; meaningful when snapshots are at year boundaries.
	- Applies: all phases.
