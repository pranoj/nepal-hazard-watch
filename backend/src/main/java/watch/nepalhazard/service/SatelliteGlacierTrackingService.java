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
import watch.nepalhazard.entity.Glacier;
import watch.nepalhazard.entity.GlacierSatelliteObservation;
import watch.nepalhazard.repository.GlacierRepository;
import watch.nepalhazard.repository.GlacierSatelliteObservationRepository;

/** Sentinel-2 NDSI ice/snow-cover reading per glacier. No historical backfill - unlike lake growth, a terminus collapse is sudden, so today's first reading is the baseline. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SatelliteGlacierTrackingService {

    private static final double AOI_HALF_WIDTH_DEG = 0.012;
    private static final int LOOKBACK_DAYS = 10;
    private static final int MIN_REFRESH_INTERVAL_DAYS = 4;
    // Same headroom as SatelliteLakeTrackingService - observed 429s were tighter than the documented 300/min.
    private static final long REQUEST_DELAY_MS = 3000;

    private final GlacierRepository glacierRepository;
    private final GlacierSatelliteObservationRepository observationRepository;
    private final SentinelHubClient sentinelHubClient;

    @Scheduled(fixedRateString = "${satellite.tracking-interval-ms}")
    public void trackAllGlaciers() {
        List<Glacier> glaciers = glacierRepository.findAll();
        log.info("Checking satellite glacier ice-cover tracking for {} glaciers", glaciers.size());

        int fetched = 0;
        int skippedFresh = 0;
        int failed = 0;

        for (Glacier glacier : glaciers) {
            if (glacier.getRgiId() == null || glacier.getTerminusLatitude() == null
                    || glacier.getTerminusLongitude() == null) {
                continue;
            }
            if (hasFreshObservation(glacier.getRgiId())) {
                skippedFresh++;
                continue;
            }

            LocalDate to = LocalDate.now();
            LocalDate from = to.minusDays(LOOKBACK_DAYS);
            if (fetchAndSave(glacier, from, to)) {
                fetched++;
            } else {
                failed++;
            }
            politeDelay();
        }

        log.info("Satellite glacier ice-cover tracking complete: {} fetched, {} already fresh, {} failed/no-data",
                fetched, skippedFresh, failed);
    }

    private boolean fetchAndSave(Glacier glacier, LocalDate from, LocalDate to) {
        try {
            SentinelHubClient.IceCoverReading reading = sentinelHubClient.fetchIceFraction(
                    glacier.getTerminusLatitude(), glacier.getTerminusLongitude(), AOI_HALF_WIDTH_DEG, from, to);

            if (reading.validPixels() <= 0 || Double.isNaN(reading.iceFraction())) {
                log.debug("No usable Sentinel-2 pass for glacier {} between {} and {}", glacier.getRgiId(), from, to);
                return false;
            }

            GlacierSatelliteObservation observation = GlacierSatelliteObservation.builder()
                    .rgiId(glacier.getRgiId())
                    .observedAt(resolveObservedAt(reading))
                    .iceFraction(reading.iceFraction())
                    .meanNdsi(reading.meanNdsi())
                    .validPixelFraction((double) reading.validPixels() / reading.totalPixels())
                    .build();
            observationRepository.save(observation);
            return true;

        } catch (Exception e) {
            log.warn("Satellite ice-cover tracking failed for glacier {}: {}", glacier.getRgiId(), e.getMessage());
            return false;
        }
    }

    private LocalDateTime resolveObservedAt(SentinelHubClient.IceCoverReading reading) {
        if (reading.intervalTo() == null) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(reading.intervalTo()), ZoneOffset.UTC);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private boolean hasFreshObservation(String rgiId) {
        List<GlacierSatelliteObservation> recent = observationRepository.findTop2ByRgiIdOrderByObservedAtDesc(rgiId);
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
