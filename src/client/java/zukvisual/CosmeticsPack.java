package zukvisual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.Random;

/**
 * Набор косметики на себе (частицы, только клиент, видны от третьего лица):
 * Hearts, Crown, Stars, Tail, Footprints, Sparkles, FlameFeet, Cloud, Helix, RainbowTrail.
 */
public final class CosmeticsPack {
    static final Random RNG = new Random();

    static int rainbow(double t) {
        int r = (int) (127 + 127 * Math.sin(2 * Math.PI * t));
        int g = (int) (127 + 127 * Math.sin(2 * Math.PI * t + 2 * Math.PI / 3));
        int b = (int) (127 + 127 * Math.sin(2 * Math.PI * t + 4 * Math.PI / 3));
        return (r << 16) | (g << 8) | b;
    }

    static DustParticleEffect themed(float t, float scale) {
        return new DustParticleEffect(Settings.fxColor(t), scale * Settings.scale());
    }

    static void tick(MinecraftClient mc, long now) {
        var p = mc.player;
        var w = mc.world;
        double x = p.getX(), y = p.getY(), z = p.getZ(), h = p.getHeight();
        double rad = Math.toRadians(p.bodyYaw);
        double rightX = -Math.cos(rad), rightZ = -Math.sin(rad);
        double backX = Math.sin(rad), backZ = -Math.cos(rad);
        boolean moving = p.getVelocity().horizontalLengthSquared() > 0.003;

        // сердечки над головой
        if (Modules.HEARTS.enabled && p.age % 10 == 0) {
            Fx.add(w, ParticleTypes.HEART,
                    x + (RNG.nextDouble() - 0.5) * 0.5, y + h + 0.3, z + (RNG.nextDouble() - 0.5) * 0.5, 0, 0.03, 0);
        }

        // корона с зубцами
        if (Modules.CROWN.enabled) {
            double hy = y + h + 0.05;
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI * 2 / 10;
                double spike = (i % 2 == 0) ? 0.28 : 0.0;
                Fx.add(w, new DustParticleEffect(0xFFC857, 0.7f),
                        x + Math.cos(a) * 0.3, hy + spike, z + Math.sin(a) * 0.3, 0, 0, 0);
            }
        }

        // три звезды вращаются на уровне пояса
        if (Modules.STARS.enabled) {
            for (int i = 0; i < 3; i++) {
                double a = now / 600.0 + i * Math.PI * 2 / 3;
                Fx.add(w, ParticleTypes.END_ROD,
                        x + Math.cos(a) * 0.8, y + h * 0.55 + Math.sin(now / 400.0 + i) * 0.2, z + Math.sin(a) * 0.8, 0, 0, 0);
            }
        }

        // хвост, который покачивается
        if (Modules.TAIL.enabled) {
            for (int k = 0; k < 8; k++) {
                double t = k / 7.0;
                double sway = Math.sin(now / 300.0 - t * 2.0) * 0.25 * t;
                double bv = 0.25 + t * 0.8;
                Fx.add(w, themed((float) t, 0.8f),
                        x + backX * bv + rightX * sway, y + h * 0.45 - t * t * 0.25, z + backZ * bv + rightZ * sway, 0, 0, 0);
            }
        }

        // следы ног
        if (Modules.FOOTPRINTS.enabled && p.isOnGround() && moving && p.age % 6 == 0) {
            int side = ((p.age / 6) % 2 == 0) ? 1 : -1;
            Fx.add(w, themed(RNG.nextFloat(), 0.8f),
                    x + rightX * 0.15 * side, y + 0.02, z + rightZ * 0.15 * side, 0, 0, 0);
        }

        // искры вокруг тела
        if (Modules.SPARKLES.enabled) {
            for (int i = 0; i < 2; i++) {
                Fx.add(w, ParticleTypes.ELECTRIC_SPARK,
                        x + (RNG.nextDouble() - 0.5) * 1.0, y + RNG.nextDouble() * h, z + (RNG.nextDouble() - 0.5) * 1.0, 0, 0, 0);
            }
        }

        // огонь у ног (только вид, урона нет)
        if (Modules.FLAME_FEET.enabled) {
            for (int i = 0; i < 2; i++) {
                double a = RNG.nextDouble() * Math.PI * 2;
                Fx.add(w, ParticleTypes.FLAME,
                        x + Math.cos(a) * 0.3, y + 0.05, z + Math.sin(a) * 0.3, 0, 0.02, 0);
            }
        }

        // тучка с дождём над головой
        if (Modules.CLOUD.enabled) {
            double cy = y + h + 0.8;
            for (int i = 0; i < 3; i++) {
                Fx.add(w, ParticleTypes.CLOUD,
                        x + (RNG.nextDouble() - 0.5) * 0.6, cy, z + (RNG.nextDouble() - 0.5) * 0.6, 0, 0, 0);
            }
            Fx.add(w, ParticleTypes.RAIN,
                    x + (RNG.nextDouble() - 0.5) * 0.5, cy - 0.1, z + (RNG.nextDouble() - 0.5) * 0.5, 0, -0.2, 0);
        }

        // двойная спираль вокруг тела
        if (Modules.HELIX.enabled) {
            for (int s = 0; s < 2; s++) {
                for (int k = 0; k < 8; k++) {
                    double yy = k / 7.0 * h;
                    double a = now / 500.0 + yy * 5 + s * Math.PI;
                    Fx.add(w, themed(k / 7f, 0.7f),
                            x + Math.cos(a) * 0.55, y + yy, z + Math.sin(a) * 0.55, 0, 0, 0);
                }
            }
        }

        // свечение тела: крупные мягкие частицы по контуру
        if (Modules.BODY_GLOW.enabled) {
            for (int i = 0; i < 3; i++) {
                double a = RNG.nextDouble() * Math.PI * 2;
                Fx.add(w, themed(RNG.nextFloat(), 1.4f),
                        x + Math.cos(a) * 0.4, y + RNG.nextDouble() * h, z + Math.sin(a) * 0.4, 0, 0, 0);
            }
        }

        // звёздное свечение вокруг
        if (Modules.STAR_GLOW.enabled) {
            double a = RNG.nextDouble() * Math.PI * 2;
            Fx.add(w, ParticleTypes.END_ROD,
                    x + Math.cos(a) * 0.7, y + RNG.nextDouble() * h, z + Math.sin(a) * 0.7, 0, 0.02, 0);
        }

        // радужный трейл
        if (Modules.RAINBOW_TRAIL.enabled && moving) {
            for (int i = 0; i < 2; i++) {
                Fx.add(w, new DustParticleEffect(rainbow(now / 1500.0 + i * 0.1), 1.0f),
                        x + (RNG.nextDouble() - 0.5) * 0.4, y + 0.1 + RNG.nextDouble() * 0.2, z + (RNG.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
            }
        }
    }

    private CosmeticsPack() {}
}
