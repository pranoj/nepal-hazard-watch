-- Distinguishes glaciers sharing the same river basin by their actual
-- nearest landmark (e.g. "Langtang Village"), instead of every glacier in
-- a basin getting an identical generic name.
ALTER TABLE glaciers ADD COLUMN nearest_town_name VARCHAR(255);
