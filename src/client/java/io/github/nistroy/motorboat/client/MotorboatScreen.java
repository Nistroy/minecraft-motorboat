package io.github.nistroy.motorboat.client;

import io.github.nistroy.motorboat.Motor;
import io.github.nistroy.motorboat.Motorboat;
import io.github.nistroy.motorboat.MotorboatMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Écran du menu de la barque : slot carburant, jauge de réserve et coffre. */
public class MotorboatScreen extends AbstractContainerScreen<MotorboatMenu> {
    private static final ResourceLocation TEXTURE = Motorboat.id("textures/gui/container/motorboat.png");

    /** Jauge : position dans le panneau, et sprite allumé rangé à droite (u = 176, v = 0). */
    private static final int FLAME_X = 30;

    private static final int FLAME_Y = 19;

    private static final int FLAME_SIZE = 14;

    public MotorboatScreen(MotorboatMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 190;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int lit = litHeight();
        if (lit > 0) {
            graphics.blit(
                    TEXTURE,
                    leftPos + FLAME_X,
                    topPos + FLAME_Y + FLAME_SIZE - lit,
                    imageWidth,
                    FLAME_SIZE - lit,
                    FLAME_SIZE,
                    lit);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isOverFlame(mouseX, mouseY)) {
            graphics.renderTooltip(
                    font, Component.translatable("motorboat.message.fuel", menu.fuel() / 20), mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    /** Hauteur allumée de la flamme, proportionnelle à la réserve. */
    private int litHeight() {
        return menu.fuel() * FLAME_SIZE / Motor.MAX_FUEL_TICKS;
    }

    private boolean isOverFlame(int mouseX, int mouseY) {
        int x = mouseX - leftPos - FLAME_X;
        int y = mouseY - topPos - FLAME_Y;
        return x >= 0 && x < FLAME_SIZE && y >= 0 && y < FLAME_SIZE;
    }
}
