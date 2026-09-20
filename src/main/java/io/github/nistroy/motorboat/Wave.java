package io.github.nistroy.motorboat;

import net.minecraft.util.Mth;

/**
 * Houle : l'assiette que prend une coque qui navigue — tangage, roulis, pilonnement (pur, testable
 * hors Minecraft). Appliqué au rendu seul par {@code MotorboatRenderer.applyWave}.
 *
 * <p><b>La houle vient par groupes</b> : l'eau est plate la plupart du temps (~70 %), puis une série
 * passe, l'étrave se lève et se repose, et ça retombe au calme. C'est ce que demande nistroy
 * (2026-09-21) et c'est aussi ce que fait la mer. Une sinusoïde permanente, elle, se lit comme un
 * tremblement — première version rejetée pour ça : périodes de 2,3 s et 1,45 s combinées à une phase
 * qui avance avec la position, soit une crête toutes les ~5 ticks en vitesse de pointe.
 *
 * <p><b>Purement visuel</b> : ni la position ni la hitbox ne bougent (les AABB de Minecraft ne
 * tournent pas), et les passagers restent dessinés debout — leurs points d'attache
 * ({@link SeatPlan}) ignorent le tangage. D'où des amplitudes faibles : au-delà, les pieds des
 * joueurs traversent le pont.
 *
 * <p>Tout se calcule à partir de l'heure du monde <b>et</b> de la position : c'est un champ de
 * vagues, pas une oscillation propre à chaque barque. Deux barques côte à côte prennent la même
 * série, et tous les clients trouvent la même valeur sans que rien ne transite sur le réseau.
 *
 * <p>Allure obtenue (mesurée hors jeu) : à quai, un groupe toutes les 25-40 s, vague de 6,5 s ; à
 * 16 blocs/s, un groupe toutes les 10-17 s, vague de 2,8 s ; à 32 blocs/s, toutes les 6-11 s, vague
 * de 1,8 s. Plus on va vite, plus on rencontre de vagues — comme un vrai bateau.
 */
public final class Wave {
    /** Assiette instantanée d'une coque : degrés pour les angles, blocs pour le pilonnement. */
    public record Motion(float pitchDegrees, float rollDegrees, double heaveBlocks) {}

    /** Tangage au sommet d'un groupe, à pleine vitesse (hors assiette de déjaugeage). */
    public static final float MAX_SWELL_DEGREES = 4.0F;

    /** Tangage au sommet d'un groupe, à l'arrêt : la barque bouge, sans donner le mal de mer. */
    public static final float REST_SWELL_DEGREES = 1.6F;

    /** Vitesse (blocs/tick) à laquelle la houle donne tout : 12 blocs/s, sous le moteur de base. */
    private static final double FULL_SWELL_SPEED = 0.6;

    /** Le roulis reste minoritaire : un hors-bord lancé tape de l'étrave, il ne gîte pas. */
    private static final float ROLL_SHARE = 0.4F;

    /** Nez relevé en vitesse (degrés) : c'est le déjaugeage qui fait lire « hors-bord ». */
    public static final float MAX_TRIM_DEGREES = 3.0F;

    /** Vitesse (blocs/tick) où la coque déjauge complètement : 20 blocs/s. */
    private static final double PLANING_SPEED = 1.0;

    /** Montée/descente sur la vague, en blocs : au-delà, la coque quitte l'eau. */
    public static final double MAX_HEAVE_BLOCKS = 0.07;

    /** Vague elle-même : 6,5 s sur place, 80 blocs de crête à crête (long = lent, pas nerveux). */
    private static final double SWELL_PERIOD_TICKS = 130.0;

    private static final double SWELL_LENGTH_BLOCKS = 80.0;

    private static final double SWELL_DIR_X = 0.9238795;

    private static final double SWELL_DIR_Z = 0.3826834;

    /**
     * Enveloppe des groupes : deux battements lents de périodes premières entre elles, multipliés.
     * Leur produit reste bas la plupart du temps et ne monte que quand les deux coïncident — c'est
     * ce qui fait des séries espacées et jamais identiques.
     */
    private static final double GROUP_A_PERIOD_TICKS = 430.0;

    private static final double GROUP_A_LENGTH_BLOCKS = 240.0;

    private static final double GROUP_B_PERIOD_TICKS = 670.0;

    private static final double GROUP_B_LENGTH_BLOCKS = 370.0;

    /** Seuils du lissage : sous {@code LOW} l'eau est plate, au-dessus de {@code HIGH} le groupe donne tout. */
    private static final double GROUP_LOW = 0.30;

    private static final double GROUP_HIGH = 0.85;

    /** Le roulis roule deux fois plus lentement que le tangage, et décalé : sinon les deux battent ensemble. */
    private static final double ROLL_FREQUENCY = 0.5;

    private static final double ROLL_PHASE_SHIFT = 1.3;

    private Wave() {}

    /**
     * Assiette de la coque à cet endroit, à cet instant, à cette vitesse.
     *
     * @param x position est-ouest, en blocs (interpolée entre deux ticks, sinon la phase saccade)
     * @param z position nord-sud, en blocs
     * @param timeTicks heure du monde + partie de tick
     * @param speedBlocksPerTick vitesse horizontale ; 1,6 au maximum (32 blocs/s)
     */
    public static Motion at(double x, double z, double timeTicks, double speedBlocksPerTick) {
        double group = groupStrength(x, z, timeTicks);
        double amplitude = swellDegrees(speedBlocksPerTick) * group;
        double phase = phase(x, z, timeTicks, SWELL_DIR_X, SWELL_DIR_Z, SWELL_LENGTH_BLOCKS, SWELL_PERIOD_TICKS);
        double lift = Math.sin(phase);
        return new Motion(
                (float) (trimDegrees(speedBlocksPerTick) + amplitude * lift),
                (float) (amplitude * ROLL_SHARE * Math.sin(phase * ROLL_FREQUENCY + ROLL_PHASE_SHIFT)),
                MAX_HEAVE_BLOCKS * group * lift);
    }

    /**
     * Force du groupe de houle ici et maintenant, de 0 (eau plate) à 1 (série au sommet). Produit de
     * deux battements lents, ramené sur {@code [0, 1]} par un lissage : l'eau reste plate ~70 % du
     * temps, et les séries tombent à des intervalles jamais réguliers.
     */
    public static double groupStrength(double x, double z, double timeTicks) {
        double a = phase(x, z, timeTicks, SWELL_DIR_X, SWELL_DIR_Z, GROUP_A_LENGTH_BLOCKS, GROUP_A_PERIOD_TICKS);
        double b = phase(x, z, timeTicks, SWELL_DIR_Z, -SWELL_DIR_X, GROUP_B_LENGTH_BLOCKS, GROUP_B_PERIOD_TICKS);
        double raw = (0.5 + 0.5 * Math.sin(a)) * (0.5 + 0.5 * Math.sin(b));
        double t = Mth.clamp((raw - GROUP_LOW) / (GROUP_HIGH - GROUP_LOW), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** Amplitude d'un groupe à cette vitesse : du repos ({@link #REST_SWELL_DEGREES}) au maximum. */
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

    /** Phase d'un train : l'avancée du temps plus celle de la position dans la direction du train. */
    private static double phase(
            double x, double z, double timeTicks, double dirX, double dirZ, double lengthBlocks, double periodTicks) {
        return 2.0 * Math.PI * (timeTicks / periodTicks + (x * dirX + z * dirZ) / lengthBlocks);
    }
}
