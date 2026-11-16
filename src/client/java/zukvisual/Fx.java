package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;

import java.util.Random;

/**
 * Единая точка создания частиц с защитой FPS:
 *  - качество (Low / Medium / High) задаёт долю частиц и лимит на тик;
 *  - AdaptiveFx сам уменьшает частицы, когда FPS проседает;
 *  - частицы дальше 48 блоков не создаются;
 *  - важные эффекты (границы зон, удар, убийство) идут через addKeep и режутся только жёстким лимитом.
 */
public final class Fx {
    public static int quality = 1; // 0 Low, 1 Medium, 2 High
    public static final String[] QUALITY_NAMES = {"Low", "Medium", "High"};

    static final double[] KEEP = {0.35, 0.7, 1.0};
    static final int[] BUDGET = {40, 120, 300};
    static final double MAX_DIST_SQ = 48.0 * 48.0;
    static final Random RNG = new Random();

    static int spawned = 0;
    static double adaptive = 1.0;

    /** Как часто (в тиках) обновляется косметика: на Low реже. */
    public static int interval() {
        return quality == 0 ? 4 : 2;
    }

    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            spawned = 0;
            if (Modules.ADAPTIVE.enabled) {
                int fps = mc.getCurrentFps();
                double target = fps >= 60 ? 1.0 : fps >= 40 ? 0.6 : fps >= 25 ? 0.35 : 0.15;
                adaptive += (target - adaptive) * 0.05;
            } else {
                adaptive = 1.0;
            }
        });
    }

    /** Обычные частицы (косметика, трейл, снег): режутся по качеству, адаптивности и лимиту. */
    public static void add(ClientWorld w, ParticleEffect fx, double x, double y, double z, double vx, double vy, double vz) {
        if (w == null) return;
        if (RNG.nextDouble() > KEEP[quality] * adaptive) return;
        if (spawned >= BUDGET[quality]) return;
        put(w, fx, x, y, z, vx, vy, vz);
    }

    /** Важные частицы (границы зон, удар, убийство): только жёсткий лимит. */
    public static void addKeep(ClientWorld w, ParticleEffect fx, double x, double y, double z, double vx, double vy, double vz) {
        if (w == null) return;
        if (spawned >= BUDGET[quality] * 3) return;
        put(w, fx, x, y, z, vx, vy, vz);
    }

    private static void put(ClientWorld w, ParticleEffect fx, double x, double y, double z, double vx, double vy, double vz) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            double dx = x - mc.player.getX(), dy = y - mc.player.getY(), dz = z - mc.player.getZ();
            if (dx * dx + dy * dy + dz * dz > MAX_DIST_SQ) return;
        }
        spawned++;
        w.addParticleClient(fx, x, y, z, vx, vy, vz);
    }

    private Fx() {}
}
