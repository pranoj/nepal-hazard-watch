package watch.nepalhazard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import watch.nepalhazard.entity.Weather;
import watch.nepalhazard.repository.WeatherRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
public class WeatherService {

    private final WeatherRepository weatherRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${weather.api-url}")
    private String weatherApiUrl;

    @Value("${weather.api-key}")
    private String apiKey;

    @Autowired
    public WeatherService(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(fixedRateString = "${weather.fetch-interval-ms}")
    public void fetchWeatherForNepalCities() {
        try {
            System.out.println("🌦️  Fetching weather for Nepal cities...");

            String[] cities = { "Kathmandu", "Pokhara" };
            double[] latitudes = { 27.7172, 28.2096 };
            double[] longitudes = { 85.3240, 83.9856 };

            int count = 0;
            for (int i = 0; i < cities.length; i++) {
                Weather weather = fetchWeatherForCity(cities[i], latitudes[i], longitudes[i]);
                if (weather != null && !weatherExists(weather)) {
                    weatherRepository.save(weather);
                    count++;
                    System.out.println("✅ Saved weather for " + cities[i]);
                }
            }

            System.out.println("✅ Added " + count + " weather records");

        } catch (Exception e) {
            System.err.println("❌ Error fetching weather: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Weather fetchWeatherForCity(String city, double latitude, double longitude) {
        try {
            String url = weatherApiUrl + "/weather?lat=" + latitude + "&lon=" + longitude
                    + "&appid=" + apiKey + "&units=metric";

            System.out.println("📡 Fetching weather for " + city);

            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.isEmpty()) {
                System.out.println("⚠️  No response from Weather API for " + city);
                return null;
            }

            return parseWeatherResponse(response, city, latitude, longitude);

        } catch (Exception e) {
            System.err.println("❌ Error fetching weather for " + city + ": " + e.getMessage());
            return null;
        }
    }

    private Weather parseWeatherResponse(String response, String city, double latitude, double longitude) {
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
            weather.setLocation(city);
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

            System.out.println("📊 " + city + ": " + temperature + "°C, " + rainfall + "mm rain");

            return weather;

        } catch (Exception e) {
            System.err.println("❌ Error parsing weather response: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean weatherExists(Weather weather) {
        try {
            List<Weather> recent = weatherRepository.findAll();
            LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);

            for (Weather w : recent) {
                if (w.getLocation().equals(weather.getLocation())
                        && w.getFetchedAt().isAfter(thirtyMinutesAgo)) {
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            System.err.println("Error checking existing weather: " + e.getMessage());
            return false;
        }
    }

    public Weather getLatestWeather(String city) {
        return weatherRepository.findMostRecentByLocation(city).orElse(null);
    }

    public List<Weather> getLatestWeatherForAllCities() {
        try {
            String[] cities = { "Kathmandu", "Pokhara" };
            List<Weather> result = new ArrayList<>();

            for (String city : cities) {
                Weather weather = getLatestWeather(city);
                if (weather != null) {
                    result.add(weather);
                }
            }
            return result;
        } catch (Exception e) {
            System.err.println("Error getting weather for all cities: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Weather> getRainfallHistory(String city, LocalDateTime startTime, LocalDateTime endTime) {
        return weatherRepository.findWeatherHistory(city, startTime, endTime);
    }

    public List<Weather> getHighRainfallEvents(double minRainfall, LocalDateTime startTime, LocalDateTime endTime) {
        return weatherRepository.findHighRainfallEvents(minRainfall, startTime, endTime);
    }
}