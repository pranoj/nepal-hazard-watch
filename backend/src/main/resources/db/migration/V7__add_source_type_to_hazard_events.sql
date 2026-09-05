-- USGS classifies seismic-network detections beyond tectonic earthquakes,
-- notably "landslide" - a real detection of mass movement (e.g. an
-- ice/rock avalanche), which is a much stronger glacier-collapse signal
-- than ordinary shaking. Previously discarded; now captured.

ALTER TABLE hazard_events ADD COLUMN source_type VARCHAR(50);
