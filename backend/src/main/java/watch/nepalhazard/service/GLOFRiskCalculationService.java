package watch.nepalhazard.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import watch.nepalhazard.entity.GlacialLake;
import watch.nepalhazard.entity.Glacier;
import watch.nepalhazard.entity.GlacierSatelliteObservation;
import watch.nepalhazard.entity.GlofRiskAssessment;
import watch.nepalhazard.entity.HazardEvent;
import watch.nepalhazard.entity.LakeSatelliteObservation;
import watch.nepalhazard.entity.RiverBasinTown;
import watch.nepalhazard.entity.Weather;
import watch.nepalhazard.repository.GlacierSatelliteObservationRepository;
import watch.nepalhazard.repository.HazardEventRepository;
import watch.nepalhazard.repository.LakeSatelliteObservationRepository;
import watch.nepalhazard.repository.RiverBasinTownRepository;
import watch.nepalhazard.repository.WeatherRepository;

@Service
public class GLOFRiskCalculationService {

    private static final long EARTHQUAKE_LOOKBACK_DAYS = 30;
    // Omori's law: aftershock hazard decays ~1/time; half-decay at 12 days (~8% left at 30).
    private static final double EARTHQUAKE_TIME_DECAY_DAYS = 12.0;
    // Tighter than earthquake radius/window - landslides are localized, so the WATCH floor shouldn't fire nationally.
    private static final double LANDSLIDE_FLOOR_RADIUS_KM = 30.0;
    private static final long LANDSLIDE_FLOOR_LOOKBACK_DAYS = 7;
    // Also requires same river basin (nearest curated basin town) - a landslide stays in its own valley; doesn't apply to earthquakes.
    private static final double BASIN_MATCH_MAX_KM = 60.0;
    private static final double KM_PER_DEG_LAT = 111.32;
    private static final List<String> ALERT_ORDER = List.of("NORMAL", "WATCH", "DANGER", "EXTREME");
    // below this fraction of usable (non-cloud) pixels, a reading isn't trustworthy enough to compare
    private static final double SATELLITE_MIN_VALID_PIXEL_FRACTION = 0.30;
    // shorter gaps make the annualized rate noisy - classification jitter needs time to average out
    private static final long SATELLITE_MIN_COMPARISON_DAYS = 20;
    // Calibrated against Rawlins, Watson et al. 2025 "Glacial Lake Observatory" (4,150 HKH lakes, Sentinel-2, 2017-2024,
    // Zenodo doi:10.5281/zenodo.17802333): significant-growth lakes averaged ~2.7%/yr, ~9%/yr at p90, ~18%/yr at the extreme.
    private static final double SATELLITE_NOISE_FLOOR_PCT_PER_YEAR = 2.0;
    private static final double SATELLITE_HAZARD_SATURATION_PCT_PER_YEAR = 18.0;
    // Higher bar than the hazard saturation above - near the dataset's p90 significant-growth rate (~9%/yr), so the hard floor only fires on genuine outliers.
    private static final double SATELLITE_GROWTH_FLOOR_PCT_PER_YEAR = 8.0;

    // Glacier ice/snow-cover sudden-drop thresholds: unlike the lake constants above, NOT backed by a published
    // calibration dataset - physically-reasoned provisional MVP defaults pending real observation history.
    private static final double SATELLITE_ICE_MIN_VALID_PIXEL_FRACTION = 0.30;
    // caps how far apart compared readings can be - a slow multi-month drop is ordinary ablation, not a collapse
    private static final long SATELLITE_ICE_MAX_COMPARISON_DAYS = 15;
    // absolute pct-point drop, not relative - termini often sit near zero already, where a relative rate breaks down
    private static final double SATELLITE_ICE_NOISE_FLOOR_PCT_POINTS = 15.0;
    private static final double SATELLITE_ICE_HAZARD_SATURATION_PCT_POINTS = 50.0;
    private static final double SATELLITE_ICE_DROP_FLOOR_PCT_POINTS = 30.0;

    private final WeatherRepository weatherRepository;
    private final HazardEventRepository hazardEventRepository;
    private final RiverBasinTownRepository riverBasinTownRepository;
    private final LakeSatelliteObservationRepository lakeSatelliteObservationRepository;
    private final GlacierSatelliteObservationRepository glacierSatelliteObservationRepository;
    private final boolean satelliteFactorEnabled;
    private final boolean satelliteGlacierFactorEnabled;

