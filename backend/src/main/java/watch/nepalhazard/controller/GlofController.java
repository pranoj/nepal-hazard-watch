package watch.nepalhazard.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import watch.nepalhazard.entity.GlofRiskAssessment;
import watch.nepalhazard.entity.RiverBasinTown;
import watch.nepalhazard.repository.GlofRiskAssessmentRepository;
import watch.nepalhazard.repository.RiverBasinTownRepository;

@RestController
@RequestMapping("/glof")
public class GlofController {

    private final GlofRiskAssessmentRepository glofRiskAssessmentRepository;
    private final RiverBasinTownRepository riverBasinTownRepository;

    @Autowired
    public GlofController(GlofRiskAssessmentRepository glofRiskAssessmentRepository,
            RiverBasinTownRepository riverBasinTownRepository) {
        this.glofRiskAssessmentRepository = glofRiskAssessmentRepository;
        this.riverBasinTownRepository = riverBasinTownRepository;
    }

    @GetMapping("/risk-map")
    public ResponseEntity<List<GlofRiskAssessment>> getRiskMap() {
        return ResponseEntity.ok(glofRiskAssessmentRepository.findAllOrderByRiskScoreDesc());
    }

    /** All basin-to-downstream-town mappings in one shot (~40 rows) so the frontend can look up client-side without a per-lake round trip. */
    @GetMapping("/downstream-towns")
    public ResponseEntity<List<RiverBasinTown>> getDownstreamTowns() {
        return ResponseEntity.ok(riverBasinTownRepository.findAll());
    }
}
