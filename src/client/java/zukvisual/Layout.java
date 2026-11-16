package zukvisual;

import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/** Положение HUD-блоков: у каждого блока есть смещение, которое задаётся в HUD-редакторе. */
public final class Layout {
    public record Rect(int x, int y, int w, int h) {}

    static final Map<String, int[]> OFF = new HashMap<>();
    /** Границы блоков, нарисованных в этом кадре (для редактора). */
    public static final Map<String, Rect> RECTS = new LinkedHashMap<>();

    public static int dx(String n) { int[] o = OFF.get(n); return o == null ? 0 : o[0]; }
    public static int dy(String n) { int[] o = OFF.get(n); return o == null ? 0 : o[1]; }

    public static void reg(String n, int x, int y, int w, int h) { RECTS.put(n, new Rect(x, y, w, h)); }

    public static void move(String n, int ddx, int ddy) {
        int[] o = OFF.computeIfAbsent(n, k -> new int[2]);
        o[0] += ddx;
        o[1] += ddy;
    }

    public static void reset() {
        OFF.clear();
        save();
    }

    static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("zukvisual-layout.properties");
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) { p.load(r); }
            for (String k : p.stringPropertyNames()) {
                if (!k.endsWith(".x")) continue;
                String name = k.substring(0, k.length() - 2);
                OFF.put(name, new int[]{Integer.parseInt(p.getProperty(k)), Integer.parseInt(p.getProperty(name + ".y", "0"))});
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        Properties p = new Properties();
        for (var e : OFF.entrySet()) {
            p.setProperty(e.getKey() + ".x", String.valueOf(e.getValue()[0]));
            p.setProperty(e.getKey() + ".y", String.valueOf(e.getValue()[1]));
        }
        try (Writer out = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
            p.store(out, "Zukvisual HUD layout");
        } catch (Exception ignored) {
        }
    }

    private Layout() {}
}
