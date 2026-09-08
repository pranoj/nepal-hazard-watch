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

/**
 * Startup data initializer
 * 
 * Automatically loads ICIMOD glacial lake data when the Spring Boot application
 * finishes starting up. This ensures all glacial lakes are in the database
 * before
 * any API requests are processed.
 * 
 * Data Source: ICIMOD (International Centre for Integrated Mountain
 * Development)
 * License: CC BY 4.0 (Creative Commons Attribution 4.0 International)
 */
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

            // Initialize "Type B" glacier watch points (steep terminus near
            // a known river corridor) - relies on river_basin_towns already
            // being seeded, which happens earlier via CommandLineRunner
            glacierSyncService.initializeGlacierData();

            // Real local terminus slope, from elevation samples - runs once,
            // skips glaciers that already have a value.
            terrainSlopeService.fillMissingLocalSlopes();

            // Real older Sentinel-2 reading for any lake that only has one
            // observation so far, so growth comparison works immediately
            // instead of waiting for the next live 5-day cycle. Skips lakes
            // that already have two or more.
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

            // Note: We log the error but don't throw - allows app to start even if ICIMOD
            // load fails
            // This is intentional for production resilience
            log.warn("⚠️  App started without ICIMOD data - manual sync may be needed");
        }
    }
}