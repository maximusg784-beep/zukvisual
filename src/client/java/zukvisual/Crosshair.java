package zukvisual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.HitResult;

/** Свой прицел в стиле темы. Подсвечивается зелёным, когда курсор наведён на сущность. */
public final class Crosshair {
    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (!Modules.CROSSHAIR.enabled || mc.player == null || mc.options.hudHidden
                || mc.currentScreen != null || !mc.options.getPerspective().isFirstPerson()) return;

        int cx = mc.getWindow().getScaledWidth() / 2, cy = mc.getWindow().getScaledHeight() / 2;
        boolean onEntity = mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY;
        int gap = 3 + (mc.player.isSprinting() ? 2 : 0), len = 5;
        int c = onEntity ? 0xFF22E58A : Gfx.wave(0f);
        int shadow = 0x90000000;

        arm(ctx, cx - gap - len, cy, cx - gap, cy + 1, c, shadow);
        arm(ctx, cx + gap + 1, cy, cx + gap + 1 + len, cy + 1, c, shadow);
        arm(ctx, cx, cy - gap - len, cx + 1, cy - gap, c, shadow);
        arm(ctx, cx, cy + gap + 1, cx + 1, cy + gap + 1 + len, c, shadow);
    }

    private static void arm(DrawContext ctx, int x1, int y1, int x2, int y2, int c, int shadow) {
        ctx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, shadow); // тёмная обводка для читаемости
        ctx.fill(x1, y1, x2, y2, c);
    }

    private Crosshair() {}
}
