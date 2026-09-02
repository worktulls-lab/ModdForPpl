package ru.tarkmull.kitmod;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Вкладка "KitMod" в творческом режиме. */
public final class ModItemGroup {

    private ModItemGroup() {
    }

    public static final RegistryKey<ItemGroup> KEY =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(KitMod.MOD_ID, "main"));

    public static void register() {
        Registry.register(Registries.ITEM_GROUP, KEY, FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.PIZZA))
                .displayName(Text.translatable("itemGroup.kitmod.main"))
                .build());

        ItemGroupEvents.modifyEntriesEvent(KEY).register(entries -> {
            entries.add(ModItems.PIZZA);
            entries.add(ModItems.PIZZA_SWORD);
            entries.add(ModItems.LEVIATHAN);
            entries.add(ModItems.VAMPIRE_SCYTHE);
            entries.add(ModItems.LEGENDARY_SWORD);
            entries.add(ModBlocks.LAVA_MOLD);
            entries.add(ModBlocks.LAVA_MOLD_FILLED);
            entries.add(ModItems.LAVA_CRYSTAL);
            entries.add(ModItems.REINFORCED_STRING);
            entries.add(ModItems.DRAGON_INGOT);
            entries.add(ModItems.ENDER_INGOT);
            entries.add(ModItems.BLOOD_INGOT);
            entries.add(ModItems.LAVA_INGOT);
            entries.add(ModItems.RUBY_DIAMOND);
            entries.add(ModItems.RARE_GLOVE);
            entries.add(ModItems.STEEL);
            entries.add(ModItems.MASTER_REDSTONE);
        });
    }
}
