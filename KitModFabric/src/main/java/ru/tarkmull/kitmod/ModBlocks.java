package ru.tarkmull.kitmod;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

/** Блоки мода: шаблон для лавы и он же с лавой. */
public final class ModBlocks {

    private ModBlocks() {
    }

    public static final Block LAVA_MOLD = register("lava_mold", Block::new,
            AbstractBlock.Settings.create()
                    .strength(3.0f, 9.0f)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.STONE),
            true);

    public static final Block LAVA_MOLD_FILLED = register("lava_mold_filled", Block::new,
            AbstractBlock.Settings.create()
                    .strength(3.0f, 9.0f)
                    .requiresTool()
                    .nonOpaque()
                    .luminance(state -> 15)
                    .sounds(BlockSoundGroup.STONE),
            true);

    // ------------------------------------------------------------------
    private static Block register(String name, Function<AbstractBlock.Settings, Block> factory,
                                  AbstractBlock.Settings settings, boolean withItem) {
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(KitMod.MOD_ID, name));
        Block block = factory.apply(settings.registryKey(blockKey));

        if (withItem) {
            RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(KitMod.MOD_ID, name));
            BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey));
            Registry.register(Registries.ITEM, itemKey, blockItem);
        }

        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    /** Принудительная загрузка класса. */
    public static void register() {
        KitMod.LOGGER.info("Регистрация блоков KitMod");
    }
}
