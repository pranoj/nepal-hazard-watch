-- Denormalized onto the assessment (like lakeName/icimodId already are) so
-- the frontend can look up downstream towns for an at-risk lake without an
-- extra join.
ALTER TABLE glof_risk_assessments ADD COLUMN river_basin VARCHAR(255);
