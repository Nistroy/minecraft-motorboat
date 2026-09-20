package io.github.nistroy.motorboat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WaveTest {
    private static final double EPS = 1.0e-6;

    /** Vitesse de pointe du mod : 32 blocs/s = 1,6 bloc/tick. */
    private static final double TOP_SPEED = 1.6;

    @Test
    void theHullNeverTipsFurtherThanAnnounced() {
        double maxPitch = Wave.MAX_TRIM_DEGREES + Wave.MAX_SWELL_DEGREES;
        forEachSample((x, z, t, speed) -> {
            Wave.Motion motion = Wave.at(x, z, t, speed);
            assertTrue(Math.abs(motion.pitchDegrees()) <= maxPitch + EPS, "tangage " + motion.pitchDegrees());
            assertTrue(
                    Math.abs(motion.rollDegrees()) <= Wave.MAX_SWELL_DEGREES + EPS, "roulis " + motion.rollDegrees());
            assertTrue(
                    Math.abs(motion.heaveBlocks()) <= Wave.MAX_HEAVE_BLOCKS + EPS, "pilonnement " + motion.heaveBlocks());
        });
    }

    @Test
    void atRestItBarelyMoves() {
        forEachSample((x, z, t, ignoredSpeed) -> {
            Wave.Motion motion = Wave.at(x, z, t, 0.0);
            assertTrue(Math.abs(motion.pitchDegrees()) <= Wave.REST_SWELL_DEGREES + EPS, "à quai " + motion.pitchDegrees());
        });
    }

    @Test
    void theFasterItGoesTheMoreItJumps() {
        assertTrue(peakPitch(TOP_SPEED) > peakPitch(0.0) * 3.0);
        assertTrue(Wave.swellDegrees(0.0) < Wave.swellDegrees(0.3));
        assertTrue(Wave.swellDegrees(0.3) < Wave.swellDegrees(TOP_SPEED));
        assertEquals(Wave.REST_SWELL_DEGREES, Wave.swellDegrees(0.0), EPS);
        assertEquals(Wave.MAX_SWELL_DEGREES, Wave.swellDegrees(TOP_SPEED), EPS);
    }

    @Test
    void trimLiftsTheBowThenSettlesOnItsCeiling() {
        assertEquals(0.0, Wave.trimDegrees(0.0), EPS);
        float previous = 0.0F;
        for (double speed = 0.0; speed <= TOP_SPEED; speed += 0.05) {
            float trim = Wave.trimDegrees(speed);
            assertTrue(trim >= previous - EPS, "assiette qui redescend à " + speed);
            previous = trim;
        }
        assertEquals(Wave.MAX_TRIM_DEGREES, Wave.trimDegrees(1.0), EPS);
        assertEquals(Wave.MAX_TRIM_DEGREES, Wave.trimDegrees(TOP_SPEED), EPS);
        // Moitié de la vitesse de déjaugeage : déjà les trois quarts du cabrage.
        assertEquals(0.75 * Wave.MAX_TRIM_DEGREES, Wave.trimDegrees(0.5), 1.0e-5);
    }

    /** Champ de vagues : deux barques bord à bord au même instant lèvent l'étrave ensemble. */
    @Test
    void boatsSideBySideRideTheSameWave() {
        Wave.Motion mine = Wave.at(120.0, -40.0, 1000.0, 1.2);
        Wave.Motion neighbour = Wave.at(121.5, -40.0, 1000.0, 1.2);
        assertTrue(Math.abs(mine.pitchDegrees() - neighbour.pitchDegrees()) < 1.5F, "voisines désaccordées");
    }

    /** Une image dure 1/20 de tick au mieux : pas de saut d'angle entre deux images. */
    @Test
    void theMotionStaysSmoothBetweenFrames() {
        double step = 0.05;
        for (double t = 0.0; t < 200.0; t += step) {
            // Pire cas : pleine vitesse en ligne droite, la phase avance par le temps et par la position.
            double x = t * TOP_SPEED;
            Wave.Motion before = Wave.at(x, 0.0, t, TOP_SPEED);
            Wave.Motion after = Wave.at(x + step * TOP_SPEED, 0.0, t + step, TOP_SPEED);
            assertTrue(
                    Math.abs(after.pitchDegrees() - before.pitchDegrees()) < 0.5F,
                    "saut de tangage à t=" + t);
        }
    }

    @Test
    void theSameSpotAtTheSameMomentAlwaysGivesTheSameTilt() {
        assertEquals(Wave.at(12.5, 8.25, 4242.5, 0.9), Wave.at(12.5, 8.25, 4242.5, 0.9));
    }

    private interface Sample {
        void check(double x, double z, double timeTicks, double speed);
    }

    private static void forEachSample(Sample sample) {
        for (double x = -64.0; x <= 64.0; x += 3.7) {
            for (double z = -64.0; z <= 64.0; z += 5.3) {
                for (double t = 0.0; t < 120.0; t += 7.0) {
                    for (double speed = 0.0; speed <= TOP_SPEED; speed += 0.4) {
                        sample.check(x, z, t, speed);
                    }
                }
            }
        }
    }

    private static float peakPitch(double speed) {
        float peak = 0.0F;
        for (double t = 0.0; t < 400.0; t += 0.5) {
            peak = Math.max(peak, Math.abs(Wave.at(0.0, 0.0, t, speed).pitchDegrees() - Wave.trimDegrees(speed)));
        }
        return peak;
    }
}
