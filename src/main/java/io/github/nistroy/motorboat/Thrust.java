package io.github.nistroy.motorboat;

/**
 * Poussée du moteur, ajoutée à celle de la rame vanilla.
 *
 * <p>Vanilla 1.21.1 (constantes relevées au javap sur {@code net.minecraft.world.entity.vehicle.Boat}) :
 * un tick de bateau enchaîne {@code floatBoat} (vitesse × {@code invFriction = 0.9} dans l'eau),
 * {@code controlBoat} (+0,04 bloc/tick² vers l'avant) puis {@code move}. Vitesse d'équilibre vanilla :
 * 0,04 / (1 − 0,9) = 0,4 bloc/tick, soit 8 blocs/s — le repère de la spec.
 *
 * <p>La poussée du moteur est ajoutée avant {@code super.tick()}, donc avant la friction : elle est
 * multipliée par {@code invFriction} au tick même. D'où v = (poussée × k + 0,04) / (1 − k).
 */
public final class Thrust {
    public static final double VANILLA_ACCELERATION = 0.04;
    public static final double WATER_INV_FRICTION = 0.9;

    private Thrust() {}

    /** Vitesse d'équilibre en blocs/s pour une poussée moteur donnée (blocs/tick²). */
    public static double topSpeedFor(double extraAcceleration) {
        return (extraAcceleration * WATER_INV_FRICTION + VANILLA_ACCELERATION) / (1.0 - WATER_INV_FRICTION) * 20.0;
    }

    /** Poussée moteur pour viser {@code topSpeed} blocs/s ; jamais négative (on ne freine pas la rame). */
    public static double extraAcceleration(double topSpeed) {
        double needed = (topSpeed / 20.0 * (1.0 - WATER_INV_FRICTION) - VANILLA_ACCELERATION) / WATER_INV_FRICTION;
        return Math.max(0.0, needed);
    }
}
