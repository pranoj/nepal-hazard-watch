package watch.nepalhazard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A curated (not computed GIS flow-routing) downstream town reference, used to answer who should be warned for a given lake. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "river_basin_towns")
public class RiverBasinTown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "river_basin", nullable = false)
    private String riverBasin;

    @Column(name = "town_name", nullable = false)
    private String townName;

    private Double latitude;

    private Double longitude;

    // 1 = closest/first downstream, increasing = further away
    @Column(name = "downstream_order", nullable = false)
    private Integer downstreamOrder;
}
