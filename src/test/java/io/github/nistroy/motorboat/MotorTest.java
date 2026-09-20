package io.github.nistroy.motorboat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MotorTest {
    private static final int COAL = 1600;

    @Test
    void runsOnlyWithFuel() {
        assertFalse(Motor.running(0));
        assertTrue(Motor.running(1));
    }

    @Test
    void loadAddsBurnTime() {
        assertEquals(COAL, Motor.load(0, COAL));
        assertEquals(2 * COAL, Motor.load(COAL, COAL));
    }

    @Test
    void loadRefusesWhenTankWouldOverflow() {
        assertEquals(Motor.REFUSED, Motor.load(Motor.MAX_FUEL_TICKS, COAL));
        assertEquals(Motor.REFUSED, Motor.load(Motor.MAX_FUEL_TICKS - COAL + 1, COAL));
        assertEquals(Motor.MAX_FUEL_TICKS, Motor.load(Motor.MAX_FUEL_TICKS - COAL, COAL));
    }

    @Test
    void loadRefusesNonFuel() {
        assertEquals(Motor.REFUSED, Motor.load(0, 0));
        assertEquals(Motor.REFUSED, Motor.load(0, -20));
    }

    @Test
    void burnsOnlyUnderThrottle() {
        assertEquals(COAL - 1, Motor.burn(COAL, true));
        assertEquals(COAL, Motor.burn(COAL, false));
        assertEquals(0, Motor.burn(0, true));
    }

    @Test
    void clampsValuesReadBack() {
        assertEquals(0, Motor.clamp(-5));
        assertEquals(Motor.MAX_FUEL_TICKS, Motor.clamp(Motor.MAX_FUEL_TICKS + 1));
        assertEquals(COAL, Motor.clamp(COAL));
    }
}
