package ru.tarkmull.kitmod;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Механики: пицца-меч, коса вампира, левиафан, шаблон для лавы. */
public final class Mechanics {

    private Mechanics() {
    }

    /** Идентификатор модификатора "навсегда отнятое здоровье". */
    public static final Identifier DRAIN_ID = Identifier.of(KitMod.MOD_ID, "stolen_health");

    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();

    // ------------------------------------------------------------------
    public static void register() {
        registerAttack();
        registerLeviathan();
        registerLavaMold();
    }

    // ------------------------------------------------------------------
    //  Удары: пицца-меч и коса вампира
    // ------------------------------------------------------------------
    private static void registerAttack() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            ItemStack stack = player.getStackInHand(hand);

            if (stack.isOf(ModItems.PIZZA_SWORD)) {
                if (tryUse(player, "pizza_sword", KitMod.CONFIG.pizzaSwordCooldown)) {
                    player.getHungerManager().add(KitMod.CONFIG.pizzaSwordHunger, 0.4f);
                    play(world, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_BURP, 0.6f, 1.4f);
                }
            } else if (stack.isOf(ModItems.VAMPIRE_SCYTHE)) {
                if (tryUse(player, "vampire_scythe", KitMod.CONFIG.scytheCooldown)) {
                    scythe(player, world, entity);
                }
            } else if (stack.isOf(ModItems.LEGENDARY_SWORD)) {
                legendarySword(world, entity);
            }
            return ActionResult.PASS;
        });
    }

    /** Легендарный меч: шанс наложить на цель Слабость и Замедление. */
    private static void legendarySword(World world, Entity target) {
        if (!(target instanceof LivingEntity victim)) return;
        double chance = KitMod.CONFIG.legendarySwordEffectChance;
        if (chance <= 0 || world.getRandom().nextDouble() * 100.0 >= chance) return;
        int ticks = Math.max(1, KitMod.CONFIG.legendarySwordEffectSeconds) * 20;
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, ticks, 1));
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 1));
        play(world, victim.getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.8f);
    }

    private static void scythe(PlayerEntity player, World world, Entity target) {
        player.heal(KitMod.CONFIG.scytheHeal);
        player.getHungerManager().add(KitMod.CONFIG.scytheHunger, 0.4f);
        play(world, player.getBlockPos(), SoundEvents.ENTITY_WITCH_DRINK, 0.7f, 0.7f);

        if (!(target instanceof LivingEntity victim) || victim == player) return;

        double chance = KitMod.CONFIG.scytheDrainChance;
        if (chance <= 0 || world.getRandom().nextDouble() * 100.0 >= chance) return;

        double amount = KitMod.CONFIG.scytheDrainAmount;
        EntityAttributeInstance max = victim.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (max == null || max.getValue() - amount < KitMod.CONFIG.scytheDrainMinHealth) return;

        drain(victim, amount);
        play(world, player.getBlockPos(), SoundEvents.ENTITY_WITHER_HURT, 0.8f, 1.6f);

        player.sendMessage(Text.literal("Вы навсегда отняли " + fmt(amount) + " HP у "
                + victim.getName().getString()).formatted(Formatting.DARK_RED), false);
        if (victim instanceof PlayerEntity victimPlayer) {
            victimPlayer.sendMessage(Text.literal("У вас навсегда отнято " + fmt(amount) + " HP!")
                    .formatted(Formatting.DARK_RED), false);
        }
    }

    /** Прибавить к уже отнятому здоровью. Модификатор сохраняется в NBT сущности. */
    public static void drain(LivingEntity entity, double amount) {
        EntityAttributeInstance inst = entity.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (inst == null) return;
        double current = stolen(entity);
        inst.removeModifier(DRAIN_ID);
        double total = current + amount;
        if (total > 0) {
            inst.addPersistentModifier(new EntityAttributeModifier(
                    DRAIN_ID, -total, EntityAttributeModifier.Operation.ADD_VALUE));
        }
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(Math.max(1.0f, entity.getMaxHealth()));
        }
    }

    /** Сколько HP отнято у сущности навсегда. */
    public static double stolen(LivingEntity entity) {
        EntityAttributeInstance inst = entity.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (inst == null) return 0;
        EntityAttributeModifier mod = inst.getModifier(DRAIN_ID);
        return mod == null ? 0 : -mod.value();
    }

    // ------------------------------------------------------------------
    //  Левиафан: ПКМ -> трезубцы с Верностью
    // ------------------------------------------------------------------
    private static void registerLeviathan() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(ModItems.LEVIATHAN)) return ActionResult.PASS;
            if (world.isClient) return ActionResult.SUCCESS;

            if (!tryUse(player, "leviathan", KitMod.CONFIG.leviathanCooldown)) {
                return ActionResult.SUCCESS;
            }

            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            int loyalty = Math.max(0, KitMod.CONFIG.leviathanLoyalty);
            if (loyalty > 0) {
                Registry<Enchantment> registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
                RegistryEntry<Enchantment> entry = registry.getOrThrow(Enchantments.LOYALTY);
                tridentStack.addEnchantment(entry, loyalty);
            }

            int count = Math.max(1, KitMod.CONFIG.leviathanTridents);
            for (int i = 0; i < count; i++) {
                float yawOffset = count > 1
                        ? (i - (count - 1) / 2.0f) * KitMod.CONFIG.leviathanSpread
                        : 0.0f;
                TridentEntity trident = new TridentEntity(world, player, tridentStack.copy());
                trident.setVelocity(player, player.getPitch(), player.getYaw() + yawOffset,
                        0.0f, KitMod.CONFIG.leviathanSpeed, 1.0f);
                trident.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
                world.spawnEntity(trident);
            }

            play(world, player.getBlockPos(), SoundEvents.ITEM_TRIDENT_THROW.value(), 1.0f, 1.0f);
            return ActionResult.SUCCESS;
        });
    }

    // ------------------------------------------------------------------
    //  Шаблон для лавы
    // ------------------------------------------------------------------
    private static void registerLavaMold() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            ItemStack stack = player.getStackInHand(hand);

            if (state.isOf(ModBlocks.LAVA_MOLD) && stack.isOf(Items.LAVA_BUCKET)) {
                world.setBlockState(pos, ModBlocks.LAVA_MOLD_FILLED.getDefaultState());
                if (!player.isCreative()) {
                    player.setStackInHand(hand, KitMod.CONFIG.lavaMoldConsumeBucket
                            ? ItemStack.EMPTY
                            : new ItemStack(Items.BUCKET));
                }
                play(world, pos, SoundEvents.ITEM_BUCKET_EMPTY_LAVA, 1.0f, 1.0f);
                return ActionResult.SUCCESS;
            }

            if (state.isOf(ModBlocks.LAVA_MOLD_FILLED) && stack.isOf(Items.BUCKET)
                    && KitMod.CONFIG.lavaMoldAllowScoop) {
                world.setBlockState(pos, ModBlocks.LAVA_MOLD.getDefaultState());
                if (!player.isCreative()) {
                    player.setStackInHand(hand, new ItemStack(Items.LAVA_BUCKET));
                }
                play(world, pos, SoundEvents.ITEM_BUCKET_FILL_LAVA, 1.0f, 1.0f);
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });
    }

    // ------------------------------------------------------------------
    //  Утилиты
    // ------------------------------------------------------------------
    /** Предмет готов? start=true — сразу запустить перезарядку. */
    static boolean ready(PlayerEntity player, String id, double seconds, boolean start) {
        if (seconds <= 0) return true;
        Map<String, Long> map = COOLDOWNS.get(player.getUuid());
        Long until = map == null ? null : map.get(id);
        if (until != null && until > System.currentTimeMillis()) return false;
        if (start) startCooldown(player, id, seconds);
        return true;
    }

    static void startCooldown(PlayerEntity player, String id, double seconds) {
        if (seconds <= 0) return;
        COOLDOWNS.computeIfAbsent(player.getUuid(), k -> new HashMap<>())
                .put(id, System.currentTimeMillis() + (long) (seconds * 1000L));
    }

    /** Сколько секунд осталось до готовности. */
    static double left(PlayerEntity player, String id) {
        Map<String, Long> map = COOLDOWNS.get(player.getUuid());
        Long until = map == null ? null : map.get(id);
        if (until == null) return 0;
        long ms = until - System.currentTimeMillis();
        return ms <= 0 ? 0 : Math.round(ms / 100.0) / 10.0;
    }

    static void cooldownMessage(PlayerEntity player, String id) {
        double left = left(player, id);
        if (left <= 0) return;
        player.sendMessage(Text.literal("Перезарядка: " + left + " сек.")
                .formatted(Formatting.GRAY), true);
    }

    /** Сбросить все перезарядки игрока. */
    public static void clearCooldowns(PlayerEntity player) {
        COOLDOWNS.remove(player.getUuid());
    }

    /** Проверить и сразу занять перезарядку, с сообщением при отказе. */
    private static boolean tryUse(PlayerEntity player, String id, double seconds) {
        if (!ready(player, id, seconds, false)) {
            cooldownMessage(player, id);
            return false;
        }
        startCooldown(player, id, seconds);
        return true;
    }

    private static void play(World world, BlockPos pos, net.minecraft.sound.SoundEvent sound,
                             float volume, float pitch) {
        world.playSound(null, pos, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
