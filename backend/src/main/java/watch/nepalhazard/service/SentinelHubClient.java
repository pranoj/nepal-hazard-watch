package watch.nepalhazard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Sentinel Hub (Copernicus Data Space Ecosystem) client used by
 * SatelliteLakeTrackingService and SatelliteGlacierTrackingService to pull
 * real NDWI water-fraction and NDSI ice-fraction readings for each
 * monitored point.
 */
@Slf4j
@Component
public class SentinelHubClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${satellite.sentinel-hub.client-id}")
    private String clientId;

    @Value("${satellite.sentinel-hub.client-secret}")
    private String clientSecret;

    @Value("${satellite.sentinel-hub.token-url}")
    private String tokenUrl;

    @Value("${satellite.sentinel-hub.statistics-url}")
    private String statisticsUrl;

    private static final int MAX_RATE_LIMIT_RETRIES = 3;
    private static final long RATE_LIMIT_BACKOFF_MS = 5000;

    private String cachedToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public SentinelHubClient(@Value("${satellite.sentinel-hub.timeout-seconds:20}") int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * NDWI-derived water fraction for a small box around (lat, lon), over
     * [from, to). Real live call to the Statistics API - no caching of the
     * result itself, this is a manual test path, not a scheduled job.
     */
    public LakeWaterReading fetchWaterFraction(double lat, double lon, double halfWidthDeg, LocalDate from,
            LocalDate to) {
        String token = getAccessToken();

        double minLon = lon - halfWidthDeg;
        double maxLon = lon + halfWidthDeg;
        double minLat = lat - halfWidthDeg;
        double maxLat = lat + halfWidthDeg;

        String requestBody = buildRequestBody(minLon, minLat, maxLon, maxLat, from, to);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        log.info("Calling Sentinel Hub Statistics API for bbox [{}, {}, {}, {}] from {} to {}",
                minLon, minLat, maxLon, maxLat, from, to);

        String body = executeWithRetry(entity);
        return parseResponse(body);
    }

    /**
     * NDSI-derived snow/ice fraction for a small box around a glacier
     * terminus, over [from, to). Same Statistics API, same OAuth/retry
     * plumbing as fetchWaterFraction - only the evalscript and output field
     * names differ. See SatelliteGlacierTrackingService.
     */
    public IceCoverReading fetchIceFraction(double lat, double lon, double halfWidthDeg, LocalDate from,
            LocalDate to) {
        String token = getAccessToken();

        double minLon = lon - halfWidthDeg;
        double maxLon = lon + halfWidthDeg;
        double minLat = lat - halfWidthDeg;
        double maxLat = lat + halfWidthDeg;

        String requestBody = buildIceRequestBody(minLon, minLat, maxLon, maxLat, from, to);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        log.info("Calling Sentinel Hub Statistics API (ice) for bbox [{}, {}, {}, {}] from {} to {}",
                minLon, minLat, maxLon, maxLat, from, to);

        String body = executeWithRetry(entity);
        return parseIceResponse(body);
    }

    private String executeWithRetry(HttpEntity<String> entity) {
        for (int attempt = 1; attempt <= MAX_RATE_LIMIT_RETRIES; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(statisticsUrl, HttpMethod.POST, entity,
                        String.class);
                return response.getBody();
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == MAX_RATE_LIMIT_RETRIES) {
                    throw e;
                }
                long backoffMs = RATE_LIMIT_BACKOFF_MS * attempt;
                log.warn("Rate limited by Sentinel Hub (attempt {}/{}), backing off {}ms",
                        attempt, MAX_RATE_LIMIT_RETRIES, backoffMs);
                sleepQuietly(backoffMs);
            }
        }
        throw new IllegalStateException("Unreachable: retry loop must return or throw");
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, entity, String.class);

        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            cachedToken = json.path("access_token").asText();
            int expiresInSeconds = json.path("expires_in").asInt(600);
            // Refresh a bit early rather than cutting it exactly at expiry.
            tokenExpiresAt = Instant.now().plusSeconds(Math.max(30, expiresInSeconds - 30));
            log.info("Obtained Sentinel Hub access token, valid ~{}s", expiresInSeconds);
            return cachedToken;
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse Sentinel Hub token response: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(double minLon, double minLat, double maxLon, double maxLat, LocalDate from,
            LocalDate to) {
        String fromIso = from.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
        String toIso = to.atStartOfDay(ZoneOffset.UTC).toInstant().toString();

        String evalscript = ""
                + "//VERSION=3\n"
                + "function setup() {\n"
                + "  return {\n"
                + "    input: [{ bands: [\"B03\", \"B08\", \"SCL\", \"dataMask\"] }],\n"
                + "    output: [\n"
                + "      { id: \"ndwi\", bands: 1, sampleType: \"FLOAT32\" },\n"
                + "      { id: \"water\", bands: 1, sampleType: \"FLOAT32\" },\n"
                + "      { id: \"dataMask\", bands: 1 }\n"
                + "    ]\n"
                + "  };\n"
                + "}\n"
                + "function evaluatePixel(samples) {\n"
                + "  let ndwi = (samples.B03 - samples.B08) / (samples.B03 + samples.B08 + 1e-9);\n"
                + "  let isWater = ndwi > 0.1 ? 1 : 0;\n"
                + "  let validScl = samples.SCL != 0 && samples.SCL != 3 && samples.SCL != 8\n"
                + "      && samples.SCL != 9 && samples.SCL != 10;\n"
                + "  let mask = samples.dataMask * (validScl ? 1 : 0);\n"
                + "  return { ndwi: [ndwi], water: [isWater], dataMask: [mask] };\n"
                + "}";

        // Escaped as a JSON string value inside the request body below.
        String escapedEvalscript = evalscript.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");

        return "{"
                + "\"input\":{"
                + "\"bounds\":{"
                + "\"bbox\":[" + minLon + "," + minLat + "," + maxLon + "," + maxLat + "],"
                + "\"properties\":{\"crs\":\"http://www.opengis.net/def/crs/OGC/1.3/CRS84\"}"
                + "},"
                + "\"data\":[{\"type\":\"sentinel-2-l2a\",\"dataFilter\":{\"mosaickingOrder\":\"leastCC\"}}]"
                + "},"
                + "\"aggregation\":{"
                + "\"timeRange\":{\"from\":\"" + fromIso + "\",\"to\":\"" + toIso + "\"},"
                + "\"aggregationInterval\":{\"of\":\"P5D\"},"
                + "\"evalscript\":\"" + escapedEvalscript + "\","
                + "\"width\":200,\"height\":200"
                + "},"
                + "\"calculations\":{\"default\":{}}"
                + "}";
    }

    private String buildIceRequestBody(double minLon, double minLat, double maxLon, double maxLat, LocalDate from,
            LocalDate to) {
        String fromIso = from.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
        String toIso = to.atStartOfDay(ZoneOffset.UTC).toInstant().toString();

        // NDSI (Normalized Difference Snow Index) = (green - SWIR1) / (green + SWIR1),
        // using Sentinel-2 B03 (10m) and B11 (20m, auto-resampled by the API to
        // match the requested output size). NDSI > 0.4 is the standard snow/ice
        // classification threshold (Hall et al., used operationally by USGS/MODIS
        // snow-cover products) - same cloud/shadow/cirrus SCL mask as the water
        // evalscript above.
        String evalscript = ""
                + "//VERSION=3\n"
                + "function setup() {\n"
                + "  return {\n"
                + "    input: [{ bands: [\"B03\", \"B11\", \"SCL\", \"dataMask\"] }],\n"
                + "    output: [\n"
                + "      { id: \"ndsi\", bands: 1, sampleType: \"FLOAT32\" },\n"
                + "      { id: \"ice\", bands: 1, sampleType: \"FLOAT32\" },\n"
                + "      { id: \"dataMask\", bands: 1 }\n"
                + "    ]\n"
                + "  };\n"
                + "}\n"
                + "function evaluatePixel(samples) {\n"
                + "  let ndsi = (samples.B03 - samples.B11) / (samples.B03 + samples.B11 + 1e-9);\n"
                + "  let isIce = ndsi > 0.4 ? 1 : 0;\n"
                + "  let validScl = samples.SCL != 0 && samples.SCL != 3 && samples.SCL != 8\n"
                + "      && samples.SCL != 9 && samples.SCL != 10;\n"
                + "  let mask = samples.dataMask * (validScl ? 1 : 0);\n"
                + "  return { ndsi: [ndsi], ice: [isIce], dataMask: [mask] };\n"
                + "}";

        String escapedEvalscript = evalscript.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");

        return "{"
                + "\"input\":{"
                + "\"bounds\":{"
                + "\"bbox\":[" + minLon + "," + minLat + "," + maxLon + "," + maxLat + "],"
                + "\"properties\":{\"crs\":\"http://www.opengis.net/def/crs/OGC/1.3/CRS84\"}"
                + "},"
                + "\"data\":[{\"type\":\"sentinel-2-l2a\",\"dataFilter\":{\"mosaickingOrder\":\"leastCC\"}}]"
                + "},"
                + "\"aggregation\":{"
                + "\"timeRange\":{\"from\":\"" + fromIso + "\",\"to\":\"" + toIso + "\"},"
                + "\"aggregationInterval\":{\"of\":\"P5D\"},"
                + "\"evalscript\":\"" + escapedEvalscript + "\","
                + "\"width\":200,\"height\":200"
                + "},"
                + "\"calculations\":{\"default\":{}}"
                + "}";
    }

    /** Package-private so the parsing logic can be unit tested with a synthetic response, no live call needed. */
    IceCoverReading parseIceResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");

            if (!data.isArray() || data.isEmpty()) {
                return IceCoverReading.empty(body);
            }

            for (int i = data.size() - 1; i >= 0; i--) {
                JsonNode outputs = data.get(i).path("outputs");
                JsonNode ndsiStats = outputs.path("ndsi").path("bands").path("B0").path("stats");
                JsonNode iceStats = outputs.path("ice").path("bands").path("B0").path("stats");

                int sampleCount = ndsiStats.path("sampleCount").asInt(0);
                int noDataCount = ndsiStats.path("noDataCount").asInt(0);
                int validPixels = sampleCount - noDataCount;

                if (validPixels > 0) {
                    return new IceCoverReading(
                            data.get(i).path("interval").path("from").asText(),
                            data.get(i).path("interval").path("to").asText(),
                            iceStats.path("mean").asDouble(Double.NaN),
                            ndsiStats.path("mean").asDouble(Double.NaN),
                            sampleCount,
                            validPixels,
                            null);
                }
            }

            return IceCoverReading.empty(body);

        } catch (Exception e) {
            log.error("Could not parse Statistics API (ice) response: {}", e.getMessage());
            return IceCoverReading.empty(body);
        }
    }

    /** Package-private so the parsing logic can be unit tested with a synthetic response, no live call needed. */
    LakeWaterReading parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");

            if (!data.isArray() || data.isEmpty()) {
                return LakeWaterReading.empty(body);
            }

            // Most recent interval with actual valid pixels, not just the
            // last one in the array (some intervals may have zero coverage
            // due to clouds).
            for (int i = data.size() - 1; i >= 0; i--) {
                JsonNode outputs = data.get(i).path("outputs");
                JsonNode ndwiStats = outputs.path("ndwi").path("bands").path("B0").path("stats");
                JsonNode waterStats = outputs.path("water").path("bands").path("B0").path("stats");

                int sampleCount = ndwiStats.path("sampleCount").asInt(0);
                int noDataCount = ndwiStats.path("noDataCount").asInt(0);
                int validPixels = sampleCount - noDataCount;

                if (validPixels > 0) {
                    return new LakeWaterReading(
                            data.get(i).path("interval").path("from").asText(),
                            data.get(i).path("interval").path("to").asText(),
                            waterStats.path("mean").asDouble(Double.NaN),
                            ndwiStats.path("mean").asDouble(Double.NaN),
                            sampleCount,
                            validPixels,
                            null);
                }
            }

            return LakeWaterReading.empty(body);

        } catch (Exception e) {
            log.error("Could not parse Statistics API response: {}", e.getMessage());
            return LakeWaterReading.empty(body);
        }
    }

    public record LakeWaterReading(
            String intervalFrom,
            String intervalTo,
            double waterFraction,
            double meanNdwi,
            int totalPixels,
            int validPixels,
            String rawResponseIfEmpty) {

        static LakeWaterReading empty(String rawResponse) {
            return new LakeWaterReading(null, null, Double.NaN, Double.NaN, 0, 0, rawResponse);
        }
    }

    public record IceCoverReading(
            String intervalFrom,
            String intervalTo,
            double iceFraction,
            double meanNdsi,
            int totalPixels,
            int validPixels,
            String rawResponseIfEmpty) {

        static IceCoverReading empty(String rawResponse) {
            return new IceCoverReading(null, null, Double.NaN, Double.NaN, 0, 0, rawResponse);
        }
    }
}
