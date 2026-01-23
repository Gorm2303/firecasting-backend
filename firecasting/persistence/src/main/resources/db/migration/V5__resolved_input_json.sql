-- Add resolved_input_json column to store the fully resolved AdvancedSimulationRequest with all defaults applied
ALTER TABLE simulation_run
    ADD COLUMN IF NOT EXISTS resolved_input_json jsonb;

-- Create index for performance (optional, but helpful if we later query on resolved data)
CREATE INDEX IF NOT EXISTS idx_simrun_resolved_input_json ON simulation_run USING gin(resolved_input_json);
