package watch.nepalhazard.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import watch.nepalhazard.entity.Region;
import watch.nepalhazard.repository.RegionRepository;

/**
 * Seeds the Nepal districts used to tag incoming earthquakes with a
 * nearest-region label. hazard_events.region_id is a NOT NULL foreign key
 * into this table, so ingestion depends on these rows existing.
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(RegionRepository regionRepo) {

        return args -> {
            // Only seed if database is empty (to avoid duplicates on restart)
            if (regionRepo.count() == 0) {

                // Nepal districts relevant to flood risk - used only for
                // nearest-region tagging of real earthquake events.
                Region rasuwa = new Region();
                rasuwa.setName("Rasuwa");
                rasuwa.setLatitude(28.18);
                rasuwa.setLongitude(85.72);
                rasuwa.setRiskLevel("HIGH");
                rasuwa.setPopulation(110000);
                regionRepo.save(rasuwa);

                Region sindhupalchok = new Region();
                sindhupalchok.setName("Sindhupalchok");
                sindhupalchok.setLatitude(27.95);
                sindhupalchok.setLongitude(85.82);
                sindhupalchok.setRiskLevel("HIGH");
                sindhupalchok.setPopulation(286000);
                regionRepo.save(sindhupalchok);

                Region nuwakot = new Region();
                nuwakot.setName("Nuwakot");
                nuwakot.setLatitude(27.87);
                nuwakot.setLongitude(85.51);
                nuwakot.setRiskLevel("MEDIUM");
                nuwakot.setPopulation(450000);
                regionRepo.save(nuwakot);

                Region dhading = new Region();
                dhading.setName("Dhading");
                dhading.setLatitude(27.85);
                dhading.setLongitude(85.15);
                dhading.setRiskLevel("MEDIUM");
                dhading.setPopulation(340000);
                regionRepo.save(dhading);

                System.out.println("✓ Regions seeded successfully");
            }
        };
    }
}