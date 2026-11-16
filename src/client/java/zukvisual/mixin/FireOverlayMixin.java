package zukvisual.mixin;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zukvisual.Modules;

/** LowFire: скрывает ванильный огонь на экране (вместо него рисуется тонкая полоса из ScreenFx). */
@Mixin(InGameOverlayRenderer.class)
public abstract class FireOverlayMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void zukvisual$lowFire(CallbackInfo ci) {
        if (Modules.LOW_FIRE.enabled) ci.cancel();
    }
}
