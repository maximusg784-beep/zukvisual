package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import zukvisual.mixin.MinecraftClientAccessor;

/**
 * FastExp: пока ты держишь кнопку использования с бутылкой опыта в руке, задержка между бросками убирается.
 * Если бутылка на кулдауне, ничего не меняется, бросок идёт в обычном темпе.
 */
public final class FastExp {
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!Modules.FAST_EXP.enabled || mc.player == null || mc.currentScreen != null) return;
            if (!mc.options.useKey.isPressed()) return;
            ItemStack st = mc.player.getMainHandStack();
            if (!st.isOf(Items.EXPERIENCE_BOTTLE)) return;
            if (mc.player.getItemCooldownManager().getCooldownProgress(st, 0f) > 0f) return;
            try {
                ((MinecraftClientAccessor) mc).zukvisual$setItemUseCooldown(0);
            } catch (ClassCastException ignored) {
                // миксин не применился в этой версии: функция просто не работает
            }
        });
    }

    private FastExp() {}
}
