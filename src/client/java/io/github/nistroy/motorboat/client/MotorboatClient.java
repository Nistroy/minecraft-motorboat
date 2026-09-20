package io.github.nistroy.motorboat.client;

import io.github.nistroy.motorboat.Motorboat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public final class MotorboatClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(MotorboatRenderer.ENGINE_LAYER, MotorboatRenderer::createEngineLayer);
        EntityModelLayerRegistry.registerModelLayer(
                MotorboatRenderer.BIG_ENGINE_LAYER, MotorboatRenderer::createBigEngineLayer);
        EntityRendererRegistry.register(Motorboat.MOTORBOAT_ENTITY, MotorboatRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(BigMotorboatRenderer.HULL_LAYER, BigMotorboatModel::createBodyModel);
        EntityRendererRegistry.register(Motorboat.BIG_MOTORBOAT_ENTITY, BigMotorboatRenderer::new);
        MenuScreens.register(Motorboat.MOTORBOAT_MENU, MotorboatScreen::new);
    }
}
