ALTER TABLE simulation_run
    ADD COLUMN IF NOT EXISTS compute_ms bigint,
    ADD COLUMN IF NOT EXISTS aggregate_ms bigint,
    ADD COLUMN IF NOT EXISTS grids_ms bigint,
    ADD COLUMN IF NOT EXISTS persist_ms bigint,
    ADD COLUMN IF NOT EXISTS total_ms bigint;
