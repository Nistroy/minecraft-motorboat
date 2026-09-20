package io.github.nistroy.motorboat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.nistroy.motorboat.Motorboat;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import org.joml.Quaternionf;

/**
 * Coque du bateau vanilla (héritée de {@code BoatRenderer}) plus le bloc moteur à la poupe.
 *
 * <p>Le moteur est dessiné dans le même repère que la coque : les transformations reprennent, dans
 * l'ordre, celles de {@code BoatRenderer.render} (relevées au javap sur 1.21.1), secousse de dégâts et
 * colonne à bulles comprises, sinon le moteur se décrocherait visuellement quand la barque encaisse.
 */
public class MotorboatRenderer extends BoatRenderer {
    public static final ModelLayerLocation ENGINE_LAYER = new ModelLayerLocation(Motorboat.id("motorboat"), "engine");

    private static final ResourceLocation ENGINE_TEXTURE = Motorboat.id("textures/entity/motor.png");

    private final ModelPart engine;

    public MotorboatRenderer(EntityRendererProvider.Context context) {
        super(context, false);
        this.engine = context.bakeLayer(ENGINE_LAYER);
    }

    /** Bloc moteur (4×7×5) posé sur le pont arrière, plus son échappement (2×4×2). */
    public static LayerDefinition createEngineLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "block",
                CubeListBuilder.create().texOffs(0, 0).addBox(-15.0F, -6.0F, -2.5F, 4.0F, 7.0F, 5.0F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "pipe",
                CubeListBuilder.create().texOffs(0, 13).addBox(-14.0F, -10.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void render(Boat boat, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(boat, yaw, partialTicks, pose, buffers, light);
        pose.pushPose();
        pose.translate(0.0F, 0.375F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        float hurtTime = (float) boat.getHurtTime() - partialTicks;
        float damage = Math.max(0.0F, boat.getDamage() - partialTicks);
        if (hurtTime > 0.0F) {
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurtTime) * hurtTime * damage / 10.0F * (float) boat.getHurtDir()));
        }
        float bubbleAngle = boat.getBubbleAngle(partialTicks);
        if (!Mth.equal(bubbleAngle, 0.0F)) {
            pose.mulPose(new Quaternionf().setAngleAxis(bubbleAngle * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
        }
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        engine.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(ENGINE_TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
