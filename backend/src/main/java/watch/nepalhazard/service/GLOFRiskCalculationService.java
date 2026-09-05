package watch.nepalhazard.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import watch.nepalhazard.entity.GlacialLake;
import watch.nepalhazard.entity.Glacier;
import watch.nepalhazard.entity.GlofRiskAssessment;
import watch.nepalhazard.entity.HazardEvent;
import watch.nepalhazard.entity.Weather;
import watch.nepalhazard.repository.HazardEventRepository;
import watch.nepalhazard.repository.WeatherRepository;

@Service
public class GLOFRiskCalculationService {

    private static final long EARTHQUAKE_LOOKBACK_DAYS = 30;
    private static final double LANDSLIDE_DETECTION_RADIUS_KM = 100.0;
    private static final List<String> ALERT_ORDER = List.of("NORMAL", "WATCH", "DANGER", "EXTREME");

    private final WeatherRepository weatherRepository;
    private final HazardEventRepository hazardEventRepository;

    @Autowired
    public GLOFRiskCalculationService(WeatherRepository weatherRepository,
            HazardEventRepository hazardEventRepository) {
        this.weatherRepository = weatherRepository;
        this.hazardEventRepository = hazardEventRepository;
    }

    /**
     * Automatic, standalone risk assessment for a single lake, using real
     * weather at that lake's own coordinates, lake type susceptibility (from
     * ICIMOD), season, and any recent nearby earthquake or landslide
     * detection. Does not require any of these to have occurred - rainfall/
     * lake-type/season alone can carry a lake into WATCH/DANGER.
     *
     * A recent landslide detection or HEAVY rainfall condition also applies
     * a minimum alert-level floor, so a single strong direct signal can't
     * get diluted away by an otherwise-low weighted average.
     */
    public GlofRiskAssessment assessLake(GlacialLake lake) {
        RainfallAssessment rainfall = assessRainfall(lake.getIcimodId(), lake.getLatitude(), lake.getLongitude());
        double lakeTypeFactor = calculateLakeTypeFactor(lake.getRiskLevel());
        double seasonalModifier = calculateSeasonalModifier(LocalDateTime.now());
        boolean meltCondition = calculateMeltCondition(lake.getIcimodId(), lake.getLatitude(), lake.getLongitude());

        EarthquakeInfluence eqInfluence = findStrongestNearbyEarthquake(lake.getLatitude(), lake.getLongitude());
        LandslideInfluence lsInfluence = findStrongestNearbyLandslide(lake.getLatitude(), lake.getLongitude());

        double riskScore = 20 * eqInfluence.hazard()
                + 25 * lsInfluence.hazard()
                + 25 * rainfall.hazard()
                + 20 * lakeTypeFactor
                + 10 * seasonalModifier;
        riskScore = Math.max(0, Math.min(100, riskScore));

        String alertLevel = getAlertLevel(riskScore);
        if (lsInfluence.detected()) {
            alertLevel = escalate(alertLevel, "WATCH");
        }
        if ("HEAVY".equals(rainfall.condition())) {
            alertLevel = escalate(alertLevel, "WATCH");
        }

        GlofRiskAssessment assessment = new GlofRiskAssessment();
        assessment.setSourceType("LAKE");
        assessment.setGlacialLakeId(lake.getId());
        assessment.setLakeName(lake.getLakeName());
        assessment.setIcimodId(lake.getIcimodId());
        assessment.setRiverBasin(lake.getRiverBasin());
        assessment.setLatitude(lake.getLatitude());
        assessment.setLongitude(lake.getLongitude());
        assessment.setRiskScore(riskScore);
        assessment.setAlertLevel(alertLevel);
        assessment.setRainfallComponent(rainfall.hazard());
        assessment.setEarthquakeComponent(eqInfluence.hazard());
        assessment.setLakeTypeComponent(lakeTypeFactor);
        assessment.setSeasonalComponent(seasonalModifier);
        assessment.setLandslideComponent(lsInfluence.hazard());
        assessment.setLandslideDetected(lsInfluence.detected());
        assessment.setRainfallCondition(rainfall.condition());
        assessment.setMeltCondition(meltCondition);
        assessment.setNearestEarthquakeId(
                lsInfluence.eventId() != null ? lsInfluence.eventId() : eqInfluence.earthquakeId());
        assessment.setAssessedAt(LocalDateTime.now());
        return assessment;
    }

