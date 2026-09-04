package watch.nepalhazard.dto;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import watch.nepalhazard.entity.GlacialLake;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ICIMODGlacialLakeDTO {

    @CsvBindByName(column = "GF_ID")
    @CsvBindByPosition(position = 0)
    private String gfId;

    @CsvBindByName(column = "GL_ID")
    @CsvBindByPosition(position = 7)
    private String glId;

    @CsvBindByName(column = "LakeDB_ID")
    @CsvBindByPosition(position = 8)
    private String lakeDbId;

    @CsvBindByName(column = "G_ID")
    @CsvBindByPosition(position = 9)
    private String gId;

    @CsvBindByName(column = "Lake_name")
    @CsvBindByPosition(position = 5)
    private String lakeName;

    @CsvBindByName(column = "Glacier_name")
    @CsvBindByPosition(position = 6)
    private String glacierName;

    @CsvBindByName(column = "Lat_lake")
    @CsvBindByPosition(position = 10)
    private Double latLake;

    @CsvBindByName(column = "Lon_lake")
    @CsvBindByPosition(position = 11)
    private Double lonLake;

    @CsvBindByName(column = "Elev_lake")
    @CsvBindByPosition(position = 12)
    private Double elevLake;

    @CsvBindByName(column = "Country")
    @CsvBindByPosition(position = 22)
    private String country;

    @CsvBindByName(column = "Province")
    @CsvBindByPosition(position = 23)
    private String province;

    @CsvBindByName(column = "Lake_type")
    @CsvBindByPosition(position = 17)
    private String lakeType;

    @CsvBindByName(column = "Area")
    @CsvBindByPosition(position = 29)
    private Double area;

    @CsvBindByName(column = "Volume")
    @CsvBindByPosition(position = 30)
    private Double volume;

    @CsvBindByName(column = "Transboundary")
    @CsvBindByPosition(position = 18)
    private String transboundary;

    @CsvBindByName(column = "Repeat")
    @CsvBindByPosition(position = 19)
    private String repeat;

    public GlacialLake toEntity() {
        String icimodId = (this.gfId != null && !this.gfId.isEmpty())
                ? this.gfId
                : "ICIMOD_" + this.glacierName + "_" + Math.round(this.latLake * 100);

        String riskLevel = inferRiskLevel();

        return GlacialLake.builder()
                .icimodId(icimodId)
                .lakeName(this.lakeName != null ? this.lakeName : "Unknown")
                .glacierName(this.glacierName)
                .latitude(this.latLake)
                .longitude(this.lonLake)
                .elevation(this.elevLake)
                .surfaceAreaKm2(this.area)
                .country(this.country)
                .riskLevel(riskLevel)
                .lastUpdated(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String inferRiskLevel() {
        if (lakeType == null) {
            return "Medium";
        }

        if (lakeType.equalsIgnoreCase("Ice dammed")) {
            return "Very High";
        }

        if ("Y".equalsIgnoreCase(this.repeat)) {
            return "High";
        }

        if (lakeType.equalsIgnoreCase("Moraine dammed")) {
            return "Medium";
        }

        if (lakeType.equalsIgnoreCase("Supraglacial")) {
            return "Low";
        }

        return "Medium";
    }

    public boolean isValid() {
        return this.latLake != null
                && this.lonLake != null
                && this.country != null
                && !this.country.isEmpty();
    }
}