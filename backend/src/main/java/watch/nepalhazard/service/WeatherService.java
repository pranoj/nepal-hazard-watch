package watch.nepalhazard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import watch.nepalhazard.entity.GlacialLake;
import watch.nepalhazard.entity.Glacier;
import watch.nepalhazard.entity.Weather;
import watch.nepalhazard.repository.GlacialLakeRepository;
import watch.nepalhazard.repository.GlacierRepository;
import watch.nepalhazard.repository.WeatherRepository;
import java.time.LocalDateTime;
import java.util.List;

/** Fetches OpenWeatherMap data at each lake/glacier's own coordinates, not a proxy city that can be hundreds of km away. */
@Slf4j
@Service
public class WeatherService {

    private final WeatherRepository weatherRepository;
    private final GlacialLakeRepository glacialLakeRepository;
    private final GlacierRepository glacierRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${weather.api-url}")
    private String weatherApiUrl;

    @Value("${weather.api-key}")
    private String apiKey;

    @Autowired
    public WeatherService(WeatherRepository weatherRepository, GlacialLakeRepository glacialLakeRepository,
            GlacierRepository glacierRepository, @Value("${weather.timeout-seconds:15}") int timeoutSeconds) {
        this.weatherRepository = weatherRepository;
        this.glacialLakeRepository = glacialLakeRepository;
        this.glacierRepository = glacierRepository;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(fixedRateString = "${weather.fetch-interval-ms}")
    public void fetchWeatherForMonitoredPoints() {
        List<GlacialLake> lakes = glacialLakeRepository.findAllLakesInNepal();
        List<Glacier> glaciers = glacierRepository.findAll();
        log.info("Fetching weather for {} glacial lakes and {} glacier watch points...", lakes.size(),
                glaciers.size());

        int count = 0;
        for (GlacialLake lake : lakes) {
            if (lake.getIcimodId() == null || lake.getLatitude() == null || lake.getLongitude() == null) {
                continue;
            }

            Weather weather = fetchWeatherForLocation(lake.getIcimodId(), lake.getLatitude(), lake.getLongitude());
            if (weather != null && !weatherExists(weather)) {
                weatherRepository.save(weather);
                count++;
            }
        }

        for (Glacier glacier : glaciers) {
            Weather weather = fetchWeatherForLocation(glacier.getRgiId(), glacier.getTerminusLatitude(),
                    glacier.getTerminusLongitude());
            if (weather != null && !weatherExists(weather)) {
                weatherRepository.save(weather);
                count++;
            }
        }

        log.info("Added {} weather records", count);
    }

    private Weather fetchWeatherForLocation(String locationKey, double latitude, double longitude) {
        try {
            String url = weatherApiUrl + "/weather?lat=" + latitude + "&lon=" + longitude
                    + "&appid=" + apiKey + "&units=metric";

            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.isEmpty()) {
                log.warn("No response from Weather API for {}", locationKey);
                return null;
            }

            return parseWeatherResponse(response, locationKey, latitude, longitude);

        } catch (Exception e) {
            log.error("Error fetching weather for {}: {}", locationKey, e.getMessage());
            return null;
        }
    }

    private Weather parseWeatherResponse(String response, String locationKey, double latitude, double longitude) {
        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode main = root.path("main");
            double temperature = main.path("temp").asDouble();
            double humidity = main.path("humidity").asDouble();

            JsonNode weatherArray = root.path("weather");
            String weatherCondition = "Unknown";
            String description = "No description";

            if (weatherArray.isArray() && weatherArray.size() > 0) {
                JsonNode weather = weatherArray.get(0);
                weatherCondition = weather.path("main").asText();
                description = weather.path("description").asText();
            }

            JsonNode rainNode = root.path("rain");
            double rainfall = 0.0;
            if (rainNode.path("1h").isNumber()) {
                rainfall = rainNode.path("1h").asDouble();
            }

            JsonNode wind = root.path("wind");
            double windSpeed = wind.path("speed").asDouble();

            Weather weather = new Weather();
            weather.setLocation(locationKey);
            weather.setLatitude(latitude);
            weather.setLongitude(longitude);
            weather.setTemperature(temperature);
            weather.setHumidity(humidity);
            weather.setRainfall(rainfall);
            weather.setWindSpeed(windSpeed);
            weather.setWeatherCondition(weatherCondition);
            weather.setDescription(description);
            weather.setRecordedAt(LocalDateTime.now());
            weather.setFetchedAt(LocalDateTime.now());
            weather.setDataSource("OPENWEATHERMAP");

            log.debug("{}: {}°C, {}mm rain", locationKey, temperature, rainfall);

            return weather;

        } catch (Exception e) {
            log.error("Error parsing weather response for {}: {}", locationKey, e.getMessage(), e);
            return null;
        }
    }

    private boolean weatherExists(Weather weather) {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        return weatherRepository.findLatestByLocation(weather.getLocation())
                .map(existing -> existing.getFetchedAt().isAfter(thirtyMinutesAgo))
                .orElse(false);
    }

    public Weather getLatestWeather(String location) {
        return weatherRepository.findLatestByLocation(location).orElse(null);
    }

    public List<Weather> getRainfallHistory(String location, LocalDateTime startTime, LocalDateTime endTime) {
        return weatherRepository.findWeatherHistory(location, startTime, endTime);
    }
}
