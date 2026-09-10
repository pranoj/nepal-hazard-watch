package watch.nepalhazard.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import watch.nepalhazard.entity.HazardEvent;

public interface HazardEventRepository extends JpaRepository<HazardEvent, Long> {
    @Query(value = "SELECT * FROM hazard_events WHERE event_type = 'EARTHQUAKE' ORDER BY event_time DESC LIMIT 2", nativeQuery = true)
    List<HazardEvent> findLatestTwoEarthquakes();

    // excludes source_type='landslide' - those are scored separately via findRecentLandslides()
    @Query(value = "SELECT * FROM hazard_events WHERE event_type = 'EARTHQUAKE' "
            + "AND (source_type IS NULL OR source_type = 'earthquake') AND event_time >= :since ORDER BY event_time DESC", nativeQuery = true)
    List<HazardEvent> findRecentEarthquakes(@Param("since") LocalDateTime since);

    @Query(value = "SELECT * FROM hazard_events WHERE event_type = 'EARTHQUAKE' AND source_type = 'landslide' "
            + "AND event_time >= :since ORDER BY event_time DESC", nativeQuery = true)
    List<HazardEvent> findRecentLandslides(@Param("since") LocalDateTime since);

    @Query(value = "SELECT COUNT(*) > 0 FROM hazard_events WHERE event_type = 'EARTHQUAKE' "
            + "AND ABS(magnitude - :magnitude) < 0.1 AND ABS(latitude - :lat) < 0.1 AND ABS(longitude - :lon) < 0.1", nativeQuery = true)
    boolean existsSimilarEarthquake(@Param("magnitude") double magnitude, @Param("lat") double lat,
            @Param("lon") double lon);
}