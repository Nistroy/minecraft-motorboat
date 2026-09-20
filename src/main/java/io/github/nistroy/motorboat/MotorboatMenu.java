package io.github.nistroy.motorboat;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Menu d'une barque : slot carburant (n'accepte qu'un combustible de four), slot moteur (un moteur
 * qui tient sur cette coque) et un coffre de 27.
 *
 * <p>Côté client, le menu est reconstruit à partir de l'id de l'entité transmis à l'ouverture
 * (voir {@code ExtendedScreenHandlerType} dans {@link Motorboat}) : la barque cliente existe déjà et
 * sert directement de {@link Container}, ce qui donne aussi la réserve pour la jauge. Si elle n'est
 * pas (encore) chargée, on retombe sur un conteneur vide pour ne pas planter l'écran.
 */
public class MotorboatMenu extends AbstractContainerMenu {
    /** Disposition de l'écran, en pixels de la texture de GUI (voir {@code MotorboatScreen}). */
    public static final int FUEL_SLOT_X = 8;

    public static final int FUEL_SLOT_Y = 18;

    public static final int MOTOR_SLOT_X = 52;

    public static final int MOTOR_SLOT_Y = 18;

    private static final int STORAGE_TOP_Y = 40;

    private static final int PLAYER_TOP_Y = 107;

    private static final int HOTBAR_Y = 165;

    private static final int SLOT_SIZE = 18;

    private static final int COLUMNS = 9;

    /** Index des slots dans le menu (≠ index dans le conteneur : le moteur y est rangé en dernier). */
    private static final int MENU_FUEL = 0;

    private static final int MENU_MOTOR = 1;

    private static final int MENU_FIRST_STORAGE = 2;

    private static final int MENU_PLAYER_START = MENU_FIRST_STORAGE + MotorboatEntity.STORAGE_SLOTS;

    private final Container container;

    @Nullable
    private final MotorboatEntity boat;

    /**
     * @param boat barque servant de conteneur ; {@code null} côté client tant qu'elle n'est pas
     *     chargée, auquel cas le menu s'ouvre sur un conteneur vide plutôt que de planter.
     */
    public MotorboatMenu(int syncId, Inventory playerInventory, @Nullable MotorboatEntity boat) {
        super(Motorboat.MOTORBOAT_MENU, syncId);
        this.boat = boat;
        this.container = boat != null ? boat : new SimpleContainer(MotorboatEntity.CONTAINER_SIZE);
        checkContainerSize(container, MotorboatEntity.CONTAINER_SIZE);
        container.startOpen(playerInventory.player);

        addSlot(new FuelSlot(container, MotorboatEntity.FUEL_SLOT, FUEL_SLOT_X, FUEL_SLOT_Y));
        addSlot(new MotorSlot(container, MotorboatEntity.MOTOR_SLOT, MOTOR_SLOT_X, MOTOR_SLOT_Y, boat));
        for (int row = 0; row < MotorboatEntity.STORAGE_SLOTS / COLUMNS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(
                        container,
                        MotorboatEntity.FIRST_STORAGE_SLOT + row * COLUMNS + column,
                        8 + column * SLOT_SIZE,
                        STORAGE_TOP_Y + row * SLOT_SIZE));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(
                        playerInventory,
                        COLUMNS + row * COLUMNS + column,
                        8 + column * SLOT_SIZE,
                        PLAYER_TOP_Y + row * SLOT_SIZE));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * SLOT_SIZE, HOTBAR_Y));
        }
    }

    /** Barque cliente correspondant à l'id transmis à l'ouverture du menu, ou {@code null}. */
    @Nullable
    public static MotorboatEntity resolve(Inventory playerInventory, int entityId) {
        Entity entity = playerInventory.player.level().getEntity(entityId);
        return entity instanceof MotorboatEntity motorboat ? motorboat : null;
    }

    /** Réserve à afficher dans la jauge, en ticks ; 0 si la barque n'est pas chargée côté client. */
    public int fuel() {
        return boat != null ? boat.fuel() : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MENU_PLAYER_START) {
            if (!moveItemStackTo(stack, MENU_PLAYER_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (AbstractFurnaceBlockEntity.isFuel(stack) && moveItemStackTo(stack, MENU_FUEL, MENU_FUEL + 1, false)) {
            // rien de plus : le combustible part en priorité dans le réservoir
        } else if (Motorboat.motorTier(stack) != MotorTier.NONE
                && moveItemStackTo(stack, MENU_MOTOR, MENU_MOTOR + 1, false)) {
            // idem pour un moteur : le slot moteur d'abord (il refuse ce qui ne tient pas sur la coque)
        } else if (!moveItemStackTo(stack, MENU_FIRST_STORAGE, MENU_PLAYER_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /**
     * N'accepte qu'un moteur qui tient sur cette coque. Côté client la barque peut ne pas être
     * chargée : on accepte alors tous les moteurs, le serveur fait autorité et renverra le refus.
     */
    private static class MotorSlot extends Slot {
        @Nullable
        private final MotorboatEntity boat;

        MotorSlot(Container container, int index, int x, int y, @Nullable MotorboatEntity boat) {
            super(container, index, x, y);
            this.boat = boat;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return boat != null ? boat.acceptsMotor(stack) : Motorboat.motorTier(stack) != MotorTier.NONE;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** N'accepte que ce qui brûle dans un four. */
    private static class FuelSlot extends Slot {
        FuelSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return AbstractFurnaceBlockEntity.isFuel(stack);
        }
    }
}
