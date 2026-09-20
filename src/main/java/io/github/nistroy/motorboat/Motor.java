package io.github.nistroy.motorboat;

/**
 * Réserve de combustible du moteur, en ticks de marche. Aucune dépendance Minecraft : l'entité stocke
 * l'entier (synchronisé client/serveur) et délègue ici toutes les règles.
 */
public final class Motor {
    /** Réserve refusée : réservoir plein, ou objet qui ne brûle pas. */
    public static final int REFUSED = -1;

    /** 10 min de marche (20 ticks/s), soit 7,5 charbons (1600 ticks pièce). */
    public static final int MAX_FUEL_TICKS = 12_000;

    private Motor() {}

    public static boolean running(int fuelTicks) {
        return fuelTicks > 0;
    }

    /** Réserve après ajout d'un combustible de {@code burnTicks}, ou {@link #REFUSED} si ça ne rentre pas. */
    public static int load(int fuelTicks, int burnTicks) {
        if (burnTicks <= 0 || fuelTicks + burnTicks > MAX_FUEL_TICKS) {
            return REFUSED;
        }
        return fuelTicks + burnTicks;
    }

    /** Réserve après un tick : ne consomme que gaz mis, pour qu'une barque à l'arrêt ne brûle rien. */
    public static int burn(int fuelTicks, boolean throttle) {
        return throttle && fuelTicks > 0 ? fuelTicks - 1 : clamp(fuelTicks);
    }

    /** Valeur relue (sauvegarde, réseau) ramenée dans les bornes. */
    public static int clamp(int fuelTicks) {
        return Math.max(0, Math.min(MAX_FUEL_TICKS, fuelTicks));
    }
}
