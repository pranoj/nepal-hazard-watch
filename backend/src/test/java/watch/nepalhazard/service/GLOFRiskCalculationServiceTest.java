package watch.nepalhazard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import watch.nepalhazard.entity.GlacialLake;
import watch.nepalhazard.entity.Glacier;
import watch.nepalhazard.entity.GlacierSatelliteObservation;
import watch.nepalhazard.entity.GlofRiskAssessment;
import watch.nepalhazard.entity.HazardEvent;
import watch.nepalhazard.entity.LakeSatelliteObservation;
import watch.nepalhazard.entity.Weather;
import watch.nepalhazard.repository.GlacierSatelliteObservationRepository;
import watch.nepalhazard.repository.HazardEventRepository;
import watch.nepalhazard.repository.LakeSatelliteObservationRepository;
import watch.nepalhazard.repository.RiverBasinTownRepository;
import watch.nepalhazard.repository.WeatherRepository;

/**
 * Formula unit tests with controlled synthetic inputs, since live
 * conditions rarely exercise every code path. Repositories are mocked.
 */
@ExtendWith(MockitoExtension.class)
class GLOFRiskCalculationServiceTest {

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private HazardEventRepository hazardEventRepository;

    @Mock
    private RiverBasinTownRepository riverBasinTownRepository;

    @Mock
    private LakeSatelliteObservationRepository lakeSatelliteObservationRepository;

    @Mock
    private GlacierSatelliteObservationRepository glacierSatelliteObservationRepository;

    private GLOFRiskCalculationService service;

    @BeforeEach
    void setUp() {
        service = new GLOFRiskCalculationService(weatherRepository, hazardEventRepository, riverBasinTownRepository,
                lakeSatelliteObservationRepository, glacierSatelliteObservationRepository, false, false);
    }

    // ---- Mass weight (earthquake/landslide hazard scaled by real area) ----

    @Test
    void massWeight_isFloorForUnknownOrZeroArea() {
        assertThat(service.calculateMassWeight(null)).isEqualTo(0.5);
        assertThat(service.calculateMassWeight(0.0)).isEqualTo(0.5);
    }

    @Test
    void massWeight_scalesUpWithRealArea() {
        assertThat(service.calculateMassWeight(5.0)).isCloseTo(0.75, offset(1e-9));
        assertThat(service.calculateMassWeight(10.0)).isEqualTo(1.0);
        // caps at 1.0 for very large glaciers/lakes rather than scaling further
        assertThat(service.calculateMassWeight(500.0)).isEqualTo(1.0);
    }

    // ---- Seasonal modifier (smooth monsoon curve, no new data) ----

    @Test
    void seasonalModifier_peaksAtMonsoonMidpoint() {
        // day-of-year 212 is the curve's defined peak (~Jul 31)
        LocalDateTime peak = LocalDateTime.of(2026, 1, 1, 0, 0).plusDays(211);
        assertThat(service.calculateSeasonalModifier(peak)).isCloseTo(1.0, offset(1e-9));
    }

    @Test
    void seasonalModifier_isZeroInDeepWinter() {
        LocalDateTime midWinter = LocalDateTime.of(2026, 1, 15, 0, 0);
        assertThat(service.calculateSeasonalModifier(midWinter)).isEqualTo(0.0);
    }

    @Test
    void seasonalModifier_isSmoothPartwayThroughTheWindow() {
        // 46 days from peak (half of the 92-day half-width) -> cos(pi/4)
        LocalDateTime partway = LocalDateTime.of(2026, 1, 1, 0, 0).plusDays(211 + 46);
        assertThat(service.calculateSeasonalModifier(partway)).isCloseTo(Math.cos(Math.PI / 4), offset(1e-9));
    }

    // ---- Landslide pre-condition (steep + sustained heavy rain) ----

    @Test
    void landslidePreCondition_trueOnlyWhenSteepAndWet() {
        assertThat(service.calculateLandslidePreCondition(45.0, "HEAVY")).isTrue();
        assertThat(service.calculateLandslidePreCondition(45.0, "ELEVATED")).isTrue();
        assertThat(service.calculateLandslidePreCondition(45.0, "NORMAL")).isFalse();
        assertThat(service.calculateLandslidePreCondition(30.0, "HEAVY")).isFalse();
    }

