package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Zones: границы зоны действия предмета (трапка, пласт, дезориентация и т.д.).
 * Пока держишь предмет, зона показана вокруг тебя; после использования остаётся на месте.
 * Граница зелёная, когда ты внутри. Размеры: config/zukvisual-zones.properties
 */
public final class Zones {
    interface Shape {
        boolean contains(double x, double y, double z);
        void draw(ClientWorld w, ParticleEffect fx);
        double cx();
        double cz();
    }

    record BoxShape(Box b) implements Shape {
        public boolean contains(double x, double y, double z) { return b.contains(x, y, z); }
        public double cx() { return b.getCenter().x; }
        public double cz() { return b.getCenter().z; }
        public void draw(ClientWorld w, ParticleEffect fx) {
            double[] xs = {b.minX, b.maxX}, ys = {b.minY, b.maxY}, zs = {b.minZ, b.maxZ};
            for (double y : ys) for (double z : zs) edge(w, fx, b.minX, y, z, b.maxX, y, z);
            for (double x : xs) for (double z : zs) edge(w, fx, x, b.minY, z, x, b.maxY, z);
            for (double x : xs) for (double y : ys) edge(w, fx, x, y, b.minZ, x, y, b.maxZ);
        }
    }

    record RingShape(double x, double y, double z, double r) implements Shape {
        public boolean contains(double px, double py, double pz) {
            double dx = px - x, dz = pz - z;
            return dx * dx + dz * dz <= r * r && Math.abs(py - y) <= r;
        }
        public double cx() { return x; }
        public double cz() { return z; }
        public void draw(ClientWorld w, ParticleEffect fx) {
            int n = (int) Math.max(12, Math.min(90, 2 * Math.PI * r / 0.5));
            for (int i = 0; i < n; i++) {
                double a = 2 * Math.PI * i / n;
                Fx.addKeep(w, fx, x + Math.cos(a) * r, y - 0.9, z + Math.sin(a) * r, 0, 0, 0);
            }
        }
    }

    static final class Zone {
        final Shape shape;
        final long until;
        final long created = System.currentTimeMillis();
        Zone(Shape shape, long until) { this.shape = shape; this.until = until; }
    }

    static final List<Zone> ACTIVE = new ArrayList<>();
    static final Map<String, String> DEFS = new LinkedHashMap<>();
    static long durationMs = 15000;
    static int tick = 0;
    static boolean insideAny = false;

    static final String DEFAULT_CONFIG = """
            # Zukvisual: зоны предметов
            # Формат: часть_названия_предмета=размер
            #   1.5          куб, половина стороны (1.5 = куб 3x3x3)
            #   1.5,1.5,0.5  коробка (полуширина, полувысота, полутолщина); толщина идёт по направлению взгляда
            #   ~10          круг, радиус в блоках
            # Трапка (куб 3x3) и пласт (стенка 3x3) взяты из описаний игроков FunTime.
            # Радиусы дезориентации и пыли взяты из серверных плагинов-клонов, это примерные значения, поправь под себя.
            seconds=15
            трап=1.5
            пласт=1.5,1.5,0.5
            дезориентация=~9
            пыль=~10
            """;

