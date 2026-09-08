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
 * A "Type B" watch point: a glacier with a steep terminus near a known
 * river corridor, capable of collapsing and damming a river directly (no
 * pre-existing lake required) - as happened at Langtang Lirung on Aug 2026.
 * Distinct from GlacialLake, which only covers already-formed, named lakes.
 * Source: RGI (Randolph Glacier Inventory) v7.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "glaciers")
public class Glacier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rgi_id", unique = true, nullable = false)
    private String rgiId;

    @Column(name = "glacier_name")
    private String glacierName;

    @Column(name = "terminus_latitude", nullable = false)
    private Double terminusLatitude;

    @Column(name = "terminus_longitude", nullable = false)
    private Double terminusLongitude;

    @Column(name = "slope_deg", nullable = false)
    private Double slopeDeg;

    /**
     * Locally-computed slope near the actual terminus, from real elevation
     * samples - more relevant to collapse risk than the whole-glacier RGI
     * mean above. Null until the one-time terrain job fills it in.
     */
    @Column(name = "local_slope_deg")
    private Double localSlopeDeg;

    @Column(name = "area_km2")
    private Double areaKm2;

    @Column(name = "nearest_river_basin")
    private String nearestRiverBasin;

    /**
     * Name of the actual nearest known landmark (e.g. "Langtang Village"),
     * used to give each glacier a distinct, locatable display name instead
     * of every glacier in the same basin sharing one generic label.
     */
    @Column(name = "nearest_town_name")
    private String nearestTownName;

    @Column(name = "nearest_river_town_km")
    private Double nearestRiverTownKm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
