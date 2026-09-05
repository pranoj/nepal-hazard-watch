package watch.nepalhazard.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Glacial Lake Entity - Represents glacial lakes in Nepal from ICIMOD database
 * 
 * Data Source: ICIMOD (International Centre for Integrated Mountain
 * Development)
 * License: CC BY 4.0 (Creative Commons Attribution 4.0 International)
 * Attribution: Data sourced from ICIMOD Glacial Lakes Inventory and monitoring
 * systems
 * 
 * IMPORTANT: Any use of this data must include proper attribution to ICIMOD
 * and comply with CC BY 4.0 licensing requirements.
 * 
 * Reference: https://www.icimod.org/
 */
@Entity
@Table(name = "glacial_lakes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlacialLake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier from ICIMOD database
     * Used to match and sync with ICIMOD data updates
     */
    @Column(name = "icimod_id", unique = true, nullable = false)
    private String icimodId;

    @Column(name = "lake_name")
    private String lakeName;

    @Column(name = "glacier_name")
    private String glacierName;

    /**
     * Latitude in decimal degrees (WGS84)
     */
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    /**
     * Longitude in decimal degrees (WGS84)
     */
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    /**
     * Elevation in meters above sea level
     */
    @Column(name = "elevation")
    private Double elevation;

    /**
     * Surface area of the glacial lake in square kilometers
     * Used for GLOF risk calculation (glacier lake factor)
     */
    @Column(name = "surface_area_km2")
    private Double surfaceAreaKm2;

    /**
     * Country where the glacial lake is located
     */
    @Column(name = "country")
    private String country;

    /**
     * ICIMOD risk level assessment (Low, Medium, High, Very High)
     */
    @Column(name = "risk_level")
    private String riskLevel;

    /**
     * True if ICIMOD flags this lake as transboundary - it may sit outside
     * Nepal (e.g. Tibet/China) while still draining into and flooding Nepal
     * directly, as with the Aug 2026 Kyirong-Rasuwa GLOF.
     */
    @Column(name = "transboundary", nullable = false)
    private Boolean transboundary;

    /**
     * ICIMOD's named river basin (e.g. "Dudh Koshi", "Humla") - the join key
     * for mapping which downstream towns/corridors this lake's water flows
     * through.
     */
    @Column(name = "river_basin")
    private String riverBasin;

    /**
     * Timestamp when this data was last updated from ICIMOD
     */
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    /**
     * Timestamp when this record was created in our database
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this record was last modified
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}