    static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("zukvisual-zones.properties");
    }

    static void loadConfig() {
        try {
            Path f = file();
            if (!Files.exists(f)) Files.writeString(f, DEFAULT_CONFIG, StandardCharsets.UTF_8);
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) { p.load(r); }
            DEFS.clear();
            for (String k : p.stringPropertyNames()) {
                String v = p.getProperty(k).trim();
                if (k.equals("seconds")) durationMs = (long) (Double.parseDouble(v) * 1000);
                else DEFS.put(k.toLowerCase(), v);
            }
        } catch (Exception ignored) {
        }
    }

    static String specFor(ItemStack st) {
        if (st == null || st.isEmpty()) return null;
        String name = st.getName().getString().toLowerCase();
        for (var e : DEFS.entrySet()) if (name.contains(e.getKey())) return e.getValue();
        return null;
    }

    static Shape make(String spec, double cx, double cy, double cz, float yaw) {
        try {
            spec = spec.trim();
            if (spec.startsWith("~")) return new RingShape(cx, cy, cz, Double.parseDouble(spec.substring(1)));
            String[] parts = spec.split(",");
            double hx, hy, hz;
            if (parts.length >= 3) {
                hx = Double.parseDouble(parts[0].trim());
                hy = Double.parseDouble(parts[1].trim());
                hz = Double.parseDouble(parts[2].trim());
                double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
                if (Math.abs(fx) > Math.abs(fz)) { double t = hx; hx = hz; hz = t; } // смотрим вдоль X: толщина по X
            } else {
                hx = hy = hz = Double.parseDouble(parts[0].trim());
            }
            return new BoxShape(new Box(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz));
        } catch (Exception e) {
            return null;
        }
    }

    static void mark(ItemStack st, double cx, double cy, double cz, float yaw) {
        String spec = specFor(st);
        if (spec == null) return;
        Shape s = make(spec, cx, cy, cz, yaw);
        if (s == null) return;
        long now = System.currentTimeMillis();
        for (Zone z : ACTIVE) { // защита от дубля при одном нажатии
            if (now - z.created < 400 && Math.abs(z.shape.cx() - cx) < 1.5 && Math.abs(z.shape.cz() - cz) < 1.5) return;
        }
        ACTIVE.add(new Zone(s, now + durationMs));
    }

    public static void init() {
        loadConfig();

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient() && Modules.ZONES.enabled) {
                mark(player.getStackInHand(hand), Math.floor(player.getX()) + 0.5, Math.floor(player.getY()) + 1.0,
                        Math.floor(player.getZ()) + 0.5, player.getYaw());
            }
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient() && Modules.ZONES.enabled) {
                BlockPos bp = hit.getBlockPos().offset(hit.getSide());
                mark(player.getStackInHand(hand), bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, player.getYaw());
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null || !Modules.ZONES.enabled) { insideAny = false; return; }
            if (++tick % 5 != 0) return;

            long now = System.currentTimeMillis();
            ACTIVE.removeIf(z -> now > z.until);

            double px = mc.player.getX(), py = mc.player.getY() + 0.9, pz = mc.player.getZ();
            insideAny = false;
            List<Shape> shapes = new ArrayList<>();
            for (Zone z : ACTIVE) shapes.add(z.shape);

            // предпросмотр зоны, пока предмет в руке
            String spec = specFor(mc.player.getMainHandStack());
            if (spec != null) {
                Shape s = make(spec, px, mc.player.getY() + 1.0, pz, mc.player.getYaw());
                if (s != null) shapes.add(s);
            }

            for (Shape s : shapes) {
                boolean inside = s.contains(px, py, pz);
                if (inside) insideAny = true;
                s.draw(mc.world, new DustParticleEffect(inside ? 0x22E58A : Gfx.A, 1.0f));
            }
        });
    }

    static void edge(ClientWorld w, ParticleEffect fx, double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double len = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        int n = (int) Math.max(1, Math.min(40, Math.ceil(len / 0.5)));
        for (int i = 0; i <= n; i++) {
            Fx.addKeep(w, fx, x1 + dx * i / n, y1 + dy * i / n, z1 + dz * i / n, 0, 0, 0);
        }
    }

    public static void renderHud(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        if (!Modules.ZONES.enabled || !insideAny) return;
        String s = "В зоне";
        int w = tr.getWidth(s) + 14, x = mc.getWindow().getScaledWidth() / 2 - w / 2;
        Gfx.box(ctx, x, 8, w, 16);
        ctx.fill(x, 8, x + 2, 24, 0xFF22E58A);
        ctx.drawTextWithShadow(tr, s, x + 8, 12, 0xFF22E58A);
    }

    private Zones() {}
}
