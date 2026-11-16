package zukvisual.mixin;

import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zukvisual.Modules;

/** ItemPhysics: предмет больше не крутится, у каждого свой постоянный угол поворота. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "getRotation", at = @At("HEAD"), cancellable = true, require = 0)
    private static void zukvisual$noSpin(float age, float uniqueOffset, CallbackInfoReturnable<Float> cir) {
        if (Modules.ITEM_PHYSICS.enabled) cir.setReturnValue(uniqueOffset);
    }
}
