package io.github.nistroy.motorboat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.Nullable;

/**
 * Config du mod (config/motorboat.json). Créée avec les défauts au premier lancement.
 *
 * <p>Une vitesse par moteur, plus le facteur de la grande coque (plus lourde, donc plus lente).
 * Pas de plafond (choix nistroy 2026-09-20) : au-delà d'une trentaine de blocs/s on traverse les
 * chunks plus vite qu'ils ne chargent, c'est le seul garde-fou.
 */
public record MotorboatConfig(double basicTopSpeed, double bigTopSpeed, double doubleTopSpeed, double bigHullFactor) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final double DEFAULT_BASIC_TOP_SPEED = 16.0;

    public static final double DEFAULT_BIG_TOP_SPEED = 24.0;

    public static final double DEFAULT_DOUBLE_TOP_SPEED = 32.0;

    public static final double DEFAULT_BIG_HULL_FACTOR = 0.85;

    /** Format JSON du fichier ; champs boxés pour distinguer « absent » de « 0 ». */
    static final class Raw {
        Double basicMotorBlocksPerSecond;
        Double bigMotorBlocksPerSecond;
        Double doubleMotorBlocksPerSecond;
        Double bigHullSpeedFactor;

        /** v0.1–v0.2 : une seule vitesse. Reprise comme vitesse du moteur de base. */
        Double topSpeedBlocksPerSecond;
    }

    /** Vitesse de pointe visée, en blocs/s ; 0 sans moteur (Thrust ne pousse alors pas). */
    public double topSpeed(MotorTier motor, boolean bigHull) {
        double base = switch (motor) {
            case NONE -> 0.0;
            case BASIC -> basicTopSpeed;
            case BIG -> bigTopSpeed;
            case DOUBLE -> doubleTopSpeed;
        };
        return bigHull ? base * bigHullFactor : base;
    }

    /** Raw écrit dans le fichier au premier lancement : tous les champs renseignés, sans l'ancienne clé. */
    private static Raw defaults() {
        Raw raw = new Raw();
        raw.basicMotorBlocksPerSecond = DEFAULT_BASIC_TOP_SPEED;
        raw.bigMotorBlocksPerSecond = DEFAULT_BIG_TOP_SPEED;
        raw.doubleMotorBlocksPerSecond = DEFAULT_DOUBLE_TOP_SPEED;
        raw.bigHullSpeedFactor = DEFAULT_BIG_HULL_FACTOR;
        return raw;
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
            raw = defaults();
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, GSON.toJson(raw));
        }
        double basic = positive(
                "basicMotorBlocksPerSecond",
                or(raw.basicMotorBlocksPerSecond, or(raw.topSpeedBlocksPerSecond, DEFAULT_BASIC_TOP_SPEED)));
        double big = positive("bigMotorBlocksPerSecond", or(raw.bigMotorBlocksPerSecond, DEFAULT_BIG_TOP_SPEED));
        double dual = positive(
                "doubleMotorBlocksPerSecond", or(raw.doubleMotorBlocksPerSecond, DEFAULT_DOUBLE_TOP_SPEED));
        double factor = or(raw.bigHullSpeedFactor, DEFAULT_BIG_HULL_FACTOR);
        if (factor <= 0.0 || factor > 1.0) {
            throw new IllegalArgumentException("bigHullSpeedFactor doit être dans ]0, 1]");
        }
        return new MotorboatConfig(basic, big, dual, factor);
    }

    private static double or(@Nullable Double value, double fallback) {
        return value != null ? value : fallback;
    }

    private static double positive(String key, double speed) {
        if (speed <= 0.0) {
            throw new IllegalArgumentException(key + " doit être > 0");
        }
        return speed;
    }
}
