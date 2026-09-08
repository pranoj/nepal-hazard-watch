package watch.nepalhazard.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import watch.nepalhazard.entity.HazardEvent;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EarthquakeAlertStatusResponse {
    private Boolean alertActive;
    private Long minutesSinceAlert;
    private EarthquakeInfo newAlert;
    private EarthquakeInfo lastEarthquake;

    @Data
    public static class EarthquakeInfo {
        private Long id;
        private Double magnitude;
        private Double latitude;
        private Double longitude;
        private LocalDateTime eventTime;
        private String description;
        private String riskAssessment;
        private String sourceType;

        public EarthquakeInfo(Long id, Double magnitude, Double latitude, Double longitude,
                LocalDateTime eventTime, String description, String riskAssessment, String sourceType) {
            this.id = id;
            this.magnitude = magnitude;
            this.latitude = latitude;
            this.longitude = longitude;
            this.eventTime = eventTime;
            this.description = description;
            this.riskAssessment = riskAssessment;
            this.sourceType = sourceType;
        }
    }

    public static EarthquakeAlertStatusResponse create(HazardEvent latest, HazardEvent previous) {
        EarthquakeAlertStatusResponse response = new EarthquakeAlertStatusResponse();

        // Calculate minutes since latest earthquake. eventTime is stored as
        // UTC (see UsgsEarthquakeService), so "now" must be UTC too - the
        // host machine's own zone (e.g. US Central on this dev box) would
        // silently skew this by several hours otherwise.
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        long minutesPassed = java.time.temporal.ChronoUnit.MINUTES.between(latest.getEventTime(), now);
        boolean isActive = minutesPassed <= 30;

        response.setAlertActive(isActive);
        response.setMinutesSinceAlert(minutesPassed);

        EarthquakeInfo newAlert = new EarthquakeInfo(
                latest.getId(),
                latest.getMagnitude(),
                latest.getLatitude(),
                latest.getLongitude(),
                latest.getEventTime(),
                latest.getDescription(),
                latest.getDescription(),
                latest.getSourceType());
        response.setNewAlert(newAlert);

        if (previous != null) {
            EarthquakeInfo lastAlert = new EarthquakeInfo(
                    previous.getId(),
                    previous.getMagnitude(),
                    previous.getLatitude(),
                    previous.getLongitude(),
                    previous.getEventTime(),
                    previous.getDescription(),
                    previous.getDescription(),
                    previous.getSourceType());
            response.setLastEarthquake(lastAlert);
        }

        return response;
    }
}