package zukvisual.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zukvisual.Modules;

/** Прячет ванильный прицел, когда включён свой. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void zukvisual$hideVanillaCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Modules.CROSSHAIR.enabled) ci.cancel();
    }
}
