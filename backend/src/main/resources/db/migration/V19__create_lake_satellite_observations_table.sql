-- Real Sentinel-2 NDWI-derived water-fraction readings per lake, collected
-- on their own schedule (roughly every 5 days, matching Sentinel-2's real
-- revisit cadence over Nepal) - independent from the 30-minute risk
-- recalculation, which just reads the latest rows here.
CREATE TABLE lake_satellite_observations (
    id BIGSERIAL PRIMARY KEY,
    icimod_id VARCHAR(255) NOT NULL,
    observed_at TIMESTAMP NOT NULL,
    water_fraction DOUBLE PRECISION NOT NULL,
    mean_ndwi DOUBLE PRECISION,
    valid_pixel_fraction DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_lake_satellite_observations_icimod_id
    ON lake_satellite_observations (icimod_id, observed_at DESC);
