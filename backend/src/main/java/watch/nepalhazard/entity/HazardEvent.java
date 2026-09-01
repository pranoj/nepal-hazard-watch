package watch.nepalhazard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    private Double latitude;

    private Double longitude;

    private Double magnitude;

    private String description;

    @Column(name = "death_toll")
    private Integer deathToll;

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

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
