CREATE TABLE regions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE,
    name VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    risk_level VARCHAR(50),
    population INTEGER
);

CREATE TABLE data_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    organization VARCHAR(255),
    data_type VARCHAR(100),
    current_status VARCHAR(50),
    last_successful_fetch TIMESTAMP,
    api_url VARCHAR(500),
    update_frequency VARCHAR(100),
    description TEXT
);

CREATE TABLE hazard_events (
    id BIGSERIAL PRIMARY KEY,
    region_id BIGINT NOT NULL REFERENCES regions(id),
    risk_level VARCHAR(50),
    event_type VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    event_time TIMESTAMP NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    magnitude DOUBLE PRECISION,
    description TEXT,
    death_toll INTEGER
);

CREATE INDEX idx_hazard_events_status ON hazard_events (status);
CREATE INDEX idx_hazard_events_region_id ON hazard_events (region_id);