    @Autowired
    public GLOFRiskCalculationService(WeatherRepository weatherRepository,
            HazardEventRepository hazardEventRepository,
            RiverBasinTownRepository riverBasinTownRepository,
            LakeSatelliteObservationRepository lakeSatelliteObservationRepository,
            GlacierSatelliteObservationRepository glacierSatelliteObservationRepository,
            @Value("${satellite.factor.enabled:false}") boolean satelliteFactorEnabled,
            @Value("${satellite.glacier-factor.enabled:false}") boolean satelliteGlacierFactorEnabled) {
        this.weatherRepository = weatherRepository;
        this.hazardEventRepository = hazardEventRepository;
        this.riverBasinTownRepository = riverBasinTownRepository;
        this.lakeSatelliteObservationRepository = lakeSatelliteObservationRepository;
        this.glacierSatelliteObservationRepository = glacierSatelliteObservationRepository;
        this.satelliteFactorEnabled = satelliteFactorEnabled;
        this.satelliteGlacierFactorEnabled = satelliteGlacierFactorEnabled;
    }

    /** Risk assessment for a lake. A detected landslide or HEAVY rainfall forces a minimum WATCH floor regardless of score. */
    public GlofRiskAssessment assessLake(GlacialLake lake) {
        RainfallAssessment rainfall = assessRainfall(lake.getIcimodId(), lake.getLatitude(), lake.getLongitude());
        double lakeTypeFactor = calculateLakeTypeFactor(lake.getRiskLevel());
        double seasonalModifier = calculateSeasonalModifier(LocalDateTime.now());
        boolean meltCondition = calculateMeltCondition(lake.getIcimodId(), lake.getLatitude(), lake.getLongitude());
        double massWeight = calculateMassWeight(lake.getSurfaceAreaKm2());
        // no slope data for lakes - steep+wet pre-condition only applies to glaciers (see assessGlacier())
        boolean landslidePreCondition = false;

        EarthquakeInfluence eqInfluence = findStrongestNearbyEarthquake(lake.getLatitude(), lake.getLongitude());
        LandslideInfluence lsInfluence = findStrongestNearbyLandslide(lake.getLatitude(), lake.getLongitude(),
                lake.getRiverBasin());
        double earthquakeHazard = eqInfluence.hazard() * massWeight;
        double landslideHazard = lsInfluence.hazard() * massWeight;
        SatelliteLakeGrowth satelliteGrowth = assessSatelliteLakeGrowth(lake.getIcimodId());

        // weights scale down x0.85 when satellite factor is on, to make room for it at 15, instead of exceeding 100
        double earthquakeWeight = satelliteFactorEnabled ? 17.0 : 20.0;
        double landslideWeight = satelliteFactorEnabled ? 21.25 : 25.0;
        double rainfallWeight = satelliteFactorEnabled ? 21.25 : 25.0;
        double lakeTypeWeight = satelliteFactorEnabled ? 17.0 : 20.0;
        double seasonWeight = satelliteFactorEnabled ? 8.5 : 10.0;
        double satelliteWeight = satelliteFactorEnabled ? 15.0 : 0.0;

        double riskScore = earthquakeWeight * earthquakeHazard
                + landslideWeight * landslideHazard
                + rainfallWeight * rainfall.hazard()
                + lakeTypeWeight * lakeTypeFactor
                + seasonWeight * seasonalModifier
                + satelliteWeight * satelliteGrowth.hazard();
        riskScore = Math.max(0, Math.min(100, riskScore));

        String alertLevel = getAlertLevel(riskScore);
        if (lsInfluence.detected()) {
            alertLevel = escalate(alertLevel, "WATCH");
        }
        if ("HEAVY".equals(rainfall.condition())) {
            alertLevel = escalate(alertLevel, "WATCH");
        }
        if (satelliteFactorEnabled && satelliteGrowth.growthDetected()) {
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
        assessment.setSatelliteWaterFraction(satelliteGrowth.currentWaterFraction());
        assessment.setSatelliteLakeGrowthDetected(satelliteGrowth.growthDetected());
        assessment.setSatelliteComponent(satelliteGrowth.hazard());
        // no glacier terminus box to measure for a lake row
        assessment.setSatelliteIceFraction(null);
        assessment.setSatelliteIceSuddenDropDetected(false);
        assessment.setAssessedAt(LocalDateTime.now());
        return assessment;
    }

    /**
     * Compares the two most recent Sentinel-2 water-fraction readings, converts to an annualized relative
     * rate, and grades against the SATELLITE_* calibration constants above. No-growth if there's no prior
     * reading, the gap is too short, the baseline is too near zero, or coverage is untrustworthy.
     */
    private SatelliteLakeGrowth assessSatelliteLakeGrowth(String icimodId) {
        if (icimodId == null) {
            return new SatelliteLakeGrowth(null, 0, false);
        }
        List<LakeSatelliteObservation> recent = lakeSatelliteObservationRepository
                .findTop2ByIcimodIdOrderByObservedAtDesc(icimodId);
        if (recent.isEmpty()) {
            return new SatelliteLakeGrowth(null, 0, false);
        }

        LakeSatelliteObservation latest = recent.get(0);
        if (recent.size() < 2) {
            return new SatelliteLakeGrowth(latest.getWaterFraction(), 0, false);
        }

        LakeSatelliteObservation previous = recent.get(1);
        boolean bothTrustworthy = latest.getValidPixelFraction() >= SATELLITE_MIN_VALID_PIXEL_FRACTION
                && previous.getValidPixelFraction() >= SATELLITE_MIN_VALID_PIXEL_FRACTION;

        long daysBetween = Duration.between(previous.getObservedAt(), latest.getObservedAt()).toDays();
        boolean comparisonLongEnough = daysBetween >= SATELLITE_MIN_COMPARISON_DAYS;
        boolean previousBaselineUsable = previous.getWaterFraction() != null && previous.getWaterFraction() >= 0.02;

        if (!bothTrustworthy || !comparisonLongEnough || !previousBaselineUsable) {
            return new SatelliteLakeGrowth(latest.getWaterFraction(), 0, false);
        }

        double relativeChangePct = (latest.getWaterFraction() - previous.getWaterFraction())
                / previous.getWaterFraction() * 100.0;
        double annualizedRatePct = relativeChangePct * (365.0 / daysBetween);

        double hazard = Math.max(0, Math.min(1,
                (annualizedRatePct - SATELLITE_NOISE_FLOOR_PCT_PER_YEAR)
                        / (SATELLITE_HAZARD_SATURATION_PCT_PER_YEAR - SATELLITE_NOISE_FLOOR_PCT_PER_YEAR)));
        boolean grew = annualizedRatePct > SATELLITE_GROWTH_FLOOR_PCT_PER_YEAR;

        return new SatelliteLakeGrowth(latest.getWaterFraction(), hazard, grew);
    }

    private record SatelliteLakeGrowth(Double currentWaterFraction, double hazard, boolean growthDetected) {
    }

    /** Risk assessment for a glacier watch point - a steep terminus near a river that can collapse and dam it directly (e.g. Langtang Lirung, Aug 2026), without an existing lake. Same shape as assessLake(), with RGI slope replacing lake type. */
    public GlofRiskAssessment assessGlacier(Glacier glacier) {
        RainfallAssessment rainfall = assessRainfall(glacier.getRgiId(), glacier.getTerminusLatitude(),
                glacier.getTerminusLongitude());
        double effectiveSlopeDeg = effectiveSlopeDeg(glacier);
        double steepnessFactor = calculateSteepnessFactor(effectiveSlopeDeg);
        double seasonalModifier = calculateSeasonalModifier(LocalDateTime.now());
        boolean meltCondition = calculateMeltCondition(glacier.getRgiId(), glacier.getTerminusLatitude(),
                glacier.getTerminusLongitude());
        double massWeight = calculateMassWeight(glacier.getAreaKm2());
        boolean landslidePreCondition = calculateLandslidePreCondition(effectiveSlopeDeg, rainfall.condition());

        EarthquakeInfluence eqInfluence = findStrongestNearbyEarthquake(glacier.getTerminusLatitude(),
                glacier.getTerminusLongitude());
        LandslideInfluence lsInfluence = findStrongestNearbyLandslide(glacier.getTerminusLatitude(),
                glacier.getTerminusLongitude(), glacier.getNearestRiverBasin());
        double earthquakeHazard = eqInfluence.hazard() * massWeight;
        double landslideHazard = lsInfluence.hazard() * massWeight;
        SatelliteIceDrop iceDrop = assessSatelliteIceDrop(glacier.getRgiId());

        // same weight-scaling as assessLake(), for the 15-point ice-drop term
        double earthquakeWeight = satelliteGlacierFactorEnabled ? 17.0 : 20.0;
        double landslideWeight = satelliteGlacierFactorEnabled ? 21.25 : 25.0;
        double rainfallWeight = satelliteGlacierFactorEnabled ? 21.25 : 25.0;
        double steepnessWeight = satelliteGlacierFactorEnabled ? 17.0 : 20.0;
        double seasonWeight = satelliteGlacierFactorEnabled ? 8.5 : 10.0;
        double satelliteWeight = satelliteGlacierFactorEnabled ? 15.0 : 0.0;

        double riskScore = earthquakeWeight * earthquakeHazard
                + landslideWeight * landslideHazard
                + rainfallWeight * rainfall.hazard()
                + steepnessWeight * steepnessFactor
                + seasonWeight * seasonalModifier
                + satelliteWeight * iceDrop.hazard();
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
        if (satelliteGlacierFactorEnabled && iceDrop.suddenDropDetected()) {
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
        // no lake to measure for a glacier watch point
        assessment.setSatelliteLakeGrowthDetected(false);
        assessment.setSatelliteWaterFraction(null);
        assessment.setSatelliteIceFraction(iceDrop.currentIceFraction());
        assessment.setSatelliteIceSuddenDropDetected(iceDrop.suddenDropDetected());
        assessment.setSatelliteComponent(iceDrop.hazard());
        assessment.setAssessedAt(LocalDateTime.now());
        return assessment;
    }

    /**
     * Compares the two most recent Sentinel-2 ice/snow-cover readings and flags a sharp, short-window drop as
     * a possible terminus collapse. Uses an absolute pct-point drop (not relative) since termini often sit
     * near zero already. No-drop if there's no prior reading, the gap is too long, or coverage is untrustworthy.
     */
    private SatelliteIceDrop assessSatelliteIceDrop(String rgiId) {
        if (rgiId == null) {
            return new SatelliteIceDrop(null, 0, false);
        }
        List<GlacierSatelliteObservation> recent = glacierSatelliteObservationRepository
                .findTop2ByRgiIdOrderByObservedAtDesc(rgiId);
        if (recent.isEmpty()) {
            return new SatelliteIceDrop(null, 0, false);
        }

        GlacierSatelliteObservation latest = recent.get(0);
        if (recent.size() < 2) {
            return new SatelliteIceDrop(latest.getIceFraction(), 0, false);
        }

        GlacierSatelliteObservation previous = recent.get(1);
        boolean bothTrustworthy = latest.getValidPixelFraction() >= SATELLITE_ICE_MIN_VALID_PIXEL_FRACTION
                && previous.getValidPixelFraction() >= SATELLITE_ICE_MIN_VALID_PIXEL_FRACTION;

        long daysBetween = Duration.between(previous.getObservedAt(), latest.getObservedAt()).toDays();
        boolean suddenEnoughWindow = daysBetween <= SATELLITE_ICE_MAX_COMPARISON_DAYS;

        if (!bothTrustworthy || !suddenEnoughWindow) {
            return new SatelliteIceDrop(latest.getIceFraction(), 0, false);
        }

        double dropPctPoints = (previous.getIceFraction() - latest.getIceFraction()) * 100.0;

        double hazard = Math.max(0, Math.min(1,
                (dropPctPoints - SATELLITE_ICE_NOISE_FLOOR_PCT_POINTS)
                        / (SATELLITE_ICE_HAZARD_SATURATION_PCT_POINTS - SATELLITE_ICE_NOISE_FLOOR_PCT_POINTS)));
        boolean suddenDrop = dropPctPoints > SATELLITE_ICE_DROP_FLOOR_PCT_POINTS;

        return new SatelliteIceDrop(latest.getIceFraction(), hazard, suddenDrop);
    }

    private record SatelliteIceDrop(Double currentIceFraction, double hazard, boolean suddenDropDetected) {
    }

    /** Scales earthquake/landslide hazard by area (more mass in motion under the same shaking). Ranges 0.5 (unknown/tiny) to 1.0 (10km2+). */
    double calculateMassWeight(Double areaKm2) {
        if (areaKm2 == null || areaKm2 <= 0) {
            return 0.5;
        }
        return Math.max(0.5, Math.min(1.0, 0.5 + (areaKm2 / 10.0) * 0.5));
    }

    /** Steep terrain plus heavy rain, ahead of any actual observed landslide. Lakes always get false (no slope data). */
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

    /** Largest combined magnitude/distance/depth/age hazard in the last 30 days - not just nearest or strongest. */
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

    /** Recent USGS landslide detections. Outside the scope check (30km, 7 days, same basin) contributes zero. */
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
     * True if the event's nearest point on a curated river course (basin towns connected in downstream order,
     * not just nearest single town) matches the lake's basin. Falls back to true when unresolvable, so missing
     * data never silently suppresses a real floor.
     */
    private boolean isSameBasin(String ownBasin, double eventLat, double eventLon) {
        if (ownBasin == null || ownBasin.isBlank()) {
            return true;
        }
        Optional<String> eventBasin = findNearestBasin(eventLat, eventLon);
        return eventBasin.map(basin -> basin.equalsIgnoreCase(ownBasin)).orElse(true);
    }

    private Optional<String> findNearestBasin(double lat, double lon) {
        Map<String, List<RiverBasinTown>> byBasin = riverBasinTownRepository.findAll().stream()
                .collect(Collectors.groupingBy(RiverBasinTown::getRiverBasin));

        String nearestBasin = null;
        double bestDistance = Double.MAX_VALUE;

        for (Map.Entry<String, List<RiverBasinTown>> entry : byBasin.entrySet()) {
            List<RiverBasinTown> course = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(RiverBasinTown::getDownstreamOrder))
                    .toList();

            for (int i = 0; i < course.size(); i++) {
                double distanceKm;
                if (i + 1 < course.size()) {
                    RiverBasinTown a = course.get(i);
                    RiverBasinTown b = course.get(i + 1);
                    distanceKm = pointToSegmentDistanceKm(lat, lon,
                            a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
                } else if (course.size() == 1) {
                    RiverBasinTown a = course.get(0);
                    distanceKm = haversineDistance(lat, lon, a.getLatitude(), a.getLongitude());
                } else {
                    continue;
                }
                if (distanceKm < bestDistance) {
                    bestDistance = distanceKm;
                    nearestBasin = entry.getKey();
                }
            }
        }

        if (nearestBasin == null || bestDistance > BASIN_MATCH_MAX_KM) {
            return Optional.empty();
        }
        return Optional.of(nearestBasin);
    }

    /** Point-to-segment distance, approximating lat/lon as flat km - fine at river-segment scale, not survey-grade. */
    private double pointToSegmentDistanceKm(double lat, double lon, double lat1, double lon1, double lat2,
            double lon2) {
        double kmPerDegLon = KM_PER_DEG_LAT * Math.cos(Math.toRadians(lat));

        double px = lon * kmPerDegLon, py = lat * KM_PER_DEG_LAT;
        double ax = lon1 * kmPerDegLon, ay = lat1 * KM_PER_DEG_LAT;
        double bx = lon2 * kmPerDegLon, by = lat2 * KM_PER_DEG_LAT;

        double abx = bx - ax, aby = by - ay;
        double abLengthSq = abx * abx + aby * aby;
        double t = abLengthSq == 0 ? 0 : ((px - ax) * abx + (py - ay) * aby) / abLengthSq;
        t = Math.max(0, Math.min(1, t));

        double closestX = ax + t * abx, closestY = ay + t * aby;
        double dx = px - closestX, dy = py - closestY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** Rainfall hazard/condition. Uses the location's own weather records, falling back to nearest by coordinate. */
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

        // sustained saturation over 1-2 weeks is scored separately, not as a multiplier on dailyComponent
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

        // below freezing, precip falls as snow not rain - only dampens dailyComponent, not cumulative or condition
        double temperatureMeltMultiplier = weather.getTemperature() == null ? 1.0
                : Math.max(0, Math.min(1, weather.getTemperature() / 15.0));

        double hazard = Math.max(0, Math.min(1,
                Math.max(dailyComponent * temperatureMeltMultiplier, cumulativeComponent)));
        return new RainfallAssessment(hazard, condition);
    }

    /** 3-day average temperature above freezing - context only, not weighted into the score. */
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
    double calculateSteepnessFactor(double slopeDeg) {
        return Math.max(0, Math.min(1, (slopeDeg - 20.0) / 50.0));
    }

    /** Prefers the locally-computed terminus slope over RGI's whole-glacier mean; falls back to RGI if not yet computed. */
    private double effectiveSlopeDeg(Glacier glacier) {
        return glacier.getLocalSlopeDeg() != null ? glacier.getLocalSlopeDeg() : glacier.getSlopeDeg();
    }

    /** Smooth monsoon-season curve peaking ~Jul 31, tapering to zero by ~May 1 and ~Oct 31. */
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
