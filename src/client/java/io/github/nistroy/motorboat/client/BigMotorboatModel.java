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
 * Coque de la grande barque : caisse ouverte de 36 × 28 px (2,25 × 1,75 bloc), proue en +X.
 *
 * <p>Repère du modèle : après les transformations du rendu de bateau, +X pointe vers la proue et
 * **-Y vers le haut** ({@code scale(-1, -1, 1)}), d'où des parois de {@code y = -6} à {@code y = 0}.
 * Les mêmes nombres sont repris dans {@code tools/generate_textures.py} : changer l'un = changer l'autre.
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
                "bottom",
                CubeListBuilder.create().texOffs(0, 0).addBox(-18.0F, 0.0F, -14.0F, 36.0F, 1.0F, 28.0F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "port",
                CubeListBuilder.create().texOffs(0, 32).addBox(-18.0F, -6.0F, 13.0F, 36.0F, 6.0F, 1.0F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "starboard",
                CubeListBuilder.create().texOffs(0, 40).addBox(-18.0F, -6.0F, -14.0F, 36.0F, 6.0F, 1.0F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "bow",
                CubeListBuilder.create().texOffs(0, 48).addBox(17.0F, -6.0F, -13.0F, 1.0F, 6.0F, 26.0F),
                PartPose.ZERO);
        hull.addOrReplaceChild(
                "stern",
                CubeListBuilder.create().texOffs(64, 48).addBox(-18.0F, -6.0F, -13.0F, 1.0F, 6.0F, 26.0F),
                PartPose.ZERO);
        // Plan horizontal masquant l'eau à l'intérieur de la coque (même astuce que le bateau vanilla :
        // boîte plate en XY, couchée par une rotation d'un quart de tour).
        root.addOrReplaceChild(
                WATER_PATCH,
                CubeListBuilder.create().texOffs(128, 0).addBox(-17.0F, -13.0F, 0.0F, 34.0F, 26.0F, 0.0F),
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
