package io.github.nistroy.motorboat;

import net.minecraft.util.Mth;

/**
 * Houle : l'assiette que prend une coque qui navigue — tangage, roulis, pilonnement (pur, testable
 * hors Minecraft). Appliqué au rendu seul par {@code MotorboatRenderer.applyWave}.
 *
 * <p><b>Purement visuel</b> : ni la position ni la hitbox ne bougent (les AABB de Minecraft ne
 * tournent pas), et les passagers restent dessinés debout — leurs points d'attache
 * ({@link SeatPlan}) ignorent le tangage. D'où des amplitudes faibles : au-delà, les pieds des
 * joueurs traversent le pont. Assis le plus loin du centre (banc d'étrave, 0,69 bloc), un passager
 * décolle de 0,69 × sin(6,5°) ≈ 0,08 bloc au pire — sous le pixel et demi.
 *
 * <p>La phase se calcule à partir de l'heure du monde <b>et</b> de la position : c'est un champ de
 * vagues, pas une oscillation propre à chaque barque. Deux barques côte à côte lèvent donc l'étrave
 * ensemble, et tous les clients trouvent la même valeur sans que rien ne transite sur le réseau.
 *
 * <p>Deux trains de vagues de périodes et de directions premières entre elles (houle longue + clapot
 * court) : la somme ne se répète pas à l'œil, là où une seule sinusoïde se lit comme un métronome.
 */
public final class Wave {
    /** Assiette instantanée d'une coque : degrés pour les angles, blocs pour le pilonnement. */
    public record Motion(float pitchDegrees, float rollDegrees, double heaveBlocks) {}

    /** Tangage de houle à pleine vitesse, en degrés (hors assiette de déjaugeage). */
    public static final float MAX_SWELL_DEGREES = 3.5F;

    /** Tangage à l'arrêt : la barque reste vivante à quai sans donner le mal de mer. */
    public static final float REST_SWELL_DEGREES = 0.8F;

    /** Vitesse (blocs/tick) à laquelle la houle donne tout : 12 blocs/s, sous le moteur de base. */
    private static final double FULL_SWELL_SPEED = 0.6;

    /** Le roulis reste minoritaire : un hors-bord lancé tape de l'étrave, il ne gîte pas. */
    private static final float ROLL_SHARE = 0.55F;

    /** Nez relevé en vitesse (degrés) : c'est le déjaugeage qui fait lire « hors-bord ». */
    public static final float MAX_TRIM_DEGREES = 3.0F;

    /** Vitesse (blocs/tick) où la coque déjauge complètement : 20 blocs/s. */
    private static final double PLANING_SPEED = 1.0;

    /** Montée/descente sur la vague, en blocs (0,05 = 0,8 px) : au-delà, la coque quitte l'eau. */
    public static final double MAX_HEAVE_BLOCKS = 0.05;

    /** Houle longue : période (ticks), longueur d'onde (blocs) et direction (unitaire). */
    private static final double SWELL_PERIOD_TICKS = 47.0;

    private static final double SWELL_LENGTH_BLOCKS = 11.0;

    private static final double SWELL_DIR_X = 0.9238795;

    private static final double SWELL_DIR_Z = 0.3826834;

    /** Clapot : plus court, plus rapide, en travers de la houle. */
    private static final double CHOP_PERIOD_TICKS = 29.0;

    private static final double CHOP_LENGTH_BLOCKS = 6.5;

    private static final double CHOP_DIR_X = -0.5;

    private static final double CHOP_DIR_Z = 0.8660254;

    /** Part du clapot dans le mélange ; la houle prend le reste, somme = 1 (borne les angles). */
    private static final double CHOP_SHARE = 0.3;

    /** Décalage du roulis (radians) : sinon il passe par zéro en même temps que le tangage. */
    private static final double ROLL_PHASE_SHIFT = 1.1;

    private Wave() {}

    /**
     * Assiette de la coque à cet endroit, à cet instant, à cette vitesse.
     *
     * @param x position est-ouest, en blocs (interpolée : la phase avance de ~1 rad par tick à
     *     pleine vitesse, la position du tick seul se verrait saccader)
     * @param z position nord-sud, en blocs
     * @param timeTicks heure du monde + partie de tick
     * @param speedBlocksPerTick vitesse horizontale ; 1,6 au maximum (32 blocs/s)
     */
    public static Motion at(double x, double z, double timeTicks, double speedBlocksPerTick) {
        double swell = phase(x, z, timeTicks, SWELL_DIR_X, SWELL_DIR_Z, SWELL_LENGTH_BLOCKS, SWELL_PERIOD_TICKS);
        double chop = phase(x, z, timeTicks, CHOP_DIR_X, CHOP_DIR_Z, CHOP_LENGTH_BLOCKS, CHOP_PERIOD_TICKS);
        double amplitude = swellDegrees(speedBlocksPerTick);
        // Sommes pondérées à 1 : |tangage de houle| et |roulis| ne dépassent jamais l'amplitude.
        double pitchWave = (1.0 - CHOP_SHARE) * Math.sin(swell) + CHOP_SHARE * Math.sin(chop);
        double rollWave = (1.0 - CHOP_SHARE) * Math.sin(chop + ROLL_PHASE_SHIFT) + CHOP_SHARE * Math.sin(swell);
        return new Motion(
                (float) (trimDegrees(speedBlocksPerTick) + amplitude * pitchWave),
                (float) (amplitude * ROLL_SHARE * rollWave),
                MAX_HEAVE_BLOCKS * (amplitude / MAX_SWELL_DEGREES) * Math.sin(swell));
    }

    /** Amplitude de la houle à cette vitesse : du repos ({@link #REST_SWELL_DEGREES}) au maximum. */
    public static double swellDegrees(double speedBlocksPerTick) {
        double ramp = Mth.clamp(speedBlocksPerTick / FULL_SWELL_SPEED, 0.0, 1.0);
        return Mth.lerp(ramp, REST_SWELL_DEGREES, MAX_SWELL_DEGREES);
    }

    /**
     * Assiette de déjaugeage : nez relevé, nul à l'arrêt, plafonné à {@link #MAX_TRIM_DEGREES}.
     * Courbe en {@code f(2 − f)} — l'étrave se lève vite dès qu'on met les gaz puis se pose sur son
     * plafond, comme une coque qui déjauge ; une rampe linéaire donne un cabrage qui n'en finit pas.
     */
    public static float trimDegrees(double speedBlocksPerTick) {
        double ramp = Mth.clamp(speedBlocksPerTick / PLANING_SPEED, 0.0, 1.0);
        return (float) (MAX_TRIM_DEGREES * ramp * (2.0 - ramp));
    }

    /** Phase d'un train de vagues : l'avancée du temps plus celle de la position dans la direction. */
    private static double phase(
            double x, double z, double timeTicks, double dirX, double dirZ, double lengthBlocks, double periodTicks) {
        return 2.0 * Math.PI * (timeTicks / periodTicks + (x * dirX + z * dirZ) / lengthBlocks);
    }
}
