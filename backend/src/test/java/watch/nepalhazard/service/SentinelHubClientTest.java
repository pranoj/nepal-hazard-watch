package watch.nepalhazard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import org.junit.jupiter.api.Test;

/**
 * Tests the response-parsing logic against synthetic Statistics API
 * responses shaped like the real one confirmed via a live call to Tsho
 * Rolpa's real coordinates.
 */
class SentinelHubClientTest {

    private final SentinelHubClient client = new SentinelHubClient(20);

    @Test
    void parseResponse_extractsWaterFractionFromARealShapedResponse() {
        // Shaped exactly like the real response returned for Tsho Rolpa on
        // 2026-09-06: 200x200 pixels, ~43% valid (rest cloud/invalid).
        String body = """
                {
                  "data": [
                    {
                      "interval": { "from": "2026-09-01T00:00:00Z", "to": "2026-09-06T00:00:00Z" },
                      "outputs": {
                        "ndwi": { "bands": { "B0": { "stats": {
                          "mean": -0.1248155136762235, "sampleCount": 40000, "noDataCount": 22664
                        } } } },
                        "water": { "bands": { "B0": { "stats": {
                          "mean": 0.2534610059990764, "sampleCount": 40000, "noDataCount": 22664
                        } } } }
                      }
                    }
                  ],
                  "status": "OK"
                }
                """;

        SentinelHubClient.LakeWaterReading reading = client.parseResponse(body);

        assertThat(reading.waterFraction()).isCloseTo(0.2535, offset(0.0001));
        assertThat(reading.meanNdwi()).isCloseTo(-0.1248, offset(0.0001));
        assertThat(reading.totalPixels()).isEqualTo(40000);
        assertThat(reading.validPixels()).isEqualTo(40000 - 22664);
        assertThat(reading.rawResponseIfEmpty()).isNull();
    }

    @Test
    void parseResponse_skipsAnAllCloudIntervalAndFallsBackToTheOlderValidOne() {
        String body = """
                {
                  "data": [
                    {
                      "interval": { "from": "2026-08-27T00:00:00Z", "to": "2026-09-01T00:00:00Z" },
                      "outputs": {
                        "ndwi": { "bands": { "B0": { "stats": { "mean": -0.05, "sampleCount": 40000, "noDataCount": 39500 } } } },
                        "water": { "bands": { "B0": { "stats": { "mean": 0.10, "sampleCount": 40000, "noDataCount": 39500 } } } }
                      }
                    },
                    {
                      "interval": { "from": "2026-09-01T00:00:00Z", "to": "2026-09-06T00:00:00Z" },
                      "outputs": {
                        "ndwi": { "bands": { "B0": { "stats": { "mean": 0.0, "sampleCount": 40000, "noDataCount": 40000 } } } },
                        "water": { "bands": { "B0": { "stats": { "mean": 0.0, "sampleCount": 40000, "noDataCount": 40000 } } } }
                      }
                    }
                  ],
                  "status": "OK"
                }
                """;

        SentinelHubClient.LakeWaterReading reading = client.parseResponse(body);

        // Most recent interval (Sept 1-6) is entirely cloud/no-data, so the
        // parser should fall back to the older Aug 27-Sept 1 interval
        // rather than reporting a false "no water" reading.
        assertThat(reading.intervalFrom()).isEqualTo("2026-08-27T00:00:00Z");
        assertThat(reading.validPixels()).isEqualTo(500);
    }

    @Test
    void parseResponse_returnsEmptyReadingWhenNoIntervalHasValidPixels() {
        String body = """
                {
                  "data": [
                    {
                      "interval": { "from": "2026-09-01T00:00:00Z", "to": "2026-09-06T00:00:00Z" },
                      "outputs": {
                        "ndwi": { "bands": { "B0": { "stats": { "mean": 0.0, "sampleCount": 40000, "noDataCount": 40000 } } } },
                        "water": { "bands": { "B0": { "stats": { "mean": 0.0, "sampleCount": 40000, "noDataCount": 40000 } } } }
                      }
                    }
                  ],
                  "status": "OK"
                }
                """;

        SentinelHubClient.LakeWaterReading reading = client.parseResponse(body);

        assertThat(Double.isNaN(reading.waterFraction())).isTrue();
        assertThat(reading.validPixels()).isZero();
        assertThat(reading.rawResponseIfEmpty()).isEqualTo(body);
    }

    @Test
    void parseIceResponse_extractsIceFractionFromARealShapedResponse() {
        // Same real response shape as the water evalscript, with ndsi/ice
        // output ids instead of ndwi/water.
        String body = """
                {
                  "data": [
                    {
                      "interval": { "from": "2026-09-01T00:00:00Z", "to": "2026-09-06T00:00:00Z" },
                      "outputs": {
                        "ndsi": { "bands": { "B0": { "stats": {
                          "mean": 0.52, "sampleCount": 40000, "noDataCount": 10000
                        } } } },
                        "ice": { "bands": { "B0": { "stats": {
                          "mean": 0.61, "sampleCount": 40000, "noDataCount": 10000
                        } } } }
                      }
                    }
                  ],
                  "status": "OK"
                }
                """;

        SentinelHubClient.IceCoverReading reading = client.parseIceResponse(body);

        assertThat(reading.iceFraction()).isCloseTo(0.61, offset(0.0001));
        assertThat(reading.meanNdsi()).isCloseTo(0.52, offset(0.0001));
        assertThat(reading.totalPixels()).isEqualTo(40000);
        assertThat(reading.validPixels()).isEqualTo(30000);
        assertThat(reading.rawResponseIfEmpty()).isNull();
    }

    @Test
    void parseIceResponse_returnsEmptyReadingWhenNoIntervalHasValidPixels() {
        String body = """
                {
                  "data": [
                    {
                      "interval": { "from": "2026-09-01T00:00:00Z", "to": "2026-09-06T00:00:00Z" },
                      "outputs": {
                        "ndsi": { "bands": { "B0": { "stats": { "mean": 0.0, "sampleCount": 40000, "noDataCount": 40000 } } } },
                        "ice": { "bands": { "B0": { "stats": { "mean": 0.0, "sampleCount": 40000, "noDataCount": 40000 } } } }
                      }
                    }
                  ],
                  "status": "OK"
                }
                """;

        SentinelHubClient.IceCoverReading reading = client.parseIceResponse(body);

        assertThat(Double.isNaN(reading.iceFraction())).isTrue();
        assertThat(reading.validPixels()).isZero();
        assertThat(reading.rawResponseIfEmpty()).isEqualTo(body);
    }
}
