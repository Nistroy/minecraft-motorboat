package io.github.nistroy.motorboat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.nistroy.motorboat.MotorTier;
import io.github.nistroy.motorboat.Motorboat;
import io.github.nistroy.motorboat.MotorboatEntity;
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

    public static final ModelLayerLocation BIG_ENGINE_LAYER =
            new ModelLayerLocation(Motorboat.id("motorboat"), "big_engine");

    private static final ResourceLocation ENGINE_TEXTURE = Motorboat.id("textures/entity/motor.png");

    private static final ResourceLocation BIG_ENGINE_TEXTURE = Motorboat.id("textures/entity/big_motor.png");

    /** Le double moteur, c'est deux hors-bord de base en travers ; capot de 6 de large, 2 d'écart. */
    private static final float[] DOUBLE_OFFSETS = {-4.0F, 4.0F};

    /** Accrochage sur la barque 2 places : arête haute du tableau arrière vanilla (javap 1.21.1). */
    public static final Mount VANILLA_MOUNT = new Mount(-16.0F, -3.0F);

    private final ModelPart engine;

    private final ModelPart bigEngine;

    public MotorboatRenderer(EntityRendererProvider.Context context) {
        super(context, false);
        this.engine = context.bakeLayer(ENGINE_LAYER);
        this.bigEngine = context.bakeLayer(BIG_ENGINE_LAYER);
    }

    /**
     * Point d'accrochage du moteur sur une coque, en unités de modèle : arête haute du tableau
     * arrière, côté extérieur. Le modèle de moteur est dessiné autour de cette origine.
     */
    public record Mount(float x, float y) {}

    /**
     * Hors-bord de base : chape à cheval sur le tableau, capot, arbre, embase et hélice sous la
     * flottaison, barre franche vers le pilote. Traduit de {@code tools/art/motor.bbmodel} —
     * ne pas retoucher les boîtes ici, retoucher la maquette (voir {@code PLAN.md}).
     */
    public static LayerDefinition createEngineLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "chape",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2F, -1F, -2F, 3F, 2F, 4F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "capot",
                CubeListBuilder.create()
                        .texOffs(14, 0)
                        .addBox(-7F, -6F, -3F, 5F, 6F, 6F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "echappement",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .addBox(-5F, -8F, -1F, 2F, 2F, 2F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "arbre",
                CubeListBuilder.create()
                        .texOffs(44, 0)
                        .addBox(-5F, 0F, -1F, 2F, 11F, 2F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "embase",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(-6F, 11F, -1.5F, 4F, 2F, 3F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "helice_v",
                CubeListBuilder.create()
                        .texOffs(14, 13)
                        .addBox(-2F, 10F, -0.5F, 1F, 4F, 1F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "helice_h",
                CubeListBuilder.create()
                        .texOffs(18, 13)
                        .addBox(-2F, 11.5F, -2.5F, 1F, 1F, 5F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "barre",
                CubeListBuilder.create()
                        .texOffs(30, 13)
                        .addBox(-2F, -2F, -0.5F, 7F, 1F, 1F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Gros hors-bord, même dessin en plus costaud, traduit de {@code tools/art/big_motor.bbmodel}. */
    public static LayerDefinition createBigEngineLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "chape",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2F, -1F, -2.5F, 3F, 2F, 5F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "capot",
                CubeListBuilder.create()
                        .texOffs(16, 0)
                        .addBox(-9F, -8F, -3.5F, 7F, 8F, 7F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "echappement_bd",
                CubeListBuilder.create()
                        .texOffs(44, 0)
                        .addBox(-6F, -10F, 1F, 2F, 2F, 2F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "echappement_td",
                CubeListBuilder.create()
                        .texOffs(52, 0)
                        .addBox(-6F, -10F, -3F, 2F, 2F, 2F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "arbre",
                CubeListBuilder.create()
                        .texOffs(0, 15)
                        .addBox(-6F, 0F, -1.5F, 2F, 11F, 3F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "embase",
                CubeListBuilder.create()
                        .texOffs(10, 15)
                        .addBox(-7F, 11F, -2F, 4F, 2F, 4F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "helice_v",
                CubeListBuilder.create()
                        .texOffs(26, 15)
                        .addBox(-3F, 9F, -0.5F, 1F, 6F, 1F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "helice_h",
                CubeListBuilder.create()
                        .texOffs(30, 15)
                        .addBox(-3F, 11.5F, -3F, 1F, 1F, 6F),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "barre",
                CubeListBuilder.create()
                        .texOffs(44, 15)
                        .addBox(-2F, -2F, -0.5F, 7F, 1F, 1F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(Boat boat, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(boat, yaw, partialTicks, pose, buffers, light);
        pose.pushPose();
        applyBoatPose(pose, boat, yaw, partialTicks);
        renderEngine(engine, bigEngine, VANILLA_MOUNT, pose, buffers, light, motorOf(boat));
        pose.popPose();
    }

    /**
     * Repère du modèle de bateau : reprend, dans l'ordre, les transformations de
     * {@code BoatRenderer.render} (relevées au javap sur 1.21.1), secousse de dégâts et colonne à
     * bulles comprises. Partagé avec la grande barque pour que les deux coques et le moteur restent
     * alignés.
     */
    public static void applyBoatPose(PoseStack pose, Boat boat, float yaw, float partialTicks) {
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
    }

    /** Moteur installé sur cette barque, {@link MotorTier#NONE} pour un bateau vanilla. */
    public static MotorTier motorOf(Boat boat) {
        return boat instanceof MotorboatEntity motorboat ? motorboat.motor() : MotorTier.NONE;
    }

    /**
     * Dessine le moteur, dans le repère posé par {@link #applyBoatPose} : rien sans moteur, le
     * hors-bord de base, le gros hors-bord, et deux hors-bord de base en travers pour le double.
     * Unités du modèle : 1/16 de bloc, {@code ModelPart} divise les coordonnées des boîtes par 16.
     *
     * <p>Le modèle est dessiné autour de son point d'accrochage : chaque coque passe le sien, c'est
     * ce qui permet aux deux barques de partager un seul moteur.
     */
    public static void renderEngine(
            ModelPart engine,
            ModelPart bigEngine,
            Mount mount,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            MotorTier motor) {
        if (motor == MotorTier.NONE) {
            return;
        }
        pose.pushPose();
        pose.translate(mount.x() / 16.0F, mount.y() / 16.0F, 0.0F);
        switch (motor) {
            case BASIC -> drawEngine(engine, ENGINE_TEXTURE, pose, buffers, light);
            case BIG -> drawEngine(bigEngine, BIG_ENGINE_TEXTURE, pose, buffers, light);
            case DOUBLE -> {
                for (float across : DOUBLE_OFFSETS) {
                    pose.pushPose();
                    pose.translate(0.0F, 0.0F, across / 16.0F);
                    drawEngine(engine, ENGINE_TEXTURE, pose, buffers, light);
                    pose.popPose();
                }
            }
            default -> {}
        }
        pose.popPose();
    }

    private static void drawEngine(
            ModelPart engine, ResourceLocation texture, PoseStack pose, MultiBufferSource buffers, int light) {
        engine.render(
                pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
    }
}
