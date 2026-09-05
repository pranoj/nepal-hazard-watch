-- River basin is the join key for mapping which downstream towns/corridors
-- a lake's water flows through. Previously only used transiently for lake
-- naming, never persisted as structured data.

ALTER TABLE glacial_lakes ADD COLUMN river_basin VARCHAR(255);
