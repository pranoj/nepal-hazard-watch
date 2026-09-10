package watch.nepalhazard.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import watch.nepalhazard.service.GlacierSyncService;
import watch.nepalhazard.service.ICIMODGlacialLakeService;
import watch.nepalhazard.service.SatelliteLakeTrackingService;
import watch.nepalhazard.service.TerrainSlopeService;

/** Loads ICIMOD glacial lake data (CC BY 4.0) on startup, before any API requests are processed. */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupDataInitializer {

    private final ICIMODGlacialLakeService icimodGlacialLakeService;
    private final GlacierSyncService glacierSyncService;
    private final TerrainSlopeService terrainSlopeService;
    private final SatelliteLakeTrackingService satelliteLakeTrackingService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("🌍 APPLICATION STARTUP - Initializing ICIMOD Data");
        log.info("═══════════════════════════════════════════════════════════");

        try {
            icimodGlacialLakeService.initializeICIMODData();
            long lakeCount = icimodGlacialLakeService.getNepalGlacialLakeCount();

            // relies on river_basin_towns already being seeded via CommandLineRunner
            glacierSyncService.initializeGlacierData();

            // local terminus slope from elevation samples; skips glaciers that already have a value
            terrainSlopeService.fillMissingLocalSlopes();

            // one-time backfill so growth comparison works before the next live 5-day cycle
            satelliteLakeTrackingService.backfillHistoricalBaseline();

            log.info("═══════════════════════════════════════════════════════════");
            log.info("✅ STARTUP COMPLETE - {} glacial lakes loaded from ICIMOD", lakeCount);
            log.info("📍 Ready to serve GLOF risk calculations!");
            log.info("═══════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("═══════════════════════════════════════════════════════════");
            log.error("❌ STARTUP ERROR - Failed to initialize ICIMOD data");
            log.error("Error: {}", e.getMessage(), e);
            log.error("═══════════════════════════════════════════════════════════");

            // intentionally not rethrown - app should start even if ICIMOD load fails
            log.warn("⚠️  App started without ICIMOD data - manual sync may be needed");
        }
    }
}