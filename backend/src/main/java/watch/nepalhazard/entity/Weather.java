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
@Table(name = "weather")
public class Weather {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String location;

    private Double latitude;

    private Double longitude;

    private Double temperature;

    private Double humidity;

    private Double rainfall;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(name = "weather_condition")
    private String weatherCondition;

    private String description;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @Column(name = "data_source")
    private String dataSource;
}