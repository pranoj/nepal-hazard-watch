package watch.nepalhazard.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import watch.nepalhazard.entity.GlacialLake;
import watch.nepalhazard.entity.LakeSatelliteObservation;
import watch.nepalhazard.repository.GlacialLakeRepository;
import watch.nepalhazard.repository.LakeSatelliteObservationRepository;

/**
 * Collects a real Sentinel-2 water-fraction reading for every lake, on a
 * cadence matched to Sentinel-2's real revisit rate rather than app
 * restarts - a lake with an observation newer than
 * MIN_REFRESH_INTERVAL_DAYS is skipped, so restarting the app during
 * development doesn't silently burn through the free processing-unit
 * quota re-fetching imagery that hasn't changed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SatelliteLakeTrackingService {

    private static final double AOI_HALF_WIDTH_DEG = 0.012;
    private static final int LOOKBACK_DAYS = 10;
    private static final int MIN_REFRESH_INTERVAL_DAYS = 4;
    // For the one-time historical backfill: a real older reading, far enough
    // back to be a genuine independent pass, not just yesterday's near-
    // duplicate, but recent enough that "growth since then" is still a
    // meaningful comparison rather than a different season entirely.
    private static final int BACKFILL_WINDOW_END_DAYS_AGO = 25;
    private static final int BACKFILL_WINDOW_LENGTH_DAYS = 15;
    // Sentinel Hub's real observed rate limit during testing was tighter
    // than the documented 300/min - a 1s gap alone still produced 429s, so
    // this leaves more headroom, backed up by SentinelHubClient's own
    // retry-with-backoff for the ones that still land close together.
    private static final long REQUEST_DELAY_MS = 3000;

    private final GlacialLakeRepository glacialLakeRepository;
    private final LakeSatelliteObservationRepository observationRepository;
    private final SentinelHubClient sentinelHubClient;

    @Scheduled(fixedRateString = "${satellite.tracking-interval-ms}")
    public void trackAllLakes() {
        List<GlacialLake> lakes = glacialLakeRepository.findAllLakesInNepal();
        log.info("Checking satellite lake-area tracking for {} lakes", lakes.size());

        int fetched = 0;
        int skippedFresh = 0;
        int failed = 0;

        for (GlacialLake lake : lakes) {
            if (lake.getIcimodId() == null || lake.getLatitude() == null || lake.getLongitude() == null) {
                continue;
            }
            if (hasFreshObservation(lake.getIcimodId())) {
                skippedFresh++;
                continue;
            }

            LocalDate to = LocalDate.now();
            LocalDate from = to.minusDays(LOOKBACK_DAYS);
            if (fetchAndSave(lake, from, to)) {
                fetched++;
            } else {
                failed++;
            }
            politeDelay();
        }

        log.info("Satellite lake-area tracking complete: {} fetched, {} already fresh, {} failed/no-data",
                fetched, skippedFresh, failed);
    }

    /**
     * One-time historical backfill: any lake with only one real observation
     * (nothing to compare it against yet) gets a real older reading pulled
     * from Sentinel-2's archive, so growth comparison works immediately
     * instead of waiting for the next 5-day live cycle. Skips lakes that
     * already have two or more - safe to call on every startup.
     */
    public void backfillHistoricalBaseline() {
        List<GlacialLake> lakes = glacialLakeRepository.findAllLakesInNepal();
        log.info("Checking satellite historical backfill for {} lakes", lakes.size());

        int fetched = 0;
        int skippedHasBaseline = 0;
        int skippedNoCurrent = 0;
        int failed = 0;

        for (GlacialLake lake : lakes) {
            if (lake.getIcimodId() == null || lake.getLatitude() == null || lake.getLongitude() == null) {
                continue;
            }
            List<LakeSatelliteObservation> existing = observationRepository
                    .findTop2ByIcimodIdOrderByObservedAtDesc(lake.getIcimodId());
            if (existing.size() >= 2) {
                skippedHasBaseline++;
                continue;
            }
            if (existing.isEmpty()) {
                // No current reading either yet - let the live job get one
                // first; backfill only fills the gap behind an existing one.
                skippedNoCurrent++;
                continue;
            }

            LocalDate to = LocalDate.now().minusDays(BACKFILL_WINDOW_END_DAYS_AGO);
            LocalDate from = to.minusDays(BACKFILL_WINDOW_LENGTH_DAYS);
            if (fetchAndSave(lake, from, to)) {
                fetched++;
            } else {
                failed++;
            }
            politeDelay();
        }

        log.info("Satellite historical backfill complete: {} fetched, {} already had a baseline, "
                + "{} had no current reading yet, {} failed/no-data",
                fetched, skippedHasBaseline, skippedNoCurrent, failed);
    }

    /** Fetches one reading for a lake and saves it if usable. Returns whether it succeeded. */
    private boolean fetchAndSave(GlacialLake lake, LocalDate from, LocalDate to) {
        try {
            SentinelHubClient.LakeWaterReading reading = sentinelHubClient.fetchWaterFraction(
                    lake.getLatitude(), lake.getLongitude(), AOI_HALF_WIDTH_DEG, from, to);

            if (reading.validPixels() <= 0 || Double.isNaN(reading.waterFraction())) {
                log.debug("No usable Sentinel-2 pass for {} between {} and {}", lake.getIcimodId(), from, to);
                return false;
            }

            LakeSatelliteObservation observation = LakeSatelliteObservation.builder()
                    .icimodId(lake.getIcimodId())
                    .observedAt(resolveObservedAt(reading))
                    .waterFraction(reading.waterFraction())
                    .meanNdwi(reading.meanNdwi())
                    .validPixelFraction((double) reading.validPixels() / reading.totalPixels())
                    .build();
            observationRepository.save(observation);
            return true;

        } catch (Exception e) {
            log.warn("Satellite tracking failed for lake {}: {}", lake.getIcimodId(), e.getMessage());
            return false;
        }
    }

    /** Uses the real date of the satellite pass when available, falling back to now. */
    private LocalDateTime resolveObservedAt(SentinelHubClient.LakeWaterReading reading) {
        if (reading.intervalTo() == null) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(reading.intervalTo()), ZoneOffset.UTC);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private boolean hasFreshObservation(String icimodId) {
        List<LakeSatelliteObservation> recent = observationRepository.findTop2ByIcimodIdOrderByObservedAtDesc(icimodId);
        if (recent.isEmpty()) {
            return false;
        }
        return recent.get(0).getObservedAt().isAfter(LocalDateTime.now().minusDays(MIN_REFRESH_INTERVAL_DAYS));
    }

    private void politeDelay() {
        try {
            Thread.sleep(REQUEST_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
