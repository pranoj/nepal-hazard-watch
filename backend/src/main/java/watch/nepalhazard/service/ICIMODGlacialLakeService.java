package watch.nepalhazard.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

/** Loads, parses, and syncs ICIMOD HMAGLOFDB glacial lake data (CC BY 4.0, https://www.icimod.org/), filtered to Nepal. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ICIMODGlacialLakeService {

    private final GlacialLakeRepository glacialLakeRepository;

    @Value("${icimod.csv.resource-path:classpath:data/HMAGLOFDB_v4.0_13122025.csv}")
    private String csvResourcePath;

    @Value("${icimod.sync.enabled:true}")
    private boolean syncEnabled;

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

    @Transactional
    public void syncGlacialLakesFromICIMOD() {
        log.info("Starting ICIMOD glacial lake synchronization...");

        try {
            List<ICIMODGlacialLakeDTO> allLakes = parseICIMODCSV();
            log.info("Parsed {} total glacial lake records from ICIMOD CSV", allLakes.size());

            List<ICIMODGlacialLakeDTO> nepalLakes = filterForNepal(allLakes);
            log.info("Filtered to {} glacial lakes in Nepal", nepalLakes.size());

            List<GlacialLake> validLakes = nepalLakes.stream()
                    .filter(ICIMODGlacialLakeDTO::isValid)
                    .map(ICIMODGlacialLakeDTO::toEntity)
                    .collect(Collectors.toList());
            log.info("Validated {} lakes for database storage", validLakes.size());

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

    // Nepal's rough bounding box (matches earthquake.nepal in application.yml) - admits transboundary lakes that drain into Nepal without pulling in unrelated border-region lakes.
    private static final double NEPAL_LAT_MIN = 26.0;
    private static final double NEPAL_LAT_MAX = 30.5;
    private static final double NEPAL_LON_MIN = 80.0;
    private static final double NEPAL_LON_MAX = 88.5;

    /** Lakes physically in Nepal, plus ICIMOD-flagged transboundary lakes within Nepal's border envelope (e.g. the Aug 2026 Kyirong-Rasuwa GLOF originated across the border). */
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

    @Transactional
    public boolean saveOrUpdateLake(GlacialLake lake) {
        try {
            Optional<GlacialLake> existing = glacialLakeRepository.findByIcimodId(lake.getIcimodId());
            boolean isNew = existing.isEmpty();

            // reuses the existing row's id so save() overwrites every field - a manual copy-list went stale before (transboundary, riverBasin)
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

    public long getNepalGlacialLakeCount() {
        return glacialLakeRepository.findAllLakesInNepal().size();
    }
}