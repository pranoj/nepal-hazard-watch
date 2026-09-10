package watch.nepalhazard.config;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import watch.nepalhazard.entity.RiverBasinTown;
import watch.nepalhazard.repository.RiverBasinTownRepository;

/**
 * Static curated river-basin -> downstream-towns lookup (see V12__create_river_basin_towns_table.sql), not
 * computed GIS flow-routing. Deliberately doesn't cover every basin in glacial_lakes: "Maquan" likely drains
 * away from Nepal (Yarlung Tsangpo headwater, included only by the source data's border-envelope filter);
 * "Kutiyangti"/"Kali"/"Kawari" left unmapped - couldn't be confidently matched without conflating rivers of similar name.
 */
@Configuration
public class RiverBasinTownSeeder {

    @Bean
    CommandLineRunner seedRiverBasinTowns(RiverBasinTownRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                return;
            }

            List<RiverBasinTown> towns = List.of(
                    // Dudh Koshi (Everest/Solukhumbu) -> Sun Koshi -> Sapt Koshi
                    town("Dudh Koshi", "Namche Bazaar", 27.8069, 86.7147, 1),
                    town("Dudh Koshi", "Phakding", 27.7511, 86.7160, 2),
                    town("Dudh Koshi", "Salleri", 27.5580, 86.5880, 3),
                    town("Dudh Koshi", "Okhaldhunga Bazar", 27.3167, 86.5000, 4),

                    // Tamor (Kanchenjunga) -> joins Sun Koshi/Arun at Triveni -> Sapt Koshi
                    town("Tamor", "Taplejung Bazar", 27.3500, 87.6667, 1),
                    town("Tamor", "Phidim", 27.1500, 87.7500, 2),
                    town("Tamor", "Triveni (Koshi confluence)", 26.9000, 87.3500, 3),

                    // Humla -> Humla Karnali -> joins Mugu Karnali -> Karnali River
                    town("Humla", "Simikot", 29.9700, 81.8200, 1),
                    town("Humla", "Manma (Kalikot)", 29.2000, 81.6167, 2),
                    town("Humla", "Surkhet (Birendranagar)", 28.6000, 81.6167, 3),

                    // Arun (fed by Pumqu from Tibet) -> joins Sun Koshi/Tamor -> Sapt Koshi
                    town("Arun", "Kimathanka (border)", 27.8500, 87.5500, 1),
                    town("Arun", "Tumlingtar", 27.3167, 87.2000, 2),
                    town("Arun", "Triveni (Koshi confluence)", 26.9000, 87.3500, 3),
                    // Pumqu is Arun's Tibetan headwater - same downstream Nepal towns
                    town("Pumqu", "Kimathanka (border)", 27.8500, 87.5500, 1),
                    town("Pumqu", "Tumlingtar", 27.3167, 87.2000, 2),
                    town("Pumqu", "Triveni (Koshi confluence)", 26.9000, 87.3500, 3),

                    // Kali Gandaki (Mustang/Annapurna) -> joins Trishuli -> Narayani
                    town("Kali Gandaki", "Jomsom", 28.7806, 83.7256, 1),
                    town("Kali Gandaki", "Beni (Myagdi)", 28.3500, 83.5667, 2),
                    town("Kali Gandaki", "Baglung Bazar", 28.2667, 83.5833, 3),
                    town("Kali Gandaki", "Narayanghat/Bharatpur", 27.6870, 84.4270, 4),

                    // Budhi Gandaki (Manaslu, Nepal+Tibet headwaters) -> Trishuli/Gandaki
                    town("Budhi Gandaki", "Machha Khola", 28.3500, 84.8500, 1),
                    town("Budhi Gandaki", "Arughat", 28.0667, 84.8667, 2),

                    // Marsyangdi (Manang/Annapurna) -> Gandaki tributary
                    town("Marsyangdi", "Chame (Manang)", 28.5500, 84.2400, 1),
                    town("Marsyangdi", "Besisahar (Lamjung)", 28.2333, 84.3833, 2),

                    // Bheri -> joins Karnali
                    town("Bheri", "Jajarkot Bazar", 28.7000, 82.2000, 1),
                    town("Bheri", "Surkhet (Birendranagar)", 28.6000, 81.6167, 2),

                    // West Seti (far-west) -> joins Karnali
                    town("West Seti", "Dadeldhura Bazar", 29.3000, 80.5833, 1),

                    // Seti (Pokhara) - distinct from West Seti - joins Trishuli/Gandaki
                    town("Seti", "Pokhara", 28.2096, 83.9856, 1),
                    town("Seti", "Damauli (Tanahun)", 27.9833, 84.2833, 2),

                    // Indrawati (Melamchi/Sindhupalchok) -> joins Sun Koshi; site of the June 2021 GLOF/debris flood
                    town("Indrawati", "Melamchi", 27.8333, 85.5500, 1),
                    town("Indrawati", "Dolalghat (confluence)", 27.6833, 85.6667, 2),

                    // Tama Koshi (Dolakha) -> joins Sun Koshi
                    town("Tama Koshi", "Singati", 27.7667, 86.1167, 1),
                    town("Tama Koshi", "Dolalghat (confluence)", 27.6833, 85.6667, 2),
                    // Rongxia (Tibet, Rolwaling corridor) feeds the same Tama Koshi system
                    town("Rongxia", "Singati", 27.7667, 86.1167, 1),
                    town("Rongxia", "Dolalghat (confluence)", 27.6833, 85.6667, 2),

                    // Mugu (Karnali headwater) -> joins Humla Karnali -> Karnali River
                    town("Mugu", "Gamgadhi", 29.5333, 82.1167, 1),
                    town("Mugu", "Surkhet (Birendranagar)", 28.6000, 81.6167, 2),

                    // Poiqu (Tibet) becomes Bhote Koshi in Nepal -> Sun Koshi -> Sapt Koshi
                    town("Poiqu", "Tatopani/Kodari (border)", 27.9500, 85.9833, 1),
                    town("Poiqu", "Barhabise", 27.7667, 85.9167, 2),
                    town("Poiqu", "Lamosangu", 27.6667, 85.9000, 3),
                    town("Poiqu", "Dolalghat (confluence)", 27.6833, 85.6667, 4),

                    // Gyirong (Kyirong, Tibet) becomes Trishuli via Rasuwa - corridor of the Aug 2026 flood;
                    // Langtang Village/Kyanjin Gompa (the actual source sub-valley) included upstream of the main road towns.
                    town("Gyirong", "Langtang Village", 28.212, 85.615, 1),
                    town("Gyirong", "Kyanjin Gompa", 28.212, 85.680, 2),
                    town("Gyirong", "Syabrubesi (Rasuwa)", 28.1667, 85.3333, 3),
                    town("Gyirong", "Dhunche (Rasuwa HQ)", 28.1000, 85.3000, 4),
                    town("Gyirong", "Trishuli Bazar", 27.9000, 85.0833, 5),
                    town("Gyirong", "Devghat (confluence)", 27.6500, 84.4200, 6));

            repo.saveAll(towns);
        };
    }

    private static RiverBasinTown town(String basin, String name, double lat, double lon, int order) {
        return RiverBasinTown.builder()
                .riverBasin(basin)
                .townName(name)
                .latitude(lat)
                .longitude(lon)
                .downstreamOrder(order)
                .build();
    }
}
