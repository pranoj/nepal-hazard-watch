package watch.nepalhazard.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import watch.nepalhazard.entity.GlacialLake;
import watch.nepalhazard.repository.GlacialLakeRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Glacial Lake queries
 * Data Source: ICIMOD (CC BY 4.0)
 */
@Slf4j
@RestController
@RequestMapping("/glacial-lakes")
@RequiredArgsConstructor
public class GlacialLakeController {

    private final GlacialLakeRepository glacialLakeRepository;

    @GetMapping
    public ResponseEntity<List<GlacialLake>> getAllLakesInNepal() {
        log.info("Fetching all glacial lakes in Nepal");
        try {
            List<GlacialLake> lakes = glacialLakeRepository.findAllLakesInNepal();
            log.info("Retrieved {} glacial lakes", lakes.size());
            return ResponseEntity.ok(lakes);
        } catch (Exception e) {
            log.error("Error fetching glacial lakes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/high-risk")
    public ResponseEntity<List<GlacialLake>> getHighRiskLakes() {
        log.info("Fetching high-risk glacial lakes");
        try {
            List<GlacialLake> highRiskLakes = glacialLakeRepository.findHighRiskLakesByCountry("Nepal");
            log.info("Retrieved {} high-risk lakes", highRiskLakes.size());
            return ResponseEntity.ok(highRiskLakes);
        } catch (Exception e) {
            log.error("Error fetching high-risk lakes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<?> findNearestLake(
            @RequestParam(name = "lat") double latitude,
            @RequestParam(name = "lon") double longitude) {
        log.info("Finding nearest lake to ({}, {})", latitude, longitude);
        try {
            Optional<GlacialLake> nearestLake = glacialLakeRepository.findNearestByCoordinates(latitude, longitude);
            if (nearestLake.isPresent()) {
                return ResponseEntity.ok(nearestLake.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No glacial lakes found"));
            }
        } catch (Exception e) {
            log.error("Error finding nearest lake: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/within-radius")
    public ResponseEntity<List<GlacialLake>> findLakesWithinRadius(
            @RequestParam(name = "lat") double latitude,
            @RequestParam(name = "lon") double longitude,
            @RequestParam(name = "radius", defaultValue = "0.5") double radiusDegrees) {
        log.info("Finding lakes within {} degrees of ({}, {})", radiusDegrees, latitude, longitude);
        try {
            List<GlacialLake> lakesInRadius = glacialLakeRepository.findLakesWithinRadius(latitude, longitude,
                    radiusDegrees);
            log.info("Found {} lakes within radius", lakesInRadius.size());
            return ResponseEntity.ok(lakesInRadius);
        } catch (Exception e) {
            log.error("Error finding lakes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{icimodId}")
    public ResponseEntity<?> getLakeByIcimodId(@PathVariable String icimodId) {
        log.info("Fetching lake: {}", icimodId);
        try {
            Optional<GlacialLake> lake = glacialLakeRepository.findByIcimodId(icimodId);
            if (lake.isPresent()) {
                return ResponseEntity.ok(lake.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Lake not found: " + icimodId));
            }
        } catch (Exception e) {
            log.error("Error fetching lake {}: {}", icimodId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/country/{country}")
    public ResponseEntity<List<GlacialLake>> getLakesByCountry(@PathVariable String country) {
        log.info("Fetching lakes in: {}", country);
        try {
            List<GlacialLake> lakes = glacialLakeRepository.findByCountry(country);
            log.info("Retrieved {} lakes in {}", lakes.size(), country);
            return ResponseEntity.ok(lakes);
        } catch (Exception e) {
            log.error("Error fetching lakes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/risk/{riskLevel}")
    public ResponseEntity<List<GlacialLake>> getLakesByRiskLevel(@PathVariable String riskLevel) {
        log.info("Fetching lakes with risk level: {}", riskLevel);
        try {
            List<GlacialLake> lakes = glacialLakeRepository.findByRiskLevel(riskLevel);
            log.info("Retrieved {} lakes", lakes.size());
            return ResponseEntity.ok(lakes);
        } catch (Exception e) {
            log.error("Error fetching lakes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("Fetching glacier statistics");
        try {
            List<GlacialLake> allLakes = glacialLakeRepository.findAllLakesInNepal();
            List<GlacialLake> highRiskLakes = glacialLakeRepository.findHighRiskLakesByCountry("Nepal");

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalLakesInNepal", allLakes.size());
            stats.put("highRiskLakes", highRiskLakes.size());
            stats.put("dataSource", "ICIMOD (CC BY 4.0)");
            stats.put("message", "Nepal Glacial Lake Database - Real-time GLOF Risk Monitoring");

            log.info("Stats: {} total, {} high-risk", allLakes.size(), highRiskLakes.size());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}