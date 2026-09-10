package watch.nepalhazard.controller;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import watch.nepalhazard.service.SentinelHubClient;

/** Manual test surface for the Sentinel Hub integration - not linked from the frontend or used by the risk formula. */
@RestController
@RequestMapping("/satellite")
public class SatelliteTestController {

    private static final double TSHO_ROLPA_LAT = 27.867;
    private static final double TSHO_ROLPA_LON = 86.467;

    private final SentinelHubClient sentinelHubClient;

    @Autowired
    public SatelliteTestController(SentinelHubClient sentinelHubClient) {
        this.sentinelHubClient = sentinelHubClient;
    }

    /** GET /api/satellite/test-tsho-rolpa?days=30 - live call against Tsho Rolpa's coordinates. */
    @GetMapping("/test-tsho-rolpa")
    public ResponseEntity<?> testTshoRolpa(@RequestParam(defaultValue = "30") int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days);
        var reading = sentinelHubClient.fetchWaterFraction(TSHO_ROLPA_LAT, TSHO_ROLPA_LON, 0.012, from, to);
        return ResponseEntity.ok(reading);
    }

    /** GET /api/satellite/test?lat=..&lon=..&halfWidthDeg=0.01&days=30 - same call for an arbitrary point. */
    @GetMapping("/test")
    public ResponseEntity<?> test(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "0.01") double halfWidthDeg,
            @RequestParam(defaultValue = "30") int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days);
        var reading = sentinelHubClient.fetchWaterFraction(lat, lon, halfWidthDeg, from, to);
        return ResponseEntity.ok(reading);
    }
}