    @Test
    void landslidePreCondition_falseForLakesWithNoSlopeData() {
        assertThat(service.calculateLandslidePreCondition(null, "HEAVY")).isFalse();
    }

    // ---- Temperature-gated rainfall hazard, via the real assessLake() path ----

    @Test
    void rainfallHazard_isSuppressedBelowFreezingButFullAboveIt() {
        GlacialLake lake = GlacialLake.builder()
                .id(1L)
                .icimodId("TEST-LAKE")
                .lakeName("Test Lake")
                .latitude(28.0)
                .longitude(85.0)
                .riskLevel("Medium")
                .build();

        // No earthquakes/landslides in scope - isolates the rainfall component.
        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(Collections.emptyList());
        when(weatherRepository.findWeatherHistory(eq("TEST-LAKE"), any(), any())).thenReturn(Collections.emptyList());

        // Same 40mm rainfall (well above the 25mm base threshold) at two
        // different temperatures.
        when(weatherRepository.findLatestByLocation("TEST-LAKE"))
                .thenReturn(Optional.of(weatherReading("TEST-LAKE", -5.0, 40.0)));
        GlofRiskAssessment coldResult = service.assessLake(lake);

        when(weatherRepository.findLatestByLocation("TEST-LAKE"))
                .thenReturn(Optional.of(weatherReading("TEST-LAKE", 20.0, 40.0)));
        GlofRiskAssessment warmResult = service.assessLake(lake);

        assertThat(coldResult.getRainfallComponent()).isCloseTo(0.0, offset(0.01));
        assertThat(warmResult.getRainfallComponent()).isGreaterThan(0.1);
        assertThat(warmResult.getRainfallComponent()).isGreaterThan(coldResult.getRainfallComponent());
    }

    /**
     * 14 days of moderate rain (22mm/day, each below the 25mm/24h floor)
     * sums to 308mm, over the 300mm HEAVY threshold. Cumulative rainfall
     * must score independently of the single-day component.
     */
    @Test
    void sustainedCumulativeRain_scoresHazardEvenWhenTodayAloneIsBelowThreshold() {
        GlacialLake lake = GlacialLake.builder()
                .id(1L).icimodId("TEST-LAKE").lakeName("Test Lake")
                .latitude(28.0).longitude(85.0).riskLevel("Medium")
                .build();

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(Collections.emptyList());
        when(weatherRepository.findLatestByLocation("TEST-LAKE"))
                .thenReturn(Optional.of(weatherReading("TEST-LAKE", 20.0, 22.0)));

        List<Weather> fourteenDaysAt22mm = Collections.nCopies(14, weatherReading("TEST-LAKE", 20.0, 22.0));
        List<Weather> sevenDaysAt22mm = Collections.nCopies(7, weatherReading("TEST-LAKE", 20.0, 22.0));

        LocalDateTime elevenDaysAgo = LocalDateTime.now().minusDays(11);
        when(weatherRepository.findWeatherHistory(eq("TEST-LAKE"),
                org.mockito.ArgumentMatchers.argThat(start -> start != null && start.isBefore(elevenDaysAgo)), any()))
                .thenReturn(fourteenDaysAt22mm);
        when(weatherRepository.findWeatherHistory(eq("TEST-LAKE"),
                org.mockito.ArgumentMatchers.argThat(start -> start != null && !start.isBefore(elevenDaysAgo)), any()))
                .thenReturn(sevenDaysAt22mm);

        GlofRiskAssessment result = service.assessLake(lake);

        // 14-day sum = 308mm (>300 HEAVY threshold), but today's own 22mm
        // never clears the 25mm daily floor - this is exactly the real
        // Aug 26 shape (300.6mm/14-day, 19.28mm that specific day).
        assertThat(result.getRainfallCondition()).isEqualTo("HEAVY");
        assertThat(result.getRainfallComponent()).isGreaterThan(0.3);
    }

