package watch.nepalhazard.service;

import com.opencsv.bean.CsvToBeanBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import watch.nepalhazard.dto.RGIGlacierDTO;
import watch.nepalhazard.entity.Glacier;
import watch.nepalhazard.entity.RiverBasinTown;
import watch.nepalhazard.repository.GlacierRepository;
import watch.nepalhazard.repository.RiverBasinTownRepository;

/**
 * Imports "Type B" glacier watch points from the RGI (Randolph Glacier
 * Inventory) v7 export - glaciers with a steep terminus near a known river
 * corridor, capable of collapsing and damming a river directly without any
 * pre-existing lake, as happened at Langtang Lirung on Aug 2026.
 *
 * Source CSV (rgi_glaciers_nepal_envelope.csv) is already pre-filtered to
 * Nepal's border envelope and slope > 20 degrees (see
 * backend/src/main/resources/data/geospatial/ for the one-time processing
 * notes). This service applies the real qualification: steeper terminus
 * AND within a plausible distance of a known river corridor point.
 *
 * Glacier physical properties don't change day to day (unlike weather or
 * earthquakes), so this runs once at startup rather than on a schedule.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlacierSyncService {

    private static final double MIN_SLOPE_DEG = 30.0;
    private static final double MAX_RIVER_DISTANCE_KM = 10.0;

    private final GlacierRepository glacierRepository;
    private final RiverBasinTownRepository riverBasinTownRepository;

    public void initializeGlacierData() {
        try {
            List<RGIGlacierDTO> candidates = parseGlacierCSV();
            log.info("Parsed {} candidate glaciers from RGI export", candidates.size());

            List<RiverBasinTown> riverTowns = riverBasinTownRepository.findAll();
            if (riverTowns.isEmpty()) {
                log.warn("No river basin towns loaded yet - glacier river-proximity filtering skipped");
                return;
            }

            int saved = 0;
            int updated = 0;
            int skipped = 0;

            for (RGIGlacierDTO candidate : candidates) {
                if (candidate.getSlopeDeg() == null || candidate.getSlopeDeg() < MIN_SLOPE_DEG) {
                    skipped++;
                    continue;
                }

                NearestTown nearest = findNearestRiverTown(candidate.getTermLat(), candidate.getTermLon(),
                        riverTowns);
                if (nearest == null || nearest.distanceKm > MAX_RIVER_DISTANCE_KM) {
                    skipped++;
                    continue;
                }

                String fallbackName = String.format("Glacier %.1fkm from %s", nearest.distanceKm,
                        nearest.town.getTownName());

                Glacier glacier = Glacier.builder()
                        .rgiId(candidate.getRgiId())
                        .glacierName(isKnownName(candidate.getGlacierName()) ? candidate.getGlacierName().trim()
                                : fallbackName)
                        .terminusLatitude(candidate.getTermLat())
                        .terminusLongitude(candidate.getTermLon())
                        .slopeDeg(candidate.getSlopeDeg())
                        .areaKm2(candidate.getAreaKm2())
                        .nearestRiverBasin(nearest.town.getRiverBasin())
                        .nearestTownName(nearest.town.getTownName())
                        .nearestRiverTownKm(nearest.distanceKm)
                        .updatedAt(LocalDateTime.now())
                        .build();

                if (saveOrUpdate(glacier)) {
                    saved++;
                } else {
                    updated++;
                }
            }

            log.info("Glacier watch-point sync complete: {} saved, {} updated, {} skipped (below threshold)",
                    saved, updated, skipped);

        } catch (Exception e) {
            log.error("Error initializing glacier watch points: {}", e.getMessage(), e);
        }
    }

    private boolean saveOrUpdate(Glacier glacier) {
        Optional<Glacier> existing = glacierRepository.findByRgiId(glacier.getRgiId());
        boolean isNew = existing.isEmpty();

        existing.ifPresent(existingGlacier -> glacier.setId(existingGlacier.getId()));
        if (isNew) {
            glacier.setCreatedAt(LocalDateTime.now());
        }
        glacierRepository.save(glacier);
        return isNew;
    }

    private NearestTown findNearestRiverTown(double lat, double lon, List<RiverBasinTown> towns) {
        RiverBasinTown best = null;
        double bestDistance = Double.MAX_VALUE;

        for (RiverBasinTown town : towns) {
            double distance = haversineDistance(lat, lon, town.getLatitude(), town.getLongitude());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = town;
            }
        }

        return best == null ? null : new NearestTown(best, bestDistance);
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.asin(Math.sqrt(a));
    }

    private boolean isKnownName(String value) {
        return value != null && !value.trim().isEmpty();
    }

    List<RGIGlacierDTO> parseGlacierCSV() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("data/rgi_glaciers_nepal_envelope.csv")) {
            if (inputStream == null) {
                throw new IOException("RGI glacier CSV not found at: data/rgi_glaciers_nepal_envelope.csv");
            }

            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            return new CsvToBeanBuilder<RGIGlacierDTO>(reader)
                    .withType(RGIGlacierDTO.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withThrowExceptions(false)
                    .build()
                    .parse();
        }
    }

    private record NearestTown(RiverBasinTown town, double distanceKm) {
    }
}
