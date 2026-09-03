package watch.nepalhazard.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import watch.nepalhazard.entity.HazardEvent;
import watch.nepalhazard.repository.HazardEventRepository;

@Service
@ConfigurationProperties(prefix = "earthquake")
public class UsgsEarthquakeService {

    private final HazardEventRepository hazardEventRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private Usgs usgs;
    private Nepal nepal;
    private GlacierZone glacierZone;
    private List<Map<String, Object>> queryRegions;

    @Autowired
    public UsgsEarthquakeService(HazardEventRepository hazardEventRepository) {
        this.hazardEventRepository = hazardEventRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(fixedRateString = "${earthquake.usgs.fetch-interval-ms}")
    public void fetchEarthquakes() {
        try {
            System.out.println("🔄 Fetching earthquakes from USGS...");
            int totalCount = 0;

            for (Map<String, Object> region : queryRegions) {
                String regionName = (String) region.get("name");
                double lat = ((Number) region.get("latitude")).doubleValue();
                double lon = ((Number) region.get("longitude")).doubleValue();

                System.out.println("📍 Querying " + regionName + " region (" + lat + ", " + lon + ")...");

                String params = "format=geojson&"
                        + "starttime=" + getPast24Hours() + "&"
                        + "minmagnitude=" + usgs.getMinMagnitude() + "&"
                        + "latitude=" + lat + "&"
                        + "longitude=" + lon + "&"
                        + "maxradius=" + usgs.getMaxRadiusKm();

                String url = usgs.getApiUrl() + "?" + params;
                System.out.println("📡 API URL: " + url);

                String response = restTemplate.getForObject(url, String.class);

                if (response == null || response.isEmpty()) {
                    System.out.println("⚠️  No response from " + regionName + " query");
                    continue;
                }

                JsonNode root = objectMapper.readTree(response);
                JsonNode features = root.path("features");

                int regionCount = 0;
                for (JsonNode feature : features) {
                    HazardEvent event = parseEarthquake(feature);
                    if (event != null && isInNepal(event)) {
                        if (!earthquakeExists(event)) {
                            hazardEventRepository.save(event);
                            regionCount++;
                            totalCount++;
                        }
                    }
                }

                System.out.println("✅ " + regionName + " region: Added " + regionCount + " earthquakes");
            }

            System.out.println("✅ Total earthquakes added: " + totalCount);

        } catch (Exception e) {
            System.err.println("❌ Error fetching earthquakes: " + e.getMessage());
            e.printStackTrace();
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
            long time = properties.path("time").asLong();
            String title = properties.path("title").asText();

            HazardEvent event = new HazardEvent();
            event.setEventType("EARTHQUAKE");
            event.setLatitude(latitude);
            event.setLongitude(longitude);
            event.setMagnitude(magnitude);
            event.setEventTime(LocalDateTime.now());
            event.setStatus("CONFIRMED");
            event.setDescription(title + " (Depth: " + String.format("%.1f", depth) + "km)");
            event.setDeathToll(0);

            String riskAssessment = assessGLOFRisk(magnitude, latitude);
            event.setDescription(event.getDescription() + " | " + riskAssessment);

            return event;

        } catch (Exception e) {
            System.err.println("Error parsing earthquake: " + e.getMessage());
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
        List<HazardEvent> existing = hazardEventRepository.findAll();
        for (HazardEvent e : existing) {
            if (e.getEventType().equals("EARTHQUAKE")
                    && Math.abs(e.getMagnitude() - event.getMagnitude()) < 0.1
                    && Math.abs(e.getLatitude() - event.getLatitude()) < 0.1
                    && Math.abs(e.getLongitude() - event.getLongitude()) < 0.1) {
                return true;
            }
        }
        return false;
    }

    private String getPast24Hours() {
        LocalDateTime past24 = LocalDateTime.now().minusHours(24);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return past24.format(formatter);
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

    public void setQueryRegions(List<Map<String, Object>> queryRegions) {
        this.queryRegions = queryRegions;
    }

    public static class Usgs {
        private String apiUrl;
        private long fetchIntervalMs;
        private double minMagnitude;
        private int maxRadiusKm;

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public long getFetchIntervalMs() {
            return fetchIntervalMs;
        }

        public void setFetchIntervalMs(long fetchIntervalMs) {
            this.fetchIntervalMs = fetchIntervalMs;
        }

        public double getMinMagnitude() {
            return minMagnitude;
        }

        public void setMinMagnitude(double minMagnitude) {
            this.minMagnitude = minMagnitude;
        }

        public int getMaxRadiusKm() {
            return maxRadiusKm;
        }

        public void setMaxRadiusKm(int maxRadiusKm) {
            this.maxRadiusKm = maxRadiusKm;
        }
    }

    public static class Nepal {
        private double latitudeMin;
        private double latitudeMax;
        private double longitudeMin;
        private double longitudeMax;

        public double getLatitudeMin() {
            return latitudeMin;
        }

        public void setLatitudeMin(double latitudeMin) {
            this.latitudeMin = latitudeMin;
        }

        public double getLatitudeMax() {
            return latitudeMax;
        }

        public void setLatitudeMax(double latitudeMax) {
            this.latitudeMax = latitudeMax;
        }

        public double getLongitudeMin() {
            return longitudeMin;
        }

        public void setLongitudeMin(double longitudeMin) {
            this.longitudeMin = longitudeMin;
        }

        public double getLongitudeMax() {
            return longitudeMax;
        }

        public void setLongitudeMax(double longitudeMax) {
            this.longitudeMax = longitudeMax;
        }
    }

    public static class GlacierZone {
        private double latitudeMin;
        private double latitudeMax;

        public double getLatitudeMin() {
            return latitudeMin;
        }

        public void setLatitudeMin(double latitudeMin) {
            this.latitudeMin = latitudeMin;
        }

        public double getLatitudeMax() {
            return latitudeMax;
        }

        public void setLatitudeMax(double latitudeMax) {
            this.latitudeMax = latitudeMax;
        }
    }
}