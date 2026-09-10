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

/** A "Type B" watch point: a glacier with a steep terminus near a river that can collapse and dam it without a pre-existing lake (e.g. Langtang Lirung, Aug 2026). Source: RGI v7. */
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

    /** Locally-computed terminus slope - more relevant to collapse risk than the whole-glacier RGI mean above. Null until the terrain job fills it in. */
    @Column(name = "local_slope_deg")
    private Double localSlopeDeg;

    @Column(name = "area_km2")
    private Double areaKm2;

    @Column(name = "nearest_river_basin")
    private String nearestRiverBasin;

    /** Nearest known landmark - gives each glacier a distinct display name instead of a shared generic basin label. */
    @Column(name = "nearest_town_name")
    private String nearestTownName;

    @Column(name = "nearest_river_town_km")
    private Double nearestRiverTownKm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
