package ru.tarkmull.kitmod;

import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.function.Function;

/** Все предметы мода. */
public final class ModItems {

    private ModItems() {
    }

    // --- с механиками ---
    public static final Item PIZZA = register("pizza", Item::new,
            new Item.Settings()
                    .maxCount(16)
                    .food(new FoodComponent.Builder()
                            .nutrition(KitMod.CONFIG.pizzaNutrition)
                            .saturationModifier(KitMod.CONFIG.pizzaSaturation)
                            .build()));

    public static final Item PIZZA_SWORD = register("pizza_sword", Item::new,
            weapon("pizza_sword", KitMod.CONFIG.pizzaSwordDamage, KitMod.CONFIG.pizzaSwordSpeed));

    public static final Item LEVIATHAN = register("leviathan", Item::new,
            weapon("leviathan", KitMod.CONFIG.leviathanDamage, KitMod.CONFIG.leviathanSpeedAttr));

    public static final Item VAMPIRE_SCYTHE = register("vampire_scythe", Item::new,
            weapon("vampire_scythe", KitMod.CONFIG.scytheDamage, KitMod.CONFIG.scytheSpeed));

    // --- декоративные ---
    public static final Item LEGENDARY_SWORD = register("legendary_sword", Item::new,
            weapon("legendary_sword", KitMod.CONFIG.legendarySwordDamage, KitMod.CONFIG.legendarySwordSpeed));

    public static final Item LAVA_CRYSTAL = register("lava_crystal", Item::new, plain(Rarity.RARE));
    public static final Item REINFORCED_STRING = register("reinforced_string", Item::new, plain(Rarity.COMMON));
    public static final Item DRAGON_INGOT = register("dragon_ingot", Item::new, plain(Rarity.EPIC));
    public static final Item ENDER_INGOT = register("ender_ingot", Item::new, plain(Rarity.RARE));
    public static final Item BLOOD_INGOT = register("blood_ingot", Item::new, plain(Rarity.RARE));
    public static final Item LAVA_INGOT = register("lava_ingot", Item::new, plain(Rarity.RARE));
    public static final Item RUBY_DIAMOND = register("ruby_diamond", Item::new, plain(Rarity.EPIC));
    public static final Item RARE_GLOVE = register("rare_glove", Item::new, plain(Rarity.RARE));
    public static final Item STEEL = register("steel", Item::new, plain(Rarity.COMMON));
    public static final Item MASTER_REDSTONE = register("master_redstone", Item::new, plain(Rarity.RARE));

    // ------------------------------------------------------------------
    private static Item.Settings plain(Rarity rarity) {
        return new Item.Settings().rarity(rarity);
    }

    /** Настройки оружия: урон и скорость атаки через компонент модификаторов. */
    private static Item.Settings weapon(String name, double damage, double speed) {
        return new Item.Settings()
                .maxCount(1)
                .rarity(Rarity.EPIC)
                .attributeModifiers(AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ATTACK_DAMAGE,
                                new EntityAttributeModifier(
                                        Identifier.of(KitMod.MOD_ID, name + "_damage"),
                                        damage - 1.0,
                                        EntityAttributeModifier.Operation.ADD_VALUE),
                                AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.ATTACK_SPEED,
                                new EntityAttributeModifier(
                                        Identifier.of(KitMod.MOD_ID, name + "_speed"),
                                        speed - 4.0,
                                        EntityAttributeModifier.Operation.ADD_VALUE),
                                AttributeModifierSlot.MAINHAND)
                        .build());
    }

    private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(KitMod.MOD_ID, name));
        Item item = factory.apply(settings.registryKey(key));
        return Registry.register(Registries.ITEM, key, item);
    }

    /** Принудительная загрузка класса. */
    public static void register() {
        KitMod.LOGGER.info("Регистрация предметов KitMod");
    }
}
