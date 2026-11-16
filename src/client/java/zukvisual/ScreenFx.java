package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * LowFire: ванильный огонь на экране скрывается, вместо него снизу рисуется тонкая полоса пламени.
 * Brightness: ставит ванильную яркость на максимум (Bright, как ползунок в настройках видео)
 * и возвращает прежнюю при выключении. Выше ванильного максимума не поднимается.
 */
public final class ScreenFx {
    static boolean applied = false;
    static Double savedGamma;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.options == null) return;
            if (Modules.BRIGHTNESS.enabled && !applied) {
                savedGamma = mc.options.getGamma().getValue();
                mc.options.getGamma().setValue(1.0);
                applied = true;
            } else if (!Modules.BRIGHTNESS.enabled && applied) {
                restore(mc);
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            if (applied) restore(mc);
        });
    }

    static void restore(MinecraftClient mc) {
        if (savedGamma != null) mc.options.getGamma().setValue(savedGamma);
        applied = false;
    }

    /** LowHP: при здоровье 3 сердца и меньше по краям экрана мерцает красная рамка. */
    public static void renderLowHp(DrawContext ctx, MinecraftClient mc) {
        if (!Modules.LOW_HP.enabled || mc.player == null || mc.options.hudHidden) return;
        float hp = mc.player.getHealth();
        if (hp <= 0f || hp > 6f) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        int a = (int) (70 + 50 * Math.sin(System.currentTimeMillis() / 180.0));
        ctx.fillGradient(0, 0, sw, 44, (a << 24) | 0xFF0000, 0x00FF0000);
        ctx.fillGradient(0, sh - 44, sw, sh, 0x00FF0000, (a << 24) | 0xFF0000);
        for (int i = 0; i < 24; i++) { // боковые края: ступенчатое затухание
            int c = (Math.max(0, a * (24 - i) / 24) << 24) | 0xFF0000;
            ctx.fill(i, 0, i + 1, sh, c);
            ctx.fill(sw - i - 1, 0, sw - i, sh, c);
        }
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (!Modules.LOW_FIRE.enabled || mc.player == null || !mc.player.isOnFire()
                || mc.currentScreen != null || !mc.options.getPerspective().isFirstPerson()) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        long now = System.currentTimeMillis();
        for (int x = 0; x < sw; x += 8) {
            int h = 8 + (int) (6 * Math.sin(now / 130.0 + x * 0.09)); // мерцающие языки пламени
            ctx.fillGradient(x, sh - h, Math.min(sw, x + 8), sh, 0x00FF8A00, 0xB0FF4500);
        }
    }

    private ScreenFx() {}
}
