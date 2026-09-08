-- Real Sentinel-2 NDSI-derived ice/snow-cover readings per glacier terminus,
-- collected on the same ~5-day cadence as lake_satellite_observations.
-- Baseline is intentionally "today" for each glacier (no historical
-- backfill) - a glacier collapse is a sudden event, not a slow multi-year
-- trend, so there's no equivalent value in reaching into the archive the
-- way the lake growth comparison does.
CREATE TABLE glacier_satellite_observations (
    id BIGSERIAL PRIMARY KEY,
    rgi_id VARCHAR(255) NOT NULL,
    observed_at TIMESTAMP NOT NULL,
    ice_fraction DOUBLE PRECISION NOT NULL,
    mean_ndsi DOUBLE PRECISION,
    valid_pixel_fraction DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_glacier_satellite_observations_rgi_id
    ON glacier_satellite_observations (rgi_id, observed_at DESC);
