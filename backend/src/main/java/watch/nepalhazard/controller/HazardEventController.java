package watch.nepalhazard.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import watch.nepalhazard.dto.HazardEventDTO;
import watch.nepalhazard.repository.HazardEventRepository;

@RestController
@RequestMapping("/hazard-events")
public class HazardEventController {

    private final HazardEventRepository hazardEventRepository;

    @Autowired
    public HazardEventController(HazardEventRepository hazardEventRepository) {
        this.hazardEventRepository = hazardEventRepository;
    }

    @GetMapping
    public ResponseEntity<List<HazardEventDTO>> getHazardEvents() {
        List<HazardEventDTO> hazardEvents = hazardEventRepository.findAll().stream()
                .map(HazardEventDTO::from)
                .toList();
        return ResponseEntity.ok(hazardEvents);
    }
}