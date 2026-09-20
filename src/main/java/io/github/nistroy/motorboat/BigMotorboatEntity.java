package io.github.nistroy.motorboat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Grande barque à moteur : même moteur et même réservoir, coque plus longue et six places.
 *
 * <p>Places assises : voir {@link SeatPlan} — le pilote (premier passager) est sur la banquette de
 * poupe, devant le moteur. Repère des points d'attache, relevé au {@code javap} sur
 * {@code Boat.getPassengerAttachmentPoint} (1.21.1) : {@code new Vec3(travers, hauteur, avant).yRot(-yRot)}
 * — X en travers, **Z vers la proue**.
 */
public class BigMotorboatEntity extends MotorboatEntity {
    /** Six places : la barre, deux rangs de deux et le banc d'étrave ({@link SeatPlan}). */
    public static final int MAX_PASSENGERS = 6;

    public BigMotorboatEntity(EntityType<? extends BigMotorboatEntity> type, Level level) {
        super(type, level);
    }

    public BigMotorboatEntity(Level level, double x, double y, double z) {
        this(Motorboat.BIG_MOTORBOAT_ENTITY, level);
        setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public boolean isBigHull() {
        return true;
    }

    @Override
    protected int getMaxPassengers() {
        return MAX_PASSENGERS;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        SeatPlan.Seat seat = SeatPlan.seat(getPassengers().indexOf(passenger));
        return new Vec3(seat.across(), seat.height(), seat.along())
                .yRot(-getYRot() * ((float) Math.PI / 180F));
    }

    @Override
    public Item getDropItem() {
        return Motorboat.BIG_MOTORBOAT_ITEM;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Motorboat.BIG_MOTORBOAT_ITEM);
    }
}
