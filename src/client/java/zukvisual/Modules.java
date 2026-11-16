package zukvisual;

import net.fabricmc.loader.api.FabricLoader;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;

public final class Modules {
    public static final Module WATERMARK = new Module("Watermark", "Ватермарк: ник и FPS", true);
    public static final Module MODULE_LIST = new Module("ModuleList", "Список модулей справа", true);
    public static final Module HIT_PARTICLES = new Module("HitParticles", "Частицы при ударе", true);
    public static final Module TRAIL = new Module("Trail", "Трейл за игроком", true);

    public static final Module INFO = new Module("Info", "Координаты XYZ", true);
    public static final Module KEYSTROKES = new Module("Keystrokes", "Клавиши WASD и пробел", true);
    public static final Module STATUS = new Module("StatusBars", "Здоровье, броня, еда", true);
    public static final Module HIT_NUMBERS = new Module("HitNumbers", "Цифры урона у прицела", true);
    public static final Module NOTIFICATIONS = new Module("Notifications", "Уведомления в углу", true);

    public static final Module ITEM_TIMERS = new Module("ItemTimers", "Кулдауны и счётчик предметов", true);
    public static final Module AUTOSWAP = new Module("AutoSwap", "Клавиши свапа в левую руку", true);

    public static final Module ZONES = new Module("Zones", "Границы зон предметов", true);
    public static final Module TARGET_HUD = new Module("TargetHUD", "Цель: имя и здоровье", true);
    public static final Module POTIONS = new Module("Potions", "Твои эффекты и таймеры", true);
    public static final Module ARMOR_HUD = new Module("ArmorHUD", "Прочность твоей брони", true);

    public static final Module ZOOM = new Module("Zoom", "Зум на удержании клавиши Z", true);
    public static final Module FREELOOK = new Module("Freelook", "Обзор камерой, удержание Left Alt", true);
    public static final Module CROSSHAIR = new Module("Crosshair", "Свой прицел в стиле темы", true);
    public static final Module WAYPOINTS = new Module("Waypoints", "Метки с компасом и расстоянием", true);
    public static final Module SNOW = new Module("Snow", "Снег вокруг игрока", false);
    public static final Module TARGET_MARKER = new Module("TargetMarker", "Кольцо вокруг цели, в которую бьёшь", true);
    public static final Module HIT_SOUND = new Module("HitSound", "Звук при ударе", true);
    public static final Module JUMP_CIRCLE = new Module("JumpCircle", "Кольцо под ногами при прыжке", true);
    public static final Module HIT_BUBBLE = new Module("HitBubble", "Пузырь из частиц при ударе", true);
    public static final Module UI_SOUNDS = new Module("UISounds", "Звуки меню и переключений", true);
    public static final Module KILL_FX = new Module("KillEffect", "Анимация и счётчик убийств", true);
    public static final Module HALO = new Module("Halo", "Нимб над головой (3-е лицо)", false);
    public static final Module AURA = new Module("Aura", "Вихрь частиц вокруг тебя (3-е лицо)", false);
    public static final Module WINGS = new Module("Wings", "Крылья из частиц (3-е лицо)", false);
    public static final Module HEARTS = new Module("Hearts", "Сердечки над головой (3-е лицо)", false);
    public static final Module CROWN = new Module("Crown", "Золотая корона (3-е лицо)", false);
    public static final Module STARS = new Module("Stars", "Три звезды вокруг пояса (3-е лицо)", false);
    public static final Module TAIL = new Module("Tail", "Покачивающийся хвост (3-е лицо)", false);
    public static final Module FOOTPRINTS = new Module("Footprints", "Следы ног (3-е лицо)", false);
    public static final Module SPARKLES = new Module("Sparkles", "Искры вокруг тела (3-е лицо)", false);
    public static final Module FLAME_FEET = new Module("FlameFeet", "Огонь у ног, урона нет (3-е лицо)", false);
    public static final Module CLOUD = new Module("Cloud", "Тучка с дождём над головой (3-е лицо)", false);
    public static final Module HELIX = new Module("Helix", "Двойная спираль вокруг тела (3-е лицо)", false);
    public static final Module RAINBOW_TRAIL = new Module("RainbowTrail", "Радужный трейл (3-е лицо)", false);
    public static final Module ADAPTIVE = new Module("AdaptiveFx", "Авто-снижение частиц при просадке FPS", true);
    public static final Module FPS_BOOST = new Module("FPSBoost", "Лёгкие настройки графики (временно)", false);
    public static final Module LOW_FIRE = new Module("LowFire", "Тонкая полоса огня вместо ванильного", false);
    public static final Module BRIGHTNESS = new Module("Brightness", "Макс. яркость, как Bright в настройках", false);
    public static final Module STATS = new Module("Stats", "Статистика: время, K/D, урон, график FPS", false);
    public static final Module ITEM_PHYSICS = new Module("ItemPhysics", "Выброшенные предметы лежат на земле плашмя", true);
    public static final Module HIT_COUNTER = new Module("HitCounter", "Счётчик ударов и комбо", true);
    public static final Module HIT_RANGE = new Module("HitRange", "Дистанция удара и рекорд", true);
    public static final Module MACE_HELPER = new Module("MaceHelper", "Булава: падение и бонус урона", true);
    public static final Module LOW_HP = new Module("LowHP", "Красное мерцание по краям при низком здоровье", true);
    public static final Module INVENTORY_HUD = new Module("InventoryHUD", "Инвентарь на экране", false);
    public static final Module BODY_GLOW = new Module("BodyGlow", "Свечение тела (3-е лицо)", false);
    public static final Module STAR_GLOW = new Module("StarGlow", "Звёздное свечение вокруг (3-е лицо)", false);
    public static final Module WORLD_PARTICLES = new Module("WorldParticles", "Частицы мира: светлячки, искры, лепестки", false);
    public static final Module ARROW_TRAIL = new Module("ArrowTrail", "След за летящими стрелами", false);
    public static final Module PHRASE_WHEEL = new Module("PhraseWheel", "Колесо фраз (клавиша G)", true);
    public static final Module PREDICTION = new Module("Prediction", "Траектория бросков и стрел, точка падения", true);
    public static final Module FAST_EXP = new Module("FastExp", "Быстрые броски опыта (на кулдауне обычно)", false);
    public static final Module AUC_HELPER = new Module("AucHelper", "Подсветка дешёвых лотов на аукционе", true);
    public static final Module FAKE_PLAYER = new Module("FakePlayer", "Твоя копия для проверки урона (локально)", false);
    public static final Module HOLIDAY = new Module("Holiday", "Праздничная косметика: шапка, частицы, цвета", false);
    public static final Module DEATH_POS = new Module("DeathPos", "Запоминает место смерти", true);

