package watch.nepalhazard.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import watch.nepalhazard.entity.HazardEvent;
import watch.nepalhazard.repository.HazardEventRepository;
import watch.nepalhazard.repository.RegionRepository;

@Slf4j
@Service
@ConfigurationProperties(prefix = "earthquake")
public class UsgsEarthquakeService {

    private final HazardEventRepository hazardEventRepository;
    private final RegionRepository regionRepository;
    private final GlofRiskScanService glofRiskScanService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private Usgs usgs;
    private Nepal nepal;
    private GlacierZone glacierZone;

    @Autowired
    public UsgsEarthquakeService(HazardEventRepository hazardEventRepository, RegionRepository regionRepository,
            GlofRiskScanService glofRiskScanService) {
        this.hazardEventRepository = hazardEventRepository;
        this.regionRepository = regionRepository;
        this.glofRiskScanService = glofRiskScanService;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(20000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    /** One request per poll against USGS's pre-built summary feed (mag 2.5+, past day); magnitude and Nepal's bounding box are filtered client-side. */
    @Scheduled(fixedRateString = "${earthquake.usgs.fetch-interval-ms}")
    public void fetchEarthquakes() {
        try {
            log.info("Fetching earthquakes from USGS...");

            String response;
            try {
                response = restTemplate.getForObject(usgs.getApiUrl(), String.class);
            } catch (Exception e) {
                log.error("USGS summary feed request failed: {}", e.getMessage());
                return;
            }

            if (response == null || response.isEmpty()) {
                log.warn("No response from USGS summary feed");
                return;
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode features = root.path("features");

            int totalCount = 0;
            for (JsonNode feature : features) {
                HazardEvent event = parseEarthquake(feature);
                if (event == null || event.getMagnitude() < usgs.getMinMagnitude() || !isInNepal(event)) {
                    continue;
                }
                if (!earthquakeExists(event)) {
                    hazardEventRepository.save(event);
                    totalCount++;
                }
            }

            log.info("Total earthquakes added: {}", totalCount);

            // rescan immediately rather than waiting up to 30 min for the next scheduled scan
            if (totalCount > 0) {
                log.info("New hazard event(s) detected - triggering immediate risk rescan");
                glofRiskScanService.scanAll();
            }

        } catch (Exception e) {
            log.error("Error fetching earthquakes: {}", e.getMessage(), e);
        }
    }

    private HazardEvent parseEarthquake(JsonNode feature) {
        try {
            JsonNode properties = feature.path("properties");
            JsonNode geometry = feature.path("geometry");
            JsonNode coordinates = geometry.path("coordinates");

            double longitude = coordinates.get(0).asDouble();
            double latitude = coordinates.get(1).asDouble();
            double depth = coordinates.get(2).asDouble();
            double magnitude = properties.path("mag").asDouble();
            String title = properties.path("title").asText();
            String sourceType = properties.path("type").asText(null);
            long occurredAtMillis = properties.path("time").asLong();
            // USGS "time" is epoch millis UTC (quake time, not fetch time); stored UTC, frontend converts to Nepal Time
            LocalDateTime eventTime = occurredAtMillis > 0
                    ? Instant.ofEpochMilli(occurredAtMillis).atZone(ZoneOffset.UTC).toLocalDateTime()
                    : LocalDateTime.now(ZoneOffset.UTC);

            HazardEvent event = new HazardEvent();
            event.setEventType("EARTHQUAKE");
            event.setSourceType(sourceType);
            event.setLatitude(latitude);
            event.setLongitude(longitude);
            event.setMagnitude(magnitude);
            event.setDepth(depth);
            event.setEventTime(eventTime);
            event.setStatus("CONFIRMED");
            event.setDescription(title + " (Depth: " + String.format("%.1f", depth) + "km)");
            event.setDeathToll(0);
            regionRepository.findNearestRegion(latitude, longitude)
                    .ifPresent(region -> event.setRegionId(region.getId()));

            String riskAssessment = assessGLOFRisk(magnitude, latitude);
            event.setDescription(event.getDescription() + " | " + riskAssessment);

            return event;

        } catch (Exception e) {
            log.error("Error parsing earthquake: {}", e.getMessage());
            return null;
        }
    }

    private String assessGLOFRisk(double magnitude, double latitude) {
        boolean inGlacierZone = latitude >= glacierZone.getLatitudeMin() && latitude <= glacierZone.getLatitudeMax();

        if (magnitude >= 6.0 && inGlacierZone) {
            return "🚨 CRITICAL GLOF RISK (75%)";
        } else if (magnitude >= 5.5 && inGlacierZone) {
            return "🔴 HIGH GLOF RISK (50%)";
        } else if (magnitude >= 5.0 && inGlacierZone) {
            return "🟠 MODERATE GLOF RISK (30%)";
        } else if (magnitude >= 5.0) {
            return "🟡 LOW GLOF RISK (10%)";
        } else {
            return "🟢 MINIMAL GLOF RISK (5%)";
        }
    }

    private boolean isInNepal(HazardEvent event) {
        return event.getLatitude() >= nepal.getLatitudeMin() && event.getLatitude() <= nepal.getLatitudeMax()
                && event.getLongitude() >= nepal.getLongitudeMin() && event.getLongitude() <= nepal.getLongitudeMax();
    }

    private boolean earthquakeExists(HazardEvent event) {
        return hazardEventRepository.existsSimilarEarthquake(event.getMagnitude(), event.getLatitude(),
                event.getLongitude());
    }

    public void setUsgs(Usgs usgs) {
        this.usgs = usgs;
    }

    public void setNepal(Nepal nepal) {
        this.nepal = nepal;
    }

    public void setGlacierZone(GlacierZone glacierZone) {
        this.glacierZone = glacierZone;
    }

    @Data
    public static class Usgs {
        private String apiUrl;
        private long fetchIntervalMs;
        private double minMagnitude;
    }

    @Data
    public static class Nepal {
        private double latitudeMin;
        private double latitudeMax;
        private double longitudeMin;
        private double longitudeMax;
    }

    @Data
    public static class GlacierZone {
        private double latitudeMin;
        private double latitudeMax;
    }
}
