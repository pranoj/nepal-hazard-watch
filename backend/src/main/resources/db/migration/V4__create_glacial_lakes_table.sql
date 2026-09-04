-- Create Glacial Lakes Table
-- Data Source: ICIMOD (International Centre for Integrated Mountain Development)
-- License: CC BY 4.0 (Creative Commons Attribution 4.0 International)
-- Attribution: Data sourced from ICIMOD Glacial Lakes Inventory
-- Reference: https://www.icimod.org/

CREATE TABLE IF NOT EXISTS glacial_lakes (
    id BIGSERIAL PRIMARY KEY,
    icimod_id VARCHAR(255) NOT NULL UNIQUE,
    lake_name VARCHAR(255),
    glacier_name VARCHAR(255),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    elevation DOUBLE PRECISION,
    surface_area_km2 DOUBLE PRECISION,
    country VARCHAR(255),
    risk_level VARCHAR(255),
    last_updated TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create indexes for efficient queries
-- Index for ICIMOD ID lookups (syncing/updating data)
CREATE INDEX idx_icimod_id ON glacial_lakes(icimod_id);

-- Index for location-based queries (nearest-neighbor searches for GLOF risk)
CREATE INDEX idx_location ON glacial_lakes(latitude, longitude);

-- Index for risk level filtering
CREATE INDEX idx_risk_level ON glacial_lakes(risk_level);

-- Index for country-based queries
CREATE INDEX idx_country ON glacial_lakes(country);