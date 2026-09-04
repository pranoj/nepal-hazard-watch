package watch.nepalhazard.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import watch.nepalhazard.entity.HazardEvent;
import watch.nepalhazard.entity.Weather;
import watch.nepalhazard.repository.WeatherRepository;

@Service
public class GLOFRiskCalculationService {

    private final WeatherRepository weatherRepository;

    @Autowired
    public GLOFRiskCalculationService(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository;
    }

    public double calculateGLOFRisk(
            HazardEvent earthquake,
            double glacierLatitude,
            double glacierLongitude,
            double glacierAreaChangePerMonth) {

        double earthquakeHazard = calculateEarthquakeHazard(
                earthquake.getMagnitude(),
                earthquake.getDepth(),
                earthquake.getLatitude(),
                earthquake.getLongitude(),
                glacierLatitude,
                glacierLongitude);

        double proximityTrigger = calculateProximityTrigger(
                earthquake.getMagnitude(),
                earthquake.getDepth(),
                earthquake.getLatitude(),
                earthquake.getLongitude(),
                glacierLatitude,
                glacierLongitude);

        double rainfallHazard = calculateRainfallHazard(
                glacierLatitude,
                glacierLongitude);

        double glacierLakeFactor = calculateGlacierLakeFactor(
                glacierAreaChangePerMonth);

        double seasonalModifier = calculateSeasonalModifier(LocalDateTime.now());

        double risk = 70 * earthquakeHazard
                + 20 * proximityTrigger
                + 10 * rainfallHazard
                + 15 * glacierLakeFactor
                + 5 * seasonalModifier;

        return Math.min(100, risk);
    }

    private double calculateEarthquakeHazard(
            double magnitude,
            double depthKm,
            double epicenterLat,
            double epicenterLon,
            double glacierLat,
            double glacierLon) {

        double distanceKm = haversineDistance(
                epicenterLat, epicenterLon,
                glacierLat, glacierLon);

        double magnitudeComponent = (magnitude - 3.8) / 1.4;
        double distanceDecay = Math.exp(-distanceKm / 60.0);
        double depthDecay = Math.exp(-depthKm / 40.0);

        double hazard = magnitudeComponent * distanceDecay * depthDecay;

        return Math.max(0, Math.min(1, hazard));
    }

    private double calculateProximityTrigger(
            double magnitude,
            double depthKm,
            double epicenterLat,
            double epicenterLon,
            double glacierLat,
            double glacierLon) {

        double distanceKm = haversineDistance(
                epicenterLat, epicenterLon,
                glacierLat, glacierLon);

        boolean meetsMinMagnitude = magnitude >= 4.2;
        boolean meetsMaxDistance = distanceKm <= 40.0;
        boolean meetsMaxDepth = depthKm <= 30.0;

        return (meetsMinMagnitude && meetsMaxDistance && meetsMaxDepth) ? 1.0 : 0.0;
    }

    private double calculateRainfallHazard(
            double glacierLat,
            double glacierLon) {

        Optional<Weather> latestWeather = weatherRepository.findNearestByCoordinates(glacierLat, glacierLon);

        if (latestWeather.isEmpty()) {
            return 0.0;
        }

        Weather weather = latestWeather.get();
        double rainfall24h = weather.getRainfall();

        double baseComponent = (rainfall24h - 25.0) / 100.0;

        double cumulativeBonus = 0.0;
        LocalDateTime now = LocalDateTime.now();

        double rainfall7day = calculateRainfallSum(
                glacierLat, glacierLon,
                now.minusDays(7), now);

        double rainfall14day = calculateRainfallSum(
                glacierLat, glacierLon,
                now.minusDays(14), now);

        if (rainfall14day > 300.0) {
            cumulativeBonus = 0.4;
        } else if (rainfall7day > 150.0) {
            cumulativeBonus = 0.2;
        }

        double hazard = baseComponent * (1.0 + cumulativeBonus);
        return Math.max(0, Math.min(1, hazard));
    }

    private double calculateGlacierLakeFactor(double areaChangePerMonth) {
        double growthRisk = (areaChangePerMonth - 5.0) / 20.0;
        double shrinkageRisk = (-areaChangePerMonth - 5.0) / 15.0;
        double risk = Math.max(growthRisk, shrinkageRisk);
        return Math.max(-1, Math.min(1, risk));
    }

    private double calculateSeasonalModifier(LocalDateTime dateTime) {
        int month = dateTime.getMonthValue();

        if (month >= 6 && month <= 9) {
            return 1.0;
        } else if (month == 5 || month == 10) {
            return 0.5;
        } else {
            return 0.0;
        }
    }

    public String getAlertLevel(double riskScore) {
        if (riskScore >= 75) {
            return "EXTREME";
        } else if (riskScore >= 50) {
            return "DANGER";
        } else if (riskScore >= 25) {
            return "WATCH";
        } else {
            return "NORMAL";
        }
    }

    public String getResponseAction(String alertLevel) {
        switch (alertLevel) {
            case "EXTREME":
                return "EMERGENCY BROADCAST: Immediate evacuation of hazard zones. Activate emergency services.";
            case "DANGER":
                return "PUBLIC ALERT: Evacuate identified flood zones. Notify local authorities.";
            case "WATCH":
                return "ENHANCED MONITORING: Notify authorities, prepare evacuation plans, monitor situation closely.";
            case "NORMAL":
                return "Routine monitoring. No action required.";
            default:
                return "Unknown alert level.";
        }
    }

    private double haversineDistance(
            double lat1, double lon1,
            double lat2, double lon2) {

        double R = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        return R * c;
    }

    private double calculateRainfallSum(
            double glacierLat,
            double glacierLon,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Weather> weatherData = weatherRepository.findWeatherHistory("", startTime, endTime);

        return weatherData.stream()
                .mapToDouble(Weather::getRainfall)
                .sum();
    }
}