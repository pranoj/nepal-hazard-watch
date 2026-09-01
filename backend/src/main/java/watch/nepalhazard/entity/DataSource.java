package watch.nepalhazard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "data_sources")
public class DataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String organization;

    @Column(name = "data_type")
    private String dataType;

    @Column(name = "current_status")
    private String currentStatus;

    @Column(name = "last_successful_fetch")
    private LocalDateTime lastSuccessfulFetch;

    @Column(name = "api_url")
    private String apiUrl;

    @Column(name = "update_frequency")
    private String updateFrequency;

    private String description;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOrganization() {
        return organization;
    }

    public String getDataType() {
        return dataType;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public LocalDateTime getLastSuccessfulFetch() {
        return lastSuccessfulFetch;
    }
}
