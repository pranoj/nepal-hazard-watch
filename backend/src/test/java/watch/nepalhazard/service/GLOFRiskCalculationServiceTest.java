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
import watch.nepalhazard.entity.GlofRiskAssessment;
import watch.nepalhazard.entity.HazardEvent;
import watch.nepalhazard.entity.Weather;
import watch.nepalhazard.repository.HazardEventRepository;
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

    private GLOFRiskCalculationService service;

    @BeforeEach
    void setUp() {
        service = new GLOFRiskCalculationService(weatherRepository, hazardEventRepository, riverBasinTownRepository);
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
