package io.github.nistroy.motorboat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SeatPlanTest {
    @Test
    void sixPlaces() {
        assertEquals(BigMotorboatEntity.MAX_PASSENGERS, SeatPlan.count());
    }

    @Test
    void lePremierPassagerEstALaBarre() {
        SeatPlan.Seat barre = SeatPlan.seat(0);
        assertEquals(0.0, barre.across());
        for (int seat = 1; seat < SeatPlan.count(); seat++) {
            assertTrue(barre.along() < SeatPlan.seat(seat).along(), "la barre est la place la plus à l'arrière");
            assertTrue(barre.height() > SeatPlan.seat(seat).height(), "la barre est assise sur la banquette");
        }
    }

    @Test
    void lesRangsSontSymetriques() {
        for (int[] paire : new int[][] {{1, 2}, {3, 4}}) {
            SeatPlan.Seat gauche = SeatPlan.seat(paire[0]);
            SeatPlan.Seat droite = SeatPlan.seat(paire[1]);
            assertEquals(-gauche.across(), droite.across());
            assertEquals(gauche.along(), droite.along());
            assertEquals(gauche.height(), droite.height());
        }
    }

    @Test
    void lesPlacesVontDeLaPoupeALaProue() {
        for (int seat = 1; seat < SeatPlan.count(); seat++) {
            assertTrue(SeatPlan.seat(seat - 1).along() <= SeatPlan.seat(seat).along());
        }
    }

    @Test
    void unIndexHorsPlanRetombeSurUnePlaceValide() {
        assertEquals(SeatPlan.seat(0), SeatPlan.seat(-3));
        assertEquals(SeatPlan.seat(SeatPlan.count() - 1), SeatPlan.seat(99));
    }
}
