package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Stats: статистика сессии. Время, убийства/смерти (K/D), нанесённый урон, график FPS за минуту. */
public final class Stats {
    static final long START = System.currentTimeMillis();
    static int deaths = 0;
    static double damage = 0;
    static final int[] FPS = new int[60];
    static int fpsCount = 0, fpsPos = 0, tickCounter = 0;
    static boolean wasDead = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;
            boolean dead = mc.player.getHealth() <= 0f;
            if (dead && !wasDead) deaths++;
            wasDead = dead;

            if (++tickCounter % 20 == 0) { // раз в секунду
                FPS[fpsPos] = mc.getCurrentFps();
                fpsPos = (fpsPos + 1) % FPS.length;
                fpsCount = Math.min(FPS.length, fpsCount + 1);
            }
        });
    }

    public static void render(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        if (!Modules.STATS.enabled || mc.player == null) return;
        long sec = (System.currentTimeMillis() - START) / 1000;
        int kills = Cosmetics.kills;
        double kd = deaths == 0 ? kills : kills / (double) deaths;

        int sum = 0, min = Integer.MAX_VALUE, max = 0;
        for (int i = 0; i < fpsCount; i++) {
            sum += FPS[i];
            min = Math.min(min, FPS[i]);
            max = Math.max(max, FPS[i]);
        }
        int avg = fpsCount == 0 ? 0 : sum / fpsCount;
        if (fpsCount == 0) min = 0;

        int w = 132, h = 78;
        int x = 6 + Layout.dx("Stats"), y = 130 + Layout.dy("Stats");
        Layout.reg("Stats", x, y, w, h);
        Gfx.box(ctx, x, y, w, h);

        int c = 0xFFE0E0F0;
        ctx.drawTextWithShadow(tr, "Время  " + (sec / 60) + ":" + String.format("%02d", sec % 60), x + 6, y + 5, c);
        ctx.drawTextWithShadow(tr, "K/D  " + kills + "/" + deaths + "  (" + String.format("%.2f", kd) + ")", x + 6, y + 16, c);
        ctx.drawTextWithShadow(tr, "Урон  " + String.format("%.1f", damage), x + 6, y + 27, c);
        ctx.drawTextWithShadow(tr, "FPS  ср " + avg + "  мин " + min, x + 6, y + 38, c);

        // график FPS за последнюю минуту
        int scale = Math.max(60, max);
        for (int i = 0; i < fpsCount; i++) {
            int idx = (fpsPos - fpsCount + i + FPS.length) % FPS.length;
            int bh = Math.max(1, (int) (18f * FPS[idx] / scale));
            ctx.fill(x + 6 + i * 2, y + h - 5 - bh, x + 6 + i * 2 + 1, y + h - 5, Gfx.mix(Gfx.A, Gfx.B, i / 59f));
        }
    }

    private Stats() {}
}
