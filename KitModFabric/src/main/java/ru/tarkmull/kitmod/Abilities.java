package ru.tarkmull.kitmod;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Способности предметов на ПКМ.
 * Всё, кроме левиафана (он живёт в Mechanics).
 */
public final class Abilities {

    private Abilities() {
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            Item item = stack.getItem();

            if (item == ModItems.LAVA_CRYSTAL) return run(player, world, "lava_crystal",
                    KitMod.CONFIG.lavaCrystalCooldown, Abilities::lavaCrystal);
            if (item == ModItems.RUBY_DIAMOND) return run(player, world, "ruby_diamond",
                    KitMod.CONFIG.rubyDiamondCooldown, Abilities::rubyDiamond);
            if (item == ModItems.RARE_GLOVE) return run(player, world, "rare_glove",
                    KitMod.CONFIG.rareGloveCooldown, Abilities::rareGlove);
            if (item == ModItems.ENDER_INGOT) return run(player, world, "ender_ingot",
                    KitMod.CONFIG.enderIngotCooldown, Abilities::enderIngot);
            if (item == ModItems.DRAGON_INGOT) return run(player, world, "dragon_ingot",
                    KitMod.CONFIG.dragonIngotCooldown, Abilities::dragonIngot);
            if (item == ModItems.BLOOD_INGOT) return run(player, world, "blood_ingot",
                    KitMod.CONFIG.bloodIngotCooldown, Abilities::bloodIngot);
            if (item == ModItems.STEEL) return run(player, world, "steel",
                    KitMod.CONFIG.steelCooldown, Abilities::steel);
            if (item == ModItems.MASTER_REDSTONE) return run(player, world, "master_redstone",
                    KitMod.CONFIG.masterRedstoneCooldown, Abilities::masterRedstone);
            if (item == ModItems.REINFORCED_STRING) return run(player, world, "reinforced_string",
                    KitMod.CONFIG.reinforcedStringCooldown, Abilities::reinforcedString);

            return ActionResult.PASS;
        });
    }

    // ------------------------------------------------------------------
    private interface Ability {
        /** @return false — способность не сработала, перезарядку не запускать. */
        boolean use(PlayerEntity player, World world);
    }

    private static ActionResult run(PlayerEntity player, World world, String id,
                                    double cooldown, Ability ability) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!Mechanics.ready(player, id, cooldown, false)) {
            Mechanics.cooldownMessage(player, id);
            return ActionResult.SUCCESS;
        }
        if (ability.use(player, world)) {
            Mechanics.startCooldown(player, id, cooldown);
        }
        return ActionResult.SUCCESS;
    }

    private static int ticks(int seconds) {
        return Math.max(1, seconds) * 20;
    }

    private static void effect(PlayerEntity player, net.minecraft.registry.entry.RegistryEntry
            <net.minecraft.entity.effect.StatusEffect> effect, int seconds, int amplifier) {
        player.addStatusEffect(new StatusEffectInstance(effect, ticks(seconds), amplifier));
    }

    private static void note(PlayerEntity player, String text, Formatting color) {
        player.sendMessage(Text.literal(text).formatted(color), true);
    }

    // ------------------------------------------------------------------
    //  Сами способности
    // ------------------------------------------------------------------

    /** Лавовый кристалл — огнестойкость. */
    private static boolean lavaCrystal(PlayerEntity player, World world) {
        effect(player, StatusEffects.FIRE_RESISTANCE, KitMod.CONFIG.lavaCrystalSeconds, 0);
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.8f, 1.2f);
        note(player, "Лавовый кристалл: огнестойкость " + KitMod.CONFIG.lavaCrystalSeconds + " сек.",
                Formatting.GOLD);
        return true;
    }

    /** Рубиновый алмаз — мгновенное лечение и регенерация. */
    private static boolean rubyDiamond(PlayerEntity player, World world) {
        player.heal(KitMod.CONFIG.rubyDiamondHeal);
        effect(player, StatusEffects.REGENERATION, KitMod.CONFIG.rubyDiamondRegenSeconds, 1);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.9f, 1.4f);
        note(player, "Рубиновый алмаз: лечение", Formatting.RED);
        return true;
    }

    /** Редкая перчатка — рывок в сторону взгляда. */
    private static boolean rareGlove(PlayerEntity player, World world) {
        Vec3d dir = player.getRotationVec(1.0f).normalize();
        player.setVelocity(dir.multiply(KitMod.CONFIG.rareGloveDashPower));
        player.velocityModified = true;
        player.fallDistance = 0.0f;
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PHANTOM_FLAP,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.8f, 1.5f);
        return true;
    }

    /** Эндер-слиток — телепорт туда, куда смотришь. */
    private static boolean enderIngot(PlayerEntity player, World world) {
        HitResult hit = player.raycast(KitMod.CONFIG.enderIngotRange, 1.0f, false);
        Vec3d target = hit.getPos();
        if (target == null) return false;
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
        player.requestTeleport(target.x, target.y, target.z);
        player.fallDistance = 0.0f;
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    /** Драконий слиток — сила и огнестойкость. */
    private static boolean dragonIngot(PlayerEntity player, World world) {
        int sec = KitMod.CONFIG.dragonIngotSeconds;
        effect(player, StatusEffects.STRENGTH, sec, 0);
        effect(player, StatusEffects.FIRE_RESISTANCE, sec, 0);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.6f);
        note(player, "Драконий слиток: сила " + sec + " сек.", Formatting.LIGHT_PURPLE);
        return true;
    }

    /** Кровавый слиток — жертвуешь здоровьем ради силы и скорости. */
    private static boolean bloodIngot(PlayerEntity player, World world) {
        float cost = (float) KitMod.CONFIG.bloodIngotHealthCost;
        if (player.getHealth() <= cost + 1.0f) {
            note(player, "Слишком мало здоровья для жертвы", Formatting.RED);
            return false;
        }
        player.setHealth(player.getHealth() - cost);
        int sec = KitMod.CONFIG.bloodIngotSeconds;
        effect(player, StatusEffects.STRENGTH, sec, 1);
        effect(player, StatusEffects.SPEED, sec, 1);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.7f, 0.6f);
        note(player, "Кровавая жертва: сила и скорость " + sec + " сек.", Formatting.DARK_RED);
        return true;
    }

    /** Сталь — сопротивление урону. */
    private static boolean steel(PlayerEntity player, World world) {
        effect(player, StatusEffects.RESISTANCE, KitMod.CONFIG.steelSeconds, 1);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.6f);
        note(player, "Сталь: сопротивление " + KitMod.CONFIG.steelSeconds + " сек.", Formatting.GRAY);
        return true;
    }

    /** Мастер-редстоун — спешка. */
    private static boolean masterRedstone(PlayerEntity player, World world) {
        effect(player, StatusEffects.HASTE, KitMod.CONFIG.masterRedstoneSeconds, 2);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                net.minecraft.sound.SoundCategory.PLAYERS, 0.8f, 1.8f);
        note(player, "Мастер-редстоун: спешка III", Formatting.RED);
        return true;
    }

    /** Укреплённая нить — медленное падение. */
    private static boolean reinforcedString(PlayerEntity player, World world) {
        effect(player, StatusEffects.SLOW_FALLING, KitMod.CONFIG.reinforcedStringSeconds, 0);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_SPIDER_STEP,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.7f, 1.2f);
        note(player, "Нить: медленное падение " + KitMod.CONFIG.reinforcedStringSeconds + " сек.",
                Formatting.WHITE);
        return true;
    }
}
