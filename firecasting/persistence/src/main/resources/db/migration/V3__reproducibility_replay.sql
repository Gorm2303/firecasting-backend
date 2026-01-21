CREATE TABLE IF NOT EXISTS reproducibility_replay (
    id              varchar(36) PRIMARY KEY,
    created_at      timestamptz NOT NULL DEFAULT now(),
    status          varchar(32) NOT NULL,
    source_app_version  varchar(128),
    current_app_version varchar(128),
    bundle_json     jsonb NOT NULL,
    replay_run_id   varchar(36),
    report_json     jsonb,

    CONSTRAINT fk_replay_run
        FOREIGN KEY (replay_run_id)
        REFERENCES simulation_run(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_replay_created_at ON reproducibility_replay(created_at);
CREATE INDEX IF NOT EXISTS idx_replay_run_id ON reproducibility_replay(replay_run_id);
