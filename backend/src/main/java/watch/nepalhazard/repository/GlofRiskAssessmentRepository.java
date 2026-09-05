package watch.nepalhazard.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import watch.nepalhazard.entity.GlofRiskAssessment;

public interface GlofRiskAssessmentRepository extends JpaRepository<GlofRiskAssessment, Long> {

    Optional<GlofRiskAssessment> findByGlacialLakeId(Long glacialLakeId);

    Optional<GlofRiskAssessment> findByGlacierId(Long glacierId);

    @Query("SELECT g FROM GlofRiskAssessment g ORDER BY g.riskScore DESC")
    List<GlofRiskAssessment> findAllOrderByRiskScoreDesc();
}
