package watch.nepalhazard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One Sentinel-2 NDWI water-fraction reading per satellite pass, not aggregated - growth comparison happens at read time in GLOFRiskCalculationService. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "lake_satellite_observations")
public class LakeSatelliteObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "icimod_id", nullable = false)
    private String icimodId;

    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    @Column(name = "water_fraction", nullable = false)
    private Double waterFraction;

    @Column(name = "mean_ndwi")
    private Double meanNdwi;

    /** Fraction of the sampled box that was usable (not cloud/shadow/invalid) - low values make the reading untrustworthy. */
    @Column(name = "valid_pixel_fraction", nullable = false)
    private Double validPixelFraction;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
