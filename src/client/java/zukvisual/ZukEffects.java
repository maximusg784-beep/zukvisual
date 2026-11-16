package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;

import java.util.Random;

/** Косметические эффекты: частицы удара (4 стиля), пузырь при ударе и трейл. Только клиент. */
public class ZukEffects {
    static final Random RNG = new Random();

    public static int hitStyle = 0;
    public static final String[] STYLES = {"Dust", "Hearts", "Stars", "Sparks"};

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity) {
                if (Modules.HIT_PARTICLES.enabled) spawnHit(entity);
                if (Modules.HIT_BUBBLE.enabled) spawnBubble(entity);
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!Modules.TRAIL.enabled || mc.player == null || mc.world == null) return;
            if (mc.player.getVelocity().horizontalLengthSquared() < 0.0004) return;
            if (mc.options.getPerspective().isFirstPerson()) return;
            for (int i = 0; i < 2; i++) {
                float t = (float) ((Math.sin(System.currentTimeMillis() / 600.0 + i) + 1) / 2);
                Fx.add(mc.world, 
                        new DustParticleEffect(Gfx.mix(Gfx.A, Gfx.B, t) & 0xFFFFFF, 1.0f),
                        mc.player.getX() + (RNG.nextDouble() - 0.5) * 0.4,
                        mc.player.getY() + 0.1 + RNG.nextDouble() * 0.2,
                        mc.player.getZ() + (RNG.nextDouble() - 0.5) * 0.4,
                        0, 0.01, 0);
            }
        });
    }

    static ParticleEffect hitFx(float t) {
        return switch (hitStyle) {
            case 1 -> ParticleTypes.HEART;
            case 2 -> ParticleTypes.END_ROD;
            case 3 -> ParticleTypes.ELECTRIC_SPARK;
            default -> new DustParticleEffect(Gfx.mix(Gfx.A, Gfx.B, t) & 0xFFFFFF, 1.2f);
        };
    }

    static void spawnHit(Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        double cy = target.getY() + target.getHeight() * 0.6;
        int count = hitStyle == 1 ? 5 : 14;
        for (int i = 0; i < count; i++) {
            Fx.addKeep(mc.world, hitFx(RNG.nextFloat()),
                    target.getX(), cy, target.getZ(),
                    (RNG.nextDouble() - 0.5) * 0.25,
                    (RNG.nextDouble() - 0.2) * 0.2,
                    (RNG.nextDouble() - 0.5) * 0.25);
        }
    }

    /** Расширяющийся «пузырь» из частиц вокруг цели. */
    static void spawnBubble(Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        double cy = target.getY() + target.getHeight() * 0.5;
        double rad = target.getWidth() * 0.5 + 0.5;
        int n = 28;
        for (int i = 0; i < n; i++) {
            double yy = 1 - 2.0 * (i + 0.5) / n;
            double r = Math.sqrt(1 - yy * yy);
            double phi = i * Math.PI * (3 - Math.sqrt(5));
            double dx = Math.cos(phi) * r, dz = Math.sin(phi) * r;
            Fx.addKeep(mc.world, 
                    new DustParticleEffect(Gfx.mix(Gfx.A, Gfx.B, i / (float) n) & 0xFFFFFF, 0.8f),
                    target.getX() + dx * rad, cy + yy * rad, target.getZ() + dz * rad,
                    dx * 0.05, yy * 0.05, dz * 0.05);
        }
    }
}
