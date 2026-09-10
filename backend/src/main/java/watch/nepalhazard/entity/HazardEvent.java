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

    // raw USGS classification (earthquake/landslide/quarry blast); eventType stays "EARTHQUAKE" for both since both are seismic-network detections

    @Column(name = "source_type")
    private String sourceType;

    private String status;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    private Double latitude;

    private Double longitude;

    private Double magnitude;

    private Double depth;

    private String description;

    @Column(name = "death_toll")
    private Integer deathToll;
}