    /**
     * The temperature multiplier should only dampen the same-day rainfall
     * component, not the multi-week cumulative saturation signal.
     */
    @Test
    void sustainedCumulativeRain_notErasedByColdTemperatureToday() {
        GlacialLake lake = GlacialLake.builder()
                .id(1L).icimodId("TEST-LAKE").lakeName("Test Lake")
                .latitude(28.0).longitude(85.0).riskLevel("Medium")
                .build();

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(Collections.emptyList());
        // Today itself is cold (-5C) and its own rainfall is below the daily
        // floor, but the preceding two weeks were a genuine 308mm soaking.
        when(weatherRepository.findLatestByLocation("TEST-LAKE"))
                .thenReturn(Optional.of(weatherReading("TEST-LAKE", -5.0, 22.0)));

        List<Weather> fourteenDaysAt22mm = Collections.nCopies(14, weatherReading("TEST-LAKE", 20.0, 22.0));
        List<Weather> sevenDaysAt22mm = Collections.nCopies(7, weatherReading("TEST-LAKE", 20.0, 22.0));

        LocalDateTime elevenDaysAgo = LocalDateTime.now().minusDays(11);
        when(weatherRepository.findWeatherHistory(eq("TEST-LAKE"),
                org.mockito.ArgumentMatchers.argThat(start -> start != null && start.isBefore(elevenDaysAgo)), any()))
                .thenReturn(fourteenDaysAt22mm);
        when(weatherRepository.findWeatherHistory(eq("TEST-LAKE"),
                org.mockito.ArgumentMatchers.argThat(start -> start != null && !start.isBefore(elevenDaysAgo)), any()))
                .thenReturn(sevenDaysAt22mm);

        GlofRiskAssessment result = service.assessLake(lake);

        // Cumulative saturation hazard survives the cold day intact; only
        // the same-day melt-relevant component would have been suppressed.
        assertThat(result.getRainfallComponent()).isGreaterThan(0.3);
    }

    // ---- Mass weighting end-to-end, via the real assessGlacier() path ----

    @Test
    void earthquakeComponent_isHigherForALargerGlacierUnderTheSameQuake() {
        Glacier smallGlacier = Glacier.builder()
                .id(1L).rgiId("SMALL").glacierName("Small")
                .terminusLatitude(28.0).terminusLongitude(85.0)
                .slopeDeg(35.0).areaKm2(0.1)
                .build();
        Glacier largeGlacier = Glacier.builder()
                .id(2L).rgiId("LARGE").glacierName("Large")
                .terminusLatitude(28.0).terminusLongitude(85.0)
                .slopeDeg(35.0).areaKm2(10.0)
                .build();

        HazardEvent quake = new HazardEvent();
        quake.setId(99L);
        quake.setEventType("EARTHQUAKE");
        quake.setSourceType("earthquake");
        quake.setMagnitude(6.0);
        quake.setDepth(10.0);
        quake.setLatitude(28.01);
        quake.setLongitude(85.01);
        quake.setEventTime(LocalDateTime.now());

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(List.of(quake));
        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(Collections.emptyList());
        when(weatherRepository.findLatestByLocation(any())).thenReturn(Optional.empty());
        when(weatherRepository.findNearestByCoordinates(anyDouble(), anyDouble())).thenReturn(Optional.empty());

        GlofRiskAssessment smallResult = service.assessGlacier(smallGlacier);
        GlofRiskAssessment largeResult = service.assessGlacier(largeGlacier);

        assertThat(smallResult.getEarthquakeComponent()).isGreaterThan(0.0);
        assertThat(largeResult.getEarthquakeComponent()).isGreaterThan(smallResult.getEarthquakeComponent());
        // large glacier (areaKm2=10 -> mass weight 1.0) should be exactly
        // double the small one (areaKm2=0.1 -> mass weight ~0.505)
        assertThat(largeResult.getEarthquakeComponent() / smallResult.getEarthquakeComponent())
                .isCloseTo(1.0 / 0.505, offset(0.01));
    }

    // ---- Earthquake hazard decays with age, not just distance/depth ----

