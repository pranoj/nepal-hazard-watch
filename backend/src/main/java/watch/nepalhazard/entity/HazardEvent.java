package watch.nepalhazard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "hazard_events")
public class HazardEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "event_type")
    private String eventType;

    private String status;

    @Column(name = "first_detected_at")
    private Instant firstDetectedAt;

    private Double latitude;

    private Double longitude;

    protected HazardEvent() {
    }

    public Long getId() {
        return id;
    }

    public Long getRegionId() {
        return regionId;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getEventType() {
        return eventType;
    }

    public String getStatus() {
        return status;
    }

    public Instant getFirstDetectedAt() {
        return firstDetectedAt;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
