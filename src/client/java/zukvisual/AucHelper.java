package zukvisual;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AucHelper: на странице аукциона подсвечивает зелёной рамкой самое дешёвое объявление (цена за штуку)
 * среди одинаковых предметов. Только подсветка, ничего не покупается и не нажимается.
 * Название окна и слова-маркеры цены настраиваются в config/zukvisual-auction.properties.
 */
public final class AucHelper {
    static final List<String> TITLE_KEYS = new ArrayList<>();
    static final List<String> PRICE_KEYS = new ArrayList<>();
    static final Pattern NUMBER = Pattern.compile("([0-9][0-9\\s.,']*)\\s*([kKкК]|[mMмМ])?");

    static final String DEFAULT = """
            # Zukvisual: AucHelper. Через запятую, без учёта регистра.
            # title: части названия окна аукциона (если пусто, работает в любом контейнере)
            # price: слова, после которых в описании предмета указана цена
            title=аукцион,auction
            price=цена,price,стоимость
            """;

    static void load() {
        TITLE_KEYS.clear();
        PRICE_KEYS.clear();
        Properties p = new Properties();
        try {
            Path f = FabricLoader.getInstance().getConfigDir().resolve("zukvisual-auction.properties");
            if (!Files.exists(f)) Files.writeString(f, DEFAULT, StandardCharsets.UTF_8);
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) { p.load(r); }
        } catch (Exception ignored) {
        }
        for (String s : p.getProperty("title", "аукцион,auction").split(",")) if (!s.isBlank()) TITLE_KEYS.add(s.trim().toLowerCase());
        for (String s : p.getProperty("price", "цена,price,стоимость").split(",")) if (!s.isBlank()) PRICE_KEYS.add(s.trim().toLowerCase());
    }

    public static void init() {
        load();
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof HandledScreen<?> hs) {
                ScreenEvents.afterRender(screen).register((s, ctx, mx, my, td) -> render(hs, ctx));
            }
        });
    }

    /** Цена из описания предмета или -1. */
    static long price(ItemStack st) {
        LoreComponent lore = st.get(DataComponentTypes.LORE);
        if (lore == null) return -1;
        for (Text line : lore.lines()) {
            String s = line.getString().toLowerCase();
            for (String key : PRICE_KEYS) {
                int i = s.indexOf(key);
                if (i < 0) continue;
                Matcher m = NUMBER.matcher(s.substring(i + key.length()));
                if (!m.find()) continue;
                String digits = m.group(1).replaceAll("[^0-9]", "");
                if (digits.isEmpty() || digits.length() > 15) continue;
                long v = Long.parseLong(digits);
                String suf = m.group(2);
                if (suf != null) v *= suf.equalsIgnoreCase("k") || suf.equalsIgnoreCase("к") ? 1_000L : 1_000_000L;
                return v;
            }
        }
        return -1;
    }

    static void render(HandledScreen<?> hs, DrawContext ctx) {
        if (!Modules.AUC_HELPER.enabled) return;
        if (!TITLE_KEYS.isEmpty()) {
            String title = hs.getTitle().getString().toLowerCase();
            boolean match = false;
            for (String k : TITLE_KEYS) if (title.contains(k)) { match = true; break; }
            if (!match) return;
        }

        var slots = hs.getScreenHandler().slots;
        int top = slots.size() - 36; // верхняя часть: объявления, нижние 36: твой инвентарь
        if (top < 9 || top % 9 != 0) return;
        int rows = top / 9;
        int x0 = (hs.width - 176) / 2, y0 = (hs.height - (114 + rows * 18)) / 2;

        Map<Item, Double> best = new HashMap<>();
        Map<Item, Integer> count = new HashMap<>();
        double[] unit = new double[top];
        for (int i = 0; i < top; i++) {
            unit[i] = -1;
            ItemStack st = slots.get(i).getStack();
            if (st.isEmpty()) continue;
            long pr = price(st);
            if (pr < 0) continue;
            unit[i] = pr / (double) Math.max(1, st.getCount());
            best.merge(st.getItem(), unit[i], Math::min);
            count.merge(st.getItem(), 1, Integer::sum);
        }

        for (int i = 0; i < top; i++) {
            if (unit[i] < 0) continue;
            Item it = slots.get(i).getStack().getItem();
            if (count.get(it) < 2 || unit[i] > best.get(it)) continue; // подсвечиваем только самое дешёвое среди одинаковых
            Slot sl = slots.get(i);
            int sx = x0 + sl.x - 1, sy = y0 + sl.y - 1;
            int c = 0xFF22E58A;
            ctx.fill(sx, sy, sx + 18, sy + 2, c);
            ctx.fill(sx, sy + 16, sx + 18, sy + 18, c);
            ctx.fill(sx, sy, sx + 2, sy + 18, c);
            ctx.fill(sx + 16, sy, sx + 18, sy + 18, c);
        }
    }

    private AucHelper() {}
}
