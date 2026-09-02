package ru.tarkmull.kitmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Конфиг мода: config/kitmod.json
 * Файл создаётся при первом запуске, дальше редактируется вручную.
 * Изменения применяются после перезапуска игры/сервера.
 */
public class KitConfig {

    // --- Пицца ---
    /** Сколько голода восстанавливает пицца (20 = полная шкала). */
    public int pizzaNutrition = 15;
    public float pizzaSaturation = 1.0f;

    // --- Пицца-меч ---
    /** Перезарядка эффекта, СЕКУНДЫ. */
    public double pizzaSwordCooldown = 2.0;
    /** Сколько голода даёт за удар. */
    public int pizzaSwordHunger = 2;
    public double pizzaSwordDamage = 8.0;
    public double pizzaSwordSpeed = 1.6;

    // --- Левиафан ---
    /** Перезарядка броска, СЕКУНДЫ. */
    public double leviathanCooldown = 2.0;
    /** Сколько трезубцев за бросок. */
    public int leviathanTridents = 1;
    /** Уровень зачарования "Верность" на трезубце. */
    public int leviathanLoyalty = 3;
    /** Начальная скорость трезубца. */
    public float leviathanSpeed = 2.5f;
    /** Разброс между трезубцами (если их больше одного). */
    public float leviathanSpread = 5.0f;
    public double leviathanDamage = 9.0;
    public double leviathanSpeedAttr = 1.6;

    // --- Коса вампира ---
    /** Перезарядка эффекта, СЕКУНДЫ. */
    public double scytheCooldown = 4.0;
    /** Сколько HP восстанавливает владельцу (2 HP = 1 сердце). */
    public float scytheHeal = 5.0f;
    /** Сколько голода даёт владельцу. */
    public int scytheHunger = 1;
    /** Шанс навсегда отнять здоровье, ПРОЦЕНТЫ. */
    public double scytheDrainChance = 1.0;
    /** Сколько HP отнимается навсегда. */
    public double scytheDrainAmount = 2.0;
    /** Ниже этого максимального здоровья отнимать нельзя. */
    public double scytheDrainMinHealth = 6.0;
    public double scytheDamage = 10.0;
    public double scytheSpeed = 1.4;

    // --- Легендарный меч ---
    public double legendarySwordDamage = 9.0;
    public double legendarySwordSpeed = 1.6;
    /** Шанс наложить на цель Слабость и Замедление, ПРОЦЕНТЫ. */
    public double legendarySwordEffectChance = 15.0;
    public int legendarySwordEffectSeconds = 5;

    // --- Способности декоративных предметов (ПКМ) ---
    /** Лавовый кристалл: огнестойкость. */
    public double lavaCrystalCooldown = 60.0;
    public int lavaCrystalSeconds = 30;

    /** Рубиновый алмаз: лечение + регенерация. */
    public double rubyDiamondCooldown = 90.0;
    public float rubyDiamondHeal = 6.0f;
    public int rubyDiamondRegenSeconds = 10;

    /** Редкая перчатка: рывок в сторону взгляда. */
    public double rareGloveCooldown = 5.0;
    public double rareGloveDashPower = 1.6;

    /** Эндер-слиток: телепорт туда, куда смотришь. */
    public double enderIngotCooldown = 8.0;
    public double enderIngotRange = 12.0;

    /** Драконий слиток: сила + сопротивление огню. */
    public double dragonIngotCooldown = 60.0;
    public int dragonIngotSeconds = 20;

    /** Кровавый слиток: жертвуешь здоровьем ради силы и скорости. */
    public double bloodIngotCooldown = 45.0;
    public double bloodIngotHealthCost = 2.0;
    public int bloodIngotSeconds = 30;

    /** Сталь: сопротивление урону. */
    public double steelCooldown = 60.0;
    public int steelSeconds = 20;

    /** Мастер-редстоун: спешка. */
    public double masterRedstoneCooldown = 60.0;
    public int masterRedstoneSeconds = 60;

    /** Укреплённая нить: медленное падение. */
    public double reinforcedStringCooldown = 30.0;
    public int reinforcedStringSeconds = 20;

    // --- Шаблон для лавы ---
    /** Забирать ли ведро совсем (false = остаётся пустое ведро). */
    public boolean lavaMoldConsumeBucket = false;
    /** Можно ли вычерпать лаву обратно пустым ведром. */
    public boolean lavaMoldAllowScoop = true;

    // ------------------------------------------------------------------

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static KitConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("kitmod.json");
        KitConfig config = new KitConfig();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                KitConfig loaded = GSON.fromJson(reader, KitConfig.class);
                if (loaded != null) config = loaded;
            } catch (IOException | RuntimeException e) {
                KitMod.LOGGER.error("Не удалось прочитать kitmod.json, беру значения по умолчанию", e);
            }
        }
        config.save(path);
        return config;
    }

    private void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            KitMod.LOGGER.error("Не удалось записать kitmod.json", e);
        }
    }
}
