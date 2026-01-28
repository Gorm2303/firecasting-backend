CREATE TABLE IF NOT EXISTS metric_summary (
  id varchar(36) PRIMARY KEY,
  run_id varchar(36) NOT NULL,
  scope varchar(32) NOT NULL,
  phase_name varchar(255) NULL,
  year integer NULL,
  metric varchar(128) NOT NULL,
  p5 double precision NOT NULL,
  p10 double precision NOT NULL,
  p25 double precision NOT NULL,
  p50 double precision NOT NULL,
  p75 double precision NOT NULL,
  p90 double precision NOT NULL,
  p95 double precision NOT NULL,
  CONSTRAINT fk_metric_summary_run FOREIGN KEY (run_id) REFERENCES simulation_run(id) ON DELETE CASCADE,
  CONSTRAINT uk_metric_summary_run_scope_phase_year_metric UNIQUE (run_id, scope, phase_name, year, metric)
);

CREATE INDEX IF NOT EXISTS idx_metric_summary_run ON metric_summary(run_id);
CREATE INDEX IF NOT EXISTS idx_metric_summary_scope ON metric_summary(scope);
CREATE INDEX IF NOT EXISTS idx_metric_summary_phase_year ON metric_summary(phase_name, year);
