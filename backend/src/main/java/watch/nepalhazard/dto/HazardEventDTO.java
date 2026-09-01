package watch.nepalhazard.dto;

import java.time.Instant;
import watch.nepalhazard.entity.HazardEvent;

public record HazardEventDTO(
        Long id,
        Long regionId,
        String riskLevel,
        String eventType,
        Instant firstDetectedAt,
        Double latitude,
        Double longitude) {

    public static HazardEventDTO from(HazardEvent hazardEvent) {
        return new HazardEventDTO(
                hazardEvent.getId(),
                hazardEvent.getRegionId(),
                hazardEvent.getRiskLevel(),
                hazardEvent.getEventType(),
                hazardEvent.getFirstDetectedAt(),
                hazardEvent.getLatitude(),
                hazardEvent.getLongitude());
    }
}
