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

/** Latest GLOF risk assessment for a lake ("LAKE") or glacier watch point ("GLACIER"). Exactly one of glacialLakeId/glacierId is set, matching sourceType. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "glof_risk_assessments")
public class GlofRiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * "LAKE" or "GLACIER" - which pathway this assessment covers.
     */
    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "glacial_lake_id", unique = true)
    private Long glacialLakeId;

    @Column(name = "glacier_id", unique = true)
    private Long glacierId;

    /**
     * Lake name, or glacier name for GLACIER rows.
     */
    @Column(name = "lake_name")
    private String lakeName;

    /**
     * ICIMOD lake id, or RGI glacier id for GLACIER rows.
     */
    @Column(name = "icimod_id")
    private String icimodId;

    @Column(name = "river_basin")
    private String riverBasin;

    private Double latitude;

    private Double longitude;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Column(name = "alert_level", nullable = false)
    private String alertLevel;

    @Column(name = "rainfall_component")
    private Double rainfallComponent;

    @Column(name = "earthquake_component")
    private Double earthquakeComponent;

    /** Lake-type susceptibility (LAKE rows) or terrain-steepness factor (GLACIER rows) - same role: baseline susceptibility. */
    @Column(name = "lake_type_component")
    private Double lakeTypeComponent;

    @Column(name = "seasonal_component")
    private Double seasonalComponent;

    /** Hazard from a nearby USGS "landslide"-type detection - distinct from ordinary tectonic earthquake shaking. */
    @Column(name = "landslide_component")
    private Double landslideComponent;

    @Column(name = "landslide_detected", nullable = false)
    private Boolean landslideDetected;

    /** Predictive signal (steep terrain + heavy rain), distinct from landslideDetected. Glacier rows only. */
    @Column(name = "landslide_pre_condition", nullable = false)
    private Boolean landslidePreCondition;

    /**
     * NORMAL / ELEVATED / HEAVY, derived from current + cumulative rainfall.
     */
    @Column(name = "rainfall_condition")
    private String rainfallCondition;

    /** True if recent average temperature is above freezing - an active glacier-melt indicator. */
    @Column(name = "melt_condition", nullable = false)
    private Boolean meltCondition;

    @Column(name = "nearest_earthquake_id")
    private Long nearestEarthquakeId;

    /** Sentinel-2 NDWI water fraction (0-1) from the most recent usable pass. Null until observed. Glacier rows never have this. */
    @Column(name = "satellite_water_fraction")
    private Double satelliteWaterFraction;

    /** True if water fraction grew meaningfully vs. the previous reading. Only raises the alert level when satellite.escalation.enabled is true. */
    @Column(name = "satellite_lake_growth_detected", nullable = false)
    private Boolean satelliteLakeGrowthDetected;

    /** Sentinel-2 NDSI ice/snow-cover fraction (0-1) from the most recent usable pass. Null until observed. Lake rows never have this. */
    @Column(name = "satellite_ice_fraction")
    private Double satelliteIceFraction;

    /** True if ice/snow cover dropped sharply within a short window (plausible terminus collapse). Only raises score when satellite.glacier-factor.enabled is true. */
    @Column(name = "satellite_ice_sudden_drop_detected", nullable = false)
    private Boolean satelliteIceSuddenDropDetected;

    /** Graded satellite hazard (0-1) actually weighted into the score - distinct from the raw satelliteWaterFraction/satelliteIceFraction readings above. */
    @Column(name = "satellite_component")
    private Double satelliteComponent;

    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt;
}
