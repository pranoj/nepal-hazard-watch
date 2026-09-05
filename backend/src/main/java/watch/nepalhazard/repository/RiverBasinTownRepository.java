package watch.nepalhazard.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import watch.nepalhazard.entity.RiverBasinTown;

public interface RiverBasinTownRepository extends JpaRepository<RiverBasinTown, Long> {

    List<RiverBasinTown> findByRiverBasinOrderByDownstreamOrderAsc(String riverBasin);
}
