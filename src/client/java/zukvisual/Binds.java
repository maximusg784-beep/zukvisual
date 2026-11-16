package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Все клавиши мода. Любую можно повесить на клавиатуру или на любую кнопку мыши (в том числе боковую). */
public final class Binds {
    public static final List<KeyBinding> ALL = new ArrayList<>();
    static final Map<KeyBinding, Module> TOGGLES = new LinkedHashMap<>();

    static final Map<String, String> LABELS = Map.of(
            "key.zukvisual.gui", "Меню",
            "key.zukvisual.freelook", "Freelook",
            "key.zukvisual.zoom", "Zoom",
            "key.zukvisual.wheel", "Колесо фраз",
            "key.zukvisual.wp_add", "Метка: добавить",
            "key.zukvisual.wp_remove", "Метка: удалить ближайшую",
            "key.zukvisual.swap_totem", "Свап: тотем",
            "key.zukvisual.swap_gapple", "Свап: яблоко");

    public static KeyBinding reg(String id, int keyCode) {
        KeyBinding kb = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(id, InputUtil.Type.KEYSYM, keyCode, KeyBinding.Category.MISC));
        ALL.add(kb);
        return kb;
    }

    public static String label(KeyBinding kb) {
        String id = kb.getId();
        if (id.startsWith("key.zukvisual.swap_")) return FtHelper.slotLabel(id.substring(id.length() - 1));
        if (id.startsWith("key.zukvisual.toggle_")) {
            String key = id.substring("key.zukvisual.toggle_".length());
            for (Module m : Modules.ALL) if (m.name.toLowerCase().equals(key)) return "Вкл/выкл: " + m.name;
        }
        return LABELS.getOrDefault(id, id);
    }

    /** Для каждого модуля создаёт клавишу включения и выключения (по умолчанию без назначения). */
    public static void initToggles() {
        for (Module m : Modules.ALL) {
            KeyBinding kb = reg("key.zukvisual.toggle_" + m.name.toLowerCase(), GLFW.GLFW_KEY_UNKNOWN);
            TOGGLES.put(kb, m);
        }
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            for (var e : TOGGLES.entrySet()) {
                while (e.getKey().wasPressed()) {
                    Module m = e.getValue();
                    m.enabled = !m.enabled;
                    Modules.save();
                    HudExtras.notify(m.name + (m.enabled ? " включён" : " выключен"));
                    Sfx.toggle(m.enabled);
                }
            }
        });
    }

    public static String keyName(KeyBinding kb) {
        return kb.getBoundKeyLocalizedText().getString();
    }

    public static void bind(KeyBinding kb, InputUtil.Key key) {
        kb.setBoundKey(key);
        KeyBinding.updateKeysByCode();
        MinecraftClient.getInstance().options.write(); // сохранить в options.txt
    }

    private Binds() {}
}
