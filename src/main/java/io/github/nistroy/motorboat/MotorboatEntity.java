package io.github.nistroy.motorboat;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Bateau vanilla plus un moteur à combustible de four.
 *
 * <p>Réparti client/serveur comme le bateau vanilla : c'est le client du pilote qui simule le bateau
 * (seul {@code LocalPlayer} appelle {@link #setInput}) et envoie sa position. La poussée est donc
 * appliquée côté client, et la consommation côté serveur, qui fait autorité sur la réserve et la
 * synchronise via {@link #DATA_FUEL}.
 */
public class MotorboatEntity extends Boat
        implements HasCustomInventoryScreen, ContainerEntity, ExtendedScreenHandlerFactory<Integer> {
    /** Slot du réservoir : le moteur y pioche tout seul. */
    public static final int FUEL_SLOT = 0;

    public static final int FIRST_STORAGE_SLOT = 1;

    /** Coffre de rangement, 3 rangées de 9 comme un coffre simple. */
    public static final int STORAGE_SLOTS = 27;

    /**
     * Slot du moteur, rangé après le coffre : les index des slots existants ne bougent pas, donc les
     * barques déjà posées gardent leur contenu.
     */
    public static final int MOTOR_SLOT = FIRST_STORAGE_SLOT + STORAGE_SLOTS;

    public static final int CONTAINER_SIZE = MOTOR_SLOT + 1;

    private static final EntityDataAccessor<Integer> DATA_FUEL =
            SynchedEntityData.defineId(MotorboatEntity.class, EntityDataSerializers.INT);

    /**
     * Palier du moteur installé. Le client simule le bateau du pilote et dessine le moteur, mais le
     * contenu du conteneur ne lui est envoyé que menu ouvert : il lui faut donc le palier synchronisé.
     */
    private static final EntityDataAccessor<Integer> DATA_MOTOR =
            SynchedEntityData.defineId(MotorboatEntity.class, EntityDataSerializers.INT);

    private static final String FUEL_TAG = "Fuel";

    /** Un « pot-pot » toutes les 5 ticks : assez pour s'entendre tourner, assez peu pour ne pas saouler. */
    private static final int SOUND_INTERVAL_TICKS = 5;

    /** Vitesse² au-delà de laquelle le moteur se voit et s'entend (~1,3 bloc/s) : à quai, il se tait. */
    private static final double EFFECTS_SPEED_SQR = 0.004;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    @Nullable
    private ResourceKey<LootTable> lootTable;

    private long lootTableSeed;

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
        builder.define(DATA_MOTOR, MotorTier.NONE.id());
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

    /** Moteur installé dans le slot moteur, {@link MotorTier#NONE} si le slot est vide. */
    public MotorTier motor() {
        return MotorTier.byId(entityData.get(DATA_MOTOR));
    }

    private void setMotor(MotorTier tier) {
        if (tier != motor()) {
            entityData.set(DATA_MOTOR, tier.id());
        }
    }

    /** Grande coque : plus lourde, donc plus lente à moteur égal (voir {@link MotorboatConfig}). */
    public boolean isBigHull() {
        return false;
    }

    /** Moteur accepté dans le slot : un moteur, et qui tient sur cette coque. */
    public boolean acceptsMotor(ItemStack stack) {
        MotorTier tier = Motorboat.motorTier(stack);
        return tier != MotorTier.NONE && tier.fitsHull(isBigHull());
    }

    /** Moteur installé et de quoi brûler : le seul cas où la barque n'est pas un bateau à rames. */
    private boolean engineRunning() {
        return motor() != MotorTier.NONE && Motor.running(fuel());
    }

    @Override
    public void setInput(boolean left, boolean right, boolean up, boolean down) {
        super.setInput(left, right, up, down);
        this.throttle = up;
    }

    @Override
    public void tick() {
        if (level().isClientSide) {
            if (throttle && engineRunning() && isControlledByLocalInstance()) {
                pushForward();
            }
            if (engineRunning() && isVehicle() && getDeltaMovement().horizontalDistanceSqr() > EFFECTS_SPEED_SQR) {
                engineEffects();
            }
        } else {
            setMotor(Motorboat.motorTier(items.get(MOTOR_SLOT)));
            if (motor() != MotorTier.NONE && !Motor.running(fuel())) {
                refuelFromTank();
            }
            setFuel(Motor.burn(fuel(), motor() != MotorTier.NONE && driverPushingForward()));
        }
        super.tick();
    }

    /** Poussée du moteur, dans l'axe du bateau, comme {@code Boat.controlBoat} pour la rame. */
    private void pushForward() {
        double thrust = Thrust.extraAcceleration(Motorboat.config().topSpeed(motor(), isBigHull()));
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

    /**
     * Accroupi + clic droit : plein direct avec un combustible de four en main, interface de la barque
     * à main vide. Sans s'accroupir, comportement du bateau vanilla (on embarque).
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) {
            return super.interact(player, hand);
        }
        if (stack.isEmpty()) {
            if (!level().isClientSide) {
                openCustomInventoryScreen(player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        int burnTicks = fuelValue(stack);
        if (burnTicks <= 0) {
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

    /** Durée de combustion d'un objet dans un four, 0 s'il ne brûle pas. */
    private static int fuelValue(ItemStack stack) {
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
    }

    /**
     * Consomme un combustible du slot réservoir quand le moteur est à sec. Serveur uniquement.
     * Le contenant d'un combustible qui en a un (seau de lave → seau vide) reste dans le slot, comme
     * dans un four.
     */
    private void refuelFromTank() {
        ItemStack stack = items.get(FUEL_SLOT);
        int loaded = Motor.autoLoad(fuel(), fuelValue(stack));
        if (loaded == Motor.REFUSED) {
            return;
        }
        setFuel(loaded);
        Item remainder = stack.getItem().hasCraftingRemainingItem() ? stack.getItem().getCraftingRemainingItem() : null;
        stack.shrink(1);
        if (stack.isEmpty() && remainder != null) {
            items.set(FUEL_SLOT, new ItemStack(remainder));
        }
        level().playSound(null, this, SoundEvents.FURNACE_FIRE_CRACKLE, getSoundSource(), 0.6F, 1.0F);
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        player.openMenu(this);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel) {
            gameEvent(GameEvent.CONTAINER_OPEN, player);
        }
    }

    @Override
    public Integer getScreenOpeningData(ServerPlayer player) {
        return getId();
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        if (lootTable != null && player.isSpectator()) {
            return null;
        }
        unpackChestVehicleLootTable(playerInventory.player);
        return new MotorboatMenu(syncId, playerInventory, this);
    }

    // --- Conteneur (délégué aux défauts de ContainerEntity, comme ChestBoat) ---

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return items;
    }

    @Override
    public void clearItemStacks() {
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    }

    @Override
    public void clearContent() {
        clearChestVehicleContent();
    }

    @Override
    public ItemStack getItem(int slot) {
        return getChestVehicleItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return removeChestVehicleItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeChestVehicleItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        setChestVehicleItem(slot, stack);
    }

    @Override
    public SlotAccess getSlot(int slot) {
        return getChestVehicleSlot(slot);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return isChestVehicleStillValid(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) {
            Containers.dropContents(level(), this, this);
        }
        super.remove(reason);
    }

    @Override
    public void destroy(DamageSource source) {
        super.destroy(source);
        chestVehicleDestroyed(source, level(), this);
    }

    @Nullable
    @Override
    public ResourceKey<LootTable> getLootTable() {
        return lootTable;
    }

    @Override
    public void setLootTable(@Nullable ResourceKey<LootTable> table) {
        lootTable = table;
    }

    @Override
    public long getLootTableSeed() {
        return lootTableSeed;
    }

    @Override
    public void setLootTableSeed(long seed) {
        lootTableSeed = seed;
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
        addChestVehicleSaveData(tag, registryAccess());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFuel(Motor.clamp(tag.getInt(FUEL_TAG)));
        readChestVehicleSaveData(tag, registryAccess());
        // Le moteur n'est pas sauvé à part : il est dans le slot, relu juste au-dessus.
        setMotor(Motorboat.motorTier(items.get(MOTOR_SLOT)));
    }
}
