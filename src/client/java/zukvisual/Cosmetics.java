package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;

import java.util.Random;

/**
 * KillEffect: анимация убийства (4 стиля), баннер и счётчик убийств.
 * Косметика на себе: Halo (нимб), Aura (вихрь), Wings (крылья из частиц).
 * Косметика видна от третьего лица и только на твоём клиенте.
 */
public final class Cosmetics {
    static final Random RNG = new Random();

    public static int killStyle = 0;
    public static final String[] KILL_STYLES = {"Burst", "Souls", "Fireworks", "Totem"};

    static LivingEntity watch;
    static long watchTime;
    static boolean handled = true;
    static int kills = 0, streak = 0;
    static String bannerName = null;
    static long bannerTime = 0;
    static boolean wasDead = false;

    /** Вызывается при каждом ударе по живой цели. */
    public static void onAttack(LivingEntity target) {
        watch = target;
        watchTime = System.currentTimeMillis();
        handled = false;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null) return;
            long now = System.currentTimeMillis();

            // убийство: цель, по которой били последние 6 секунд, умерла
            if (watch != null && !handled && !watch.isAlive() && now - watchTime < 6000) {
                handled = true;
                if (Modules.KILL_FX.enabled) {
                    kills++;
                    streak++;
                    bannerName = watch.getName().getString();
                    bannerTime = now;
                    playKill(watch);
                }
            }
            if (watch != null && now - watchTime > 6000) watch = null;

            // серия сбрасывается, когда умираешь сам
            boolean dead = mc.player.getHealth() <= 0f;
            if (dead && !wasDead) streak = 0;
            wasDead = dead;

