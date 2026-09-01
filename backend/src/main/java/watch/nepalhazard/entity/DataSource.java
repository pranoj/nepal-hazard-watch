package watch.nepalhazard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

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
    private Instant lastSuccessfulFetch;

    protected DataSource() {
    }

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

    public Instant getLastSuccessfulFetch() {
        return lastSuccessfulFetch;
    }
}
