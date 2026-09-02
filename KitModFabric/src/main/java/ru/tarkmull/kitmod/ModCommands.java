package ru.tarkmull.kitmod;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Команды мода:
 *   /kitgive  <ник> <предмет> [кол-во]   — выдать предмет KitMod
 *   /hpgive   <ник> <хп>                 — вернуть навсегда отнятое здоровье
 *   /hpsteal  <ник> <хп>                 — отнять здоровье навсегда
 *   /hpreset  <ник>                      — вернуть всё отнятое сразу
 *   /kitmod list | abilities | reload | cooldowns
 */
public final class ModCommands {

    private ModCommands() {
    }

    /** id -> предмет. Порядок сохраняется для подсказок и /kitmod list. */
    private static final Map<String, ItemConvertible> ITEMS = new LinkedHashMap<>();

    public static final List<String> IDS;

    static {
        ITEMS.put("pizza", ModItems.PIZZA);
        ITEMS.put("pizza_sword", ModItems.PIZZA_SWORD);
        ITEMS.put("leviathan", ModItems.LEVIATHAN);
        ITEMS.put("vampire_scythe", ModItems.VAMPIRE_SCYTHE);
        ITEMS.put("legendary_sword", ModItems.LEGENDARY_SWORD);
        ITEMS.put("lava_mold", ModBlocks.LAVA_MOLD);
        ITEMS.put("lava_mold_filled", ModBlocks.LAVA_MOLD_FILLED);
        ITEMS.put("lava_crystal", ModItems.LAVA_CRYSTAL);
        ITEMS.put("reinforced_string", ModItems.REINFORCED_STRING);
        ITEMS.put("dragon_ingot", ModItems.DRAGON_INGOT);
        ITEMS.put("ender_ingot", ModItems.ENDER_INGOT);
        ITEMS.put("blood_ingot", ModItems.BLOOD_INGOT);
        ITEMS.put("lava_ingot", ModItems.LAVA_INGOT);
        ITEMS.put("ruby_diamond", ModItems.RUBY_DIAMOND);
        ITEMS.put("rare_glove", ModItems.RARE_GLOVE);
        ITEMS.put("steel", ModItems.STEEL);
        ITEMS.put("master_redstone", ModItems.MASTER_REDSTONE);
        IDS = List.copyOf(ITEMS.keySet());
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // ---------------- /kitgive ----------------
            dispatcher.register(CommandManager.literal("kitgive")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("target", EntityArgumentType.player())
                            .then(CommandManager.argument("item", StringArgumentType.word())
                                    .suggests((ctx, builder) -> CommandSource.suggestMatching(IDS, builder))
                                    .executes(ctx -> give(ctx.getSource(),
                                            EntityArgumentType.getPlayer(ctx, "target"),
                                            StringArgumentType.getString(ctx, "item"), 1))
                                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 2304))
                                            .executes(ctx -> give(ctx.getSource(),
                                                    EntityArgumentType.getPlayer(ctx, "target"),
                                                    StringArgumentType.getString(ctx, "item"),
                                                    IntegerArgumentType.getInteger(ctx, "count")))))));

            // ---------------- /hpgive ----------------
            dispatcher.register(CommandManager.literal("hpgive")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("target", EntityArgumentType.player())
                            .then(CommandManager.argument("hp", DoubleArgumentType.doubleArg(0.1))
                                    .executes(ctx -> changeHp(ctx.getSource(),
                                            EntityArgumentType.getPlayer(ctx, "target"),
                                            -DoubleArgumentType.getDouble(ctx, "hp"))))));

            // ---------------- /hpsteal ----------------
            dispatcher.register(CommandManager.literal("hpsteal")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("target", EntityArgumentType.player())
                            .then(CommandManager.argument("hp", DoubleArgumentType.doubleArg(0.1))
                                    .executes(ctx -> changeHp(ctx.getSource(),
                                            EntityArgumentType.getPlayer(ctx, "target"),
                                            DoubleArgumentType.getDouble(ctx, "hp"))))));

            // ---------------- /hpreset ----------------
            dispatcher.register(CommandManager.literal("hpreset")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ctx -> {
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                double stolen = Mechanics.stolen(target);
                                setStolen(target, 0);
                                ctx.getSource().sendFeedback(() -> Text.literal(
                                        "Игроку " + target.getName().getString() + " возвращено всё: "
                                                + Mechanics.fmt(stolen) + " HP").formatted(Formatting.GREEN), true);
                                return 1;
                            })));

            // ---------------- /hpcheck ----------------
            dispatcher.register(CommandManager.literal("hpcheck")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ctx -> {
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                double stolen = Mechanics.stolen(target);
                                ctx.getSource().sendFeedback(() -> Text.literal(
                                        "У " + target.getName().getString() + " отнято "
                                                + Mechanics.fmt(stolen) + " HP (макс. здоровье: "
                                                + Mechanics.fmt(target.getMaxHealth()) + ")"), false);
                                return 1;
                            })));

            // ---------------- /kitmod ----------------
            dispatcher.register(CommandManager.literal("kitmod")
                    .then(CommandManager.literal("list").executes(ctx -> {
                        ctx.getSource().sendFeedback(() -> Text.literal("Предметы KitMod:")
                                .formatted(Formatting.GOLD), false);
                        for (String id : IDS) {
                            ctx.getSource().sendFeedback(() -> Text.literal("  kitmod:" + id), false);
                        }
                        return 1;
                    }))
                    .then(CommandManager.literal("abilities").executes(ctx -> {
                        for (String line : ABILITY_HELP) {
                            ctx.getSource().sendFeedback(() -> Text.literal(line), false);
                        }
                        return 1;
                    }))
                    .then(CommandManager.literal("cooldowns").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) {
                            ctx.getSource().sendError(Text.literal("Только для игрока."));
                            return 0;
                        }
                        Mechanics.clearCooldowns(player);
                        ctx.getSource().sendFeedback(() -> Text.literal("Перезарядки сброшены.")
                                .formatted(Formatting.GREEN), false);
                        return 1;
                    }))
                    .then(CommandManager.literal("reload")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(ctx -> {
                                KitMod.CONFIG = KitConfig.load();
                                ctx.getSource().sendFeedback(() -> Text.literal(
                                        "Конфиг перечитан. Урон и скорость атаки предметов "
                                                + "обновятся только после перезапуска.")
                                        .formatted(Formatting.GREEN), true);
                                return 1;
                            }))
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(() -> Text.literal("KitMod — /kitmod list | abilities | cooldowns | reload")
                                .formatted(Formatting.GOLD), false);
                        return 1;
                    }));
        });
    }

    private static final String[] ABILITY_HELP = {
            "Способности (ПКМ, если не указано иначе):",
            "  Пицца — еда, 15 голода",
            "  Пицца-меч — удар даёт голод",
            "  Левиафан — бросает трезубец с Верностью",
            "  Коса вампира — удар лечит, 1% отнять 2 HP навсегда",
            "  Легендарный меч — удар: шанс Слабости и Замедления",
            "  Лавовый кристалл — огнестойкость",
            "  Рубиновый алмаз — лечение и регенерация",
            "  Редкая перчатка — рывок вперёд",
            "  Эндер-слиток — телепорт по взгляду",
            "  Драконий слиток — сила и огнестойкость",
            "  Кровавый слиток — отдать HP за силу и скорость",
            "  Сталь — сопротивление урону",
            "  Мастер-редстоун — спешка III",
            "  Укреплённая нить — медленное падение",
            "  Шаблон для лавы — блок, залей ведром лавы"
    };

    // ------------------------------------------------------------------
    private static int give(ServerCommandSource source, ServerPlayerEntity target,
                            String id, int count) {
        ItemConvertible item = ITEMS.get(id);
        if (item == null) {
            source.sendError(Text.literal("Неизвестный предмет: " + id));
            return 0;
        }
        int max = new ItemStack(item).getMaxCount();
        int given = 0;
        while (given < count) {
            int part = Math.min(max, count - given);
            ItemStack stack = new ItemStack(item, part);
            if (!target.getInventory().insertStack(stack)) {
                target.dropItem(stack, false);
            }
            given += part;
        }
        final int total = count;
        source.sendFeedback(() -> Text.literal("Выдано " + total + "x kitmod:" + id
                + " игроку " + target.getName().getString()).formatted(Formatting.GREEN), true);
        return 1;
    }

    /** delta > 0 — отнять навсегда, delta < 0 — вернуть. */
    private static int changeHp(ServerCommandSource source, ServerPlayerEntity target, double delta) {
        double stolen = Mechanics.stolen(target);
        double result = Math.max(0, stolen + delta);
        if (delta < 0 && stolen <= 0) {
            source.sendError(Text.literal("У игрока " + target.getName().getString()
                    + " ничего не отнято."));
            return 0;
        }
        setStolen(target, result);

        final double diff = Math.abs(result - stolen);
        final double left = result;
        if (delta < 0) {
            source.sendFeedback(() -> Text.literal("Возвращено " + Mechanics.fmt(diff) + " HP игроку "
                    + target.getName().getString() + " (осталось отнято: " + Mechanics.fmt(left) + ")")
                    .formatted(Formatting.GREEN), true);
            target.sendMessage(Text.literal("Вам вернули " + Mechanics.fmt(diff) + " HP")
                    .formatted(Formatting.GREEN), false);
        } else {
            source.sendFeedback(() -> Text.literal("Отнято " + Mechanics.fmt(diff) + " HP у игрока "
                    + target.getName().getString() + " (всего отнято: " + Mechanics.fmt(left) + ")")
                    .formatted(Formatting.DARK_RED), true);
            target.sendMessage(Text.literal("У вас навсегда отнято " + Mechanics.fmt(diff) + " HP!")
                    .formatted(Formatting.DARK_RED), false);
        }
        return 1;
    }

    private static void setStolen(ServerPlayerEntity target, double value) {
        EntityAttributeInstance inst = target.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (inst == null) return;
        inst.removeModifier(Mechanics.DRAIN_ID);
        if (value > 0.0001) {
            inst.addPersistentModifier(new EntityAttributeModifier(
                    Mechanics.DRAIN_ID, -value, EntityAttributeModifier.Operation.ADD_VALUE));
        }
        if (target.getHealth() > target.getMaxHealth()) {
            target.setHealth(Math.max(1.0f, target.getMaxHealth()));
        }
    }
}
