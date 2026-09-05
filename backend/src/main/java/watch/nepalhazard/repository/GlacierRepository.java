package watch.nepalhazard.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import watch.nepalhazard.entity.Glacier;

public interface GlacierRepository extends JpaRepository<Glacier, Long> {

    Optional<Glacier> findByRgiId(String rgiId);
}
