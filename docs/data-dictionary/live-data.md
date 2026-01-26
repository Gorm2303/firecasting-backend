# Live data

This documents the live simulation stream/state returned by the API.

See also:
- `glossary.md` for tooltip-ready definitions.
- `csv-export.md` for exported column meaning.
- `resolutions-and-percentiles.md` for supported resolutions and percentile sets.
- `docs/invariants/timing-model.md` for the order of operations.

## Notes

- Live data uses the same semantic fields as CSV export (capital, deposited, returned, tax, fee, etc).
- Timing conventions (month-end vs year-end) matter for interpretation.
	- Month-end events include deposits/withdrawals (and monthly return when configured).
	- Year-end events include notional tax (if enabled), inflation compounding, and yearly fees.
- Fees are applied at year end.
