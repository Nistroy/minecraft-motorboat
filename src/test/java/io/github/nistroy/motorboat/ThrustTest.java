package io.github.nistroy.motorboat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThrustTest {
    private static final double EPS = 1.0e-9;

    @Test
    void withoutEngineItIsTheVanillaBoat() {
        assertEquals(8.0, Thrust.topSpeedFor(0.0), EPS);
    }

    @Test
    void thrustHitsTheTargetSpeed() {
        assertEquals(16.0, Thrust.topSpeedFor(Thrust.extraAcceleration(16.0)), EPS);
        assertEquals(24.0, Thrust.topSpeedFor(Thrust.extraAcceleration(24.0)), EPS);
        assertTrue(Thrust.extraAcceleration(16.0) > 0.0);
    }

    @Test
    void neverSlowerThanPaddling() {
        assertEquals(0.0, Thrust.extraAcceleration(8.0), EPS);
        assertEquals(0.0, Thrust.extraAcceleration(2.0), EPS);
    }
}
