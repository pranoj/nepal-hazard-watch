package watch.nepalhazard.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import watch.nepalhazard.entity.GlofRiskAssessment;
import watch.nepalhazard.entity.RiverBasinTown;
import watch.nepalhazard.repository.GlofRiskAssessmentRepository;
import watch.nepalhazard.repository.RiverBasinTownRepository;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
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

    @GetMapping("/test")
    public String test() {
        return "GLOF Controller is working!";
    }

    @GetMapping("/risk-map")
    public ResponseEntity<List<GlofRiskAssessment>> getRiskMap() {
        return ResponseEntity.ok(glofRiskAssessmentRepository.findAllOrderByRiskScoreDesc());
    }

    /**
     * All known basin-to-downstream-town mappings in one shot (small, ~40
     * rows) so the frontend can look up towns for any at-risk lake's basin
     * client-side without a per-lake round trip.
     */
    @GetMapping("/downstream-towns")
    public ResponseEntity<List<RiverBasinTown>> getDownstreamTowns() {
        return ResponseEntity.ok(riverBasinTownRepository.findAll());
    }
}
