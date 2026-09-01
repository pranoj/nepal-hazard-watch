CREATE TABLE regions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE data_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    organization VARCHAR(255),
    data_type VARCHAR(100),
    current_status VARCHAR(50),
    last_successful_fetch TIMESTAMPTZ
);

CREATE TABLE hazard_events (
    id BIGSERIAL PRIMARY KEY,
    region_id BIGINT NOT NULL REFERENCES regions(id),
    risk_level VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    first_detected_at TIMESTAMPTZ NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION
);

CREATE INDEX idx_hazard_events_status ON hazard_events (status);
CREATE INDEX idx_hazard_events_region_id ON hazard_events (region_id);
