-- The data_sources table only ever held static, hand-seeded "ACTIVE" rows
-- that were never refreshed from anything real - a decorative panel, not a
-- live data source registry. Nothing else references it (no FK).
DROP TABLE data_sources;
