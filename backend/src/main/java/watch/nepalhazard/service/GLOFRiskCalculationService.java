package watch.nepalhazard.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import watch.nepalhazard.entity.GlacialLake;
import watch.nepalhazard.entity.Glacier;
import watch.nepalhazard.entity.GlofRiskAssessment;
import watch.nepalhazard.entity.HazardEvent;
import watch.nepalhazard.entity.RiverBasinTown;
import watch.nepalhazard.entity.Weather;
import watch.nepalhazard.repository.HazardEventRepository;
import watch.nepalhazard.repository.RiverBasinTownRepository;
import watch.nepalhazard.repository.WeatherRepository;

@Service
public class GLOFRiskCalculationService {

    private static final long EARTHQUAKE_LOOKBACK_DAYS = 30;
    // Aftershocks decay roughly 1/time (Omori's law), so quake hazard fades
    // exponentially with age instead of cutting off sharply at 30 days.
    // Half-decay at 12 days: ~37% left at 12 days, ~8% at 30.
    private static final double EARTHQUAKE_TIME_DECAY_DAYS = 12.0;
    // Tighter than the earthquake radius/window on purpose - a landslide
    // is localized, so the WATCH floor should only trigger for lakes
    // actually near it, not everything within a loose national radius.
    private static final double LANDSLIDE_FLOOR_RADIUS_KM = 30.0;
    private static final long LANDSLIDE_FLOOR_LOOKBACK_DAYS = 7;
    // A landslide stays in its own valley, so proximity alone isn't enough -
    // also require the same river basin (nearest curated basin town) where
    // that's resolvable. Doesn't apply to earthquakes, which aren't confined
    // to a watershed.
    private static final double BASIN_MATCH_MAX_KM = 60.0;
    private static final List<String> ALERT_ORDER = List.of("NORMAL", "WATCH", "DANGER", "EXTREME");

    private final WeatherRepository weatherRepository;
    private final HazardEventRepository hazardEventRepository;
    private final RiverBasinTownRepository riverBasinTownRepository;

    @Autowired
    public GLOFRiskCalculationService(WeatherRepository weatherRepository,
            HazardEventRepository hazardEventRepository,
            RiverBasinTownRepository riverBasinTownRepository) {
        this.weatherRepository = weatherRepository;
        this.hazardEventRepository = hazardEventRepository;
        this.riverBasinTownRepository = riverBasinTownRepository;
    }

    /**
     * Risk assessment for a single lake: weather, ICIMOD lake type, season,
     * and any recent nearby earthquake or landslide. A detected landslide
     * or HEAVY rainfall also forces a minimum WATCH floor regardless of the
     * weighted score.
     */
    public GlofRiskAssessment assessLake(GlacialLake lake) {
        RainfallAssessment rainfall = assessRainfall(lake.getIcimodId(), lake.getLatitude(), lake.getLongitude());
        double lakeTypeFactor = calculateLakeTypeFactor(lake.getRiskLevel());
        double seasonalModifier = calculateSeasonalModifier(LocalDateTime.now());
        boolean meltCondition = calculateMeltCondition(lake.getIcimodId(), lake.getLatitude(), lake.getLongitude());
        double massWeight = calculateMassWeight(lake.getSurfaceAreaKm2());
        // No slope data for lakes, so the steep+wet pre-condition only
        // applies to glaciers - see assessGlacier().
        boolean landslidePreCondition = false;

        EarthquakeInfluence eqInfluence = findStrongestNearbyEarthquake(lake.getLatitude(), lake.getLongitude());
        LandslideInfluence lsInfluence = findStrongestNearbyLandslide(lake.getLatitude(), lake.getLongitude(),
                lake.getRiverBasin());
        double earthquakeHazard = eqInfluence.hazard() * massWeight;
        double landslideHazard = lsInfluence.hazard() * massWeight;

        double riskScore = 20 * earthquakeHazard
                + 25 * landslideHazard
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
        assessment.setEarthquakeComponent(earthquakeHazard);
        assessment.setLakeTypeComponent(lakeTypeFactor);
        assessment.setSeasonalComponent(seasonalModifier);
        assessment.setLandslideComponent(landslideHazard);
        assessment.setLandslideDetected(lsInfluence.detected());
        assessment.setLandslidePreCondition(landslidePreCondition);
        assessment.setRainfallCondition(rainfall.condition());
        assessment.setMeltCondition(meltCondition);
        assessment.setNearestEarthquakeId(
                lsInfluence.eventId() != null ? lsInfluence.eventId() : eqInfluence.earthquakeId());
        assessment.setAssessedAt(LocalDateTime.now());
        return assessment;
    }

