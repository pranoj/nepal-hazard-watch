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

/**
 * Latest GLOF risk assessment for a monitored point - either a glacial lake
 * ("LAKE", the traditional GLOF pathway) or a glacier watch point ("GLACIER",
 * a steep terminus near a river that can collapse and dam it directly, as
 * happened at Langtang Lirung on Aug 2026). Recomputed on a schedule by
 * GlofRiskScanService. Exactly one of glacialLakeId/glacierId is set,
 * matching sourceType.
 */
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

    /**
     * Lake-type susceptibility (from ICIMOD classification) for LAKE rows,
     * or terrain-steepness factor (from RGI slope_deg) for GLACIER rows -
     * both represent the same role: structural/baseline susceptibility.
     */
    @Column(name = "lake_type_component")
    private Double lakeTypeComponent;

    @Column(name = "seasonal_component")
    private Double seasonalComponent;

    /**
     * Hazard from a nearby USGS "landslide"-type detection - a direct
     * seismic-network sighting of actual mass movement (e.g. an ice/rock
     * avalanche), distinct from ordinary tectonic earthquake shaking.
     */
    @Column(name = "landslide_component")
    private Double landslideComponent;

    @Column(name = "landslide_detected", nullable = false)
    private Boolean landslideDetected;

    /**
     * Predictive signal (steep terrain + sustained heavy rain), distinct
     * from landslideDetected which requires an actual observed event.
     * Glacier rows only - lakes have no terrain slope data.
     */
    @Column(name = "landslide_pre_condition", nullable = false)
    private Boolean landslidePreCondition;

    /**
     * NORMAL / ELEVATED / HEAVY, derived from current + cumulative rainfall.
     */
    @Column(name = "rainfall_condition")
    private String rainfallCondition;

    /**
     * True if recent average temperature at this lake is above freezing -
     * an active glacier-melt indicator.
     */
    @Column(name = "melt_condition", nullable = false)
    private Boolean meltCondition;

    @Column(name = "nearest_earthquake_id")
    private Long nearestEarthquakeId;

    /**
     * Real Sentinel-2 NDWI water fraction (0-1), from the most recent
     * satellite pass with usable pixels. Null until a lake has at least
     * one real observation. Glacier rows never have this - no lake to
     * measure. See SatelliteLakeTrackingService.
     */
    @Column(name = "satellite_water_fraction")
    private Double satelliteWaterFraction;

    /**
     * True if the water fraction grew meaningfully vs. the previous
     * satellite reading. Always computed and stored, but only allowed to
     * actually raise the alert level when satellite.escalation.enabled is
     * true - see GLOFRiskCalculationService.
     */
    @Column(name = "satellite_lake_growth_detected", nullable = false)
    private Boolean satelliteLakeGrowthDetected;

    /**
     * Real Sentinel-2 NDSI ice/snow-cover fraction (0-1) for a glacier
     * terminus, from the most recent satellite pass with usable pixels.
     * Null until a glacier has at least one real observation. Lake rows
     * never have this - no glacier terminus box to measure. See
     * SatelliteGlacierTrackingService.
     */
    @Column(name = "satellite_ice_fraction")
    private Double satelliteIceFraction;

    /**
     * True if ice/snow cover dropped sharply vs. the previous satellite
     * reading, within a short enough window to plausibly be a sudden event
     * (terminus collapse) rather than ordinary seasonal melt. Always
     * computed and stored, but only allowed to actually raise the alert
     * level or score when satellite.glacier-factor.enabled is true - see
     * GLOFRiskCalculationService.
     */
    @Column(name = "satellite_ice_sudden_drop_detected", nullable = false)
    private Boolean satelliteIceSuddenDropDetected;

    /**
     * The satellite hazard value (0-1) actually weighted into the score:
     * satelliteGrowth.hazard() for a LAKE row, iceDrop.hazard() for a
     * GLACIER row. Distinct from satelliteWaterFraction/satelliteIceFraction
     * above, which are the raw sensor reading, not the graded hazard - this
     * is what the frontend needs to show a "Satellite factor: X%" row the
     * same way it already shows rainfall/earthquake/landslide/etc.
     */
    @Column(name = "satellite_component")
    private Double satelliteComponent;

    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt;
}
