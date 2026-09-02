package ru.tarkmull.kitmod;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KitMod implements ModInitializer {

    public static final String MOD_ID = "kitmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Конфиг мода (config/kitmod.json). */
    public static KitConfig CONFIG;

    @Override
    public void onInitialize() {
        CONFIG = KitConfig.load();

        ModBlocks.register();
        ModItems.register();
        ModItemGroup.register();
        Mechanics.register();
        Abilities.register();
        ModCommands.register();

        LOGGER.info("KitMod загружен.");
    }
}
