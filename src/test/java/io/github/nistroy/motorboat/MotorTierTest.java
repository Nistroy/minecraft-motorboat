package io.github.nistroy.motorboat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MotorTierTest {
    @Test
    void theSmallHullOnlyTakesTheBasicMotor() {
        assertTrue(MotorTier.NONE.fitsHull(false));
        assertTrue(MotorTier.BASIC.fitsHull(false));
        assertFalse(MotorTier.BIG.fitsHull(false));
        assertFalse(MotorTier.DOUBLE.fitsHull(false));
    }

    @Test
    void theBigHullTakesThemAll() {
        for (MotorTier tier : MotorTier.values()) {
            assertTrue(tier.fitsHull(true), tier.name());
        }
    }

    @Test
    void idRoundTripsAndClampsOnGarbage() {
        for (MotorTier tier : MotorTier.values()) {
            assertEquals(tier, MotorTier.byId(tier.id()));
        }
        assertEquals(MotorTier.NONE, MotorTier.byId(-1));
        assertEquals(MotorTier.NONE, MotorTier.byId(MotorTier.values().length));
    }
}
