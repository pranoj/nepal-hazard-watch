package watch.nepalhazard.dto;

import java.time.LocalDateTime;
import watch.nepalhazard.entity.HazardEvent;

public record HazardEventDTO(
        Long id,
        Long regionId,
        String eventType,
        String sourceType,
        String status,
        LocalDateTime eventTime,
        Double latitude,
        Double longitude,
        Double magnitude,
        String description,
        Integer deathToll) {

    public static HazardEventDTO from(HazardEvent hazardEvent) {
        return new HazardEventDTO(
                hazardEvent.getId(),
                hazardEvent.getRegionId(),
                hazardEvent.getEventType(),
                hazardEvent.getSourceType(),
                hazardEvent.getStatus(),
                hazardEvent.getEventTime(),
                hazardEvent.getLatitude(),
                hazardEvent.getLongitude(),
                hazardEvent.getMagnitude(),
                hazardEvent.getDescription(),
                hazardEvent.getDeathToll());
    }
}