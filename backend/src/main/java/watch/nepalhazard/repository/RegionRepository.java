package watch.nepalhazard.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import watch.nepalhazard.entity.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {

    @Query(value = "SELECT * FROM regions ORDER BY (latitude - :lat) * (latitude - :lat) + (longitude - :lon) * (longitude - :lon) ASC LIMIT 1", nativeQuery = true)
    Optional<Region> findNearestRegion(@Param("lat") double latitude, @Param("lon") double longitude);
}
