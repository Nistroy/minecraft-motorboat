package io.github.nistroy.motorboat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Config du mod (config/motorboat.json). Créée avec les défauts au premier lancement. */
public record MotorboatConfig(double topSpeed) {
    /** Plancher : la rame vanilla. Plafond : au-delà on traverse les chunks plus vite qu'ils ne chargent. */
    public static final double MIN_TOP_SPEED = 8.0;

    public static final double MAX_TOP_SPEED = 40.0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Format JSON du fichier ; valeurs = défauts. */
    static final class Raw {
        double topSpeedBlocksPerSecond = 16.0;
    }

    public static MotorboatConfig load(Path file) throws IOException {
        Raw raw;
        if (Files.exists(file)) {
            try {
                raw = GSON.fromJson(Files.readString(file), Raw.class);
            } catch (JsonParseException e) {
                throw new IllegalArgumentException("config illisible : " + file, e);
            }
            if (raw == null) {
                raw = new Raw();
            }
        } else {
            raw = new Raw();
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, GSON.toJson(raw));
        }
        if (raw.topSpeedBlocksPerSecond < MIN_TOP_SPEED || raw.topSpeedBlocksPerSecond > MAX_TOP_SPEED) {
            throw new IllegalArgumentException(
                    "topSpeedBlocksPerSecond doit être entre " + MIN_TOP_SPEED + " et " + MAX_TOP_SPEED);
        }
        return new MotorboatConfig(raw.topSpeedBlocksPerSecond);
    }
}
