package watch.nepalhazard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import watch.nepalhazard.entity.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {
}
