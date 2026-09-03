package watch.nepalhazard.dto;

import java.time.LocalDateTime;
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

        public EarthquakeInfo(Long id, Double magnitude, Double latitude, Double longitude,
                LocalDateTime eventTime, String description, String riskAssessment) {
            this.id = id;
            this.magnitude = magnitude;
            this.latitude = latitude;
            this.longitude = longitude;
            this.eventTime = eventTime;
            this.description = description;
            this.riskAssessment = riskAssessment;
        }
    }

    public static EarthquakeAlertStatusResponse create(HazardEvent latest, HazardEvent previous) {
        EarthquakeAlertStatusResponse response = new EarthquakeAlertStatusResponse();

        // Calculate minutes since latest earthquake
        LocalDateTime now = LocalDateTime.now();
        long minutesPassed = java.time.temporal.ChronoUnit.MINUTES.between(latest.getEventTime(), now);

        // Alert is active if earthquake happened in last 30 minutes
        boolean isActive = minutesPassed <= 30;

        response.setAlertActive(isActive);
        response.setMinutesSinceAlert(minutesPassed);

        // Convert to DTO
        EarthquakeInfo newAlert = new EarthquakeInfo(
                latest.getId(),
                latest.getMagnitude(),
                latest.getLatitude(),
                latest.getLongitude(),
                latest.getEventTime(),
                latest.getDescription(),
                latest.getDescription());
        response.setNewAlert(newAlert);

        // Add previous earthquake if it exists
        if (previous != null) {
            EarthquakeInfo lastAlert = new EarthquakeInfo(
                    previous.getId(),
                    previous.getMagnitude(),
                    previous.getLatitude(),
                    previous.getLongitude(),
                    previous.getEventTime(),
                    previous.getDescription(),
                    previous.getDescription());
            response.setLastEarthquake(lastAlert);
        }

        return response;
    }
}