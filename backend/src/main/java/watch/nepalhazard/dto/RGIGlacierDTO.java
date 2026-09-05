package watch.nepalhazard.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row of the pre-filtered RGI (Randolph Glacier Inventory) v7 export -
 * see rgi_glaciers_nepal_envelope.csv and the geospatial/ processing notes.
 * Already restricted to Nepal's border envelope and slope > 20 degrees;
 * the real "is this a watch point" qualification (steeper + near a known
 * river corridor) is applied in GlacierSyncService.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RGIGlacierDTO {

    @CsvBindByName(column = "rgi_id")
    private String rgiId;

    @CsvBindByName(column = "glac_name")
    private String glacierName;

    @CsvBindByName(column = "termlat")
    private Double termLat;

    @CsvBindByName(column = "termlon")
    private Double termLon;

    @CsvBindByName(column = "slope_deg")
    private Double slopeDeg;

    @CsvBindByName(column = "area_km2")
    private Double areaKm2;
}
