package watch.nepalhazard.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import watch.nepalhazard.entity.GlacierSatelliteObservation;

public interface GlacierSatelliteObservationRepository extends JpaRepository<GlacierSatelliteObservation, Long> {

    /** Newest first; index [0] is latest, [1] is the one to compare it against for a sudden-drop check. */
    List<GlacierSatelliteObservation> findTop2ByRgiIdOrderByObservedAtDesc(String rgiId);
}
