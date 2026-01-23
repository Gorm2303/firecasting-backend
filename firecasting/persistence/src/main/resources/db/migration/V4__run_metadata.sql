ALTER TABLE simulation_run
    ADD COLUMN IF NOT EXISTS model_app_version varchar(128),
    ADD COLUMN IF NOT EXISTS model_build_time varchar(64),
    ADD COLUMN IF NOT EXISTS model_spring_boot_version varchar(64),
    ADD COLUMN IF NOT EXISTS model_java_version varchar(64),
    ADD COLUMN IF NOT EXISTS rng_seed bigint;
