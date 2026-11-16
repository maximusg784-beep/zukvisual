package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.Random;

/**
 * WorldParticles: фоновые частицы вокруг (светлячки, искры, лепестки, звёзды), стиль выбирается в меню.
 * ArrowTrail: красивый след за летящими стрелами (стрелы и так видны, это только оформление).
 */
public final class WorldFx {
    static final Random RNG = new Random();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null) return;
            var w = mc.world;
            double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();

            if (Modules.WORLD_PARTICLES.enabled) {
                for (int i = 0; i < 3; i++) {
                    double x = px + (RNG.nextDouble() - 0.5) * 32;
                    double z = pz + (RNG.nextDouble() - 0.5) * 32;
                    double y = py + (RNG.nextDouble() - 0.3) * 6;
                    switch (Settings.worldStyle) {
                        case 1 -> Fx.add(w, ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.03, 0);
                        case 2 -> Fx.add(w, ParticleTypes.CHERRY_LEAVES, x, y + 3, z, 0, -0.03, 0);
                        case 3 -> Fx.add(w, ParticleTypes.END_ROD, x, y, z, 0, 0.005, 0);
                        default -> Fx.add(w, ParticleTypes.GLOW, x, y, z,
                                (RNG.nextDouble() - 0.5) * 0.02, (RNG.nextDouble() - 0.5) * 0.02, (RNG.nextDouble() - 0.5) * 0.02);
                    }
                }
            }

            if (Modules.ARROW_TRAIL.enabled) {
                int count = 0;
                for (Entity e : w.getEntities()) {
                    if (!(e instanceof PersistentProjectileEntity)) continue;
                    if (e.getVelocity().lengthSquared() < 0.01) continue; // стрела воткнулась
                    if (e.squaredDistanceTo(mc.player) > 48 * 48) continue;
                    Fx.add(w, new DustParticleEffect(Settings.fxColor(0.5f), 0.8f), e.getX(), e.getY(), e.getZ(), 0, 0, 0);
                    if (++count > 20) break;
                }
            }
        });
    }

    private WorldFx() {}
}