    @Test
    void earthquakeHazard_decaysExponentiallyWithAge() {
        Glacier glacier = Glacier.builder()
                .id(1L).rgiId("TEST").glacierName("Test")
                .terminusLatitude(28.0).terminusLongitude(85.0)
                .slopeDeg(35.0).areaKm2(10.0) // mass weight = 1.0, keeps the math clean
                .build();

        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(Collections.emptyList());
        when(weatherRepository.findLatestByLocation(any())).thenReturn(Optional.empty());
        when(weatherRepository.findNearestByCoordinates(anyDouble(), anyDouble())).thenReturn(Optional.empty());

        GlofRiskAssessment freshResult = assessWithQuakeAge(glacier, 0);
        GlofRiskAssessment twelveDayResult = assessWithQuakeAge(glacier, 12);
        GlofRiskAssessment thirtyDayResult = assessWithQuakeAge(glacier, 30);

        // Same quake, same distance/depth/magnitude - only age differs, so
        // any difference in the score is purely the new time-decay term.
        // 12-day decay constant: exp(-12/12) ~= 0.368, exp(-30/12) ~= 0.082.
        assertThat(twelveDayResult.getEarthquakeComponent())
                .isCloseTo(freshResult.getEarthquakeComponent() * 0.368, offset(0.01));
        assertThat(thirtyDayResult.getEarthquakeComponent())
                .isCloseTo(freshResult.getEarthquakeComponent() * 0.082, offset(0.01));
        assertThat(thirtyDayResult.getEarthquakeComponent()).isLessThan(twelveDayResult.getEarthquakeComponent());
    }

    private GlofRiskAssessment assessWithQuakeAge(Glacier glacier, long daysAgo) {
        HazardEvent quake = new HazardEvent();
        quake.setId(1L);
        quake.setEventType("EARTHQUAKE");
        quake.setSourceType("earthquake");
        quake.setMagnitude(6.0);
        quake.setDepth(10.0);
        quake.setLatitude(28.01);
        quake.setLongitude(85.01);
        quake.setEventTime(LocalDateTime.now(java.time.ZoneOffset.UTC).minusDays(daysAgo));

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(List.of(quake));
        return service.assessGlacier(glacier);
    }

    // ---- Landslide floor is basin-scoped, not just distance-scoped ----

    @Test
    void landslideFloor_onlyTriggersForAGlacierInTheSameBasinAsTheEvent() {
        HazardEvent landslide = new HazardEvent();
        landslide.setId(1L);
        landslide.setEventType("EARTHQUAKE");
        landslide.setSourceType("landslide");
        landslide.setMagnitude(5.0);
        landslide.setDepth(0.0);
        landslide.setLatitude(28.0);
        landslide.setLongitude(85.0);
        landslide.setEventTime(LocalDateTime.now().minusDays(1));

        // Only one curated town near the landslide, tagged "BasinA" - the
        // nearest-basin lookup should resolve the event to that basin.
        watch.nepalhazard.entity.RiverBasinTown townA = watch.nepalhazard.entity.RiverBasinTown.builder()
                .riverBasin("BasinA").townName("Town A").latitude(28.001).longitude(85.001).downstreamOrder(1)
                .build();

        Glacier sameBasinGlacier = Glacier.builder()
                .id(1L).rgiId("SAME-BASIN").glacierName("Same basin")
                .terminusLatitude(28.005).terminusLongitude(85.005)
                .slopeDeg(35.0).areaKm2(1.0).nearestRiverBasin("BasinA")
                .build();
        Glacier otherBasinGlacier = Glacier.builder()
                .id(2L).rgiId("OTHER-BASIN").glacierName("Other basin")
                .terminusLatitude(28.005).terminusLongitude(85.005)
                .slopeDeg(35.0).areaKm2(1.0).nearestRiverBasin("BasinB")
                .build();

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(List.of(landslide));
        when(riverBasinTownRepository.findAll()).thenReturn(List.of(townA));
        when(weatherRepository.findLatestByLocation(any())).thenReturn(Optional.empty());
        when(weatherRepository.findNearestByCoordinates(anyDouble(), anyDouble())).thenReturn(Optional.empty());

        GlofRiskAssessment sameBasinResult = service.assessGlacier(sameBasinGlacier);
        GlofRiskAssessment otherBasinResult = service.assessGlacier(otherBasinGlacier);

        // Same physical distance from the landslide (~0.7km) in both cases -
        // only the basin match should decide whether the floor fires.
        assertThat(sameBasinResult.getLandslideDetected()).isTrue();
        assertThat(otherBasinResult.getLandslideDetected()).isFalse();
    }

