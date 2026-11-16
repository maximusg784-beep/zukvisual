package zukvisual;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Звуки мода (только клиентские). Какие звуки играть, задаётся в config/zukvisual-sounds.properties
 * (id звуков игры без minecraft:, например entity.arrow.hit_player).
 */
public final class Sfx {
    static SoundEvent hit = SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
    static SoundEvent kill = SoundEvents.ENTITY_PLAYER_LEVELUP;
    static SoundEvent uiOn = SoundEvents.ENTITY_ITEM_PICKUP;
    static SoundEvent uiOff = SoundEvents.ENTITY_ITEM_PICKUP;
    static boolean loaded = false;

    static final String DEFAULT = """
            # Zukvisual: звуки (id звуков Minecraft без minecraft:)
            hit=entity.experience_orb.pickup
            kill=entity.player.levelup
            ui_on=entity.item.pickup
            ui_off=entity.item.pickup
            """;

    static SoundEvent resolve(Properties p, String key, SoundEvent fallback) {
        try {
            String id = p.getProperty(key);
            if (id == null || id.isBlank()) return fallback;
            id = id.trim();
            SoundEvent ev = Registries.SOUND_EVENT.get(Identifier.of(id.contains(":") ? id : "minecraft:" + id));
            return ev != null ? ev : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    static void ensure() {
        if (loaded) return;
        loaded = true;
        try {
            Path f = FabricLoader.getInstance().getConfigDir().resolve("zukvisual-sounds.properties");
            if (!Files.exists(f)) Files.writeString(f, DEFAULT, StandardCharsets.UTF_8);
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) { p.load(r); }
            hit = resolve(p, "hit", hit);
            kill = resolve(p, "kill", kill);
            uiOn = resolve(p, "ui_on", uiOn);
            uiOff = resolve(p, "ui_off", uiOff);
        } catch (Exception ignored) {
        }
    }

    public static SoundEvent hit() { ensure(); return hit; }
    public static SoundEvent kill() { ensure(); return kill; }

    public static void play(SoundEvent ev, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSoundManager() == null) return;
        mc.getSoundManager().play(PositionedSoundInstance.master(ev, pitch,));
    }

    /** Короткий звук при включении (выше) и выключении (ниже). */
    public static void toggle(boolean on) {
        if (!Modules.UI_SOUNDS.enabled) return;
        ensure();
        play(on ? uiOn : uiOff, on ? 1.5f : 0.8f);
    }

    private Sfx() {}
}
