package watch.nepalhazard.service;

import java.util.List;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import watch.nepalhazard.entity.GlacialLake;
import watch.nepalhazard.entity.Glacier;
import watch.nepalhazard.entity.GlofRiskAssessment;
import watch.nepalhazard.repository.GlacialLakeRepository;
import watch.nepalhazard.repository.GlacierRepository;
import watch.nepalhazard.repository.GlofRiskAssessmentRepository;

/** Periodically recomputes GLOF risk for every lake ("Type A") and glacier watch point ("Type B"), so the map always has a current reading rather than computing on demand. */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlofRiskScanService {

    private final GlacialLakeRepository glacialLakeRepository;
    private final GlacierRepository glacierRepository;
    private final GlofRiskAssessmentRepository glofRiskAssessmentRepository;
    private final GLOFRiskCalculationService glofRiskCalculationService;

    @Scheduled(fixedRateString = "${glof.risk-scan-interval-ms:1800000}")
    public void scanAll() {
        scanAllLakes();
        scanAllGlaciers();
    }

    private void scanAllLakes() {
        List<GlacialLake> lakes = glacialLakeRepository.findAllLakesInNepal();
        log.info("Starting GLOF risk scan for {} lakes", lakes.size());

        int aboveNormal = 0;
        for (GlacialLake lake : lakes) {
            try {
                GlofRiskAssessment assessment = glofRiskCalculationService.assessLake(lake);
                saveOrUpdate(assessment, glofRiskAssessmentRepository.findByGlacialLakeId(lake.getId()));
                if (!"NORMAL".equals(assessment.getAlertLevel())) {
                    aboveNormal++;
                }
            } catch (Exception e) {
                log.error("Failed to assess GLOF risk for lake {}: {}", lake.getIcimodId(), e.getMessage(), e);
            }
        }

        log.info("GLOF risk scan complete: {} lakes assessed, {} above NORMAL", lakes.size(), aboveNormal);
    }

    private void scanAllGlaciers() {
        List<Glacier> glaciers = glacierRepository.findAll();
        log.info("Starting GLOF risk scan for {} glacier watch points", glaciers.size());

        int aboveNormal = 0;
        for (Glacier glacier : glaciers) {
            try {
                GlofRiskAssessment assessment = glofRiskCalculationService.assessGlacier(glacier);
                saveOrUpdate(assessment, glofRiskAssessmentRepository.findByGlacierId(glacier.getId()));
                if (!"NORMAL".equals(assessment.getAlertLevel())) {
                    aboveNormal++;
                }
            } catch (Exception e) {
                log.error("Failed to assess GLOF risk for glacier {}: {}", glacier.getRgiId(), e.getMessage(), e);
            }
        }

        log.info("Glacier risk scan complete: {} assessed, {} above NORMAL", glaciers.size(), aboveNormal);
    }

    // reuses the existing row's id so save() overwrites every field in one shot, rather than a copy-list that goes stale when a field is added
    private void saveOrUpdate(GlofRiskAssessment assessment, Optional<GlofRiskAssessment> existing) {
        existing.ifPresent(e -> assessment.setId(e.getId()));
        glofRiskAssessmentRepository.save(assessment);
    }
}
