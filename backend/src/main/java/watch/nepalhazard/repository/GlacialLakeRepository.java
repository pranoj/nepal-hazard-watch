package watch.nepalhazard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import watch.nepalhazard.entity.GlacialLake;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlacialLakeRepository extends JpaRepository<GlacialLake, Long> {

    Optional<GlacialLake> findByIcimodId(@Param("icimodId") String icimodId);

    /** Lakes physically in Nepal, plus transboundary lakes within Nepal's border envelope (e.g. the Aug 2026 Kyirong-Rasuwa GLOF). */
    @Query("SELECT g FROM GlacialLake g WHERE g.country = 'Nepal' "
            + "OR (g.transboundary = true AND g.latitude BETWEEN 26.0 AND 30.5 AND g.longitude BETWEEN 80.0 AND 88.5) "
            + "ORDER BY g.latitude DESC, g.longitude ASC")
    List<GlacialLake> findAllLakesInNepal();
}