    public static final List<Module> ALL = List.of(WATERMARK, MODULE_LIST, INFO, KEYSTROKES, STATUS,
            ARMOR_HUD, POTIONS, TARGET_HUD, ITEM_TIMERS, ZONES, AUTOSWAP, ZOOM, FREELOOK, CROSSHAIR, WAYPOINTS, TARGET_MARKER, SNOW, HIT_SOUND, JUMP_CIRCLE, HIT_BUBBLE, UI_SOUNDS, KILL_FX, HALO, AURA, WINGS, HEARTS, CROWN, STARS, TAIL, FOOTPRINTS,
            SPARKLES, FLAME_FEET, CLOUD, HELIX, RAINBOW_TRAIL, ADAPTIVE, FPS_BOOST, LOW_FIRE, BRIGHTNESS, STATS, ITEM_PHYSICS,
            HIT_COUNTER, HIT_RANGE, MACE_HELPER, LOW_HP, INVENTORY_HUD, BODY_GLOW, STAR_GLOW, WORLD_PARTICLES, ARROW_TRAIL, PHRASE_WHEEL,
            PREDICTION, FAST_EXP, AUC_HELPER, FAKE_PLAYER, HOLIDAY,
            DEATH_POS,
            HIT_PARTICLES, TRAIL, HIT_NUMBERS, NOTIFICATIONS);

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("zukvisual.properties");
    }

    private static Path profile(int n) {
        return FabricLoader.getInstance().getConfigDir().resolve("zukvisual-profile" + n + ".properties");
    }

    /** Сохранить текущие настройки в профиль n. */
    public static void saveProfile(int n) {
        save();
        try {
            Files.copy(file(), profile(n), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
        }
    }

    /** Загрузить профиль n. Возвращает false, если профиль пуст. */
    public static boolean loadProfile(int n) {
        try {
            if (!Files.exists(profile(n))) return false;
            Files.copy(profile(n), file(), StandardCopyOption.REPLACE_EXISTING);
            load();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void load() {
        Properties p = new Properties();
        try (var in = Files.newInputStream(file())) {
            p.load(in);
        } catch (Exception e) {
            return; // первого запуска файла ещё нет
        }
        try { Gfx.setTheme(Integer.parseInt(p.getProperty("Theme", "0"))); } catch (Exception ignored) {}
        try { ZukEffects.hitStyle = Integer.parseInt(p.getProperty("HitStyle", "0")) % ZukEffects.STYLES.length; } catch (Exception ignored) {}
        try { Cosmetics.killStyle = Integer.parseInt(p.getProperty("KillStyle", "0")) % Cosmetics.KILL_STYLES.length; } catch (Exception ignored) {}
        try { Fx.quality = Math.max(0, Math.min(2, Integer.parseInt(p.getProperty("Quality", "1")))); } catch (Exception ignored) {}
        Settings.load(p);
        for (Module m : ALL) {
            String v = p.getProperty(m.name);
            if (v != null) m.enabled = Boolean.parseBoolean(v);
            m.anim = m.enabled ? 1f : 0f;
            m.toggle = m.anim;
        }
    }

    public static void save() {
        Properties p = new Properties();
        for (Module m : ALL) p.setProperty(m.name, String.valueOf(m.enabled));
        p.setProperty("Theme", String.valueOf(Gfx.themeIndex));
        p.setProperty("HitStyle", String.valueOf(ZukEffects.hitStyle));
        p.setProperty("KillStyle", String.valueOf(Cosmetics.killStyle));
        p.setProperty("Quality", String.valueOf(Fx.quality));
        Settings.save(p);
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "Zukvisual");
        } catch (Exception ignored) {
        }
    }
}