    // ---- Basin matching uses the river course, not just the nearest town ----

    @Test
    void basinMatch_findsTheCorrectBasinEvenWhenAStrayTownIsCloser() {
        HazardEvent landslide = new HazardEvent();
        landslide.setId(1L);
        landslide.setEventType("EARTHQUAKE");
        landslide.setSourceType("landslide");
        landslide.setMagnitude(5.0);
        landslide.setDepth(0.0);
        // Sits on the line between BasinA's two curated towns (~150km apart),
        // but far from either individual endpoint (~75km from each).
        landslide.setLatitude(28.0);
        landslide.setLongitude(85.763);
        landslide.setEventTime(LocalDateTime.now().minusDays(1));

        watch.nepalhazard.entity.RiverBasinTown basinAStart = watch.nepalhazard.entity.RiverBasinTown.builder()
                .riverBasin("BasinA").townName("A-start").latitude(28.0).longitude(85.0).downstreamOrder(1).build();
        watch.nepalhazard.entity.RiverBasinTown basinAEnd = watch.nepalhazard.entity.RiverBasinTown.builder()
                .riverBasin("BasinA").townName("A-end").latitude(28.0).longitude(86.53).downstreamOrder(2).build();
        // A single stray town from an unrelated basin, much closer in plain
        // point distance (~22km) than either of BasinA's own reference towns.
        watch.nepalhazard.entity.RiverBasinTown basinBTown = watch.nepalhazard.entity.RiverBasinTown.builder()
                .riverBasin("BasinB").townName("B-town").latitude(27.8).longitude(85.763).downstreamOrder(1).build();

        Glacier onBasinACourse = Glacier.builder()
                .id(1L).rgiId("ON-COURSE").glacierName("On course")
                .terminusLatitude(28.0).terminusLongitude(85.76)
                .slopeDeg(35.0).areaKm2(1.0).nearestRiverBasin("BasinA")
                .build();
        Glacier taggedOtherBasin = Glacier.builder()
                .id(2L).rgiId("OTHER-BASIN").glacierName("Tagged other basin")
                .terminusLatitude(28.0).terminusLongitude(85.76)
                .slopeDeg(35.0).areaKm2(1.0).nearestRiverBasin("BasinB")
                .build();

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(List.of(landslide));
        when(riverBasinTownRepository.findAll()).thenReturn(List.of(basinAStart, basinAEnd, basinBTown));
        when(weatherRepository.findLatestByLocation(any())).thenReturn(Optional.empty());
        when(weatherRepository.findNearestByCoordinates(anyDouble(), anyDouble())).thenReturn(Optional.empty());

        GlofRiskAssessment onCourseResult = service.assessGlacier(onBasinACourse);
        GlofRiskAssessment otherBasinResult = service.assessGlacier(taggedOtherBasin);

        // The event genuinely sits on BasinA's river course, so a glacier
        // tagged BasinA should get the floor even though neither of BasinA's
        // own towns is individually close - and a glacier tagged BasinB
        // should not, even though a single BasinB town happens to be nearer
        // in plain point-distance than BasinA's own endpoints are.
        assertThat(onCourseResult.getLandslideDetected()).isTrue();
        assertThat(otherBasinResult.getLandslideDetected()).isFalse();
    }

    // ---- Satellite lake growth: computed and stored, escalates only when enabled ----

    private GlacialLake testLake() {
        return GlacialLake.builder()
                .id(1L).icimodId("TEST-LAKE").lakeName("Test Lake")
                .latitude(28.0).longitude(85.0).riskLevel("Low")
                .build();
    }

    private void stubNoTriggers() {
        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(Collections.emptyList());
        when(weatherRepository.findLatestByLocation(any())).thenReturn(Optional.empty());
        when(weatherRepository.findNearestByCoordinates(anyDouble(), anyDouble())).thenReturn(Optional.empty());
    }

    private LakeSatelliteObservation reading(double waterFraction, double validPixelFraction, LocalDateTime observedAt) {
        LakeSatelliteObservation observation = new LakeSatelliteObservation();
        observation.setWaterFraction(waterFraction);
        observation.setValidPixelFraction(validPixelFraction);
        observation.setObservedAt(observedAt);
        return observation;
    }

