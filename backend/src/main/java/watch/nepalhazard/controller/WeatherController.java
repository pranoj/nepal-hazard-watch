package watch.nepalhazard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import watch.nepalhazard.entity.Weather;
import watch.nepalhazard.service.WeatherService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @GetMapping("/{location}")
    public ResponseEntity<?> getLatestWeatherForLocation(@PathVariable String location) {
        Weather weather = weatherService.getLatestWeather(location);
        if (weather == null) {
            return ResponseEntity.status(404).body(Map.of("message", "No weather data found for " + location));
        }
        return ResponseEntity.ok(weather);
    }

    @GetMapping("/history/{location}")
    public ResponseEntity<?> getWeatherHistory(
            @PathVariable String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(7);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        List<Weather> history = weatherService.getRainfallHistory(location, startTime, endTime);
        if (history.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No weather history found for " + location));
        }
        return ResponseEntity.ok(history);
    }
}