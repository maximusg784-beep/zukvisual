package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;

/**
 * Информационные панели боя (только про твои действия и состояние):
 *  HitCounter: сколько ударов по цели и комбо подряд без получения урона;
 *  HitRange: дистанция последнего удара и рекорд;
 *  MaceHelper: твоя высота падения и бонус урона булавы по правилам игры (удар ты наносишь сам).
 */
public final class Combat {
    static LivingEntity target;
    static long lastHit = 0;
    static int hits = 0, combo = 0;
    static float lastRange = 0, maxRange = 0;
    static boolean wasHurt = false;

    public static void onAttack(LivingEntity t, float range) {
        long now = System.currentTimeMillis();
        if (t != target || now - lastHit > 5000) hits = 0;
        target = t;
        hits++;
        combo++;
        lastHit = now;
        lastRange = range;
        maxRange = Math.max(maxRange, range);
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;
            boolean hurt = mc.player.hurtTime > 0;
            if (hurt && !wasHurt) combo = 0; // получил урон: комбо сброшено
            wasHurt = hurt;
            if (lastHit != 0 && System.currentTimeMillis() - lastHit > 5000) combo = 0;
        });
    }

    /** Бонус урона булавы за падение (правила игры): 4 за блок до 3, 2 за блок до 8, дальше 1. */
    static double maceBonus(double d) {
        if (d <= 3) return 4 * d;
        if (d <= 8) return 12 + 2 * (d - 3);
        return 22 + (d - 8);
    }

    public static void render(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        if (mc.player == null) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        long now = System.currentTimeMillis();

        boolean recent = lastHit != 0 && now - lastHit < 8000;
        if (recent && (Modules.HIT_COUNTER.enabled || Modules.HIT_RANGE.enabled)) {
            int lines = (Modules.HIT_COUNTER.enabled ? 1 : 0) + (Modules.HIT_RANGE.enabled ? 1 : 0);
            int w = 130, h = 6 + lines * 11;
            int x = sw / 2 + 24 + Layout.dx("HitCounter"), y = sh / 2 + 58 + Layout.dy("HitCounter");
            Layout.reg("HitCounter", x, y, w, h);
            Gfx.box(ctx, x, y, w, h);
            int ly = y + 5;
            if (Modules.HIT_COUNTER.enabled) {
                ctx.drawTextWithShadow(tr, "Hits " + hits + "   Combo " + combo, x + 6, ly, 0xFFE0E0F0);
                ly += 11;
            }
            if (Modules.HIT_RANGE.enabled) {
                ctx.drawTextWithShadow(tr, String.format("Range %.2f  max %.2f", lastRange, maxRange), x + 6, ly, 0xFFB8B8C8);
            }
        }

        if (Modules.MACE_HELPER.enabled && mc.player.getMainHandStack().isOf(Items.MACE)) {
            double fall = mc.player.fallDistance;
            String s = fall < 1.5
                    ? "Mace: падение " + String.format("%.1f", fall) + " м (нужно от 1.5)"
                    : "Mace: падение " + String.format("%.1f", fall) + " м  бонус +" + String.format("%.1f", maceBonus(fall));
            int w = tr.getWidth(s) + 14;
            int x = sw / 2 - w / 2 + Layout.dx("MaceHelper"), y = sh / 2 + 90 + Layout.dy("MaceHelper");
            Layout.reg("MaceHelper", x, y, w, 16);
            Gfx.box(ctx, x, y, w, 16);
            ctx.drawTextWithShadow(tr, s, x + 7, y + 4, fall >= 1.5 ? 0xFF22E58A : 0xFFE0E0F0);
        }
    }

    private Combat() {}
}
