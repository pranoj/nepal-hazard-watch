-- Lakes physically outside Nepal (e.g. Tibet/China) that ICIMOD flags as
-- transboundary can still flood Nepal directly (e.g. the Aug 2026
-- Kyirong-Rasuwa GLOF originated from a lake just across the border).
-- Track this so such lakes can be included in Nepal monitoring alongside
-- lakes physically inside Nepal.

ALTER TABLE glacial_lakes ADD COLUMN transboundary BOOLEAN NOT NULL DEFAULT FALSE;
