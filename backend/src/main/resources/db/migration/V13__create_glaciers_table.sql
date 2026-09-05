-- "Type B" watch points: glaciers with a steep terminus near a known river
-- corridor, capable of collapsing and damming a river directly (as
-- happened at Langtang Lirung on Aug 2026) - distinct from the existing
-- glacial_lakes table, which only covers already-formed, named lakes.
-- Source: RGI (Randolph Glacier Inventory) v7, regions 14+15.

CREATE TABLE glaciers (
    id BIGSERIAL PRIMARY KEY,
    rgi_id VARCHAR(255) NOT NULL UNIQUE,
    glacier_name VARCHAR(255),
    terminus_latitude DOUBLE PRECISION NOT NULL,
    terminus_longitude DOUBLE PRECISION NOT NULL,
    slope_deg DOUBLE PRECISION NOT NULL,
    area_km2 DOUBLE PRECISION,
    nearest_river_basin VARCHAR(255),
    nearest_river_town_km DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_glaciers_river_basin ON glaciers (nearest_river_basin);
