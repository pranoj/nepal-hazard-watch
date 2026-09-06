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
     * Find all glacial lakes relevant to Nepal: lakes physically inside
     * Nepal, plus transboundary lakes (e.g. in Tibet/China) within Nepal's
     * border envelope that can still flood Nepal directly - such as the
     * lake behind the Aug 2026 Kyirong-Rasuwa GLOF.
     */
    @Query("SELECT g FROM GlacialLake g WHERE g.country = 'Nepal' "
            + "OR (g.transboundary = true AND g.latitude BETWEEN 26.0 AND 30.5 AND g.longitude BETWEEN 80.0 AND 88.5) "
            + "ORDER BY g.latitude DESC, g.longitude ASC")
    List<GlacialLake> findAllLakesInNepal();
}