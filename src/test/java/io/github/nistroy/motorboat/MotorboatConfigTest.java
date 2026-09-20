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
    @TempDir
    Path dir;

    @Test
    void writesDefaultsWhenMissing() throws IOException {
        Path file = dir.resolve("motorboat.json");
        MotorboatConfig config = MotorboatConfig.load(file);
        assertEquals(16.0, config.topSpeed());
        assertTrue(Files.exists(file));
        assertEquals(config, MotorboatConfig.load(file));
    }

    @Test
    void readsValue() throws IOException {
        MotorboatConfig config = MotorboatConfig.load(write("{\"topSpeedBlocksPerSecond\": 24.0}"));
        assertEquals(24.0, config.topSpeed());
    }

    @Test
    void refusesOutOfRangeOrBrokenFile() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> MotorboatConfig.load(write("{\"topSpeedBlocksPerSecond\": 4}")));
        assertThrows(IllegalArgumentException.class, () -> MotorboatConfig.load(write("{\"topSpeedBlocksPerSecond\": 100}")));
        assertThrows(IllegalArgumentException.class, () -> MotorboatConfig.load(write("{pas du json")));
    }

    private Path write(String json) throws IOException {
        Path file = Files.createTempFile(dir, "config", ".json");
        Files.writeString(file, json);
        return file;
    }
}
