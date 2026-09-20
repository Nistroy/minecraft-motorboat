package io.github.nistroy.motorboat.client;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.vehicle.Boat;

/**
 * Coque de la grande barque : barque vanilla agrandie, 42 × 28 px (2,625 × 1,75 bloc), proue en +X.
 *
 * <p>Repère du modèle : après les transformations du rendu de bateau, +X pointe vers la proue et
 * **-Y vers le haut** ({@code scale(-1, -1, 1)}), d'où des parois de {@code y = -6} à {@code y = 0}.
 *
 * <p>Les boîtes ne sont pas écrites à la main : elles sont traduites de {@code tools/art/big_hull.bbmodel}
 * (maquette Blockbench, source de la forme **et** de la texture). Conversion : x et z inchangés,
 * {@code y_mod = 1 - y_bb}, et **rotation Y de même signe** : le {@code scale(-1, -1, 1)} est une
 * rotation de 180° autour de Z (déterminant +1), pas un miroir, il ne retourne pas le sens des
 * rotations (vérifié en jeu le 2026-09-20 — l'inverser ouvrait l'étrave en ailes). Retoucher la maquette → retraduire ici et relancer
 * {@code tools/generate_textures.py} ; voir {@code PLAN.md}.
 */
public class BigMotorboatModel extends ListModel<Boat> {
    public static final int TEXTURE_WIDTH = 256;

    public static final int TEXTURE_HEIGHT = 128;

    private static final String HULL = "hull";

    private static final String WATER_PATCH = "water_patch";

    private final ModelPart hull;

    private final ModelPart waterPatch;

    public BigMotorboatModel(ModelPart root) {
        this.hull = root.getChild(HULL);
        this.waterPatch = root.getChild(WATER_PATCH);
    }

    public static LayerDefinition createBodyModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition hull = root.addOrReplaceChild(HULL, CubeListBuilder.create(), PartPose.ZERO);
        hull.addOrReplaceChild(
                "fond",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-21F, 0F, -13F, 32F, 1F, 26F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "fond_etrave_1",
                CubeListBuilder.create()
                        .texOffs(116, 0)
                        .addBox(11F, 0F, -11.5F, 2F, 1F, 23F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "fond_etrave_2",
                CubeListBuilder.create()
                        .texOffs(166, 0)
                        .addBox(13F, 0F, -9F, 2F, 1F, 18F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "fond_etrave_3",
                CubeListBuilder.create()
                        .texOffs(206, 0)
                        .addBox(15F, 0F, -6.5F, 2F, 1F, 13F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "fond_etrave_4",
                CubeListBuilder.create()
                        .texOffs(236, 0)
                        .addBox(17F, 0F, -4F, 2F, 1F, 8F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "fond_etrave_5",
                CubeListBuilder.create()
                        .texOffs(0, 27)
                        .addBox(19F, 0F, -1.5F, 1.5F, 1F, 3F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "borde_bd",
                CubeListBuilder.create()
                        .texOffs(10, 27)
                        .addBox(-21F, -6F, 12F, 32F, 6F, 2F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "borde_td",
                CubeListBuilder.create()
                        .texOffs(78, 27)
                        .addBox(-21F, -6F, -14F, 32F, 6F, 2F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "tableau",
                CubeListBuilder.create()
                        .texOffs(146, 27)
                        .addBox(-21F, -6F, -12F, 2F, 6F, 24F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "liston_bd",
                CubeListBuilder.create()
                        .texOffs(0, 57)
                        .addBox(-21F, -7F, 11.5F, 32F, 1F, 2.5F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "liston_td",
                CubeListBuilder.create()
                        .texOffs(70, 57)
                        .addBox(-21F, -7F, -14F, 32F, 1F, 2.5F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "banquette_poupe",
                CubeListBuilder.create()
                        .texOffs(140, 57)
                        .addBox(-21F, -8F, -11.5F, 4F, 2F, 23F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "pont_avant_1",
                CubeListBuilder.create()
                        .texOffs(194, 57)
                        .addBox(15.5F, -7F, -6F, 1.5F, 1F, 12F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "pont_avant_2",
                CubeListBuilder.create()
                        .texOffs(222, 57)
                        .addBox(17F, -7F, -3.5F, 2F, 1F, 7F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "pont_avant_3",
                CubeListBuilder.create()
                        .texOffs(240, 57)
                        .addBox(19F, -7F, -2F, 1.5F, 1F, 4F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "etrave",
                CubeListBuilder.create()
                        .texOffs(0, 82)
                        .addBox(18.5F, -9F, -2.75F, 2.5F, 9F, 5.5F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "banc_milieu_ar",
                CubeListBuilder.create()
                        .texOffs(18, 82)
                        .addBox(-10F, -2F, -10F, 4F, 2F, 20F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "banc_milieu_av",
                CubeListBuilder.create()
                        .texOffs(66, 82)
                        .addBox(-1F, -2F, -10F, 4F, 2F, 20F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "banc_etrave",
                CubeListBuilder.create()
                        .texOffs(114, 82)
                        .addBox(9F, -2F, -5F, 4F, 2F, 10F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "proue_bd",
                CubeListBuilder.create()
                        .texOffs(142, 82)
                        .addBox(0F, -7F, -1F, 14.6F, 7F, 2F)
                        .texOffs(176, 82)
                        .addBox(0F, -8F, -1.5F, 14.6F, 1F, 2.5F),
                PartPose.offsetAndRotation(11F, 0F, 13F,
                        0.0F, 0.9068F, 0.0F));
        hull.addOrReplaceChild(
                "proue_td",
                CubeListBuilder.create()
                        .texOffs(212, 82)
                        .addBox(0F, -7F, -1F, 14.6F, 7F, 2F)
                        .texOffs(0, 104)
                        .addBox(0F, -8F, -1F, 14.6F, 1F, 2.5F),
                PartPose.offsetAndRotation(11F, 0F, -13F,
                        0.0F, -0.9068F, 0.0F));
        // Plan horizontal masquant l'eau à l'intérieur de la coque (même astuce que le bateau vanilla :
        // boîte plate en XY, couchée par une rotation d'un quart de tour). Rendu en masque d'eau : ses
        // UV ne sont jamais échantillonnées, seules ses dimensions comptent (intérieur x -19..11, z ±12).
        root.addOrReplaceChild(
                WATER_PATCH,
                CubeListBuilder.create().texOffs(0, 0).addBox(-19.0F, -12.0F, 0.0F, 30.0F, 24.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, -0.5F, 0.0F, ((float) Math.PI / 2F), 0.0F, 0.0F));
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    /** Le plan d'eau est rendu à part (masque), il ne fait pas partie des pièces texturées. */
    @Override
    public ImmutableList<ModelPart> parts() {
        return ImmutableList.of(hull);
    }

    public ModelPart waterPatch() {
        return waterPatch;
    }

    @Override
    public void setupAnim(Boat boat, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {}
}
