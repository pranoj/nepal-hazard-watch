package watch.nepalhazard.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One row of the pre-filtered RGI v7 export (Nepal envelope, slope &gt; 20deg). Real watch-point qualification is applied in GlacierSyncService. */
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
