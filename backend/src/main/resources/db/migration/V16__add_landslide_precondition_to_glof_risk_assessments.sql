-- Predictive signal (steep terrain + sustained heavy rain), distinct from
-- landslide_detected (an actual USGS-observed event). Lets the alert floor
-- react to dangerous ground conditions before any event has occurred.
ALTER TABLE glof_risk_assessments ADD COLUMN landslide_pre_condition BOOLEAN NOT NULL DEFAULT FALSE;
