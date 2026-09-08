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

/**
 * A single real Sentinel-2 NDWI-derived water-fraction reading for one
 * lake, from watch.nepalhazard.service.SentinelHubClient. One row per
 * successful satellite pass, not a scored/aggregated value - the
 * short-term growth comparison happens at read time in
 * GLOFRiskCalculationService.
 */
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

    /**
     * Fraction of the sampled box that was actually usable (not cloud/
     * shadow/invalid) - a reading built on a low fraction of this is not
     * trustworthy for a growth comparison, even if the number itself looks
     * dramatic.
     */
    @Column(name = "valid_pixel_fraction", nullable = false)
    private Double validPixelFraction;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
