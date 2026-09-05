package watch.nepalhazard.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import watch.nepalhazard.service.GlacierSyncService;
import watch.nepalhazard.service.ICIMODGlacialLakeService;

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

    /**
     * Initialize ICIMOD data when Spring Boot application is ready
     * This runs automatically on startup - no manual intervention needed!
     * 
     * Fires after:
     * - All beans are created
     * - Application context is fully loaded
     * - Server is about to start listening for requests
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("🌍 APPLICATION STARTUP - Initializing ICIMOD Data");
        log.info("═══════════════════════════════════════════════════════════");

        try {
            // Initialize ICIMOD glacial lake data
            icimodGlacialLakeService.initializeICIMODData();

            // Get count of loaded lakes
            long lakeCount = icimodGlacialLakeService.getNepalGlacialLakeCount();

            // Initialize "Type B" glacier watch points (steep terminus near
            // a known river corridor) - relies on river_basin_towns already
            // being seeded, which happens earlier via CommandLineRunner
            glacierSyncService.initializeGlacierData();

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