            cosmetics(mc, now);
        });
    }

    static DustParticleEffect dust(float t, float scale) {
        return new DustParticleEffect(Settings.fxColor(t), scale * Settings.scale());
    }

    static void playKill(LivingEntity e) {
        var w = MinecraftClient.getInstance().world;
        if (w == null) return;
        double x = e.getX(), y = e.getY(), z = e.getZ(), h = e.getHeight();

        // расходящееся кольцо у ног (всегда)
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI * 2 / 32;
            Fx.addKeep(w, dust(i / 31f, 1.2f), x + Math.cos(a) * 0.4, y + 0.05, z + Math.sin(a) * 0.4,
                    Math.cos(a) * 0.18, 0.02, Math.sin(a) * 0.18);
        }

        switch (killStyle) {
            case 1 -> { // души поднимаются вверх
                for (int i = 0; i < 14; i++) {
                    Fx.addKeep(w, ParticleTypes.SOUL,
                            x + (RNG.nextDouble() - 0.5) * 0.5, y + RNG.nextDouble() * h, z + (RNG.nextDouble() - 0.5) * 0.5,
                            0, 0.08 + RNG.nextDouble() * 0.05, 0);
                }
            }
            case 2 -> { // салют
                for (int i = 0; i < 30; i++) {
                    Fx.addKeep(w, ParticleTypes.FIREWORK, x, y + h * 0.5, z,
                            (RNG.nextDouble() - 0.5) * 0.4, (RNG.nextDouble() - 0.2) * 0.4, (RNG.nextDouble() - 0.5) * 0.4);
                }
            }
            case 3 -> { // как тотем бессмертия
                for (int i = 0; i < 40; i++) {
                    Fx.addKeep(w, ParticleTypes.TOTEM_OF_UNDYING, x, y + h * 0.5, z,
                            (RNG.nextDouble() - 0.5) * 0.5, RNG.nextDouble() * 0.5, (RNG.nextDouble() - 0.5) * 0.5);
                }
            }
            default -> { // сфера градиентных частиц
                int n = 36;
                for (int i = 0; i < n; i++) {
                    double yy = 1 - 2.0 * (i + 0.5) / n;
                    double r = Math.sqrt(1 - yy * yy);
                    double phi = i * Math.PI * (3 - Math.sqrt(5));
                    Fx.addKeep(w, dust(i / (float) n, 1.0f), x, y + h * 0.5, z,
                            Math.cos(phi) * r * 0.2, yy * 0.2, Math.sin(phi) * r * 0.2);
                }
            }
        }
        Sfx.play(Sfx.kill(), 1.1f);
    }

    static void cosmetics(MinecraftClient mc, long now) {
        var p = mc.player;
        var w = mc.world;
        if (p.age % Fx.interval() != 0 || mc.options.getPerspective().isFirstPerson()) return;
        CosmeticsPack.tick(mc, now);
        boolean any = Modules.HALO.enabled || Modules.AURA.enabled || Modules.WINGS.enabled;
        if (!any) return;
        double x = p.getX(), y = p.getY(), z = p.getZ();

        if (Modules.HALO.enabled) {
            double hy = y + p.getHeight() + 0.25;
            for (int i = 0; i < 10; i++) {
                double a = now / 500.0 + i * Math.PI * 2 / 10;
                Fx.add(w, dust(i / 9f, 0.6f), x + Math.cos(a) * 0.35, hy, z + Math.sin(a) * 0.35, 0, 0, 0);
            }
        }

        if (Modules.AURA.enabled) {
            for (int i = 0; i < 6; i++) {
                double a = now / 350.0 + i * Math.PI / 3;
                double yy = ((now / 700.0 + i / 6.0) % 1.0) * p.getHeight();
                Fx.add(w, dust(i / 5f, 0.7f), x + Math.cos(a) * 0.6, y + yy, z + Math.sin(a) * 0.6, 0, 0, 0);
            }
        }

        if (Modules.WINGS.enabled) {
            double rad = Math.toRadians(p.bodyYaw);
            double rightX = -Math.cos(rad), rightZ = -Math.sin(rad);
            double backX = Math.sin(rad), backZ = -Math.cos(rad);
            double flap = 0.85 + 0.15 * Math.sin(now / 220.0); // взмах
            for (int side = -1; side <= 1; side += 2) {
                for (int k = 0; k < 10; k++) {
                    double t = k / 9.0;
                    double rv = side * (0.2 + 0.9 * t * flap);
                    double up = 1.3 + 0.45 * Math.sin(t * Math.PI) - 0.45 * t;
                    double bv = 0.22 + 0.12 * t;
                    Fx.add(w, dust((float) t, 0.7f),
                            x + rightX * rv + backX * bv, y + up, z + rightZ * rv + backZ * bv, 0, 0, 0);
                }
            }
        }
    }

    public static void render(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        if (!Modules.KILL_FX.enabled || mc.player == null) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        long now = System.currentTimeMillis();

        // баннер убийства: выезжает сверху и держится около полутора секунд
        if (bannerName != null && now - bannerTime < 1600) {
            long age = now - bannerTime;
            float in = Math.min(1f, age / 220f);
            String name = bannerName.length() > 16 ? bannerName.substring(0, 16) : bannerName;
            int w = tr.getWidth("УБИТ  " + name) + 20;
            int x = sw / 2 - w / 2, y = sh / 4 - 12 + (int) ((1f - in) * -14);
            Gfx.box(ctx, x, y, w, 20);
            ctx.fill(x, y, x + 2, y + 20, Gfx.wave(0f));
            Gfx.gradientText(ctx, tr, "УБИТ", x + 10, y + 6, 0f);
            ctx.drawTextWithShadow(tr, name, x + 10 + tr.getWidth("УБИТ  "), y + 6, 0xFFFFFFFF);
        }

        // счётчик убийств
        if (kills > 0) {
            String s = "Kills " + kills + "  x" + streak;
            int w = tr.getWidth(s) + 14;
            int x = sw - w - 6 + Layout.dx("Kills"), y = sh / 2 + 64 + Layout.dy("Kills");
            Layout.reg("Kills", x, y, w, 16);
            Gfx.box(ctx, x, y, w, 16);
            ctx.drawTextWithShadow(tr, s, x + 7, y + 4, 0xFFE0E0F0);
        }
    }

    private Cosmetics() {}
}
