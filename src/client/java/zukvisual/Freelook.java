package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Freelook: пока держишь клавишу (по умолчанию Left Alt), камера свободно вращается вокруг тебя.
 * Сам игрок не поворачивается, поэтому направление движения и ударов не меняется.
 * Работает через миксины EntityMixin (мышь) и CameraMixin (вид камеры).
 */
public final class Freelook {
    public static boolean active = false;
    public static float yaw, pitch;
    static KeyBinding key;
    static Perspective saved;

    public static void init() {
        key = Binds.reg("key.zukvisual.freelook", GLFW.GLFW_KEY_LEFT_ALT);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            boolean down = Modules.FREELOOK.enabled && mc.player != null && mc.currentScreen == null && key.isPressed();
            if (down && !active) {
                active = true;
                yaw = mc.player.getYaw();
                pitch = mc.player.getPitch();
                saved = mc.options.getPerspective();
                mc.options.setPerspective(Perspective.THIRD_PERSON_BACK); // камера вокруг игрока
            } else if (!down && active) {
                active = false;
                if (saved != null) mc.options.setPerspective(saved);
            }
        });
    }

    private Freelook() {}
}
