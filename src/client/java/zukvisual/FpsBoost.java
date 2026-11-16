package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticlesMode;

/**
 * FPSBoost: пока модуль включён, ставит лёгкие настройки (меньше ванильных частиц,
 * без теней сущностей, ближе дальность прорисовки сущностей). При выключении и при выходе
 * из игры возвращает прежние значения. Блоки, чанки и рельеф не затрагиваются.
 */
public final class FpsBoost {
    static boolean applied = false;
    static ParticlesMode savedParticles;
    static Boolean savedShadows;
    static Double savedDist;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.options == null) return;
            if (Modules.FPS_BOOST.enabled && !applied) apply(mc);
            else if (!Modules.FPS_BOOST.enabled && applied) restore(mc);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            if (applied) restore(mc);
        });
    }

    static void apply(MinecraftClient mc) {
        var o = mc.options;
        savedParticles = o.getParticles().getValue();
        savedShadows = o.getEntityShadows().getValue();
        savedDist = o.getEntityDistanceScaling().getValue();
        o.getParticles().setValue(ParticlesMode.DECREASED);
        o.getEntityShadows().setValue(false);
        o.getEntityDistanceScaling().setValue(0.5);
        applied = true;
        HudExtras.notify("FPSBoost: лёгкая графика");
    }

    static void restore(MinecraftClient mc) {
        var o = mc.options;
        if (savedParticles != null) o.getParticles().setValue(savedParticles);
        if (savedShadows != null) o.getEntityShadows().setValue(savedShadows);
        if (savedDist != null) o.getEntityDistanceScaling().setValue(savedDist);
        applied = false;
        HudExtras.notify("FPSBoost выключен");
    }

    private FpsBoost() {}
}