    @Test
    void satelliteGrowth_notDetectedWithOnlyOneObservation() {
        GlacialLake lake = testLake();
        stubNoTriggers();
        when(lakeSatelliteObservationRepository.findTop2ByIcimodIdOrderByObservedAtDesc("TEST-LAKE"))
                .thenReturn(List.of(reading(0.25, 0.9, LocalDateTime.now())));

        GlofRiskAssessment result = service.assessLake(lake);

        // A first-ever reading has nothing to compare against yet.
        assertThat(result.getSatelliteWaterFraction()).isCloseTo(0.25, offset(1e-9));
        assertThat(result.getSatelliteLakeGrowthDetected()).isFalse();
    }

    @Test
    void satelliteGrowth_ignoresALowConfidenceJumpEvenIfLarge() {
        GlacialLake lake = testLake();
        stubNoTriggers();
        // Water fraction jumped from 0.10 to 0.60 over a real 60-day gap,
        // but neither reading has enough valid (non-cloud) coverage to
        // trust that jump.
        when(lakeSatelliteObservationRepository.findTop2ByIcimodIdOrderByObservedAtDesc("TEST-LAKE"))
                .thenReturn(List.of(
                        reading(0.60, 0.15, LocalDateTime.now()),
                        reading(0.10, 0.20, LocalDateTime.now().minusDays(60))));

        GlofRiskAssessment result = service.assessLake(lake);

        assertThat(result.getSatelliteLakeGrowthDetected()).isFalse();
    }

    @Test
    void satelliteGrowth_ignoresAComparisonWindowThatIsTooShort() {
        GlacialLake lake = testLake();
        stubNoTriggers();
        // Same real jump as the "escalates" test below, both readings
        // trustworthy - but only 5 days apart, too short to annualize
        // sensibly (SATELLITE_MIN_COMPARISON_DAYS is 20).
        when(lakeSatelliteObservationRepository.findTop2ByIcimodIdOrderByObservedAtDesc("TEST-LAKE"))
                .thenReturn(List.of(
                        reading(0.25, 0.9, LocalDateTime.now()),
                        reading(0.20, 0.9, LocalDateTime.now().minusDays(5))));

        GlofRiskAssessment result = service.assessLake(lake);

        assertThat(result.getSatelliteLakeGrowthDetected()).isFalse();
    }

    @Test
    void satelliteGrowth_detectedButDoesNotEscalateWhenDisabled() {
        GlacialLake lake = testLake();
        stubNoTriggers();
        // 25% relative growth (0.20 -> 0.25) over a real 60-day gap,
        // annualizes to a rate real HKH-wide data says is a clear outlier,
        // not noise - but the shared `service` instance has the satellite
        // factor disabled (the default), so it can't affect the alert.
        when(lakeSatelliteObservationRepository.findTop2ByIcimodIdOrderByObservedAtDesc("TEST-LAKE"))
                .thenReturn(List.of(
                        reading(0.25, 0.9, LocalDateTime.now()),
                        reading(0.20, 0.9, LocalDateTime.now().minusDays(60))));

        GlofRiskAssessment result = service.assessLake(lake);

        assertThat(result.getSatelliteLakeGrowthDetected()).isTrue();
        assertThat(result.getAlertLevel()).isEqualTo("NORMAL");

        // Disabled means the satellite term contributes nothing and the
        // other five weights are exactly today's 20/25/25/20/10 - real
        // growth being computed in the background must not shift the score
        // by even a fraction of a point while the switch is off.
        double seasonalModifier = service.calculateSeasonalModifier(LocalDateTime.now());
        double expectedScore = 20.0 * 0.15 + 10.0 * seasonalModifier;
        assertThat(result.getRiskScore()).isCloseTo(expectedScore, offset(1e-9));
    }

