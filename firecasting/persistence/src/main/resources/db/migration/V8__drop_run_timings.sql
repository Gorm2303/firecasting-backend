-- Timings are runtime-only metadata and must not be persisted.
-- Drop the previously added timing columns from the simulation_run table.

ALTER TABLE simulation_run
    DROP COLUMN IF EXISTS compute_ms,
    DROP COLUMN IF EXISTS aggregate_ms,
    DROP COLUMN IF EXISTS grids_ms,
    DROP COLUMN IF EXISTS persist_ms,
    DROP COLUMN IF EXISTS total_ms;
