package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Колесо фраз: клавиша (по умолчанию G, меняется в Binds) открывает круг заготовленных фраз.
 * Клик по сектору отправляет фразу в чат. Фразы, начинающиеся с /, уходят как команды.
 * Отправка только по твоему клику. Фразы: config/zukvisual-phrases.properties (до 8 штук).
 */
public final class PhraseWheel {
    static KeyBinding key;
    static final List<String> PHRASES = new ArrayList<>();

    static final String DEFAULT = """
            # Zukvisual: колесо фраз (до 8). Фразы, начинающиеся с /, отправляются как команды.
            phrase1=gg
            phrase2=Спасибо!
            phrase3=Привет!
            phrase4=/home
            phrase5=/spawn
            phrase6=Помогите!
            phrase7=Иду к вам
            phrase8=Ожидайте
            """;

    static void load() {
        PHRASES.clear();
        try {
            Path f = FabricLoader.getInstance().getConfigDir().resolve("zukvisual-phrases.properties");
            if (!Files.exists(f)) Files.writeString(f, DEFAULT, StandardCharsets.UTF_8);
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) { p.load(r); }
            for (int i = 1; i <= 8; i++) {
                String v = p.getProperty("phrase" + i);
                if (v != null && !v.isBlank()) PHRASES.add(v.trim());
            }
        } catch (Exception ignored) {
        }
    }

    public static void init() {
        load();
        key = Binds.reg("key.zukvisual.wheel", GLFW.GLFW_KEY_G);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (key.wasPressed()) {
                if (Modules.PHRASE_WHEEL.enabled && mc.currentScreen == null && mc.player != null && !PHRASES.isEmpty()) {
                    mc.setScreen(new PhraseWheelScreen());
                }
            }
        });
    }

    static void send(MinecraftClient mc, String text) {
        if (mc.player == null) return;
        var h = mc.player.networkHandler;
        if (text.startsWith("/")) h.sendChatCommand(text.substring(1));
        else h.sendChatMessage(text);
    }

    private PhraseWheel() {}
}
