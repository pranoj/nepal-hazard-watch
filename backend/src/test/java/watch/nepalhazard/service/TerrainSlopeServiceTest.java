package watch.nepalhazard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import org.junit.jupiter.api.Test;

class TerrainSlopeServiceTest {

    @Test
    void computeSlopeDeg_isZeroOnFlatTerrain() {
        double slope = TerrainSlopeService.computeSlopeDeg(100, 100, 100, 100, 100);
        assertThat(slope).isEqualTo(0.0);
    }

    @Test
    void computeSlopeDeg_matchesKnownGradient() {
        // 20m rise over 200m (north-south samples 100m apart each side of
        // center) is a 0.1 gradient -> atan(0.1) in degrees.
        double slope = TerrainSlopeService.computeSlopeDeg(110, 90, 100, 100, 100);
        assertThat(slope).isCloseTo(Math.toDegrees(Math.atan(0.1)), offset(1e-9));
    }

    @Test
    void computeSlopeDeg_combinesBothDirections() {
        // Equal 0.1 gradient in both lat and lon -> combined magnitude
        // sqrt(0.1^2 + 0.1^2), not just one direction's value.
        double slope = TerrainSlopeService.computeSlopeDeg(110, 90, 110, 90, 100);
        double expectedGradient = Math.sqrt(0.1 * 0.1 + 0.1 * 0.1);
        assertThat(slope).isCloseTo(Math.toDegrees(Math.atan(expectedGradient)), offset(1e-9));
    }
}
