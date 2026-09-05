-- Adds a distinct landslide/mass-movement signal (separate from ordinary
-- tectonic earthquake hazard) plus explicit rainfall/melt condition flags,
-- so a single strong direct signal (e.g. a landslide detected right next
-- to a lake) can't get diluted away by an averaged composite score.

ALTER TABLE glof_risk_assessments ADD COLUMN landslide_component DOUBLE PRECISION;
ALTER TABLE glof_risk_assessments ADD COLUMN landslide_detected BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE glof_risk_assessments ADD COLUMN rainfall_condition VARCHAR(20);
ALTER TABLE glof_risk_assessments ADD COLUMN melt_condition BOOLEAN NOT NULL DEFAULT FALSE;
