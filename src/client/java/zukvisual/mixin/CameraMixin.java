package zukvisual.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import zukvisual.Freelook;

/** Подменяет углы камеры на углы Freelook. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float zukvisual$yaw(float yaw) {
        return Freelook.active ? Freelook.yaw : yaw;
    }

    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float zukvisual$pitch(float pitch) {
        return Freelook.active ? Freelook.pitch : pitch;
    }
}