    @Test
    void satelliteGrowth_escalatesToWatchWhenEnabled() {
        GLOFRiskCalculationService escalatingService = new GLOFRiskCalculationService(
                weatherRepository, hazardEventRepository, riverBasinTownRepository,
                lakeSatelliteObservationRepository, glacierSatelliteObservationRepository, true, false);

        GlacialLake lake = testLake();
        stubNoTriggers();
        when(lakeSatelliteObservationRepository.findTop2ByIcimodIdOrderByObservedAtDesc("TEST-LAKE"))
                .thenReturn(List.of(
                        reading(0.25, 0.9, LocalDateTime.now()),
                        reading(0.20, 0.9, LocalDateTime.now().minusDays(60))));

        GlofRiskAssessment result = escalatingService.assessLake(lake);

        assertThat(result.getSatelliteLakeGrowthDetected()).isTrue();
        assertThat(result.getAlertLevel()).isEqualTo("WATCH");

        // 152%/year annualized is far past the real 18%/year saturation
        // point, so satellite hazard should be fully saturated at 1.0,
        // contributing exactly its 15-point weight - and the other four
        // weights should be the redistributed (x0.85) values, not the
        // original 20/25/25/20/10, given the factor is enabled here.
        double seasonalModifier = escalatingService.calculateSeasonalModifier(LocalDateTime.now());
        double expectedScore = 17.0 * 0 + 21.25 * 0 + 21.25 * 0 + 17.0 * 0.15 + 8.5 * seasonalModifier + 15.0 * 1.0;
        assertThat(result.getRiskScore()).isCloseTo(expectedScore, offset(0.05));
    }

    // ---- Satellite glacier ice-cover sudden drop: computed and stored, escalates only when enabled ----

    private Glacier testGlacier() {
        return Glacier.builder()
                .id(1L).rgiId("TEST-GLACIER").glacierName("Test Glacier")
                .terminusLatitude(28.0).terminusLongitude(85.0)
                .slopeDeg(35.0).areaKm2(1.0)
                .build();
    }

    private GlacierSatelliteObservation iceReading(double iceFraction, double validPixelFraction,
            LocalDateTime observedAt) {
        GlacierSatelliteObservation observation = new GlacierSatelliteObservation();
        observation.setIceFraction(iceFraction);
        observation.setValidPixelFraction(validPixelFraction);
        observation.setObservedAt(observedAt);
        return observation;
    }

    @Test
    void iceDrop_notDetectedWithOnlyOneObservation() {
        Glacier glacier = testGlacier();
        stubNoTriggers();
        when(glacierSatelliteObservationRepository.findTop2ByRgiIdOrderByObservedAtDesc("TEST-GLACIER"))
                .thenReturn(List.of(iceReading(0.40, 0.9, LocalDateTime.now())));

        GlofRiskAssessment result = service.assessGlacier(glacier);

        // A first-ever reading has nothing to compare against yet - this is
        // exactly the "today is the baseline" cold start.
        assertThat(result.getSatelliteIceFraction()).isCloseTo(0.40, offset(1e-9));
        assertThat(result.getSatelliteIceSuddenDropDetected()).isFalse();
    }

    @Test
    void iceDrop_ignoresALowConfidenceDropEvenIfLarge() {
        Glacier glacier = testGlacier();
        stubNoTriggers();
        // Ice fraction fell from 0.70 to 0.10 (a 60-point drop) over 5 days,
        // but neither reading has enough valid (non-cloud) coverage to
        // trust that - a cloud passing over looks exactly like ice vanishing.
        when(glacierSatelliteObservationRepository.findTop2ByRgiIdOrderByObservedAtDesc("TEST-GLACIER"))
                .thenReturn(List.of(
                        iceReading(0.10, 0.15, LocalDateTime.now()),
                        iceReading(0.70, 0.20, LocalDateTime.now().minusDays(5))));

        GlofRiskAssessment result = service.assessGlacier(glacier);

        assertThat(result.getSatelliteIceSuddenDropDetected()).isFalse();
    }

    @Test
    void iceDrop_ignoresAComparisonWindowThatIsTooLongToBeSudden() {
        Glacier glacier = testGlacier();
        stubNoTriggers();
        // Same real drop as the "escalates" test below, both readings
        // trustworthy - but 60 days apart, too long to call "sudden"
        // (SATELLITE_ICE_MAX_COMPARISON_DAYS is 15) - more likely ordinary
        // seasonal melt than a collapse.
        when(glacierSatelliteObservationRepository.findTop2ByRgiIdOrderByObservedAtDesc("TEST-GLACIER"))
                .thenReturn(List.of(
                        iceReading(0.10, 0.9, LocalDateTime.now()),
                        iceReading(0.70, 0.9, LocalDateTime.now().minusDays(60))));

        GlofRiskAssessment result = service.assessGlacier(glacier);

        assertThat(result.getSatelliteIceSuddenDropDetected()).isFalse();
    }

