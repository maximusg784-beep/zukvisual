package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Properties;
import java.util.Random;

/**
 * Праздничная косметика: Хэллоуин, Новый год, день рождения, 8 Марта.
 * Включает праздничные цвета интерфейса, шапку из частиц на голове (третье лицо),
 * частицы вокруг тебя и в мире, а также поздравление на экране.
 * Режим Auto сам выбирает праздник по дате. День рождения: config/zukvisual-holiday.properties (birthday=MM-dd).
 */
public final class Holiday {
    static final Random RNG = new Random();

    // цвета: Хэллоуин, Новый год, день рождения, 8 Марта
    static final int[][] PAL = {{0xFF7A1A, 0x8A2BE2}, {0xFFD54A, 0x7FE9FF}, {0xFF4DA6, 0x4DD2FF}, {0xFF7AD9, 0xFFC857}};
    static final String[] GREET = {"С Хэллоуином!", "С Новым годом!", "С днём рождения!", "С 8 Марта!"};

    static int applied = 0; // 0 = нет праздника, 1..4 = активный
    static String birthday = "";
    static boolean greeted = false;
    static String banner = null;
    static long bannerTime = 0;

    static final String DEFAULT = """
            # Zukvisual: праздники
            # birthday: твой день рождения в формате MM-dd (например 05-17). В этот день режим Auto включает праздник.
            birthday=
            """;

