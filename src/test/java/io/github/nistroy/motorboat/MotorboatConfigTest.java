package io.github.nistroy.motorboat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MotorboatConfigTest {
    private static final double EPS = 1.0e-9;

    @TempDir
    Path dir;

    @Test
    void writesDefaultsWhenMissing() throws IOException {
        Path file = dir.resolve("motorboat.json");
        MotorboatConfig config = MotorboatConfig.load(file);
        assertEquals(16.0, config.basicTopSpeed());
        assertEquals(24.0, config.bigTopSpeed());
        assertEquals(32.0, config.doubleTopSpeed());
        assertEquals(0.85, config.bigHullFactor());
        assertTrue(Files.exists(file));
        assertEquals(config, MotorboatConfig.load(file));
    }

    @Test
    void speedDependsOnTheMotorAndOnTheHull() throws IOException {
        MotorboatConfig config = MotorboatConfig.load(dir.resolve("motorboat.json"));
        assertEquals(0.0, config.topSpeed(MotorTier.NONE, false), EPS);
        assertEquals(0.0, config.topSpeed(MotorTier.NONE, true), EPS);
        assertEquals(16.0, config.topSpeed(MotorTier.BASIC, false), EPS);
        assertEquals(13.6, config.topSpeed(MotorTier.BASIC, true), EPS);
        assertEquals(20.4, config.topSpeed(MotorTier.BIG, true), EPS);
        assertEquals(27.2, config.topSpeed(MotorTier.DOUBLE, true), EPS);
    }

    @Test
    void motorsAreOrderedBySpeedAndTheBigHullIsSlower() throws IOException {
        MotorboatConfig config = MotorboatConfig.load(dir.resolve("motorboat.json"));
        assertTrue(config.topSpeed(MotorTier.BASIC, true) < config.topSpeed(MotorTier.BIG, true));
        assertTrue(config.topSpeed(MotorTier.BIG, true) < config.topSpeed(MotorTier.DOUBLE, true));
        assertTrue(config.topSpeed(MotorTier.BASIC, true) < config.topSpeed(MotorTier.BASIC, false));
    }

    @Test
    void readsValues() throws IOException {
        MotorboatConfig config = MotorboatConfig.load(write("{\"basicMotorBlocksPerSecond\": 12.0,"
                + "\"bigMotorBlocksPerSecond\": 18.0,"
                + "\"doubleMotorBlocksPerSecond\": 20.0,"
                + "\"bigHullSpeedFactor\": 0.5}"));
        assertEquals(12.0, config.basicTopSpeed());
        assertEquals(18.0, config.bigTopSpeed());
        assertEquals(20.0, config.doubleTopSpeed());
        assertEquals(10.0, config.topSpeed(MotorTier.DOUBLE, true), EPS);
    }

    @Test
    void readsTheOldSingleSpeedAsTheBasicMotorSpeed() throws IOException {
        MotorboatConfig config = MotorboatConfig.load(write("{\"topSpeedBlocksPerSecond\": 20.0}"));
        assertEquals(20.0, config.basicTopSpeed());
        assertEquals(24.0, config.bigTopSpeed());
    }

    @Test
    void hasNoSpeedCeiling() throws IOException {
        assertEquals(400.0, MotorboatConfig.load(write("{\"doubleMotorBlocksPerSecond\": 400}")).doubleTopSpeed());
    }

    @Test
    void refusesNonsenseOrBrokenFile() throws IOException {
        assertThrows(
                IllegalArgumentException.class, () -> MotorboatConfig.load(write("{\"basicMotorBlocksPerSecond\": 0}")));
        assertThrows(
                IllegalArgumentException.class, () -> MotorboatConfig.load(write("{\"bigMotorBlocksPerSecond\": -1}")));
        assertThrows(IllegalArgumentException.class, () -> MotorboatConfig.load(write("{\"bigHullSpeedFactor\": 1.5}")));
        assertThrows(IllegalArgumentException.class, () -> MotorboatConfig.load(write("{\"bigHullSpeedFactor\": 0}")));
        assertThrows(IllegalArgumentException.class, () -> MotorboatConfig.load(write("{pas du json")));
    }

    private Path write(String json) throws IOException {
        Path file = Files.createTempFile(dir, "config", ".json");
        Files.writeString(file, json);
        return file;
    }
}
