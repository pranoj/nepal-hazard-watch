package watch.nepalhazard.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import watch.nepalhazard.dto.EarthquakeAlertStatusResponse;
import watch.nepalhazard.entity.HazardEvent;
import watch.nepalhazard.repository.HazardEventRepository;

@RestController
@RequestMapping("/latest-earthquake")
@CrossOrigin(origins = "http://localhost:5173")
public class LatestEarthquakeController {

    private final HazardEventRepository hazardEventRepository;

    @Autowired
    public LatestEarthquakeController(HazardEventRepository hazardEventRepository) {
        this.hazardEventRepository = hazardEventRepository;
    }

    @GetMapping("/alert-status")
    public EarthquakeAlertStatusResponse getEarthquakeAlertStatus() {
        List<HazardEvent> latestTwo = hazardEventRepository.findLatestTwoEarthquakes();

        if (latestTwo.isEmpty()) {
            return null;
        }

        HazardEvent latest = latestTwo.get(0);
        HazardEvent previous = latestTwo.size() > 1 ? latestTwo.get(1) : null;

        return EarthquakeAlertStatusResponse.create(latest, previous);
    }
}
