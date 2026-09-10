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

/** Sentinel-2 water-fraction reading per lake. Skips lakes with a fresh-enough observation so app restarts don't burn the free processing-unit quota. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SatelliteLakeTrackingService {

    private static final double AOI_HALF_WIDTH_DEG = 0.012;
    private static final int LOOKBACK_DAYS = 10;
    private static final int MIN_REFRESH_INTERVAL_DAYS = 4;
    // Backfill window: old enough to be an independent pass, recent enough that growth-since-then is still meaningful.
    private static final int BACKFILL_WINDOW_END_DAYS_AGO = 25;
    private static final int BACKFILL_WINDOW_LENGTH_DAYS = 15;
    // Observed rate limit is tighter than the documented 300/min; a 1s gap alone still produced 429s.
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

    /** One-time backfill: pulls an older archive reading for any lake with only one observation. Safe to call on every startup. */
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
                // no current reading yet; backfill only fills the gap behind an existing one
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
