CREATE TABLE IF NOT EXISTS simulation_run (
                                              id          varchar(36) PRIMARY KEY,
    input_json  text NOT NULL,
    input_hash  varchar(64) NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_simrun_input_hash ON simulation_run(input_hash);

CREATE TABLE IF NOT EXISTS yearly_summary (
                                              id                         varchar(36) PRIMARY KEY,
    run_id                     varchar(36) NOT NULL REFERENCES simulation_run(id) ON DELETE CASCADE,
    phase_name                 varchar(255) NOT NULL,
    year                       int NOT NULL,
    average_capital            double precision,
    median_capital             double precision,
    min_capital                double precision,
    max_capital                double precision,
    stddev_capital             double precision,
    cumulative_growth_rate     double precision,
    quantile5                  double precision,
    quantile25                 double precision,
    quantile75                 double precision,
    quantile95                 double precision,
    var_value                  double precision,
    cvar_value                 double precision,
    neg_capital_pct            double precision,
    percentiles                double precision[] NOT NULL,
    CONSTRAINT uk_summary_run_phase_year UNIQUE (run_id, phase_name, year)
    );

CREATE INDEX IF NOT EXISTS idx_summary_run ON yearly_summary(run_id);
CREATE INDEX IF NOT EXISTS idx_summary_phase_year ON yearly_summary(phase_name, year);
