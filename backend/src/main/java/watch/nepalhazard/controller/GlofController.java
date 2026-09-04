package watch.nepalhazard.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import watch.nepalhazard.dto.GLOFRiskRequest;
import watch.nepalhazard.entity.HazardEvent;
import watch.nepalhazard.repository.HazardEventRepository;
import watch.nepalhazard.service.GLOFRiskCalculationService;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/glof")
public class GlofController {

    private final HazardEventRepository hazardEventRepository;
    private final GLOFRiskCalculationService glofService;

    @Autowired
    public GlofController(HazardEventRepository hazardEventRepository, GLOFRiskCalculationService glofService) {
        this.hazardEventRepository = hazardEventRepository;
        this.glofService = glofService;
    }

    @GetMapping("/test")
    public String test() {
        return "GLOF Controller is working!";
    }

    @PostMapping("/calculate-risk")
    public ResponseEntity<?> calculateGLOFRisk(@RequestBody GLOFRiskRequest request) {
        try {
            if (request.getEarthquakeId() == null || request.getEarthquakeId() <= 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid earthquake ID");
                return ResponseEntity.badRequest().body(error);
            }

            Optional<HazardEvent> earthquake = hazardEventRepository.findById(request.getEarthquakeId());
            if (earthquake.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Earthquake not found with ID: " + request.getEarthquakeId());
                return ResponseEntity.status(404).body(error);
            }

            Double glacierLat = request.getGlacierLat();
            Double glacierLon = request.getGlacierLon();
            if (glacierLat == null || glacierLon == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Glacier latitude and longitude are required");
                return ResponseEntity.badRequest().body(error);
            }

            double riskScore = glofService.calculateGLOFRisk(
                    earthquake.get(),
                    glacierLat,
                    glacierLon,
                    0.0);

            String alertLevel = glofService.getAlertLevel(riskScore);

            Map<String, Object> response = new HashMap<>();
            response.put("riskScore", riskScore);
            response.put("alertLevel", alertLevel);
            response.put("earthquake", earthquake.get());
            response.put("glacierLocation", Map.of(
                    "latitude", glacierLat,
                    "longitude", glacierLon));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error calculating GLOF risk: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}