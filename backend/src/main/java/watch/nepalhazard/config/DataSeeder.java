package watch.nepalhazard.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import watch.nepalhazard.entity.Region;
import watch.nepalhazard.repository.RegionRepository;

/** Seeds Nepal districts for nearest-region tagging. hazard_events.region_id is a NOT NULL FK into this table. */
@Slf4j
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(RegionRepository regionRepo) {

        return args -> {
            // Only seed if database is empty (to avoid duplicates on restart)
            if (regionRepo.count() == 0) {
                regionRepo.save(region("Rasuwa", 28.18, 85.72, "HIGH", 110000));
                regionRepo.save(region("Sindhupalchok", 27.95, 85.82, "HIGH", 286000));
                regionRepo.save(region("Nuwakot", 27.87, 85.51, "MEDIUM", 450000));
                regionRepo.save(region("Dhading", 27.85, 85.15, "MEDIUM", 340000));

                log.info("Regions seeded successfully");
            }
        };
    }

    private static Region region(String name, double lat, double lon, String riskLevel, int population) {
        Region region = new Region();
        region.setName(name);
        region.setLatitude(lat);
        region.setLongitude(lon);
        region.setRiskLevel(riskLevel);
        region.setPopulation(population);
        return region;
    }
}