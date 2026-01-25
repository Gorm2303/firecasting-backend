# CSV export

This file is the source of truth for what each exported CSV column means.

## Column dictionary

Add one entry per column, matching the exact header name.

- `phase`: Simulation phase.
- `day`: Day counter within the simulation timeline.
- `month`: Month counter within the simulation timeline.
- `year`: Year counter within the simulation timeline.
- `date`: Date label for the snapshot.
- `capital`: Portfolio value.
- `deposited`: Cumulative deposits.
- `deposit`: Deposit applied at this step.
- `passive`: Passive income / passive flow at this step.
- `returned`: Cumulative returns.
- `return`: Return applied at this step.
- `withdrawn`: Cumulative withdrawals.
- `withdraw`: Withdrawal applied at this step.
- `taxed`: Cumulative taxes.
- `tax`: Tax applied at this step.
- `fees`: Cumulative fees.
- `fee`: Fee applied at this step.
- `inflation`: Inflation applied at this step.
- `nettotal`: Cumulative net total.
- `net`: Net applied at this step.
- `y-return`: Yearly return delta.
- `y-withdraw`: Yearly withdrawal delta.
- `y-tax`: Yearly tax delta.
- `y-net`: Yearly net delta.

If you add/remove a CSV column, update this file and regenerate the drift guard.
