package io.github.nistroy.motorboat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Bateau vanilla plus un moteur à combustible de four.
 *
 * <p>Réparti client/serveur comme le bateau vanilla : c'est le client du pilote qui simule le bateau
 * (seul {@code LocalPlayer} appelle {@link #setInput}) et envoie sa position. La poussée est donc
 * appliquée côté client, et la consommation côté serveur, qui fait autorité sur la réserve et la
 * synchronise via {@link #DATA_FUEL}.
 */
public class MotorboatEntity extends Boat {
    private static final EntityDataAccessor<Integer> DATA_FUEL =
            SynchedEntityData.defineId(MotorboatEntity.class, EntityDataSerializers.INT);

    private static final String FUEL_TAG = "Fuel";

    /** Un « pot-pot » toutes les 5 ticks : assez pour s'entendre tourner, assez peu pour ne pas saouler. */
    private static final int SOUND_INTERVAL_TICKS = 5;

    /** Saisie « en avant » du pilote. Remplie côté client seulement (voir {@link #setInput}). */
    private boolean throttle;

    public MotorboatEntity(EntityType<? extends MotorboatEntity> type, Level level) {
        super(type, level);
    }

    public MotorboatEntity(Level level, double x, double y, double z) {
        this(Motorboat.MOTORBOAT_ENTITY, level);
        setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FUEL, 0);
    }

    /** Réserve restante, en ticks de marche. */
    public int fuel() {
        return entityData.get(DATA_FUEL);
    }

    private void setFuel(int fuelTicks) {
        if (fuelTicks != fuel()) {
            entityData.set(DATA_FUEL, fuelTicks);
        }
    }

    @Override
    public void setInput(boolean left, boolean right, boolean up, boolean down) {
        super.setInput(left, right, up, down);
        this.throttle = up;
    }

    @Override
    public void tick() {
        if (level().isClientSide) {
            if (throttle && Motor.running(fuel()) && isControlledByLocalInstance()) {
                pushForward();
            }
            if (Motor.running(fuel()) && isVehicle()) {
                engineEffects();
            }
        } else {
            setFuel(Motor.burn(fuel(), driverPushingForward()));
        }
        super.tick();
    }

    /** Poussée du moteur, dans l'axe du bateau, comme {@code Boat.controlBoat} pour la rame. */
    private void pushForward() {
        double thrust = Thrust.extraAcceleration(Motorboat.config().topSpeed());
        float radians = getYRot() * ((float) Math.PI / 180F);
        setDeltaMovement(getDeltaMovement().add(Mth.sin(-radians) * thrust, 0.0, Mth.cos(radians) * thrust));
    }

    /**
     * Gaz vus du serveur : il ne reçoit pas {@code setInput}, mais il a la saisie du pilote
     * ({@code zza}, via {@code ServerboundPlayerInputPacket}).
     */
    private boolean driverPushingForward() {
        return getControllingPassenger() instanceof LivingEntity driver && driver.zza > 0.0F;
    }

    private void engineEffects() {
        Vec3 stern = getViewVector(1.0F).scale(-0.9);
        double x = getX() + stern.x;
        double z = getZ() + stern.z;
        level().addParticle(ParticleTypes.BUBBLE, x, getY() - 0.1, z, 0.0, 0.0, 0.0);
        level().addParticle(ParticleTypes.SMOKE, x, getY() + 0.7, z, 0.0, 0.02, 0.0);
        if (tickCount % SOUND_INTERVAL_TICKS == 0) {
            level().playLocalSound(getX(), getY(), getZ(), SoundEvents.PISTON_CONTRACT, getSoundSource(), 0.25F, 0.7F, false);
        }
    }

    /** Accroupi + clic droit avec un combustible de four : plein. Sinon, comportement du bateau vanilla. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int burnTicks = AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
        if (!player.isSecondaryUseActive() || burnTicks <= 0) {
            return super.interact(player, hand);
        }
        int loaded = Motor.load(fuel(), burnTicks);
        if (loaded == Motor.REFUSED) {
            player.displayClientMessage(Component.translatable("motorboat.message.tank_full"), true);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!level().isClientSide) {
            setFuel(loaded);
            stack.consume(1, player);
            level().playSound(null, this, SoundEvents.FURNACE_FIRE_CRACKLE, getSoundSource(), 1.0F, 1.0F);
        }
        player.displayClientMessage(Component.translatable("motorboat.message.fuel", loaded / 20), true);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public Item getDropItem() {
        return Motorboat.MOTORBOAT_ITEM;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Motorboat.MOTORBOAT_ITEM);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(FUEL_TAG, fuel());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFuel(Motor.clamp(tag.getInt(FUEL_TAG)));
    }
}
