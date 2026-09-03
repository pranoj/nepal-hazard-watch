package watch.nepalhazard.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import watch.nepalhazard.entity.HazardEvent;

public interface HazardEventRepository extends JpaRepository<HazardEvent, Long> {
    HazardEvent findByStatus(String status);

    HazardEvent findTopByOrderByEventTimeDesc();

    @Query(value = "SELECT * FROM hazard_events ORDER BY event_time DESC LIMIT 2", nativeQuery = true)
    List<HazardEvent> findLatestTwoEarthquakes();
}