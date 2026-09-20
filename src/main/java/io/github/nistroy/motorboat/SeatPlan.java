package io.github.nistroy.motorboat;

import net.minecraft.util.Mth;

/**
 * Plan de sièges de la grande barque : six places, le pilote assis sur la banquette de poupe.
 *
 * <p>Classe pure (aucune entité, aucun niveau) pour être testable hors Minecraft. Les nombres
 * viennent de la maquette {@code tools/art/big_hull.bbmodel} : {@code along} = x de la maquette / 16,
 * {@code across} = z / 16, {@code height} = {@code 0,375 + (dessus - 1) / 16 - 0,1875} (0,375 =
 * translation du rendu, 0,1875 = enfoncement de l'assise, celui de vanilla). Changer la maquette =
 * changer cette table, et {@code PLAN.md} §Plan de sièges avec.
 *
 * <p>Rangs écartés de 0,75 bloc (0,69 entre les deux derniers) : le bateau vanilla espace ses deux
 * places de 0,8 (offsets 0,2 et −0,6, relevés au {@code javap}), en dessous les jambes de chacun
 * traversent le dos du précédent.
 *
 * <p>Le siège 0 est la barre : {@code Boat.getControllingPassenger} rend le premier passager
 * (relevé au {@code javap}, 1.21.1), donc c'est lui qui pilote.
 */
public final class SeatPlan {
    /** Une place : travers (X), hauteur (Y) et position avant-arrière (Z) côté entité, en blocs. */
    public record Seat(double across, double height, double along) {}

    private static final double BENCH = 0.3125;

    private static final double HELM = 0.6875;

    private static final double SIDE = 0.4;

    private static final Seat[] SEATS = {
        new Seat(0.0, HELM, -1.375), // banquette de poupe, devant le moteur
        new Seat(-SIDE, BENCH, -0.625),
        new Seat(SIDE, BENCH, -0.625),
        new Seat(-SIDE, BENCH, 0.125),
        new Seat(SIDE, BENCH, 0.125),
        new Seat(0.0, BENCH, 0.8125), // banc d'étrave, une place au centre
    };

    private SeatPlan() {}

    public static int count() {
        return SEATS.length;
    }

    /** Place du passager, index écrêté : un index hors plan ne doit pas jeter en plein rendu. */
    public static Seat seat(int index) {
        return SEATS[Mth.clamp(index, 0, SEATS.length - 1)];
    }
}
