package watch.nepalhazard.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import watch.nepalhazard.entity.DataSource;
import watch.nepalhazard.entity.Region;
import watch.nepalhazard.entity.HazardEvent;
import watch.nepalhazard.repository.DataSourceRepository;
import watch.nepalhazard.repository.RegionRepository;
import watch.nepalhazard.repository.HazardEventRepository;
import java.time.LocalDateTime;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(
            DataSourceRepository dsRepo,
            RegionRepository regionRepo,
            HazardEventRepository eventRepo) {

        return args -> {
            // Only seed if database is empty (to avoid duplicates on restart)
            if (regionRepo.count() == 0) {

                // 1. Add Regions (Nepal districts relevant to flood risk)
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

            if (dsRepo.count() == 0) {

                // 2. Add Data Sources (verified from Phase 0 research)
                DataSource usgs = new DataSource();
                usgs.setName("USGS Earthquake Catalog");
                usgs.setOrganization("United States Geological Survey");
                usgs.setDataType("Seismic");
                usgs.setCurrentStatus("ACTIVE");
                usgs.setLastSuccessfulFetch(LocalDateTime.now());
                usgs.setApiUrl("https://earthquake.usgs.gov/fdsnws/event/1/query");
                usgs.setUpdateFrequency("60 seconds");
                usgs.setDescription(
                        "Real-time earthquake and landslide detection via automated seismic waveform analysis");
                dsRepo.save(usgs);

                DataSource sentinel1 = new DataSource();
                sentinel1.setName("ESA Copernicus Sentinel-1 SAR");
                sentinel1.setOrganization("European Space Agency");
                sentinel1.setDataType("Satellite SAR");
                sentinel1.setCurrentStatus("ACTIVE");
                sentinel1.setLastSuccessfulFetch(LocalDateTime.now());
                sentinel1.setApiUrl("https://dataspace.copernicus.eu/");
                sentinel1.setUpdateFrequency("3-12 days");
                sentinel1.setDescription(
                        "Cloud-penetrating SAR imagery; revisit every 3-12 days for glacier/lake monitoring");
                dsRepo.save(sentinel1);

                DataSource gpm = new DataSource();
                gpm.setName("NASA GPM IMERG (Early Run)");
                gpm.setOrganization("National Aeronautics and Space Administration");
                gpm.setDataType("Rainfall/Precipitation");
                gpm.setCurrentStatus("ACTIVE");
                gpm.setLastSuccessfulFetch(LocalDateTime.now());
                gpm.setApiUrl("https://ges.disc.nasa.gov/");
                gpm.setUpdateFrequency("4 hours");
                gpm.setDescription(
                        "Real-time rainfall estimates at 10km resolution; contextual input for flood risk scoring");
                dsRepo.save(gpm);

                DataSource icimod = new DataSource();
                icimod.setName("ICIMOD GLOF Database");
                icimod.setOrganization("International Centre for Integrated Mountain Development");
                icimod.setDataType("Historical Inventory");
                icimod.setCurrentStatus("ACTIVE");
                icimod.setLastSuccessfulFetch(LocalDateTime.now());
                icimod.setApiUrl("https://rds.icimod.org/");
                icimod.setUpdateFrequency("Manual");
                icimod.setDescription(
                        "766 documented glacial lake outburst flood events (1533-2025); backtesting reference");
                dsRepo.save(icimod);

                System.out.println("✓ Data Sources seeded successfully");
            }

            if (eventRepo.count() == 0) {

                // 3. Add Sample Hazard Events (real incidents for backtesting)
                HazardEvent aug2026 = new HazardEvent();
                aug2026.setRegionId(1L); // Rasuwa
                aug2026.setEventType("GLOF");
                aug2026.setEventTime(LocalDateTime.of(2026, 8, 26, 8, 10));
                aug2026.setLatitude(28.18);
                aug2026.setLongitude(85.72);
                aug2026.setMagnitude(5.2);
                aug2026.setStatus("CONFIRMED");
                aug2026.setDescription(
                        "Kyirong-Rasuwa flood debris avalanche and flash flood; GLOF on Lhende Khola tributary");
                aug2026.setDeathToll(1000);
                eventRepo.save(aug2026);

                HazardEvent july2025 = new HazardEvent();
                july2025.setRegionId(1L); // Rasuwa
                july2025.setEventType("GLOF");
                july2025.setEventTime(LocalDateTime.of(2025, 7, 8, 14, 30));
                july2025.setLatitude(28.20);
                july2025.setLongitude(85.80);
                july2025.setMagnitude(4.8);
                july2025.setStatus("CONFIRMED");
                july2025.setDescription("Bhote Koshi GLOF near Purepu area; glacier/lake-failure event");
                july2025.setDeathToll(25);
                eventRepo.save(july2025);

                HazardEvent sim1 = new HazardEvent();
                sim1.setRegionId(2L); // Sindhupalchok
                sim1.setEventType("HEAVY_RAINFALL");
                sim1.setEventTime(LocalDateTime.of(2026, 7, 15, 10, 0));
                sim1.setLatitude(27.95);
                sim1.setLongitude(85.82);
                sim1.setMagnitude(3.5);
                sim1.setStatus("SIMULATED");
                sim1.setDescription("Simulated high-intensity rainfall event for model backtesting");
                sim1.setDeathToll(0);
                eventRepo.save(sim1);

                System.out.println("✓ Hazard Events seeded successfully");
            }

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("Data seeding complete! Test your APIs:");
            System.out.println("  → GET http://localhost:8080/api/regions");
            System.out.println("  → GET http://localhost:8080/api/data-sources");
            System.out.println("  → GET http://localhost:8080/api/hazard-events");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        };
    }
}