    @Test
    void iceDrop_detectedButDoesNotEscalateWhenDisabled() {
        Glacier glacier = testGlacier();
        stubNoTriggers();
        // 60-point drop (0.70 -> 0.10) over a real 5-day gap, well past the
        // 30-point sudden-drop floor - but the shared `service` instance
        // has the glacier satellite factor disabled (the default), so it
        // can't affect the alert or score.
        when(glacierSatelliteObservationRepository.findTop2ByRgiIdOrderByObservedAtDesc("TEST-GLACIER"))
                .thenReturn(List.of(
                        iceReading(0.10, 0.9, LocalDateTime.now()),
                        iceReading(0.70, 0.9, LocalDateTime.now().minusDays(5))));

        GlofRiskAssessment result = service.assessGlacier(glacier);

        assertThat(result.getSatelliteIceSuddenDropDetected()).isTrue();
        assertThat(result.getAlertLevel()).isEqualTo("NORMAL");

        // Disabled means the satellite term contributes nothing and the
        // other five weights are exactly today's 20/25/25/20/10 - a real
        // drop being computed in the background must not shift the score
        // by even a fraction of a point while the switch is off.
        double seasonalModifier = service.calculateSeasonalModifier(LocalDateTime.now());
        double steepnessFactor = service.calculateSteepnessFactor(35.0);
        double expectedScore = 20.0 * steepnessFactor + 10.0 * seasonalModifier;
        assertThat(result.getRiskScore()).isCloseTo(expectedScore, offset(1e-9));
    }

    @Test
    void iceDrop_escalatesToWatchWhenEnabled() {
        GLOFRiskCalculationService escalatingService = new GLOFRiskCalculationService(
                weatherRepository, hazardEventRepository, riverBasinTownRepository,
                lakeSatelliteObservationRepository, glacierSatelliteObservationRepository, false, true);

        Glacier glacier = testGlacier();
        stubNoTriggers();
        when(glacierSatelliteObservationRepository.findTop2ByRgiIdOrderByObservedAtDesc("TEST-GLACIER"))
                .thenReturn(List.of(
                        iceReading(0.10, 0.9, LocalDateTime.now()),
                        iceReading(0.70, 0.9, LocalDateTime.now().minusDays(5))));

        GlofRiskAssessment result = escalatingService.assessGlacier(glacier);

        assertThat(result.getSatelliteIceSuddenDropDetected()).isTrue();
        assertThat(result.getAlertLevel()).isEqualTo("WATCH");

        // 60-point drop is past the 50-point saturation ceiling, so ice-drop
        // hazard should be fully saturated at 1.0, contributing exactly its
        // 15-point weight - and the other four weights should be the
        // redistributed (x0.85) values, not the original 20/25/25/20/10.
        double seasonalModifier = escalatingService.calculateSeasonalModifier(LocalDateTime.now());
        double steepnessFactor = escalatingService.calculateSteepnessFactor(35.0);
        double expectedScore = 17.0 * 0 + 21.25 * 0 + 21.25 * 0
                + 17.0 * steepnessFactor + 8.5 * seasonalModifier + 15.0 * 1.0;
        assertThat(result.getRiskScore()).isCloseTo(expectedScore, offset(0.05));
    }

    private Weather weatherReading(String location, double temperature, double rainfall) {
        Weather weather = new Weather();
        weather.setLocation(location);
        weather.setLatitude(28.0);
        weather.setLongitude(85.0);
        weather.setTemperature(temperature);
        weather.setRainfall(rainfall);
        weather.setHumidity(80.0);
        weather.setWindSpeed(1.0);
        weather.setWeatherCondition("Rain");
        weather.setDescription("test");
        weather.setRecordedAt(LocalDateTime.now());
        weather.setFetchedAt(LocalDateTime.now());
        weather.setDataSource("TEST");
        return weather;
    }
}
