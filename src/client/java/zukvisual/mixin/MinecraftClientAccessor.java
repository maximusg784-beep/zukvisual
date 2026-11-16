package zukvisual.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Доступ к задержке между использованиями предмета (нужен для FastExp). */
@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("itemUseCooldown")
    void zukvisual$setItemUseCooldown(int value);
}
