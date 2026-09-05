-- Generalizes glof_risk_assessments to cover glacier watch points
-- ("Type B") alongside existing glacial lakes ("Type A"). glacial_lake_id
-- stays as-is for lake rows; glacier_id is used for glacier rows instead.
-- Exactly one of the two is set per row (enforced in application code).

ALTER TABLE glof_risk_assessments ALTER COLUMN glacial_lake_id DROP NOT NULL;
ALTER TABLE glof_risk_assessments ADD COLUMN glacier_id BIGINT UNIQUE REFERENCES glaciers(id);
ALTER TABLE glof_risk_assessments ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT 'LAKE';
