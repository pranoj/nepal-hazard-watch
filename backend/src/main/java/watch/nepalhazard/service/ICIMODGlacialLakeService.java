package watch.nepalhazard.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import watch.nepalhazard.dto.ICIMODGlacialLakeDTO;
import watch.nepalhazard.entity.GlacialLake;
import watch.nepalhazard.repository.GlacialLakeRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for integrating ICIMOD Glacial Lake data into the application
 * 
 * Data Source: ICIMOD (International Centre for Integrated Mountain
 * Development)
 * Database: HMAGLOFDB (Hindu-Kush Himalayan Glacial Lake Outburst Flood
 * Database)
 * License: CC BY 4.0 (Creative Commons Attribution 4.0 International)
 * 
 * IMPORTANT: Any use of this data must include proper attribution to ICIMOD
 * and comply with CC BY 4.0 licensing requirements.
 * 
 * Reference: https://www.icimod.org/
 * 
 * Responsibilities:
 * - Load ICIMOD CSV data from project resources
 * - Parse CSV into GlacialLake entities
 * - Filter for Nepal-specific data only
 * - Sync with database (avoid duplicates using icimodId)
 * - Run periodic refreshes on a schedule
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ICIMODGlacialLakeService {

    private final GlacialLakeRepository glacialLakeRepository;

    @Value("${icimod.csv.resource-path:classpath:data/HMAGLOFDB_v4.0_13122025.csv}")
    private String csvResourcePath;

    @Value("${icimod.sync.enabled:true}")
    private boolean syncEnabled;

    /**
     * Initialize ICIMOD data on application startup
     * Loads glacial lakes from CSV if sync is enabled
     */
    public void initializeICIMODData() {
        if (!syncEnabled) {
            log.info("ICIMOD sync disabled in configuration");
            return;
        }

        log.info("Initializing ICIMOD glacial lake data from CSV...");
        try {
            syncGlacialLakesFromICIMOD();
            log.info("✓ ICIMOD data initialization completed successfully");
        } catch (Exception e) {
            log.error("✗ Error initializing ICIMOD data: {}", e.getMessage(), e);
        }
    }

    /**
     * Main synchronization method: Load, parse, and save ICIMOD glacial lake data
     * Filters for Nepal only and avoids duplicate entries using icimodId
     */
    @Transactional
    public void syncGlacialLakesFromICIMOD() {
        log.info("Starting ICIMOD glacial lake synchronization...");

        try {
            // Step 1: Parse CSV into DTOs
            List<ICIMODGlacialLakeDTO> allLakes = parseICIMODCSV();
            log.info("Parsed {} total glacial lake records from ICIMOD CSV", allLakes.size());

            // Step 2: Filter for Nepal only
            List<ICIMODGlacialLakeDTO> nepalLakes = filterForNepal(allLakes);
            log.info("Filtered to {} glacial lakes in Nepal", nepalLakes.size());

            // Step 3: Validate and convert to entities
            List<GlacialLake> validLakes = nepalLakes.stream()
                    .filter(ICIMODGlacialLakeDTO::isValid)
                    .map(ICIMODGlacialLakeDTO::toEntity)
                    .collect(Collectors.toList());
            log.info("Validated {} lakes for database storage", validLakes.size());

            // Step 4: Save or update each lake
            int saved = 0;
            int updated = 0;
            for (GlacialLake lake : validLakes) {
                if (saveOrUpdateLake(lake)) {
                    saved++;
                } else {
                    updated++;
                }
            }

            log.info("✓ ICIMOD sync completed: {} new lakes saved, {} existing lakes updated", saved, updated);

        } catch (IOException e) {
            log.error("✗ Error reading ICIMOD CSV file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load ICIMOD CSV data", e);
        } catch (Exception e) {
            log.error("✗ Unexpected error during ICIMOD synchronization: {}", e.getMessage(), e);
            throw new RuntimeException("ICIMOD synchronization failed", e);
        }
    }

    /**
     * Parse ICIMOD CSV file from classpath resources
     * Uses OpenCSV's CsvToBeanBuilder for automatic column mapping
     * 
     * @return List of ICIMODGlacialLakeDTO objects parsed from CSV
     * @throws IOException if CSV file cannot be read
     */
    private List<ICIMODGlacialLakeDTO> parseICIMODCSV() throws IOException {
        log.debug("Loading ICIMOD CSV from: {}", csvResourcePath);

        String resourcePath = csvResourcePath.startsWith("classpath:")
                ? csvResourcePath.substring("classpath:".length())
                : csvResourcePath;

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("ICIMOD CSV resource not found at: " + resourcePath);
            }

            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            CsvToBean<ICIMODGlacialLakeDTO> csvToBean = new CsvToBeanBuilder<ICIMODGlacialLakeDTO>(reader)
                    .withType(ICIMODGlacialLakeDTO.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withThrowExceptions(false)
                    .build();

            List<ICIMODGlacialLakeDTO> beans = csvToBean.parse();

            List<CsvException> skippedRows = csvToBean.getCapturedExceptions();
            if (!skippedRows.isEmpty()) {
                log.warn("Skipped {} malformed row(s) in ICIMOD CSV", skippedRows.size());
                skippedRows.forEach(e -> log.debug("Skipped CSV line {}: {}", e.getLineNumber(), e.getMessage()));
            }

            return beans;

        } catch (IOException e) {
            log.error("Failed to parse ICIMOD CSV: {}", e.getMessage());
            throw e;
        }
    }

    // Nepal's rough bounding box - matches earthquake.nepal in application.yml.
    // Used to admit transboundary lakes (e.g. in Tibet/China) that still
    // drain into and flood Nepal directly, without pulling in unrelated
    // transboundary lakes from other Himalayan border regions in the CSV.
    private static final double NEPAL_LAT_MIN = 26.0;
    private static final double NEPAL_LAT_MAX = 30.5;
    private static final double NEPAL_LON_MIN = 80.0;
    private static final double NEPAL_LON_MAX = 88.5;

    /**
     * Filter glacial lake records relevant to Nepal: lakes physically inside
     * Nepal, plus lakes ICIMOD flags as transboundary that sit within
     * Nepal's border envelope (e.g. the Aug 2026 Kyirong-Rasuwa GLOF
     * originated from a Tibetan lake just across the border).
     *
     * @param allLakes All glacial lake records from ICIMOD database
     * @return Filtered list of records relevant to Nepal's flood risk
     */
    private List<ICIMODGlacialLakeDTO> filterForNepal(List<ICIMODGlacialLakeDTO> allLakes) {
        return allLakes.stream()
                .filter(this::isRelevantToNepal)
                .peek(lake -> log.debug("Including glacier lake relevant to Nepal: {} at ({}, {})",
                        lake.getLakeName(),
                        lake.getLatLake(),
                        lake.getLonLake()))
                .collect(Collectors.toList());
    }

    private boolean isRelevantToNepal(ICIMODGlacialLakeDTO lake) {
        if (lake.getCountry() != null && lake.getCountry().equalsIgnoreCase("Nepal")) {
            return true;
        }

        boolean isTransboundary = "Y".equalsIgnoreCase(lake.getTransboundary());
        if (!isTransboundary || lake.getLatLake() == null || lake.getLonLake() == null) {
            return false;
        }

        return lake.getLatLake() >= NEPAL_LAT_MIN && lake.getLatLake() <= NEPAL_LAT_MAX
                && lake.getLonLake() >= NEPAL_LON_MIN && lake.getLonLake() <= NEPAL_LON_MAX;
    }

    /**
     * Save a new glacial lake or update an existing one
     * Uses icimodId as the unique identifier to prevent duplicates
     * 
     * @param lake GlacialLake entity to save or update
     * @return true if new lake was created, false if updated existing
     */
    @Transactional
    public boolean saveOrUpdateLake(GlacialLake lake) {
        try {
            Optional<GlacialLake> existing = glacialLakeRepository.findByIcimodId(lake.getIcimodId());
            boolean isNew = existing.isEmpty();

            // Reuse the existing row's id (if any) so save() updates every
            // field on the freshly-parsed lake in one shot, rather than
            // manually copying each field one-by-one - a list that silently
            // goes stale every time a new field is added (as happened with
            // transboundary and riverBasin before this).
            existing.ifPresent(existingLake -> lake.setId(existingLake.getId()));
            lake.setLastUpdated(LocalDateTime.now());
            glacialLakeRepository.save(lake);

            log.debug("{} glacial lake: {} ({})", isNew ? "Saved new" : "Updated existing",
                    lake.getIcimodId(), lake.getLakeName());
            return isNew;
        } catch (Exception e) {
            log.error("Error saving/updating glacial lake {}: {}", lake.getIcimodId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save glacial lake", e);
        }
    }

    /**
     * Check if a glacial lake already exists in the database
     * 
     * @param icimodId ICIMOD unique identifier
     * @return true if lake exists, false otherwise
     */
    public boolean lakeExists(String icimodId) {
        return glacialLakeRepository.findByIcimodId(icimodId).isPresent();
    }

    /**
     * Get count of glacial lakes in Nepal
     * 
     * @return Number of glacial lakes stored in database
     */
    public long getNepalGlacialLakeCount() {
        return glacialLakeRepository.findAllLakesInNepal().size();
    }

    /**
     * Periodic synchronization scheduled to run daily at 2:00 AM UTC
     * Can be configured via application.properties: icimod.sync.cron
     * 
     * Commented out for initial setup - uncomment when ready for production:
     * 
     * @Scheduled(cron = "${icimod.sync.cron:0 0 2 * * *}")
     */
    // @Scheduled(cron = "${icimod.sync.cron:0 0 2 * * *}")
    @Transactional
    public void periodicICIMODSync() {
        if (!syncEnabled) {
            return;
        }

        log.info("Running scheduled ICIMOD synchronization...");
        try {
            syncGlacialLakesFromICIMOD();
        } catch (Exception e) {
            log.error("Scheduled ICIMOD sync failed: {}", e.getMessage(), e);
        }
    }
}