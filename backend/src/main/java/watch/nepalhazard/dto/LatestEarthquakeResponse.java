package watch.nepalhazard.dto;

import java.time.LocalDateTime;
import watch.nepalhazard.entity.HazardEvent;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LatestEarthquakeResponse {
    private String status;
    private String message;
    private EarthquakeInfo earthquake;

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

    public static LatestEarthquakeResponse noEarthquakes() {
        LatestEarthquakeResponse response = new LatestEarthquakeResponse();
        response.setStatus("NO_DATA");
        response.setMessage("No earthquakes recorded yet");
        response.setEarthquake(null);
        return response;
    }

    public static LatestEarthquakeResponse withNewEarthquakes(HazardEvent event) {
        LatestEarthquakeResponse response = new LatestEarthquakeResponse();
        response.setStatus("SUCCESS");
        response.setMessage("Latest earthquake recorded");

        EarthquakeInfo info = new EarthquakeInfo(
                event.getId(),
                event.getMagnitude(),
                event.getLatitude(),
                event.getLongitude(),
                event.getEventTime(),
                event.getDescription(),
                event.getDescription());
        response.setEarthquake(info);
        return response;
    }
}