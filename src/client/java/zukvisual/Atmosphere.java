package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;

import java.util.Random;

/** Snow: снег вокруг игрока в верхнем мире, только под открытым небом. Чисто визуальный эффект. */
public final class Atmosphere {
    static final Random RNG = new Random();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!Modules.SNOW.enabled || mc.player == null || mc.world == null) return;
            if (mc.world.getRegistryKey() != World.OVERWORLD) return;
            if (!mc.world.isSkyVisible(mc.player.getBlockPos())) return;
            for (int i = 0; i < Settings.SNOW_V[Settings.snow]; i++) {
                Fx.add(mc.world, ParticleTypes.SNOWFLAKE,
                        mc.player.getX() + (RNG.nextDouble() - 0.5) * 24,
                        mc.player.getY() + 4 + RNG.nextDouble() * 6,
                        mc.player.getZ() + (RNG.nextDouble() - 0.5) * 24,
                        0, -0.08, 0);
            }
        });
    }

    private Atmosphere() {}
}
