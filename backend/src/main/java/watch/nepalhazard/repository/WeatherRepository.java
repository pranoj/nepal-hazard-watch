package watch.nepalhazard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import watch.nepalhazard.entity.Weather;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherRepository extends JpaRepository<Weather, Long> {

    @Query(value = "SELECT * FROM weather WHERE location = :location ORDER BY recorded_at DESC LIMIT 1", nativeQuery = true)
    Optional<Weather> findLatestByLocation(@Param("location") String location);

    @Query(value = "SELECT * FROM weather ORDER BY (latitude - :lat) * (latitude - :lat) + (longitude - :lon) * (longitude - :lon) ASC LIMIT 1", nativeQuery = true)
    Optional<Weather> findNearestByCoordinates(@Param("lat") double latitude, @Param("lon") double longitude);

    @Query("SELECT w FROM Weather w WHERE w.location = :location AND w.recordedAt BETWEEN :startTime AND :endTime ORDER BY w.recordedAt DESC")
    List<Weather> findWeatherHistory(@Param("location") String location, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}