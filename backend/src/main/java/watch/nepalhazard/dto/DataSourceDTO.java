package watch.nepalhazard.dto;

import java.time.Instant;
import watch.nepalhazard.entity.DataSource;

public record DataSourceDTO(
        Long id,
        String name,
        String organization,
        String dataType,
        String currentStatus,
        Instant lastSuccessfulFetch) {

    public static DataSourceDTO from(DataSource dataSource) {
        return new DataSourceDTO(
                dataSource.getId(),
                dataSource.getName(),
                dataSource.getOrganization(),
                dataSource.getDataType(),
                dataSource.getCurrentStatus(),
                dataSource.getLastSuccessfulFetch());
    }
}
