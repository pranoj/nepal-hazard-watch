package watch.nepalhazard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import watch.nepalhazard.entity.Glacier;
import watch.nepalhazard.repository.GlacierRepository;

/** Computes a local terminus slope from nearby elevation samples - more relevant to collapse risk than RGI's whole-glacier mean. Runs once at startup. */
@Slf4j
@Service
public class TerrainSlopeService {

    private static final double KM_PER_DEG_LAT = 111.32;
    private static final double SAMPLE_OFFSET_METERS = 100.0;
    private static final long REQUEST_DELAY_MS = 250;

    private final GlacierRepository glacierRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${terrain.elevation.api-url}")
    private String elevationApiUrl;

    @Autowired
    public TerrainSlopeService(GlacierRepository glacierRepository,
            @Value("${terrain.elevation.timeout-seconds:15}") int timeoutSeconds) {
        this.glacierRepository = glacierRepository;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    public void fillMissingLocalSlopes() {
        List<Glacier> pending = glacierRepository.findByLocalSlopeDegIsNull();
        if (pending.isEmpty()) {
            log.info("Local terminus slope already computed for every glacier");
            return;
        }
        log.info("Computing local terminus slope for {} glaciers via {}", pending.size(), elevationApiUrl);

        int computed = 0;
        for (Glacier glacier : pending) {
            Double slope = fetchLocalSlope(glacier.getTerminusLatitude(), glacier.getTerminusLongitude());
            if (slope != null) {
                glacier.setLocalSlopeDeg(slope);
                glacierRepository.save(glacier);
                computed++;
            } else {
                log.warn("Could not compute local slope for glacier {} - leaving RGI mean slope as the fallback",
                        glacier.getRgiId());
            }
            politeDelay();
        }
        log.info("Local terminus slope computed for {}/{} glaciers", computed, pending.size());
    }

    private Double fetchLocalSlope(double lat, double lon) {
        try {
            double latOffsetDeg = SAMPLE_OFFSET_METERS / 1000.0 / KM_PER_DEG_LAT;
            double lonOffsetDeg = SAMPLE_OFFSET_METERS / 1000.0 / (KM_PER_DEG_LAT * Math.cos(Math.toRadians(lat)));

            String locations = String.format(
                    "%f,%f|%f,%f|%f,%f|%f,%f",
                    lat + latOffsetDeg, lon,
                    lat - latOffsetDeg, lon,
                    lat, lon + lonOffsetDeg,
                    lat, lon - lonOffsetDeg);
            String url = elevationApiUrl + "?locations=" + locations;

            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isEmpty()) {
                return null;
            }

            JsonNode results = objectMapper.readTree(response).path("results");
            if (!results.isArray() || results.size() < 4) {
                return null;
            }

            double elevNorth = results.get(0).path("elevation").asDouble();
            double elevSouth = results.get(1).path("elevation").asDouble();
            double elevEast = results.get(2).path("elevation").asDouble();
            double elevWest = results.get(3).path("elevation").asDouble();

            return computeSlopeDeg(elevNorth, elevSouth, elevEast, elevWest, SAMPLE_OFFSET_METERS);

        } catch (Exception e) {
            log.warn("Elevation lookup failed for ({}, {}): {}", lat, lon, e.getMessage());
            return null;
        }
    }

    /** Central-difference slope from four elevation samples. Package-private so it can be unit tested without a live network call. */
    static double computeSlopeDeg(double elevNorth, double elevSouth, double elevEast, double elevWest,
            double offsetMeters) {
        double gradLat = (elevNorth - elevSouth) / (2 * offsetMeters);
        double gradLon = (elevEast - elevWest) / (2 * offsetMeters);
        double gradientMagnitude = Math.sqrt(gradLat * gradLat + gradLon * gradLon);
        return Math.toDegrees(Math.atan(gradientMagnitude));
    }

    private void politeDelay() {
        try {
            Thread.sleep(REQUEST_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
