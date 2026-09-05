-- Backfill source_type for rows saved before that column existed. The
-- description text already carries USGS's classification verbatim
-- (e.g. "M 5.2 Landslide - ..."), so it can be recovered directly.

UPDATE hazard_events
SET source_type = 'landslide'
WHERE source_type IS NULL AND description ILIKE '%Landslide%';

UPDATE hazard_events
SET source_type = 'earthquake'
WHERE source_type IS NULL AND event_type = 'EARTHQUAKE';
