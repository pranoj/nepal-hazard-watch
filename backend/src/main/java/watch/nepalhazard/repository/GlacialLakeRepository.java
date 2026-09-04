package watch.nepalhazard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import watch.nepalhazard.entity.GlacialLake;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GlacialLake entity
 * Provides database access methods for glacial lake data from ICIMOD
 */
@Repository
public interface GlacialLakeRepository extends JpaRepository<GlacialLake, Long> {

    /**
     * Find a glacial lake by its ICIMOD unique identifier
     * Used for syncing/updating data from ICIMOD source
     */
    Optional<GlacialLake> findByIcimodId(@Param("icimodId") String icimodId);

    /**
     * Find the nearest glacial lake to given coordinates
     * Used in GLOF risk calculations to identify nearby lakes
     * Returns the lake with minimum Euclidean distance
     */
    @Query(value = "SELECT * FROM glacial_lakes ORDER BY (latitude - :lat) * (latitude - :lat) + (longitude - :lon) * (longitude - :lon) ASC LIMIT 1", nativeQuery = true)
    Optional<GlacialLake> findNearestByCoordinates(@Param("lat") double latitude, @Param("lon") double longitude);

    /**
     * Find all glacial lakes within a specified distance from coordinates
     * Used to identify all lakes that could be affected by an earthquake
     * 
     * Note: Uses simple Euclidean distance (degrees squared).
     * For production accuracy, consider replacing with Haversine formula
     * when distance precision becomes critical.
     * 
     * @param latitude      Center latitude
     * @param longitude     Center longitude
     * @param radiusDegrees Search radius in decimal degrees (~111 km per degree)
     * @return List of glacial lakes within radius
     */
    @Query(value = "SELECT * FROM glacial_lakes WHERE (latitude - :lat) * (latitude - :lat) + (longitude - :lon) * (longitude - :lon) <= :radiusDegrees * :radiusDegrees ORDER BY (latitude - :lat) * (latitude - :lat) + (longitude - :lon) * (longitude - :lon) ASC", nativeQuery = true)
    List<GlacialLake> findLakesWithinRadius(@Param("lat") double latitude, @Param("lon") double longitude,
            @Param("radiusDegrees") double radiusDegrees);

    /**
     * Find all glacial lakes in a specific country
     * Used for regional GLOF risk assessments
     */
    List<GlacialLake> findByCountry(@Param("country") String country);

    /**
     * Find all glacial lakes with a specific risk level
     * Filters by ICIMOD risk assessment (Low, Medium, High, Very High)
     */
    List<GlacialLake> findByRiskLevel(@Param("riskLevel") String riskLevel);

    /**
     * Find high-risk glacial lakes in Nepal
     * Combines country and risk level filters for focused monitoring
     */
    @Query("SELECT g FROM GlacialLake g WHERE g.country = :country AND g.riskLevel IN ('High', 'Very High') ORDER BY g.surfaceAreaKm2 DESC")
    List<GlacialLake> findHighRiskLakesByCountry(@Param("country") String country);

    /**
     * Find all glacial lakes in Nepal (convenience method)
     */
    @Query("SELECT g FROM GlacialLake g WHERE g.country = 'Nepal' ORDER BY g.latitude DESC, g.longitude ASC")
    List<GlacialLake> findAllLakesInNepal();
}