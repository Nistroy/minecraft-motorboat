package io.github.nistroy.motorboat;

import java.io.IOException;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Point d'entrée commun : registres et config. Le rendu est dans le source set client. */
public final class Motorboat implements ModInitializer {
    public static final String MOD_ID = "motorboat";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Onglet créatif vanilla « Outils et utilitaires » : la clé n'est pas publique côté Minecraft. */
    private static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace("tools_and_utilities"));

    /** Mêmes dimensions et portée de suivi que {@code EntityType.BOAT}. */
    public static final EntityType<MotorboatEntity> MOTORBOAT_ENTITY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("motorboat"),
            EntityType.Builder.<MotorboatEntity>of(MotorboatEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .clientTrackingRange(10)
                    .build("motorboat"));

    public static final Item MOTOR = Registry.register(BuiltInRegistries.ITEM, id("motor"), new Item(new Item.Properties()));

    public static final Item MOTORBOAT_ITEM = Registry.register(
            BuiltInRegistries.ITEM, id("motorboat"), new MotorboatItem(new Item.Properties().stacksTo(1)));

    /**
     * Menu de la barque. {@code MenuType.<init>} est privé côté Minecraft : on passe par le type
     * étendu de Fabric, qui transmet en plus l'id de l'entité au client (jauge de carburant).
     */
    public static final MenuType<MotorboatMenu> MOTORBOAT_MENU = Registry.register(
            BuiltInRegistries.MENU,
            id("motorboat"),
            new ExtendedScreenHandlerType<>(
                    (syncId, inventory, entityId) ->
                            new MotorboatMenu(syncId, inventory, MotorboatMenu.resolve(inventory, entityId)),
                    ByteBufCodecs.VAR_INT));

    private static MotorboatConfig config = new MotorboatConfig(16.0);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Config chargée au démarrage ; défauts si le fichier est absent ou illisible. */
    public static MotorboatConfig config() {
        return config;
    }

    @Override
    public void onInitialize() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
        try {
            config = MotorboatConfig.load(file);
        } catch (IOException | IllegalArgumentException e) {
            // Une config cassée ne doit pas empêcher le serveur de démarrer : on garde les défauts.
            LOGGER.warn("config {} ignorée ({}), valeurs par défaut", file, e.getMessage());
        }
        ItemGroupEvents.modifyEntriesEvent(TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(MOTOR);
            entries.accept(MOTORBOAT_ITEM);
        });
    }
}
