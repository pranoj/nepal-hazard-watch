-- Diagnostic satellite fields on the risk assessment. Null/false until a
-- lake has at least one real satellite observation. Growth escalation is
-- gated by satellite.escalation.enabled (default false) - see
-- GLOFRiskCalculationService - so these can be visible and logged before
-- they're trusted enough to actually raise an alert.
ALTER TABLE glof_risk_assessments ADD COLUMN satellite_water_fraction DOUBLE PRECISION;
ALTER TABLE glof_risk_assessments ADD COLUMN satellite_lake_growth_detected BOOLEAN NOT NULL DEFAULT false;
