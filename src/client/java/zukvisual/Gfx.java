package zukvisual;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Общий стиль: тёмный фон + фиолетово-голубой градиент. */
public final class Gfx {
    public static int A = 0x7B5CFF;
    public static int B = 0x2DD4FF;

    public static final String[] THEME_NAMES = {"Violet", "Crimson", "Emerald", "Candy", "Ocean", "Gold", "Mono", "Toxic"};
    static final int[][] THEMES = {
            {0x7B5CFF, 0x2DD4FF}, {0xFF3B6B, 0xFF9A3B}, {0x22E58A, 0x2DD4FF}, {0xFF7AD9, 0xFFC857},
            {0x2D7BFF, 0x2DFFD4}, {0xFFC857, 0xFF7A3B}, {0xE0E0F0, 0x8888A0}, {0xB6FF3B, 0x22E58A}};
    public static int themeIndex = 0;

    public static void setTheme(int i) {
        themeIndex = ((i % THEMES.length) + THEMES.length) % THEMES.length;
        A = THEMES[themeIndex][0];
        B = THEMES[themeIndex][1];
    }

    public static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    static int lerpArgb(int a, int b, float t) {
        int al = (int) (((a >>> 24) & 255) * (1 - t) + ((b >>> 24) & 255) * t);
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (al << 24) | (r << 16) | (g << 8) | bl;
    }

    /** На сколько пикселей срезать строку, которая на j пикселей от края, чтобы получился скруглённый угол. */
    static int inset(int r, int j) {
        double dy = r - j - 0.5;
        return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    /** Скруглённый прямоугольник с вертикальным градиентом: углы построчно, середина одним вызовом. */
    public static void roundedGradient(DrawContext ctx, int x, int y, int w, int h, int r, int top, int bot) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        float d = Math.max(1, h - 1);
        for (int row = 0; row < r; row++) {
            int in = inset(r, row);
            ctx.fill(x + in, y + row, x + w - in, y + row + 1, lerpArgb(top, bot, row / d));
        }
        if (h - 2 * r > 0) {
            ctx.fillGradient(x, y + r, x + w, y + h - r, lerpArgb(top, bot, r / d), lerpArgb(top, bot, (h - r) / d));
        }
        for (int row = h - r; row < h; row++) {
            int in = inset(r, h - 1 - row);
            ctx.fill(x + in, y + row, x + w - in, y + row + 1, lerpArgb(top, bot, row / d));
        }
    }

    /** Градиентная полоска заполнения блоками по 4 пикселя (вместо вызова на каждый пиксель). */
    public static void gradBar(DrawContext ctx, int x, int y, int fw, int total, int h, boolean low) {
        for (int i = 0; i < fw; i += 4) {
            int seg = Math.min(4, fw - i);
            float t = i / (float) Math.max(1, total);
            ctx.fill(x + i, y, x + i + seg, y + h, low ? mix(0xFF3B3B, 0xFF9A3B, t) : mix(A, B, t));
        }
    }

    public static void box(DrawContext ctx, int x, int y, int w, int h) {
        roundedGradient(ctx, x, y, w, h, 3, BG_TOP, BG_BOT);
        gradientLine(ctx, x + 3, y, w - 6, 1);
    }
    public static int BG_TOP = 0xD014141E;
    public static int BG_BOT = 0xD00A0A12;

    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    public static int wave(float offset) {
        double time = System.currentTimeMillis() / 1500.0;
        float t = (float) ((Math.sin((time + offset) * Math.PI * 2) + 1) / 2);
        return mix(A, B, t);
    }

    public static void gradientText(DrawContext ctx, TextRenderer tr, String s, int x, int y, float phase) {
        int cx = x;
        for (int i = 0; i < s.length(); i += 2) { // по 2 символа на цвет: вдвое меньше вызовов
            String ch = s.substring(i, Math.min(s.length(), i + 2));
            ctx.drawTextWithShadow(tr, ch, cx, y, wave(phase + i * 0.06f));
            cx += tr.getWidth(ch);
        }
    }

    public static void gradientLine(DrawContext ctx, int x, int y, int w, int h) {
        int seg = 24;
        for (int i = 0; i < seg; i++) {
            ctx.fill(x + w * i / seg, y, x + w * (i + 1) / seg, y + h, mix(A, B, i / (float) seg));
        }
    }

    private Gfx() {}
}
