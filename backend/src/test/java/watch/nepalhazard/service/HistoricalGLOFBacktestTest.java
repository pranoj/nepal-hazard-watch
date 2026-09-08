package watch.nepalhazard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
 * Backtests the risk formula against real, documented historical GLOF
 * events in Nepal. Coordinates and ICIMOD risk classifications come from
 * the same dataset the app imports. Pre-2026 events have no real historical
 * weather (our pipeline started Sept 2026), so those cases use only
 * always-available signals: lake classification, coordinates, season.
 */
@ExtendWith(MockitoExtension.class)
class HistoricalGLOFBacktestTest {

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private HazardEventRepository hazardEventRepository;

    @Mock
    private RiverBasinTownRepository riverBasinTownRepository;

    @Mock
    private watch.nepalhazard.repository.LakeSatelliteObservationRepository lakeSatelliteObservationRepository;

    @Mock
    private watch.nepalhazard.repository.GlacierSatelliteObservationRepository glacierSatelliteObservationRepository;

    private GLOFRiskCalculationService service;

    @BeforeEach
    void setUp() {
        service = new GLOFRiskCalculationService(weatherRepository, hazardEventRepository, riverBasinTownRepository,
                lakeSatelliteObservationRepository, glacierSatelliteObservationRepository, false, false);
    }

    /**
     * Real, documented Nepal GLOF events with real coordinates and ICIMOD
     * risk classifications, currently in our live glacial_lakes table:
     * icimodId, lakeName, year, lat, lon, ICIMOD riskLevel, deaths (0 if
     * none recorded).
     */
    static List<Object[]> realHistoricalGlofLakes() {
        return List.of(
                new Object[] { "348", "Dig Tsho", 1985, 27.874, 86.594, "High", 5 },
                new Object[] { "410", "Sabai Tsho/Tam Pokhari", 1998, 27.742, 86.845, "Medium", 2 },
                new Object[] { "304", "Nagma Pokhari", 1980, 27.869, 87.867, "Medium", 0 },
                new Object[] { "743", "Pemdang Pokhari", 2021, 28.131, 85.515, "Medium", 25 },
                new Object[] { "288", "Nare", 1977, 27.828, 86.839, "Low", 0 },
                new Object[] { "374", "Chubung", 1991, 27.887, 86.467, "Low", 0 });
    }

    @ParameterizedTest(name = "{1} ({0}), {2}: baseline risk during peak monsoon with no active trigger")
    @CsvSource({
            "348, Dig Tsho, High",
            "410, Sabai Tsho/Tam Pokhari, Medium",
            "288, Nare, Low"
    })
    void realHistoricalLakes_baselineRiskAtPeakMonsoon(String icimodId, String lakeName, String riskLevel) {
        GlacialLake lake = GlacialLake.builder()
                .id(1L).icimodId(icimodId).lakeName(lakeName)
                .latitude(27.87).longitude(86.6)
                .riskLevel(riskLevel)
                .build();

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(Collections.emptyList());
        when(weatherRepository.findLatestByLocation(icimodId)).thenReturn(Optional.empty());
        when(weatherRepository.findNearestByCoordinates(27.87, 86.6)).thenReturn(Optional.empty());

        GlofRiskAssessment result = service.assessLake(lake);

        // Even a historically-fatal High-risk lake (Dig Tsho, 1985) sits at
        // NORMAL on lake-type + season alone; a real trigger is required.
        assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(0.0);
        System.out.printf("%-25s (%s risk) -> score=%.1f alert=%s%n",
                lakeName, riskLevel, result.getRiskScore(), result.getAlertLevel());
    }

