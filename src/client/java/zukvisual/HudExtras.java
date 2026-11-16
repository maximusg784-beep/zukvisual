package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Дополнительные косметические HUD-элементы: Info, Keystrokes, StatusBars, HitNumbers, Notifications. */
public final class HudExtras {
    static final Random RNG = new Random();

    static class Pending { LivingEntity e; float hp; int ticks; Pending(LivingEntity e) { this.e = e; this.hp = e.getHealth(); } }
    record Pop(String text, long t, int dx) {}
    record Note(String text, long t) {}

    static final List<Pending> PENDING = new ArrayList<>();
    static final List<Pop> POPS = new ArrayList<>();
    static final List<Note> NOTES = new ArrayList<>();

    public static void notify(String text) {
        if (!Modules.NOTIFICATIONS.enabled) return;
        NOTES.add(new Note(text, System.currentTimeMillis()));
        if (NOTES.size() > 4) NOTES.remove(0);
    }

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && (Modules.HIT_NUMBERS.enabled || Modules.STATS.enabled) && entity instanceof LivingEntity le) {
                PENDING.add(new Pending(le));
            }
            return ActionResult.PASS;
        });

        // через 2 тика смотрим, сколько здоровья потерла цель
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Iterator<Pending> it = PENDING.iterator();
            while (it.hasNext()) {
                Pending p = it.next();
                if (++p.ticks < 2) continue;
                float d = p.hp - p.e.getHealth();
                if (d > 0.05f) {
                    Stats.damage += d;
                    if (Modules.HIT_NUMBERS.enabled) POPS.add(new Pop(String.format("-%.1f", d), System.currentTimeMillis(), RNG.nextInt(41) - 20));
                }
                it.remove();
            }
        });
    }

    public static void render(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        if (Modules.INFO.enabled) drawInfo(ctx, mc, tr);
        if (Modules.KEYSTROKES.enabled) drawKeys(ctx, mc, tr);
        if (Modules.STATUS.enabled) drawStatus(ctx, mc, tr);
        if (Modules.HIT_NUMBERS.enabled) drawPops(ctx, mc, tr);
        drawNotes(ctx, mc, tr);
    }

    static void drawInfo(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        var p = mc.player;
        String dir = switch (p.getHorizontalFacing()) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case WEST -> "W";
            case EAST -> "E";
            default -> "?";
        };
        String s = "XYZ " + p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ()
                + " | " + dir + " | " + String.format("%.1f", ExtraHud.bps) + " bps";
        String s2 = (Modules.DEATH_POS.enabled && ExtraHud.lastDeath != null) ? "Смерть: " + ExtraHud.lastDeath : null;
        int w = Math.max(tr.getWidth(s), s2 == null ? 0 : tr.getWidth(s2)) + 12;
        int ix = 6 + Layout.dx("Info"), iy = 26 + Layout.dy("Info"), ih = s2 == null ? 16 : 28;
        Layout.reg("Info", ix, iy, w, ih);
        Gfx.box(ctx, ix, iy, w, ih);
        ctx.drawTextWithShadow(tr, s, ix + 6, iy + 4, 0xFFE0E0F0);
        if (s2 != null) ctx.drawTextWithShadow(tr, s2, ix + 6, iy + 16, 0xFF8888A0);
    }

    static void key(DrawContext ctx, TextRenderer tr, String label, boolean down, int x, int y, int w, int h) {
        int bg = down ? Gfx.withAlpha(Gfx.wave(x * 0.01f), 0xC0) : 0x90101018;
        ctx.fill(x, y, x + w, y + h, bg);
        ctx.drawTextWithShadow(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2 + 1, 0xFFFFFFFF);
    }

    static void drawKeys(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        int x = 6 + Layout.dx("Keystrokes"), y = mc.getWindow().getScaledHeight() / 2 - 30 + Layout.dy("Keystrokes");
        Layout.reg("Keystrokes", x, y, 58, 68);
        var o = mc.options;
        key(ctx, tr, "W", o.forwardKey.isPressed(), x + 20, y, 18, 18);
        key(ctx, tr, "A", o.leftKey.isPressed(), x, y + 20, 18, 18);
        key(ctx, tr, "S", o.backKey.isPressed(), x + 20, y + 20, 18, 18);
        key(ctx, tr, "D", o.rightKey.isPressed(), x + 40, y + 20, 18, 18);
        key(ctx, tr, "SPACE", o.jumpKey.isPressed(), x, y + 40, 58, 12);
        key(ctx, tr, "L " + ExtraHud.cps(true), o.attackKey.isPressed(), x, y + 54, 28, 14);
        key(ctx, tr, "R " + ExtraHud.cps(false), o.useKey.isPressed(), x + 30, y + 54, 28, 14);
    }

    static void bar(DrawContext ctx, TextRenderer tr, String label, float value, float max, int x, int y) {
        ctx.drawTextWithShadow(tr, label, x + 4, y + 1, 0xFFB8B8C8);
        int bx = x + 34, bw = 70;
        ctx.fill(bx, y + 2, bx + bw, y + 8, 0x40FFFFFF);
        int fw = (int) (bw * Math.max(0f, Math.min(1f, value / max)));
        boolean low = label.equals("HP") && value / max < 0.3f;
        Gfx.gradBar(ctx, bx, y + 2, fw, bw, 6, low);
    }

    static void drawStatus(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        var p = mc.player;
        int x = 6 + Layout.dx("StatusBars"), y = mc.getWindow().getScaledHeight() - 67 + Layout.dy("StatusBars");
        Layout.reg("StatusBars", x, y, 112, 57);
        Gfx.box(ctx, x, y, 112, 57);
        bar(ctx, tr, "HP", p.getHealth(), p.getMaxHealth(), x, y + 5);
        bar(ctx, tr, "ARM", p.getArmor(), 20f, x, y + 18);
        bar(ctx, tr, "FOOD", p.getHungerManager().getFoodLevel(), 20f, x, y + 31);
        bar(ctx, tr, "SAT", p.getHungerManager().getSaturationLevel(), 20f, x, y + 44);
    }

    static void drawPops(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        long now = System.currentTimeMillis();
        int cx = mc.getWindow().getScaledWidth() / 2, cy = mc.getWindow().getScaledHeight() / 2;
        POPS.removeIf(p -> now - p.t() > 900);
        for (Pop p : POPS) {
            float prog = (now - p.t()) / 900f;
            int alpha = (int) (255 * (1 - prog));
            if (alpha < 8) continue;
            int x = cx + 14 + p.dx();
            int y = cy - 8 - (int) (24 * prog);
            ctx.drawTextWithShadow(tr, p.text(), x, y, Gfx.withAlpha(Gfx.wave(prog), alpha));
        }
    }

    static void drawNotes(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        long now = System.currentTimeMillis();
        NOTES.removeIf(n -> now - n.t() > 2200);
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        int i = 0;
        for (int k = NOTES.size() - 1; k >= 0; k--) {
            Note n = NOTES.get(k);
            long age = now - n.t();
            float slide = Math.min(1f, age / 250f);
            int w = tr.getWidth(n.text()) + 14;
            int x = sw - (int) (w * slide) - 6;
            int y = sh - 30 - i * 20;
            Gfx.box(ctx, x, y, w, 16);
            ctx.fill(x, y, x + 2, y + 16, Gfx.wave(i * 0.15f));
            ctx.drawTextWithShadow(tr, n.text(), x + 8, y + 4, 0xFFFFFFFF);
            i++;
        }
    }

    private HudExtras() {}
}
