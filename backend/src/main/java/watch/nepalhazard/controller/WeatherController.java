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
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @GetMapping
    public ResponseEntity<?> getLatestWeatherForAllLocations() {
        try {
            List<Weather> weatherList = weatherService.getLatestWeatherForAllLocations();
            if (weatherList.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "No weather data available yet");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.ok(weatherList);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error fetching weather: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/{location}")
    public ResponseEntity<?> getLatestWeatherForLocation(@PathVariable String location) {
        try {
            Weather weather = weatherService.getLatestWeather(location);
            if (weather == null) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "No weather data found for " + location);
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.ok(weather);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error fetching weather for " + location + ": " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/history/{location}")
    public ResponseEntity<?> getWeatherHistory(
            @PathVariable String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(7);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }

            List<Weather> history = weatherService.getRainfallHistory(location, startTime, endTime);
            if (history.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "No weather history found for " + location);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error fetching weather history: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/rainfall/high-events")
    public ResponseEntity<?> getHighRainfallEvents(
            @RequestParam(defaultValue = "10.0") Double minRainfall,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(30);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }

            List<Weather> events = weatherService.getHighRainfallEvents(minRainfall, startTime, endTime);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error fetching high rainfall events: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}