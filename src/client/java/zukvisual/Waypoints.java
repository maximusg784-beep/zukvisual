package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Waypoints: свои метки. Клавиши назначаются в меню (Binds): «Метка: добавить» ставит точку
 * там, где стоишь, «Метка: удалить ближайшую» убирает ближайшую. Сверху HUD-компас с расстоянием.
 */
public final class Waypoints {
    record WP(String name, String dim, double x, double y, double z) {}

    static final List<WP> LIST = new ArrayList<>();
    static int counter = 1;
    static KeyBinding addKey, removeKey;

    static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("zukvisual-waypoints.properties");
    }

    static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) { p.load(r); }
            counter = Integer.parseInt(p.getProperty("counter", "1"));
            for (int i = 0; ; i++) {
                String v = p.getProperty("wp" + i);
                if (v == null) break;
                String[] s = v.split(";");
                LIST.add(new WP(s[0], s[1], Double.parseDouble(s[2]), Double.parseDouble(s[3]), Double.parseDouble(s[4])));
            }
        } catch (Exception ignored) {
        }
    }

    static void save() {
        Properties p = new Properties();
        p.setProperty("counter", String.valueOf(counter));
        for (int i = 0; i < LIST.size(); i++) {
            WP w = LIST.get(i);
            p.setProperty("wp" + i, w.name() + ";" + w.dim() + ";" + w.x() + ";" + w.y() + ";" + w.z());
        }
        try (Writer out = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
            p.store(out, "Zukvisual waypoints");
        } catch (Exception ignored) {
        }
    }

    static String dim(MinecraftClient mc) {
        return mc.world.getRegistryKey().getValue().toString();
    }

    /** Автометка места смерти: заменяет предыдущую метку «Смерть». */
    static void setDeath(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        LIST.removeIf(w -> w.name().equals("Смерть"));
        LIST.add(new WP("Смерть", dim(mc), mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        save();
    }

    public static void init() {
        load();
        addKey = Binds.reg("key.zukvisual.wp_add", GLFW.GLFW_KEY_UNKNOWN);
        removeKey = Binds.reg("key.zukvisual.wp_remove", GLFW.GLFW_KEY_UNKNOWN);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null) return;
            while (addKey.wasPressed()) {
                if (!Modules.WAYPOINTS.enabled) continue;
                String name = "WP " + counter++;
                LIST.add(new WP(name, dim(mc), mc.player.getX(), mc.player.getY(), mc.player.getZ()));
                save();
                HudExtras.notify("Метка " + name + " добавлена");
            }
            while (removeKey.wasPressed()) {
                if (!Modules.WAYPOINTS.enabled) continue;
                WP best = null;
                double bd = Double.MAX_VALUE;
                for (WP w : LIST) {
                    if (!w.dim().equals(dim(mc))) continue;
                    double dx = w.x() - mc.player.getX(), dz = w.z() - mc.player.getZ();
                    double d = dx * dx + dz * dz;
                    if (d < bd) { bd = d; best = w; }
                }
                if (best != null) {
                    LIST.remove(best);
                    save();
                    HudExtras.notify("Метка " + best.name() + " удалена");
                }
            }
        });
    }

    public static void render(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        if (!Modules.WAYPOINTS.enabled || mc.player == null || mc.world == null || LIST.isEmpty()) return;
        int cx = mc.getWindow().getScaledWidth() / 2 + Layout.dx("Waypoints"), y = 30 + Layout.dy("Waypoints"), barW = 220;
        Layout.reg("Waypoints", cx - barW / 2, y, barW, 14);
        Gfx.box(ctx, cx - barW / 2, y, barW, 14);
        ctx.fill(cx, y + 2, cx + 1, y + 12, 0x60FFFFFF); // центр компаса

        float yaw = mc.player.getYaw();
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        String here = dim(mc);
        int idx = 0;
        for (WP w : LIST) {
            if (!w.dim().equals(here)) continue;
            double dx = w.x() - px, dy = w.y() - py, dz = w.z() - pz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float diff = MathHelper.wrapDegrees(bearing - yaw);
            if (Math.abs(diff) > 90f) continue;
            int x = cx + (int) (diff / 90f * (barW / 2 - 8));
            int c = Gfx.wave(idx * 0.15f);
            ctx.fill(x - 1, y + 3, x + 2, y + 11, c);
            String label = w.name() + " " + (int) dist + "m";
            ctx.drawTextWithShadow(tr, label, x - tr.getWidth(label) / 2, y + 16, 0xFFE0E0F0);
            idx++;
        }
    }

    private Waypoints() {}
}
