-- Persisted GLOF risk assessment per glacial lake, recomputed on a schedule
-- from real weather + earthquake + lake-type data (no satellite data yet).

CREATE TABLE glof_risk_assessments (
    id BIGSERIAL PRIMARY KEY,
    glacial_lake_id BIGINT NOT NULL UNIQUE REFERENCES glacial_lakes(id),
    lake_name VARCHAR(255),
    icimod_id VARCHAR(255),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    risk_score DOUBLE PRECISION NOT NULL,
    alert_level VARCHAR(20) NOT NULL,
    rainfall_component DOUBLE PRECISION,
    earthquake_component DOUBLE PRECISION,
    lake_type_component DOUBLE PRECISION,
    seasonal_component DOUBLE PRECISION,
    nearest_earthquake_id BIGINT,
    assessed_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_glof_risk_alert_level ON glof_risk_assessments (alert_level);
CREATE INDEX idx_glof_risk_score ON glof_risk_assessments (risk_score);
