ALTER TABLE simulation_run
ALTER COLUMN input_json TYPE jsonb
  USING input_json::jsonb;
CREATE INDEX IF NOT EXISTS idx_simrun_input_json_gin
    ON simulation_run USING GIN (input_json);
