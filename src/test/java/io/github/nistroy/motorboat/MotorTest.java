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

    @Test
    void autoLoadFillsUpToTheTankAndNeverRefusesFuel() {
        assertEquals(COAL, Motor.autoLoad(0, COAL));
        // Un seau de lave (20 000 ticks) dépasse la réserve : on écrête au lieu de refuser, sinon il
        // resterait coincé dans le slot carburant à chaque tick.
        assertEquals(Motor.MAX_FUEL_TICKS, Motor.autoLoad(0, 20_000));
        assertEquals(Motor.MAX_FUEL_TICKS, Motor.autoLoad(Motor.MAX_FUEL_TICKS - 1, COAL));
    }

    @Test
    void autoLoadRefusesWhenTankIsFullOrItemDoesNotBurn() {
        assertEquals(Motor.REFUSED, Motor.autoLoad(Motor.MAX_FUEL_TICKS, COAL));
        assertEquals(Motor.REFUSED, Motor.autoLoad(0, 0));
        assertEquals(Motor.REFUSED, Motor.autoLoad(0, -1));
    }
}
