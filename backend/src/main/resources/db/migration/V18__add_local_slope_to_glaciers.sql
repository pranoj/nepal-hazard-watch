-- RGI's slope_deg is a whole-glacier mean, not the slope right at the
-- terminus where a collapse would actually start. This holds a locally
-- computed slope from real elevation samples near the terminus point,
-- filled in by a one-time background job. Null until that job succeeds -
-- the risk calculation falls back to the RGI mean slope until then.
ALTER TABLE glaciers ADD COLUMN local_slope_deg DOUBLE PRECISION;
