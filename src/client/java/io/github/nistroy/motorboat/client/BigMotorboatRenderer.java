package io.github.nistroy.motorboat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.nistroy.motorboat.Motorboat;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

/**
 * Rendu de la grande barque : coque maison plus le même bloc moteur que la barque 2 places.
 *
 * <p>Reprend la séquence de {@code BoatRenderer.render} (repère commun dans
 * {@link MotorboatRenderer#applyBoatPose}) : pas de rames, la grande barque n'en a pas.
 */
public class BigMotorboatRenderer extends EntityRenderer<Boat> {
    public static final ModelLayerLocation HULL_LAYER =
            new ModelLayerLocation(Motorboat.id("big_motorboat"), "main");

    private static final ResourceLocation HULL_TEXTURE = Motorboat.id("textures/entity/big_motorboat.png");

    /**
     * Accrochage du moteur sur la grande coque : arête haute du tableau arrière, face extérieure —
     * x = −24 (bordé), y = −8 (dessus de la banquette de poupe, {@code y_bb} 9). Le hors-bord pend
     * donc derrière le tableau, dans le dos du pilote, barre franche vers l'avant.
     */
    private static final MotorboatRenderer.Mount MOUNT = new MotorboatRenderer.Mount(-24.0F, -8.0F);

    private final BigMotorboatModel model;

    private final ModelPart engine;

    private final ModelPart bigEngine;

    public BigMotorboatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.25F;
        this.model = new BigMotorboatModel(context.bakeLayer(HULL_LAYER));
        this.engine = context.bakeLayer(MotorboatRenderer.ENGINE_LAYER);
        this.bigEngine = context.bakeLayer(MotorboatRenderer.BIG_ENGINE_LAYER);
    }

    @Override
    public void render(Boat boat, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        MotorboatRenderer.applyBoatPose(pose, boat, yaw, partialTicks);
        model.setupAnim(boat, partialTicks, 0.0F, -0.1F, 0.0F, 0.0F);
        VertexConsumer hull = buffers.getBuffer(model.renderType(HULL_TEXTURE));
        model.renderToBuffer(pose, hull, light, OverlayTexture.NO_OVERLAY, -1);
        if (!boat.isUnderWater()) {
            model.waterPatch().render(pose, buffers.getBuffer(RenderType.waterMask()), light, OverlayTexture.NO_OVERLAY);
        }
        MotorboatRenderer.renderEngine(
                engine, bigEngine, MOUNT, pose, buffers, light, MotorboatRenderer.motorOf(boat));
        pose.popPose();
        super.render(boat, yaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Boat boat) {
        return HULL_TEXTURE;
    }
}