    /**
     * Risk assessment for a glacier watch point - a steep terminus near a
     * river that can collapse and dam it directly, without an existing lake
     * (Langtang Lirung, Aug 2026, is the real example). Same shape as
     * assessLake(), with RGI slope replacing ICIMOD lake type.
     */
    public GlofRiskAssessment assessGlacier(Glacier glacier) {
        RainfallAssessment rainfall = assessRainfall(glacier.getRgiId(), glacier.getTerminusLatitude(),
                glacier.getTerminusLongitude());
        double steepnessFactor = calculateSteepnessFactor(glacier.getSlopeDeg());
        double seasonalModifier = calculateSeasonalModifier(LocalDateTime.now());
        boolean meltCondition = calculateMeltCondition(glacier.getRgiId(), glacier.getTerminusLatitude(),
                glacier.getTerminusLongitude());
        double massWeight = calculateMassWeight(glacier.getAreaKm2());
        boolean landslidePreCondition = calculateLandslidePreCondition(glacier.getSlopeDeg(), rainfall.condition());

        EarthquakeInfluence eqInfluence = findStrongestNearbyEarthquake(glacier.getTerminusLatitude(),
                glacier.getTerminusLongitude());
        LandslideInfluence lsInfluence = findStrongestNearbyLandslide(glacier.getTerminusLatitude(),
                glacier.getTerminusLongitude(), glacier.getNearestRiverBasin());
        double earthquakeHazard = eqInfluence.hazard() * massWeight;
        double landslideHazard = lsInfluence.hazard() * massWeight;

        double riskScore = 20 * earthquakeHazard
                + 25 * landslideHazard
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
        if (landslidePreCondition) {
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
        assessment.setEarthquakeComponent(earthquakeHazard);
        assessment.setLakeTypeComponent(steepnessFactor);
        assessment.setSeasonalComponent(seasonalModifier);
        assessment.setLandslideComponent(landslideHazard);
        assessment.setLandslideDetected(lsInfluence.detected());
        assessment.setLandslidePreCondition(landslidePreCondition);
        assessment.setRainfallCondition(rainfall.condition());
        assessment.setMeltCondition(meltCondition);
        assessment.setNearestEarthquakeId(
                lsInfluence.eventId() != null ? lsInfluence.eventId() : eqInfluence.earthquakeId());
        assessment.setAssessedAt(LocalDateTime.now());
        return assessment;
    }

    /**
     * Scales earthquake/landslide hazard by the point's real area (bigger
     * lake or glacier = more mass in motion under the same shaking).
     * Ranges 0.5 (unknown/tiny) to 1.0 (10km2+).
     */
    double calculateMassWeight(Double areaKm2) {
        if (areaKm2 == null || areaKm2 <= 0) {
            return 0.5;
        }
        return Math.max(0.5, Math.min(1.0, 0.5 + (areaKm2 / 10.0) * 0.5));
    }

    /**
     * Steep terrain plus sustained heavy rain: a "conditions look dangerous
     * right now" flag, ahead of any actual observed landslide. Only glaciers
     * have slope data, so lakes always get false here.
     */
    boolean calculateLandslidePreCondition(Double slopeDeg, String rainfallCondition) {
        if (slopeDeg == null) {
            return false;
        }
        boolean steepEnough = slopeDeg >= 40.0;
        boolean wetEnough = "ELEVATED".equals(rainfallCondition) || "HEAVY".equals(rainfallCondition);
        return steepEnough && wetEnough;
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
     * Picks the last-30-days earthquake with the largest combined
     * magnitude/distance/depth/age hazard for this point, not just the
     * nearest or the strongest. Landslide-type detections are handled
     * separately by findStrongestNearbyLandslide(), which has no time decay.
     */
    private EarthquakeInfluence findStrongestNearbyEarthquake(double lakeLat, double lakeLon) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime since = now.minusDays(EARTHQUAKE_LOOKBACK_DAYS);
        List<HazardEvent> recentEarthquakes = hazardEventRepository.findRecentEarthquakes(since);

        double bestHazard = 0.0;
        Long bestId = null;

        for (HazardEvent eq : recentEarthquakes) {
            double depth = eq.getDepth() != null ? eq.getDepth() : 15.0;
            double hazard = calculateEarthquakeHazard(
                    eq.getMagnitude(), depth,
                    eq.getLatitude(), eq.getLongitude(),
                    lakeLat, lakeLon);

            if (eq.getEventTime() != null) {
                double daysAgo = Duration.between(eq.getEventTime(), now).toMinutes() / (24.0 * 60.0);
                double timeDecay = Math.exp(-Math.max(0, daysAgo) / EARTHQUAKE_TIME_DECAY_DAYS);
                hazard *= timeDecay;
            }

            if (hazard > bestHazard) {
                bestHazard = hazard;
                bestId = eq.getId();
            }
        }

        return new EarthquakeInfluence(bestHazard, bestId);
    }

    /**
     * Recent USGS "landslide"-type detections near this lake - a direct
     * sighting of mass movement, not just shaking. Outside the scope check
     * (30km, 7 days, same river basin where resolvable) it contributes
     * exactly zero; inside it, hazard is graded the same way as earthquakes.
     */
    private LandslideInfluence findStrongestNearbyLandslide(double lakeLat, double lakeLon, String ownBasin) {
        LocalDateTime floorSince = LocalDateTime.now(ZoneOffset.UTC).minusDays(LANDSLIDE_FLOOR_LOOKBACK_DAYS);
        List<HazardEvent> recentLandslides = hazardEventRepository.findRecentLandslides(floorSince);

        double bestHazard = 0.0;
        Long bestId = null;
        boolean detected = false;

        for (HazardEvent landslide : recentLandslides) {
            double distanceKm = haversineDistance(
                    landslide.getLatitude(), landslide.getLongitude(),
                    lakeLat, lakeLon);

            boolean closeAndRecent = distanceKm <= LANDSLIDE_FLOOR_RADIUS_KM
                    && landslide.getEventTime() != null && landslide.getEventTime().isAfter(floorSince);
            boolean sameBasin = isSameBasin(ownBasin, landslide.getLatitude(), landslide.getLongitude());

            if (!closeAndRecent || !sameBasin) {
                continue;
            }
            detected = true;

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

        return new LandslideInfluence(bestHazard, bestId, detected);
    }

    /**
     * True if the event's nearest curated basin town matches the lake's own
     * basin. No real watershed polygons, so this is a nearest-neighbor
     * proxy. Falls back to true (don't restrict) when either side can't be
     * resolved, so missing data never silently suppresses a real floor.
     */
    private boolean isSameBasin(String ownBasin, double eventLat, double eventLon) {
        if (ownBasin == null || ownBasin.isBlank()) {
            return true;
        }
        Optional<String> eventBasin = findNearestBasin(eventLat, eventLon);
        return eventBasin.map(basin -> basin.equalsIgnoreCase(ownBasin)).orElse(true);
    }

    private Optional<String> findNearestBasin(double lat, double lon) {
        List<RiverBasinTown> towns = riverBasinTownRepository.findAll();

        String nearestBasin = null;
        double bestDistance = Double.MAX_VALUE;
        for (RiverBasinTown town : towns) {
            double distanceKm = haversineDistance(town.getLatitude(), town.getLongitude(), lat, lon);
            if (distanceKm < bestDistance) {
                bestDistance = distanceKm;
                nearestBasin = town.getRiverBasin();
            }
        }

        if (nearestBasin == null || bestDistance > BASIN_MATCH_MAX_KM) {
            return Optional.empty();
        }
        return Optional.of(nearestBasin);
    }

    /**
     * Rainfall hazard and condition (NORMAL/ELEVATED/HEAVY) for a location.
     * Uses the location's own weather records when available, otherwise the
     * nearest recorded weather point by coordinate.
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

        double dailyComponent = Math.max(0, Math.min(1, (rainfall24h - 25.0) / 100.0));

        LocalDateTime now = LocalDateTime.now();
        double rainfall7day = calculateRainfallSum(resolvedLocation, now.minusDays(7), now);
        double rainfall14day = calculateRainfallSum(resolvedLocation, now.minusDays(14), now);

        // Sustained ground saturation over 1-2 weeks is its own hazard,
        // independent of any single day's total, so it's scored separately
        // rather than as a multiplier on dailyComponent.
        double cumulative14Component = Math.max(0, Math.min(1, (rainfall14day - 200.0) / 200.0));
        double cumulative7Component = Math.max(0, Math.min(1, (rainfall7day - 100.0) / 150.0));
        double cumulativeComponent = Math.max(cumulative14Component, cumulative7Component);

        String condition;
        if (rainfall14day > 300.0 || rainfall24h > 50.0) {
            condition = "HEAVY";
        } else if (rainfall7day > 150.0 || rainfall24h > 25.0) {
            condition = "ELEVATED";
        } else {
            condition = "NORMAL";
        }

        // Below freezing, precipitation falls as snow rather than
        // melt-driving rain, so only dampen the same-day component with it.
        // cumulativeComponent (ground saturation) stays temperature-
        // independent, same for the reported HEAVY/ELEVATED condition.
        double temperatureMeltMultiplier = weather.getTemperature() == null ? 1.0
                : Math.max(0, Math.min(1, weather.getTemperature() / 15.0));

        double hazard = Math.max(0, Math.min(1,
                Math.max(dailyComponent * temperatureMeltMultiplier, cumulativeComponent)));
        return new RainfallAssessment(hazard, condition);
    }

    /**
     * Recent (3-day) average temperature above freezing - surfaced to the
     * frontend as context, not weighted into the score.
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

    /** Susceptibility from the lake's ICIMOD-assigned risk level. */
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

    /** Terrain-steepness susceptibility from RGI's mean slope_deg. */
    private double calculateSteepnessFactor(double slopeDeg) {
        return Math.max(0, Math.min(1, (slopeDeg - 20.0) / 50.0));
    }

    /**
     * Smooth monsoon-season curve peaking ~Jul 31, tapering to zero by
     * ~May 1 and ~Oct 31, instead of a hard on/off month boundary.
     */
    double calculateSeasonalModifier(LocalDateTime dateTime) {
        int dayOfYear = dateTime.getDayOfYear();
        double peakDay = 212.0;
        double halfWidthDays = 92.0;

        double distance = Math.abs(dayOfYear - peakDay);
        if (distance > halfWidthDays) {
            return 0.0;
        }
        return Math.cos((distance / halfWidthDays) * (Math.PI / 2));
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
