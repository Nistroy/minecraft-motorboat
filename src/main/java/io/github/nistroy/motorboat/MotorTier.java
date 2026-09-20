package io.github.nistroy.motorboat;

/**
 * Moteur installé dans une barque, par ordre de vitesse croissante. Aucune dépendance Minecraft :
 * l'entité synchronise {@link #id()} et la correspondance item ↔ palier est dans {@code Motorboat}.
 */
public enum MotorTier {
    /** Pas de moteur : la barque reste un bateau à rames. */
    NONE,
    BASIC,
    BIG,
    DOUBLE;

    private static final MotorTier[] BY_ID = values();

    /** Entier synchronisé et sauvé ; {@link #byId} le relit. */
    public int id() {
        return ordinal();
    }

    /** Palier relu (sauvegarde, réseau) ; {@link #NONE} si l'entier ne correspond à rien. */
    public static MotorTier byId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : NONE;
    }

    /**
     * Le pont de la barque 2 places n'a la place que du moteur basique ; la grande coque prend les
     * trois.
     */
    public boolean fitsHull(boolean bigHull) {
        return bigHull || this == NONE || this == BASIC;
    }
}
