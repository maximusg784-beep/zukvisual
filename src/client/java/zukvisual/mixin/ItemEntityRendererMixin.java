package zukvisual.mixin;

import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zukvisual.Modules;

/**
 * ItemPhysics: выброшенный предмет лежит на земле плашмя, без покачивания и вращения.
 * Подмена идёт точечно (поворот и покачивание), поэтому если в твоей версии вызов называется иначе,
 * функция просто не сработает, игра при этом запустится.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    // покачивание вверх-вниз: при ItemPhysics выключаем (синус даёт -1, итоговый сдвиг нулевой)
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;sin(F)F"), require = 0)
    private float zukvisual$noBob(float value) {
        return Modules.ITEM_PHYSICS.enabled ? -1.0f : MathHelper.sin(value);
    }

    // поворот вокруг Y: добавляем «положить плашмя» и опускаем предмет к земле
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionfc;)V"), require = 0)
    private void zukvisual$flatFc(MatrixStack stack, Quaternionfc q) {
        zukvisual$apply(stack, new Quaternionf(q));
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionf;)V"), require = 0)
    private void zukvisual$flat(MatrixStack stack, Quaternionf q) {
        zukvisual$apply(stack, q);
    }

    private void zukvisual$apply(MatrixStack stack, Quaternionf q) {
        if (!Modules.ITEM_PHYSICS.enabled) {
            stack.multiply(q);
            return;
        }
        stack.translate(0.0f, -0.2f, 0.0f);
        stack.multiply(q);
        stack.multiply(new Quaternionf().rotationX(-1.5707964f)); // лежит плашмя
    }
}