    /**
     * "Type B" risk assessment for a glacier watch point - a steep terminus
     * near a known river corridor that can collapse and dam the river
     * directly, without any pre-existing lake (as happened at Langtang
     * Lirung on Aug 2026). Mirrors assessLake(), substituting a terrain-
     * steepness factor (from RGI slope_deg) for lake-type susceptibility,
     * since glaciers have no ICIMOD risk classification.
     */
    public GlofRiskAssessment assessGlacier(Glacier glacier) {
        RainfallAssessment rainfall = assessRainfall(glacier.getRgiId(), glacier.getTerminusLatitude(),
                glacier.getTerminusLongitude());
        double steepnessFactor = calculateSteepnessFactor(glacier.getSlopeDeg());
        double seasonalModifier = calculateSeasonalModifier(LocalDateTime.now());
        boolean meltCondition = calculateMeltCondition(glacier.getRgiId(), glacier.getTerminusLatitude(),
                glacier.getTerminusLongitude());

        EarthquakeInfluence eqInfluence = findStrongestNearbyEarthquake(glacier.getTerminusLatitude(),
                glacier.getTerminusLongitude());
        LandslideInfluence lsInfluence = findStrongestNearbyLandslide(glacier.getTerminusLatitude(),
                glacier.getTerminusLongitude());

        double riskScore = 20 * eqInfluence.hazard()
                + 25 * lsInfluence.hazard()
                + 25 * rainfall.hazard()
                + 20 * steepnessFactor
                + 10 * seasonalModifier;
        riskScore = Math.max(0, Math.min(100, riskScore));

        String alertLevel = getAlertLevel(riskScore);
        if (lsInfluence.detected()) {
            alertLevel = escalate(alertLevel, "WATCH");
        }
        if ("HEAVY".equals(rainfall.condition())) {
            alertLevel = escalate(alertLevel, "WATCH");
        }

        GlofRiskAssessment assessment = new GlofRiskAssessment();
        assessment.setSourceType("GLACIER");
        assessment.setGlacierId(glacier.getId());
        assessment.setLakeName(glacier.getGlacierName());
        assessment.setIcimodId(glacier.getRgiId());
        assessment.setRiverBasin(glacier.getNearestRiverBasin());
        assessment.setLatitude(glacier.getTerminusLatitude());
        assessment.setLongitude(glacier.getTerminusLongitude());
        assessment.setRiskScore(riskScore);
        assessment.setAlertLevel(alertLevel);
        assessment.setRainfallComponent(rainfall.hazard());
        assessment.setEarthquakeComponent(eqInfluence.hazard());
        assessment.setLakeTypeComponent(steepnessFactor);
        assessment.setSeasonalComponent(seasonalModifier);
        assessment.setLandslideComponent(lsInfluence.hazard());
        assessment.setLandslideDetected(lsInfluence.detected());
        assessment.setRainfallCondition(rainfall.condition());
        assessment.setMeltCondition(meltCondition);
        assessment.setNearestEarthquakeId(
                lsInfluence.eventId() != null ? lsInfluence.eventId() : eqInfluence.earthquakeId());
        assessment.setAssessedAt(LocalDateTime.now());
        return assessment;
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

    /**
     * Finds the recent (last 30 days) tectonic earthquake whose magnitude/
     * distance/depth combination produces the largest hazard contribution
     * for this lake - not simply the nearest or the strongest in isolation,
     * since a large-but-far quake can matter more than a small-but-close
     * one. Landslide-type detections are excluded here; see
     * findStrongestNearbyLandslide().
     */
    private EarthquakeInfluence findStrongestNearbyEarthquake(double lakeLat, double lakeLon) {
        LocalDateTime since = LocalDateTime.now().minusDays(EARTHQUAKE_LOOKBACK_DAYS);
        List<HazardEvent> recentEarthquakes = hazardEventRepository.findRecentEarthquakes(since);

        double bestHazard = 0.0;
        Long bestId = null;

        for (HazardEvent eq : recentEarthquakes) {
            double depth = eq.getDepth() != null ? eq.getDepth() : 15.0;
            double hazard = calculateEarthquakeHazard(
                    eq.getMagnitude(), depth,
                    eq.getLatitude(), eq.getLongitude(),
                    lakeLat, lakeLon);

            if (hazard > bestHazard) {
                bestHazard = hazard;
                bestId = eq.getId();
            }
        }

        return new EarthquakeInfluence(bestHazard, bestId);
    }

    /**
     * Finds recent (last 30 days) USGS "landslide"-type detections near this
     * lake - a direct seismic-network sighting of actual mass movement (e.g.
     * an ice/rock avalanche), not just shaking that might destabilize
     * something. "detected" is a plain proximity check (within 100km),
     * independent of the decayed hazard value, so a real nearby detection
     * always registers even when far enough away that the smooth decay
     * curve alone would round it down to nearly nothing.
     */
    private LandslideInfluence findStrongestNearbyLandslide(double lakeLat, double lakeLon) {
        LocalDateTime since = LocalDateTime.now().minusDays(EARTHQUAKE_LOOKBACK_DAYS);
        List<HazardEvent> recentLandslides = hazardEventRepository.findRecentLandslides(since);

        double bestHazard = 0.0;
        Long bestId = null;
        double closestDistanceKm = Double.MAX_VALUE;

        for (HazardEvent landslide : recentLandslides) {
            double distanceKm = haversineDistance(
                    landslide.getLatitude(), landslide.getLongitude(),
                    lakeLat, lakeLon);
            closestDistanceKm = Math.min(closestDistanceKm, distanceKm);

            double depth = landslide.getDepth() != null ? landslide.getDepth() : 5.0;
            double hazard = calculateEarthquakeHazard(
                    landslide.getMagnitude(), depth,
                    landslide.getLatitude(), landslide.getLongitude(),
                    lakeLat, lakeLon);

            if (hazard > bestHazard) {
                bestHazard = hazard;
                bestId = landslide.getId();
            }
        }

        boolean detected = closestDistanceKm <= LANDSLIDE_DETECTION_RADIUS_KM;
        return new LandslideInfluence(bestHazard, bestId, detected);
    }

    /**
     * Rainfall hazard and condition (NORMAL/ELEVATED/HEAVY) for a location.
     * If preferredLocationKey (a lake's icimodId) has its own weather
     * records, uses those directly; otherwise falls back to the nearest
     * recorded weather point by coordinate (used by the manual single-point
     * endpoint, which has no lake identifier).
     */
    private RainfallAssessment assessRainfall(String preferredLocationKey, double lat, double lon) {
        Optional<Weather> latestWeather = Optional.empty();
        if (preferredLocationKey != null) {
            latestWeather = weatherRepository.findLatestByLocation(preferredLocationKey);
        }
        if (latestWeather.isEmpty()) {
            latestWeather = weatherRepository.findNearestByCoordinates(lat, lon);
        }
        if (latestWeather.isEmpty()) {
            return new RainfallAssessment(0.0, "NORMAL");
        }

        Weather weather = latestWeather.get();
        String resolvedLocation = weather.getLocation();
        double rainfall24h = weather.getRainfall();

        double baseComponent = (rainfall24h - 25.0) / 100.0;

        LocalDateTime now = LocalDateTime.now();
        double rainfall7day = calculateRainfallSum(resolvedLocation, now.minusDays(7), now);
        double rainfall14day = calculateRainfallSum(resolvedLocation, now.minusDays(14), now);

        double cumulativeBonus;
        String condition;
        if (rainfall14day > 300.0 || rainfall24h > 50.0) {
            cumulativeBonus = 0.4;
            condition = "HEAVY";
        } else if (rainfall7day > 150.0 || rainfall24h > 25.0) {
            cumulativeBonus = 0.2;
            condition = "ELEVATED";
        } else {
            cumulativeBonus = 0.0;
            condition = "NORMAL";
        }

        double hazard = Math.max(0, Math.min(1, baseComponent * (1.0 + cumulativeBonus)));
        return new RainfallAssessment(hazard, condition);
    }

    /**
     * Active glacier-melt indicator: recent (3-day) average temperature at
     * this lake above freezing. Not weighted into the score directly - it's
     * a conditioning signal surfaced to the frontend rather than a scored
     * component, since melt alone rarely is the acute trigger.
     */
    private boolean calculateMeltCondition(String locationKey, double lat, double lon) {
        LocalDateTime now = LocalDateTime.now();
        List<Weather> recent = weatherRepository.findWeatherHistory(locationKey, now.minusDays(3), now);

        if (!recent.isEmpty()) {
            double avgTemp = recent.stream().mapToDouble(Weather::getTemperature).average().orElse(Double.NaN);
            return avgTemp > 0.0;
        }

        return weatherRepository.findNearestByCoordinates(lat, lon)
                .map(w -> w.getTemperature() != null && w.getTemperature() > 0.0)
                .orElse(false);
    }

    /**
     * Susceptibility derived from the lake's ICIMOD-assigned risk level
     * (lake type: ice-dammed/moraine-dammed/supraglacial, repeat-GLOF
     * history) - real inventory data, not a placeholder.
     */
    private double calculateLakeTypeFactor(String riskLevel) {
        if (riskLevel == null) {
            return 0.4;
        }
        switch (riskLevel) {
            case "Very High":
                return 1.0;
            case "High":
                return 0.7;
            case "Medium":
                return 0.4;
            case "Low":
                return 0.15;
            default:
                return 0.4;
        }
    }

    /**
     * Terrain-steepness susceptibility for a glacier terminus, from RGI's
     * mean slope_deg. GlacierSyncService already filters candidates to
     * slope >= 30 degrees, so this scales that practical range (20-70)
     * toward the high end rather than starting from 0.
     */
    private double calculateSteepnessFactor(double slopeDeg) {
        return Math.max(0, Math.min(1, (slopeDeg - 20.0) / 50.0));
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

    private String escalate(String currentLevel, String floorLevel) {
        return ALERT_ORDER.indexOf(floorLevel) > ALERT_ORDER.indexOf(currentLevel) ? floorLevel : currentLevel;
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
            String locationKey,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Weather> weatherData = weatherRepository.findWeatherHistory(locationKey, startTime, endTime);

        return weatherData.stream()
                .mapToDouble(Weather::getRainfall)
                .sum();
    }

    private record EarthquakeInfluence(double hazard, Long earthquakeId) {
    }

    private record LandslideInfluence(double hazard, Long eventId, boolean detected) {
    }

    private record RainfallAssessment(double hazard, String condition) {
    }
}