    static void load() {
        try {
            Path f = FabricLoader.getInstance().getConfigDir().resolve("zukvisual-holiday.properties");
            if (!Files.exists(f)) Files.writeString(f, DEFAULT, StandardCharsets.UTF_8);
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) { p.load(r); }
            birthday = p.getProperty("birthday", "").trim();
        } catch (Exception ignored) {
        }
    }

    /** Какой праздник сейчас: 0 нет, 1 Хэллоуин, 2 Новый год, 3 день рождения, 4 8 Марта. */
    static int resolve() {
        int mode = Settings.holidayMode;
        if (mode != 0) return mode;
        LocalDate d = LocalDate.now();
        int m = d.getMonthValue(), day = d.getDayOfMonth();
        if (!birthday.isEmpty() && String.format("%02d-%02d", m, day).equals(birthday)) return 3;
        if ((m == 10 && day >= 25) || (m == 11 && day == 1)) return 1;
        if ((m == 12 && day >= 20) || (m == 1 && day <= 10)) return 2;
        if (m == 3 && day >= 6 && day <= 9) return 4;
        return 0;
    }

    static void greet(int act) {
        banner = GREET[act - 1];
        bannerTime = System.currentTimeMillis();
        greeted = true;
    }

    public static void init() {
        load();
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null) { greeted = false; return; }
            int act = Modules.HOLIDAY.enabled ? resolve() : 0;
            if (act != applied) {
                applied = act;
                if (act == 0) Gfx.setTheme(Gfx.themeIndex); // вернуть обычную тему
                else greet(act);
            }
            if (act == 0) return;

            Gfx.A = PAL[act - 1][0];
            Gfx.B = PAL[act - 1][1];
            if (!greeted) greet(act); // поздравление при входе в мир

            worldFx(mc, act);
            if (mc.player.age % Fx.interval() == 0 && !mc.options.getPerspective().isFirstPerson()) selfFx(mc, act);
        });
    }

    static void ring(ClientWorld w, double x, double y, double z, double r, int rgb, float scale) {
        int n = Math.max(6, (int) (r * 28));
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            Fx.addKeep(w, new DustParticleEffect(rgb, scale), x + Math.cos(a) * r, y, z + Math.sin(a) * r, 0, 0, 0);
        }
    }

    static void dot(ClientWorld w, double x, double y, double z, int rgb, float scale) {
        Fx.addKeep(w, new DustParticleEffect(rgb, scale), x, y, z, 0, 0, 0);
    }

    /** Шапка из частиц на голове и частицы вокруг тебя (видно от третьего лица). */
    static void selfFx(MinecraftClient mc, int act) {
        var p = mc.player;
        var w = mc.world;
        double x = p.getX(), y = p.getY(), z = p.getZ(), h = p.getHeight(), y0 = y + h;
        long now = System.currentTimeMillis();

        switch (act) {
            case 1 -> { // ведьмина шляпа и призрачные огоньки
                ring(w, x, y0 + 0.03, z, 0.45, 0x2A1A3A, 0.9f);
                ring(w, x, y0 + 0.10, z, 0.29, 0xFF7A1A, 0.8f);
                ring(w, x, y0 + 0.22, z, 0.22, 0x4B0082, 0.8f);
                ring(w, x, y0 + 0.36, z, 0.14, 0x4B0082, 0.8f);
                dot(w, x, y0 + 0.52, z, 0x4B0082, 0.9f);
                Fx.add(w, ParticleTypes.WITCH, x + (RNG.nextDouble() - 0.5), y + RNG.nextDouble() * h, z + (RNG.nextDouble() - 0.5), 0, 0.02, 0);
                Fx.add(w, ParticleTypes.SOUL_FIRE_FLAME, x + (RNG.nextDouble() - 0.5) * 0.6, y + 0.05, z + (RNG.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
            }
            case 2 -> { // колпак Деда Мороза, снег и салют
                ring(w, x, y0 + 0.03, z, 0.31, 0xFFFFFF, 1.0f);
                ring(w, x, y0 + 0.12, z, 0.26, 0xE02020, 0.9f);
                ring(w, x, y0 + 0.24, z, 0.20, 0xE02020, 0.9f);
                ring(w, x, y0 + 0.36, z, 0.13, 0xE02020, 0.9f);
                dot(w, x, y0 + 0.50, z, 0xFFFFFF, 1.5f);
                Fx.add(w, ParticleTypes.SNOWFLAKE, x + (RNG.nextDouble() - 0.5) * 1.5, y0 + 0.5, z + (RNG.nextDouble() - 0.5) * 1.5, 0, -0.03, 0);
                if (p.age % 40 == 0) {
                    for (int i = 0; i < 10; i++) {
                        Fx.add(w, ParticleTypes.FIREWORK, x, y0 + 1.6, z,
                                (RNG.nextDouble() - 0.5) * 0.15, (RNG.nextDouble() - 0.3) * 0.15, (RNG.nextDouble() - 0.5) * 0.15);
                    }
                }
            }
            case 3 -> { // праздничный колпак и конфетти
                for (int i = 0; i < 4; i++) {
                    double r = 0.27 - i * 0.065;
                    ring(w, x, y0 + 0.06 + i * 0.13, z, Math.max(0.03, r), CosmeticsPack.rainbow(i * 0.22 + now / 4000.0), 0.9f);
                }
                Fx.add(w, ParticleTypes.END_ROD, x, y0 + 0.62, z, 0, 0.01, 0);
                for (int i = 0; i < 3; i++) {
                    Fx.add(w, new DustParticleEffect(CosmeticsPack.rainbow(RNG.nextDouble()), 0.8f),
                            x + (RNG.nextDouble() - 0.5) * 1.4, y0 + 0.9, z + (RNG.nextDouble() - 0.5) * 1.4, 0, -0.05, 0);
                }
                if (p.age % 20 == 0) Fx.add(w, ParticleTypes.HEART, x + (RNG.nextDouble() - 0.5) * 0.5, y0 + 0.3, z + (RNG.nextDouble() - 0.5) * 0.5, 0, 0.03, 0);
            }
            case 4 -> { // венок из цветов, лепестки и сердечки
                int n = 12;
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n;
                    int c = i % 3 == 0 ? 0xFF7AD9 : i % 3 == 1 ? 0xFFFFFF : 0xFFC857;
                    dot(w, x + Math.cos(a) * 0.32, y0 + 0.05, z + Math.sin(a) * 0.32, c, 1.1f);
                }
                Fx.add(w, ParticleTypes.CHERRY_LEAVES, x + (RNG.nextDouble() - 0.5) * 1.2, y0 + 0.6, z + (RNG.nextDouble() - 0.5) * 1.2, 0, -0.03, 0);
                if (p.age % 20 == 0) Fx.add(w, ParticleTypes.HEART, x + (RNG.nextDouble() - 0.5) * 0.5, y0 + 0.3, z + (RNG.nextDouble() - 0.5) * 0.5, 0, 0.03, 0);
            }
            default -> { }
        }
    }

    /** Фоновые праздничные частицы вокруг игрока (видны в любом виде). */
    static void worldFx(MinecraftClient mc, int act) {
        var p = mc.player;
        var w = mc.world;
        boolean sky = w.isSkyVisible(p.getBlockPos());
        for (int i = 0; i < 2; i++) {
            double x = p.getX() + (RNG.nextDouble() - 0.5) * 28;
            double z = p.getZ() + (RNG.nextDouble() - 0.5) * 28;
            double top = p.getY() + 4 + RNG.nextDouble() * 5;
            switch (act) {
                case 1 -> Fx.add(w, ParticleTypes.ASH, x, p.getY() + RNG.nextDouble() * 5, z, 0, -0.01, 0);
                case 2 -> { if (sky) Fx.add(w, ParticleTypes.SNOWFLAKE, x, top, z, 0, -0.08, 0); }
                case 3 -> { if (sky) Fx.add(w, new DustParticleEffect(CosmeticsPack.rainbow(RNG.nextDouble()), 0.9f), x, top, z, 0, -0.06, 0); }
                case 4 -> { if (sky) Fx.add(w, ParticleTypes.CHERRY_LEAVES, x, top, z, 0, -0.04, 0); }
                default -> { }
            }
        }
    }

    /** Поздравление вверху экрана на несколько секунд. */
    public static void render(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        if (banner == null || applied == 0 || mc.player == null) return;
        long age = System.currentTimeMillis() - bannerTime;
        if (age > 5000) return;
        int w = tr.getWidth(banner) + 28;
        int x = mc.getWindow().getScaledWidth() / 2 - w / 2;
        int y = 70 + (int) ((1f - Math.min(1f, age / 300f)) * -14);
        Gfx.box(ctx, x, y, w, 20);
        ctx.fill(x, y, x + 2, y + 20, Gfx.wave(0f));
        Gfx.gradientText(ctx, tr, banner, x + 14, y + 6, 0f);
    }

    private Holiday() {}
}
