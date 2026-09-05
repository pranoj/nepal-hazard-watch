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

    @CsvBindByName(column = "River_Basin")
    @CsvBindByPosition(position = 24)
    private String riverBasin;

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
                .lakeName(resolveLakeName(icimodId))
                .glacierName(this.glacierName)
                .latitude(this.latLake)
                .longitude(this.lonLake)
                .elevation(this.elevLake)
                .surfaceAreaKm2(this.area)
                .country(this.country)
                .riskLevel(riskLevel)
                .transboundary("Y".equalsIgnoreCase(this.transboundary))
                .riverBasin(isKnownName(this.riverBasin) ? this.riverBasin.trim() : null)
                .lastUpdated(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * The ICIMOD source data itself often stores the literal string
     * "Unknown"/"Unnamed" for Lake_name (not a blank we're defaulting).
     * Fall through progressively less specific but still real fields -
     * glacier, then river basin, then province - before resorting to a
     * bare ID, so a name always tells you at least roughly where the lake is.
     */
    private String resolveLakeName(String icimodId) {
        if (isKnownName(this.lakeName)) {
            return this.lakeName.trim();
        }
        if (isKnownName(this.glacierName)) {
            return this.glacierName.trim() + " Glacier Lake";
        }
        if (isKnownName(this.riverBasin)) {
            return "Unnamed Lake, " + this.riverBasin.trim() + " Basin";
        }
        if (isKnownName(this.province)) {
            return "Unnamed Lake, " + this.province.trim();
        }
        return "Unnamed Lake (" + icimodId + ")";
    }

    private boolean isKnownName(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty()
                && !trimmed.equalsIgnoreCase("Unknown")
                && !trimmed.equalsIgnoreCase("Unnamed")
                && !trimmed.equalsIgnoreCase("NA");
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