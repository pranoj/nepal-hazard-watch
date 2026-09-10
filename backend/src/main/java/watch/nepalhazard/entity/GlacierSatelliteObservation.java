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

/** One Sentinel-2 NDSI ice/snow-cover reading per satellite pass - sudden-drop comparison happens at read time in GLOFRiskCalculationService. */
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

    /** Fraction of the sampled box usable (not cloud/shadow/invalid) - a cloud passing over looks exactly like ice vanishing at low values. */
    @Column(name = "valid_pixel_fraction", nullable = false)
    private Double validPixelFraction;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
