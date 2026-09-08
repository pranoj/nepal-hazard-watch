package watch.nepalhazard.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import watch.nepalhazard.entity.LakeSatelliteObservation;

public interface LakeSatelliteObservationRepository extends JpaRepository<LakeSatelliteObservation, Long> {

    /** Newest first; index [0] is latest, [1] is the one to compare it against for short-term growth. */
    List<LakeSatelliteObservation> findTop2ByIcimodIdOrderByObservedAtDesc(String icimodId);
}
