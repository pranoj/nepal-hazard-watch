-- Curated reference table: which towns/cities sit downstream on each river
-- basin a monitored lake drains into. A simple static lookup rather than
-- computed GIS flow-routing (deferred - see roadmap) - accurate enough to
-- answer "who should be warned" without a full hydrological pipeline.

CREATE TABLE river_basin_towns (
    id BIGSERIAL PRIMARY KEY,
    river_basin VARCHAR(255) NOT NULL,
    town_name VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    downstream_order INTEGER NOT NULL
);

CREATE INDEX idx_river_basin_towns_basin ON river_basin_towns (river_basin);
