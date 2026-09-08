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
 * A single real Sentinel-2 NDSI-derived ice/snow-cover reading for one
 * glacier terminus, from watch.nepalhazard.service.SentinelHubClient. One
 * row per successful satellite pass - the sudden-drop comparison happens at
 * read time in GLOFRiskCalculationService, same pattern as
 * LakeSatelliteObservation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "glacier_satellite_observations")
public class GlacierSatelliteObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rgi_id", nullable = false)
    private String rgiId;

    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    @Column(name = "ice_fraction", nullable = false)
    private Double iceFraction;

    @Column(name = "mean_ndsi")
    private Double meanNdsi;

    /**
     * Fraction of the sampled box that was actually usable (not cloud/
     * shadow/invalid) - a reading built on a low fraction of this is not
     * trustworthy for a sudden-drop comparison, even if the number itself
     * looks dramatic (a cloud passing over looks exactly like ice vanishing).
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