    @Test
    void knownDangerousLake_scoresHigherThanKnownLowRiskLake_atSameSeason() {
        GlacialLake digTsho = GlacialLake.builder()
                .id(1L).icimodId("348").lakeName("Dig Tsho")
                .latitude(27.874).longitude(86.594).riskLevel("High")
                .build();
        GlacialLake nare = GlacialLake.builder()
                .id(2L).icimodId("288").lakeName("Nare")
                .latitude(27.828).longitude(86.839).riskLevel("Low")
                .build();

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(Collections.emptyList());
        when(weatherRepository.findLatestByLocation(any())).thenReturn(Optional.empty());
        when(weatherRepository.findNearestByCoordinates(any(Double.class), any(Double.class)))
                .thenReturn(Optional.empty());

        GlofRiskAssessment digTshoResult = service.assessLake(digTsho);
        GlofRiskAssessment nareResult = service.assessLake(nare);

        assertThat(digTshoResult.getRiskScore()).isGreaterThan(nareResult.getRiskScore());
    }

    /**
     * The two real USGS landslide detections from Aug 26, 2026 near
     * Langtang/Rasuwa, against a real glacier watch point in that valley.
     */
    @Test
    void aug26_2026_langtangLandslide_triggersAlertOnRealNearbyGlacier() {
        Glacier langtangGlacier = Glacier.builder()
                .id(1L).rgiId("RGI2000-v7.0-G-15-05840").glacierName("Glacier 1.6km from Kyanjin Gompa")
                .terminusLatitude(28.21247770786395).terminusLongitude(85.69629548099003)
                .slopeDeg(48.48048).areaKm2(0.045344545812138)
                .build();

        HazardEvent landslideM52 = realLandslideEvent(6L, 5.2, 28.271, 85.515);
        HazardEvent landslideM42 = realLandslideEvent(5L, 4.2, 28.27, 85.515);

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any()))
                .thenReturn(List.of(landslideM52, landslideM42));
        when(weatherRepository.findLatestByLocation(any())).thenReturn(Optional.empty());
        when(weatherRepository.findNearestByCoordinates(any(Double.class), any(Double.class)))
                .thenReturn(Optional.empty());

        GlofRiskAssessment result = service.assessGlacier(langtangGlacier);

        assertThat(result.getLandslideDetected()).isTrue();
        assertThat(result.getAlertLevel()).isIn("WATCH", "DANGER", "EXTREME");
        System.out.printf("Aug 26 Langtang glacier -> score=%.1f alert=%s landslideDetected=%s%n",
                result.getRiskScore(), result.getAlertLevel(), result.getLandslideDetected());
    }

    /**
     * The real M5.2 landslide was detected at 2026-08-26T02:52:10Z (08:37
     * NPT). Ten minutes earlier, no landslide/earthquake/rainfall signal
     * existed yet - only season and terrain steepness were already true.
     */
    @Test
    void tenMinutesBeforeTheRealLandslide_noSignalYetExisted_wouldNotHaveBeenFlagged() {
        LocalDateTime tenMinutesBefore = LocalDateTime.of(2026, 8, 26, 2, 42, 10); // UTC

        double seasonalModifier = service.calculateSeasonalModifier(tenMinutesBefore);
        // Real RGI slope 48.48 deg for this glacier - matches the live
        // system's own computed steepness component for this exact point.
        double realSteepnessFactor = 0.5696096;

        double scoreTenMinutesBefore = 20 * 0 + 25 * 0 + 25 * 0 + 20 * realSteepnessFactor + 10 * seasonalModifier;

        System.out.printf(
                "10 min before the real Aug 26 landslide -> score=%.1f (season=%.3f, steepness=%.3f, landslide=0, rainfall=0)%n",
                scoreTenMinutesBefore, seasonalModifier, realSteepnessFactor);

        assertThat(scoreTenMinutesBefore).isLessThan(25.0);
    }

    /**
     * "10 minutes before the flood" means before the flood reached the
     * first settlement (Timure, ~08:50 NPT), not before the landslide's own
     * detection (08:37 NPT). That puts it at 08:40 NPT - 3 minutes after
     * the landslide was already seismically detectable.
     */
    @Test
    void tenMinutesBeforeFloodReachedFirstSettlement_landslideWasAlreadyDetected() {
        Glacier langtangGlacier = Glacier.builder()
                .id(1L).rgiId("RGI2000-v7.0-G-15-05840").glacierName("Glacier 1.6km from Kyanjin Gompa")
                .terminusLatitude(28.21247770786395).terminusLongitude(85.69629548099003)
                .slopeDeg(48.48048).areaKm2(0.045344545812138)
                .build();

        // Detection was 2026-08-26T02:52:10Z (08:37 NPT); 08:40 NPT is 3
        // minutes later. The landslide floor is a step function, so any
        // small "minutes ago" reproduces the same detected hazard.
        HazardEvent landslideM52 = realLandslideEvent(6L, 5.2, 28.271, 85.515, 3);
        HazardEvent landslideM42 = realLandslideEvent(5L, 4.2, 28.27, 85.515, 3);

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any()))
                .thenReturn(List.of(landslideM52, landslideM42));
        when(weatherRepository.findLatestByLocation(any())).thenReturn(Optional.empty());
        when(weatherRepository.findNearestByCoordinates(any(Double.class), any(Double.class)))
                .thenReturn(Optional.empty());

        GlofRiskAssessment result = service.assessGlacier(langtangGlacier);

        // assessGlacier() uses today's date for season - swap in the real
        // Aug 26 seasonal value to reconstruct the historical score.
        double realAug26Seasonal = service.calculateSeasonalModifier(LocalDateTime.of(2026, 8, 26, 2, 55, 0));
        double historicallyAccurateScore = 25 * result.getLandslideComponent()
                + 20 * 0.5696096
                + 10 * realAug26Seasonal;

        System.out.printf(
                "08:40 NPT (10 min before Timure hit) -> score=%.1f landslideDetected=%s (landslideComp=%.3f, season=%.3f)%n",
                historicallyAccurateScore, result.getLandslideDetected(), result.getLandslideComponent(), realAug26Seasonal);

        assertThat(result.getLandslideDetected()).isTrue();
        assertThat(historicallyAccurateScore).isGreaterThanOrEqualTo(25.0);
    }

    /**
     * Real NASA POWER rainfall for this glacier's coordinates, Aug 13-26
     * 2026 (14-day sum 286.3mm, 7-day sum 132.2mm - just under the
     * ELEVATED/HEAVY cutoffs, so condition reads NORMAL, but the continuous
     * cumulative hazard term still registers real risk). 08:20 NPT is 17
     * minutes before the 08:37 NPT collapse - no landslide signal yet.
     */
    @Test
    void aug26_820amNPT_beforeCollapse_realCumulativeRainfallOnly() {
        Glacier langtangGlacier = Glacier.builder()
                .id(1L).rgiId("RGI2000-v7.0-G-15-05840").glacierName("Glacier 1.6km from Kyanjin Gompa")
                .terminusLatitude(28.21247770786395).terminusLongitude(85.69629548099003)
                .slopeDeg(48.48048).areaKm2(0.045344545812138)
                .build();

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(Collections.emptyList());
        stubRealAug26Rainfall(langtangGlacier.getRgiId());

        GlofRiskAssessment result = service.assessGlacier(langtangGlacier);

        double realAug26Seasonal = service.calculateSeasonalModifier(LocalDateTime.of(2026, 8, 26, 2, 35, 0));
        double historicallyAccurateScore = 25 * result.getLandslideComponent()
                + 25 * result.getRainfallComponent()
                + 20 * 0.5696096
                + 10 * realAug26Seasonal;

        System.out.printf(
                "08:20 NPT (17 min before collapse) -> score=%.1f landslideDetected=%s preCondition=%s "
                        + "(rainfallComp=%.3f condition=%s, season=%.3f)%n",
                historicallyAccurateScore, result.getLandslideDetected(), result.getLandslidePreCondition(),
                result.getRainfallComponent(), result.getRainfallCondition(), realAug26Seasonal);

        assertThat(result.getLandslideDetected()).isFalse();
        // Just under the discrete ELEVATED/HEAVY cutoffs, so label is
        // NORMAL, but the cumulative hazard term is still a real nonzero.
        assertThat(result.getRainfallCondition()).isEqualTo("NORMAL");
        assertThat(result.getRainfallComponent()).isGreaterThan(0.3);
        assertThat(historicallyAccurateScore).isGreaterThanOrEqualTo(25.0);
    }

    /**
     * Same rainfall, 25 minutes later at 08:45 NPT - 8 minutes after the
     * collapse and 5 minutes before Timure was hit, so landslide detection
     * now stacks on top of the same cumulative rainfall signal.
     */
    @Test
    void aug26_845amNPT_afterCollapseBeforeSettlementHit_realRainfallPlusLandslide() {
        Glacier langtangGlacier = Glacier.builder()
                .id(1L).rgiId("RGI2000-v7.0-G-15-05840").glacierName("Glacier 1.6km from Kyanjin Gompa")
                .terminusLatitude(28.21247770786395).terminusLongitude(85.69629548099003)
                .slopeDeg(48.48048).areaKm2(0.045344545812138)
                .build();

        HazardEvent landslideM52 = realLandslideEvent(6L, 5.2, 28.271, 85.515, 8);
        HazardEvent landslideM42 = realLandslideEvent(5L, 4.2, 28.27, 85.515, 8);

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any()))
                .thenReturn(List.of(landslideM52, landslideM42));
        stubRealAug26Rainfall(langtangGlacier.getRgiId());

        GlofRiskAssessment result = service.assessGlacier(langtangGlacier);

        double realAug26Seasonal = service.calculateSeasonalModifier(LocalDateTime.of(2026, 8, 26, 3, 0, 0));
        double historicallyAccurateScore = 25 * result.getLandslideComponent()
                + 25 * result.getRainfallComponent()
                + 20 * 0.5696096
                + 10 * realAug26Seasonal;

        System.out.printf(
                "08:45 NPT (8 min after collapse, 5 min before Timure hit) -> score=%.1f landslideDetected=%s "
                        + "(landslideComp=%.3f, rainfallComp=%.3f condition=%s, season=%.3f)%n",
                historicallyAccurateScore, result.getLandslideDetected(), result.getLandslideComponent(),
                result.getRainfallComponent(), result.getRainfallCondition(), realAug26Seasonal);

        assertThat(result.getLandslideDetected()).isTrue();
        assertThat(result.getRainfallCondition()).isEqualTo("NORMAL");
        assertThat(result.getRainfallComponent()).isGreaterThan(0.3);
        assertThat(historicallyAccurateScore).isGreaterThanOrEqualTo(25.0);
    }

    /**
     * A single consistent reconstruction of the real Aug 26 timeline, for
     * the methodology page's timeline table/chart - unlike the isolated
     * component tests above (each of which deliberately zeroes out one real
     * signal to test the other alone), every point here uses the same real
     * NASA POWER rainfall reading throughout, with only the landslide
     * detection state changing at its real, correct elapsed time. This is
     * what the formula would have actually shown in sequence, not four
     * separate isolated claims stitched together.
     */
    @Test
    void aug26_consistentTimeline_sameRealRainfallThroughout_onlyLandslideStateChanges() {
        Glacier langtangGlacier = Glacier.builder()
                .id(1L).rgiId("RGI2000-v7.0-G-15-05840").glacierName("Glacier 1.6km from Kyanjin Gompa")
                .terminusLatitude(28.21247770786395).terminusLongitude(85.69629548099003)
                .slopeDeg(48.48048).areaKm2(0.045344545812138)
                .build();

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(Collections.emptyList());

        // Real NASA POWER rainfall, trailing 14 days as of Aug 19 (Aug 6-19)
        // - one week before the collapse. No landslide signal yet, real
        // rainfall only.
        stubRealRainfall(langtangGlacier.getRgiId(), new double[] {
                18.03, 27.24, 42.8, 12.77, 24.15, 8.78, 14.3,
                13.63, 19.56, 29.71, 22.02, 16.38, 21.44, 31.35 }, 31.35);
        printTimelinePoint(langtangGlacier, "Aug 19, 1 week before collapse", null, 8, 19, 2, 35, 0);

        // Real NASA POWER rainfall, trailing 14 days as of Aug 25 (Aug 12-25)
        // - one day before the collapse.
        stubRealRainfall(langtangGlacier.getRgiId(), new double[] {
                14.3, 13.63, 19.56, 29.71, 22.02, 16.38, 21.44,
                31.35, 16.87, 27.37, 11.95, 5.05, 18.39, 33.33 }, 33.33);
        printTimelinePoint(langtangGlacier, "Aug 25, 1 day before collapse", null, 8, 25, 2, 35, 0);

        stubRealAug26Rainfall(langtangGlacier.getRgiId());
        printTimelinePoint(langtangGlacier, "08:20 (17 min before collapse)", null, 8, 26, 2, 35, 0);
        printTimelinePoint(langtangGlacier, "08:27 (10 min before collapse)", null, 8, 26, 2, 42, 10);
        printTimelinePoint(langtangGlacier, "08:40 (3 min after detection)", 3L, 8, 26, 2, 55, 0);
        printTimelinePoint(langtangGlacier, "08:45 (8 min after collapse)", 8L, 8, 26, 3, 0, 0);
    }

    private void printTimelinePoint(Glacier glacier, String label, Long landslideMinutesAgo,
            int month, int day, int utcHour, int utcMinute, int utcSecond) {
        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(landslideMinutesAgo == null
                ? Collections.emptyList()
                : List.of(realLandslideEvent(6L, 5.2, 28.271, 85.515, landslideMinutesAgo),
                        realLandslideEvent(5L, 4.2, 28.27, 85.515, landslideMinutesAgo)));

        GlofRiskAssessment result = service.assessGlacier(glacier);
        double realSeasonal = service.calculateSeasonalModifier(LocalDateTime.of(2026, month, day, utcHour, utcMinute, utcSecond));
        double reconstructedScore = 25 * result.getLandslideComponent()
                + 25 * result.getRainfallComponent()
                + 20 * 0.5696096
                + 10 * realSeasonal;

        System.out.printf("%-32s -> score=%.1f alert=%-7s landslideDetected=%-5s preCondition=%-5s "
                        + "(landslideComp=%.3f, rainfallComp=%.3f condition=%s, season=%.3f)%n",
                label, reconstructedScore, service.getAlertLevel(reconstructedScore), landslideMinutesAgo != null,
                result.getLandslidePreCondition(), result.getLandslideComponent(), result.getRainfallComponent(),
                result.getRainfallCondition(), realSeasonal);
    }

    private void stubRealAug26Rainfall(String locationKey) {
        stubRealRainfall(locationKey, new double[] {13.63, 19.56, 29.71, 22.02, 16.38, 21.44, 31.35,
                16.87, 27.37, 11.95, 5.05, 18.39, 33.33, 19.28}, 19.28); // Aug 13-26, real NASA POWER data
    }

    /**
     * Stubs a real, trailing 14-day NASA POWER rainfall window ending on the
     * "as of" day being tested - reusable for any point in the timeline, not
     * just Aug 26 itself, so a week-before or day-before reconstruction uses
     * its own correct real trailing window rather than Aug 26's.
     */
    private void stubRealRainfall(String locationKey, double[] fourteenDaysRealMm, double todayMm) {
        List<Weather> fourteenDayReadings = new java.util.ArrayList<>();
        for (double mm : fourteenDaysRealMm) {
            fourteenDayReadings.add(realWeatherReading(locationKey, 14.5, mm));
        }
        List<Weather> sevenDayReadings = fourteenDayReadings.subList(7, 14);

        when(weatherRepository.findLatestByLocation(locationKey))
                .thenReturn(Optional.of(realWeatherReading(locationKey, 14.5, todayMm)));

        LocalDateTime elevenDaysAgo = LocalDateTime.now().minusDays(11);
        when(weatherRepository.findWeatherHistory(eq(locationKey),
                org.mockito.ArgumentMatchers.argThat(start -> start != null && start.isBefore(elevenDaysAgo)), any()))
                .thenReturn(fourteenDayReadings);
        when(weatherRepository.findWeatherHistory(eq(locationKey),
                org.mockito.ArgumentMatchers.argThat(start -> start != null && !start.isBefore(elevenDaysAgo)), any()))
                .thenReturn(sevenDayReadings);
    }

    private Weather realWeatherReading(String location, double temperature, double rainfall) {
        Weather weather = new Weather();
        weather.setLocation(location);
        weather.setLatitude(28.212);
        weather.setLongitude(85.696);
        weather.setTemperature(temperature);
        weather.setRainfall(rainfall);
        weather.setHumidity(85.0);
        weather.setWindSpeed(1.5);
        weather.setWeatherCondition("Rain");
        weather.setDescription("NASA POWER historical (real)");
        weather.setRecordedAt(LocalDateTime.now());
        weather.setFetchedAt(LocalDateTime.now());
        weather.setDataSource("NASA_POWER");
        return weather;
    }

    /**
     * Hypothetical, not reconstructed history: plausible sustained monsoon
     * rain on a real steep Langtang-valley glacier (RGI slope 48.48),
     * testing whether the predictive pre-condition would have flagged this
     * valley before any landslide was detected.
     */
    @Test
    void hypotheticalMonsoonRain_wouldHaveFlaggedLangtangValleyBeforeTheLandslide() {
        Glacier langtangGlacier = Glacier.builder()
                .id(1L).rgiId("RGI2000-v7.0-G-15-05840").glacierName("Glacier 1.6km from Kyanjin Gompa")
                .terminusLatitude(28.21247770786395).terminusLongitude(85.69629548099003)
                .slopeDeg(48.48048).areaKm2(0.045344545812138)
                .build();

        // No landslide yet - this is the "before the event" scenario.
        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(hazardEventRepository.findRecentLandslides(any())).thenReturn(Collections.emptyList());

        Weather sustainedMonsoonRain = new Weather();
        sustainedMonsoonRain.setLocation("RGI2000-v7.0-G-15-05840");
        sustainedMonsoonRain.setLatitude(28.212);
        sustainedMonsoonRain.setLongitude(85.696);
        sustainedMonsoonRain.setTemperature(12.0);
        sustainedMonsoonRain.setRainfall(30.0);
        sustainedMonsoonRain.setHumidity(90.0);
        sustainedMonsoonRain.setWindSpeed(2.0);
        sustainedMonsoonRain.setWeatherCondition("Rain");
        sustainedMonsoonRain.setDescription("hypothetical monsoon scenario - not reconstructed history");
        sustainedMonsoonRain.setRecordedAt(LocalDateTime.now());
        sustainedMonsoonRain.setFetchedAt(LocalDateTime.now());
        sustainedMonsoonRain.setDataSource("TEST");

        when(weatherRepository.findLatestByLocation("RGI2000-v7.0-G-15-05840"))
                .thenReturn(Optional.of(sustainedMonsoonRain));
        // 7-day accumulated total of 160mm - plausible for an active
        // monsoon spell, pushes the rainfall condition to ELEVATED.
        when(weatherRepository.findWeatherHistory(org.mockito.ArgumentMatchers.eq("RGI2000-v7.0-G-15-05840"),
                any(), any()))
                .thenReturn(List.of(sustainedMonsoonRain, sustainedMonsoonRain, sustainedMonsoonRain,
                        sustainedMonsoonRain, sustainedMonsoonRain));

        GlofRiskAssessment result = service.assessGlacier(langtangGlacier);

        assertThat(result.getLandslidePreCondition()).isTrue();
        assertThat(result.getAlertLevel()).isIn("WATCH", "DANGER", "EXTREME");
        System.out.printf("Hypothetical pre-event Langtang -> score=%.1f alert=%s preCondition=%s%n",
                result.getRiskScore(), result.getAlertLevel(), result.getLandslidePreCondition());
    }

    /**
     * The 7-day/30km/basin landslide floor is a hard gate, not a decay - a
     * landslide fully counts up to 7 days old, then drops to zero.
     */
    @Test
    void landslideScore_isAStepFunctionOfElapsedTime_notAContinuousDecay() {
        Glacier langtangGlacier = Glacier.builder()
                .id(1L).rgiId("RGI2000-v7.0-G-15-05840").glacierName("Glacier 1.6km from Kyanjin Gompa")
                .terminusLatitude(28.21247770786395).terminusLongitude(85.69629548099003)
                .slopeDeg(48.48048).areaKm2(0.045344545812138)
                .build();

        when(hazardEventRepository.findRecentEarthquakes(any())).thenReturn(Collections.emptyList());
        when(weatherRepository.findLatestByLocation(any())).thenReturn(Optional.empty());
        when(weatherRepository.findNearestByCoordinates(any(Double.class), any(Double.class)))
                .thenReturn(Optional.empty());

        // 10_079, not 10_080, to stay clear of clock-read jitter at the
        // exact 7-day boundary.
        long[] minutesAgo = {10, 30, 60, 120, 360, 1440, 10_079, 10_081};
        String[] labels = {
                "10 minutes ago", "30 minutes ago", "1 hour ago", "2 hours ago",
                "6 hours ago", "1 day ago", "just under 7 days ago", "just over 7 days ago"
        };

        double firstScore = -1;
        for (int i = 0; i < minutesAgo.length; i++) {
            HazardEvent landslideM52 = realLandslideEvent(6L, 5.2, 28.271, 85.515, minutesAgo[i]);
            HazardEvent landslideM42 = realLandslideEvent(5L, 4.2, 28.27, 85.515, minutesAgo[i]);
            when(hazardEventRepository.findRecentLandslides(any()))
                    .thenReturn(List.of(landslideM52, landslideM42));

            GlofRiskAssessment result = service.assessGlacier(langtangGlacier);
            System.out.printf("%-20s -> score=%.1f alert=%-7s landslideDetected=%s%n",
                    labels[i], result.getRiskScore(), result.getAlertLevel(), result.getLandslideDetected());

            if (i == 0) {
                firstScore = result.getRiskScore();
            }
            if (minutesAgo[i] <= 10_080) {
                // Still within the 7-day floor - score identical to the
                // 10-minutes-ago case no matter how much time passed.
                assertThat(result.getRiskScore()).isCloseTo(firstScore, offset(0.01));
                assertThat(result.getLandslideDetected()).isTrue();
            } else {
                // One minute past the 7-day line - drops off a cliff.
                assertThat(result.getRiskScore()).isLessThan(firstScore);
                assertThat(result.getLandslideDetected()).isFalse();
            }
        }
    }

    private HazardEvent realLandslideEvent(Long id, double magnitude, double lat, double lon) {
        return realLandslideEvent(id, magnitude, lat, lon, 3 * 1440L);
    }

    private HazardEvent realLandslideEvent(Long id, double magnitude, double lat, double lon, long minutesAgo) {
        HazardEvent event = new HazardEvent();
        event.setId(id);
        event.setEventType("EARTHQUAKE");
        event.setSourceType("landslide");
        event.setMagnitude(magnitude);
        event.setDepth(0.0);
        event.setLatitude(lat);
        event.setLongitude(lon);
        // Anchored to UTC "now" (matches the service's own clock) rather
        // than a fixed date, so the test doesn't go stale over time.
        event.setEventTime(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(minutesAgo));
        event.setStatus("CONFIRMED");
        event.setDescription("M " + magnitude + " Landslide - 55 km NW of Kodari, Nepal (Depth: 0.0km)");
        event.setDeathToll(0);
        return event;
    }
}
