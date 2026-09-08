ALTER TABLE glof_risk_assessments
    ADD COLUMN satellite_ice_fraction DOUBLE PRECISION,
    ADD COLUMN satellite_ice_sudden_drop_detected BOOLEAN NOT NULL DEFAULT false;
