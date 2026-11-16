package zukvisual;

import java.util.Properties;

/** Общие настройки вида: цвет и размер косметики, прозрачность панелей, густота снега. */
public final class Settings {
    public static int colorMode = 0, sizeMode = 1, opacity = 1, snow = 1, worldStyle = 0, holidayMode = 0;

    public static final String[] COLOR = {"Theme", "Rainbow"};
    public static final String[] SIZE = {"Small", "Normal", "Big"};
    public static final String[] OPACITY = {"Glass", "Normal", "Solid"};
    public static final String[] SNOW = {"Light", "Normal", "Heavy"};
    public static final String[] WORLD = {"Fireflies", "Embers", "Petals", "Stars"};
    public static final String[] HOLIDAY = {"Auto", "Halloween", "NewYear", "Birthday", "Women"};

    static final float[] SIZE_V = {0.7f, 1.0f, 1.4f};
    static final int[] ALPHA = {0xA0, 0xD0, 0xF0};
    public static final int[] SNOW_V = {3, 6, 12};

    /** Цвет частиц косметики: цвет темы или радуга. */
    public static int fxColor(float t) {
        if (colorMode == 1) return CosmeticsPack.rainbow(t * 0.6 + System.currentTimeMillis() / 3000.0);
        return Gfx.mix(Gfx.A, Gfx.B, t) & 0xFFFFFF;
    }

    public static float scale() { return SIZE_V[sizeMode]; }

    public static void applyOpacity() {
        Gfx.BG_TOP = (ALPHA[opacity] << 24) | 0x14141E;
        Gfx.BG_BOT = (ALPHA[opacity] << 24) | 0x0A0A12;
    }

    static int geti(Properties p, String key, int def, int n) {
        try {
            return Math.max(0, Math.min(n - 1, Integer.parseInt(p.getProperty(key, String.valueOf(def)))));
        } catch (Exception e) {
            return def;
        }
    }

    public static void load(Properties p) {
        colorMode = geti(p, "ColorMode", 0, COLOR.length);
        sizeMode = geti(p, "FxSize", 1, SIZE.length);
        opacity = geti(p, "Opacity", 1, OPACITY.length);
        snow = geti(p, "SnowDensity", 1, SNOW.length);
        worldStyle = geti(p, "WorldStyle", 0, WORLD.length);
        holidayMode = geti(p, "HolidayMode", 0, HOLIDAY.length);
        applyOpacity();
    }

    public static void save(Properties p) {
        p.setProperty("ColorMode", String.valueOf(colorMode));
        p.setProperty("FxSize", String.valueOf(sizeMode));
        p.setProperty("Opacity", String.valueOf(opacity));
        p.setProperty("SnowDensity", String.valueOf(snow));
        p.setProperty("WorldStyle", String.valueOf(worldStyle));
        p.setProperty("HolidayMode", String.valueOf(holidayMode));
    }

    private Settings() {}
}
