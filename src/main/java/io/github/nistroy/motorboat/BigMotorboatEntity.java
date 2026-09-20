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
 * <p>Places assises : trois rangs de deux, le pilote (premier passager) à l'avant gauche. Repère
 * des points d'attache, relevé au {@code javap} sur {@code Boat.getPassengerAttachmentPoint} (1.21.1) :
 * {@code new Vec3(travers, hauteur, avant).yRot(-yRot)} — X en travers, **Z vers la proue**.
 */
public class BigMotorboatEntity extends MotorboatEntity {
    /** Six places : trois rangs (proue, milieu, poupe) de deux. */
    public static final int MAX_PASSENGERS = 6;

    private static final double[] ROW_OFFSETS = {0.7, 0.0, -0.7};

    private static final double SEAT_OFFSET = 0.4;

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
    protected int getMaxPassengers() {
        return MAX_PASSENGERS;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        int seat = Math.max(0, getPassengers().indexOf(passenger));
        double across = seat % 2 == 0 ? -SEAT_OFFSET : SEAT_OFFSET;
        double along = ROW_OFFSETS[Math.min(seat / 2, ROW_OFFSETS.length - 1)];
        return new Vec3(across, dimensions.height() / 3.0, along).yRot(-getYRot() * ((float) Math.PI / 180F));
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
