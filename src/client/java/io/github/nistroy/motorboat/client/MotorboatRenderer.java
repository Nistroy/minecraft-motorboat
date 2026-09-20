package io.github.nistroy.motorboat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.nistroy.motorboat.MotorTier;
import io.github.nistroy.motorboat.Motorboat;
import io.github.nistroy.motorboat.MotorboatEntity;
import io.github.nistroy.motorboat.Wave;
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
import net.minecraft.tags.FluidTags;
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

    /** Hauteur d'eau (blocs) au-delà de laquelle la coque est pleinement portée : 2 px suffisent. */
    private static final double AFLOAT_DEPTH = 0.125;

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
        // La coque de cette barque-là est dessinée par BoatRenderer, dans son propre repère : la
        // houle doit l'envelopper, sinon le moteur tangue tout seul et se décroche du tableau.
        pose.pushPose();
        applyWave(pose, boat, yaw, partialTicks);
        super.render(boat, yaw, partialTicks, pose, buffers, light);
        pose.popPose();
        pose.pushPose();
        applyBoatPose(pose, boat, yaw, partialTicks);
        renderEngine(engine, bigEngine, VANILLA_MOUNT, pose, buffers, light, motorOf(boat));
        pose.popPose();
    }

    /**
     * Houle : incline la coque comme si elle naviguait sur des vagues, étrave qui sautille et nez
     * qui se lève en vitesse. Angles et amplitudes dans {@link Wave}, qui dit aussi pourquoi ils
     * restent petits. Purement visuel : rien ne bouge côté entité, donc rien à synchroniser.
     *
     * <p>À poser là où le repère est encore aligné sur le monde et centré sur l'entité — le pivot
     * tombe alors à la flottaison, au milieu de la coque. Le lacet sert à entrer dans le repère de
     * la barque (tangage = X, roulis = Z, l'étrave est en −Z avant le retournement de
     * {@link #applyBoatPose}) puis à en ressortir : l'appelant retrouve son repère intact.
     *
     * <p>Rien hors de l'eau : à terre ou en vol, la coque reste droite.
     */
    public static void applyWave(PoseStack pose, Boat boat, float yaw, float partialTicks) {
        float afloat = afloat(boat);
        if (afloat <= 0.0F) {
            return;
        }
        // Vitesse prise sur le déplacement du dernier tick : getDeltaMovement() vaut ~0 sur les
        // barques des autres joueurs, que le client interpole au lieu de les simuler.
        double speed = Math.hypot(boat.getX() - boat.xOld, boat.getZ() - boat.zOld);
        Wave.Motion motion = Wave.at(
                boat.getX(partialTicks),
                boat.getZ(partialTicks),
                (double) boat.level().getGameTime() + partialTicks,
                speed);
        pose.translate(0.0, motion.heaveBlocks() * afloat, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        pose.mulPose(Axis.XP.rotationDegrees(motion.pitchDegrees() * afloat));
        pose.mulPose(Axis.ZP.rotationDegrees(motion.rollDegrees() * afloat));
        pose.mulPose(Axis.YP.rotationDegrees(yaw - 180.0F));
    }

    /**
     * À quel point la coque est portée par l'eau, de 0 (à sec) à 1 (à flot). **Un fondu, pas un
     * interrupteur** : {@code isInWater()} suit un drapeau qui peut basculer d'un tick à l'autre sur
     * une coque qui flotte au ras de la surface, et une houle qu'on allume et éteint 10 fois par
     * seconde se voit comme un tremblement. La hauteur d'eau, elle, varie continûment.
     */
    private static float afloat(Boat boat) {
        double submerged = boat.getFluidHeight(FluidTags.WATER);
        if (boat.isInWater()) {
            submerged = Math.max(submerged, AFLOAT_DEPTH);
        }
        return (float) Mth.clamp(submerged / AFLOAT_DEPTH, 0.0, 1.0);
    }

    /**
     * Repère du modèle de bateau : reprend, dans l'ordre, les transformations de
     * {@code BoatRenderer.render} (relevées au javap sur 1.21.1), secousse de dégâts et colonne à
     * bulles comprises, précédées de la houle ({@link #applyWave}). Partagé avec la grande barque
     * pour que les deux coques et le moteur restent alignés.
     */
    public static void applyBoatPose(PoseStack pose, Boat boat, float yaw, float partialTicks) {
        applyWave(pose, boat, yaw, partialTicks);
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
