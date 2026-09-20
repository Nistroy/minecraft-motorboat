package io.github.nistroy.motorboat.client;

import io.github.nistroy.motorboat.Motorboat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class MotorboatClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(MotorboatRenderer.ENGINE_LAYER, MotorboatRenderer::createEngineLayer);
        EntityRendererRegistry.register(Motorboat.MOTORBOAT_ENTITY, MotorboatRenderer::new);
    }
}
