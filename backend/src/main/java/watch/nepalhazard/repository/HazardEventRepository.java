package watch.nepalhazard.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import watch.nepalhazard.entity.HazardEvent;

public interface HazardEventRepository extends JpaRepository<HazardEvent, Long> {

    List<HazardEvent> findByStatus(String status);
}
