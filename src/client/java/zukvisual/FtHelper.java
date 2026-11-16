package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Помощники: ItemTimers (кулдауны и счётчик предметов) и AutoSwap (свап в левую руку).
 * AutoSwap: 4 слота. Какой предмет в каком слоте, задаётся в config/zukvisual-swaps.properties,
 * а клавиша или кнопка мыши каждого слота назначается на странице Binds в меню.
 * Свап срабатывает только по нажатию игроком, один раз за нажатие.
 */
public final class FtHelper {
    static final int SLOTS = 4;
    static final KeyBinding[] swapKeys = new KeyBinding[SLOTS];
    static final Item[] swapItems = new Item[SLOTS];
    static final String[] swapNames = new String[SLOTS];

    record Tracked(String label, Item item) {}

    static final List<Tracked> TRACKED = new ArrayList<>();

    static final String TIMERS_DEFAULT = """
            # Zukvisual: предметы для ItemTimers (счётчик и кулдаун), id через запятую, без minecraft:
            items=totem_of_undying,ender_pearl,golden_apple,enchanted_golden_apple,chorus_fruit
            """;

    static void loadTimers() {
        TRACKED.clear();
        String items = "totem_of_undying,ender_pearl,golden_apple,enchanted_golden_apple,chorus_fruit";
        try {
            Path f = FabricLoader.getInstance().getConfigDir().resolve("zukvisual-timers.properties");
            if (!Files.exists(f)) Files.writeString(f, TIMERS_DEFAULT, StandardCharsets.UTF_8);
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) { p.load(r); }
            items = p.getProperty("items", items);
        } catch (Exception ignored) {
        }
        for (String id : items.split(",")) {
            Item it = resolve(id);
            if (it == null) continue;
            String n = new ItemStack(it).getName().getString();
            if (n.length() > 7) n = n.substring(0, 7);
            TRACKED.add(new Tracked(n, it));
            if (TRACKED.size() >= 8) break;
        }
    }

    static final String DEFAULT_CONFIG = """
            # Zukvisual: слоты AutoSwap (предмет, который кладётся в левую руку по нажатию клавиши)
            # Пиши id предмета Minecraft без minecraft: (например totem_of_undying, shield, golden_apple).
            # Клавиши слотов назначаются в меню: Right Shift, затем Binds.
            slot1=totem_of_undying
            slot2=enchanted_golden_apple
            slot3=shield
            slot4=
            """;

    static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("zukvisual-swaps.properties");
    }

    static Item resolve(String id) {
        try {
            if (id == null || id.isBlank()) return null;
            id = id.trim();
            Item it = Registries.ITEM.get(Identifier.of(id.contains(":") ? id : "minecraft:" + id));
            return it == Items.AIR ? null : it;
        } catch (Exception e) {
            return null;
        }
    }

    static void loadConfig() {
        Properties p = new Properties();
        try {
            Path f = file();
            if (!Files.exists(f)) Files.writeString(f, DEFAULT_CONFIG, StandardCharsets.UTF_8);
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) { p.load(r); }
        } catch (Exception ignored) {
        }
        for (int i = 0; i < SLOTS; i++) {
            String id = p.getProperty("slot" + (i + 1), "");
            swapItems[i] = resolve(id);
            swapNames[i] = swapItems[i] == null ? null : id.trim();
        }
    }

    /** Название слота для страницы Binds. */
    static String slotLabel(String number) {
        try {
            int i = Integer.parseInt(number) - 1;
            return swapNames[i] == null ? "Свап " + number + " (пусто)" : "Свап: " + swapNames[i];
        } catch (Exception e) {
            return "Свап " + number;
        }
    }

    public static void init() {
        loadConfig();
        loadTimers();

        // слоты 1 и 2 по умолчанию на V и B, остальные без клавиши (назначаются в меню)
        int[] defaults = {GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_UNKNOWN, GLFW.GLFW_KEY_UNKNOWN};
        for (int i = 0; i < SLOTS; i++) swapKeys[i] = Binds.reg("key.zukvisual.swap_" + (i + 1), defaults[i]);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            for (int i = 0; i < SLOTS; i++) {
                while (swapKeys[i].wasPressed()) {
                    if (swapItems[i] != null) swapToOffhand(mc, swapItems[i], swapNames[i]);
                }
            }
        });
    }

    /** Один свап предмета в левую руку по нажатию клавиши. */
    static void swapToOffhand(MinecraftClient mc, Item item, String label) {
        if (!Modules.AUTOSWAP.enabled || mc.player == null || mc.interactionManager == null) return;
        var p = mc.player;
        if (p.getOffHandStack().isOf(item)) return;
        var inv = p.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isOf(item)) {
                int slot = i < 9 ? 36 + i : i; // номера слотов в PlayerScreenHandler
                mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, p);
                HudExtras.notify(label + " → левая рука");
                return;
            }
        }
        HudExtras.notify(label + " не найден");
    }

    static int count(MinecraftClient mc, Item item) {
        var inv = mc.player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (st.isOf(item)) n += st.getCount();
        }
        return n;
    }

    public static void render(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        if (!Modules.ITEM_TIMERS.enabled || mc.player == null) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        int w = 112, rowH = 16;
        int x = sw - w - 6 + Layout.dx("ItemTimers"), y = sh / 2 - 30 + Layout.dy("ItemTimers");
        Layout.reg("ItemTimers", x, y, w, TRACKED.size() * rowH + 6);
        Gfx.box(ctx, x, y, w, TRACKED.size() * rowH + 6);

        int ry = y + 5;
        for (Tracked t : TRACKED) {
            float cd = mc.player.getItemCooldownManager().getCooldownProgress(new ItemStack(t.item()), 0f);
            ctx.drawTextWithShadow(tr, t.label() + " x" + count(mc, t.item()), x + 6, ry, 0xFFE0E0F0);

            int bx = x + 66, bw = 40;
            ctx.fill(bx, ry + 2, bx + bw, ry + 7, 0x40FFFFFF);
            if (cd > 0f) {
                int fw = (int) (bw * Math.min(1f, cd));
                Gfx.gradBar(ctx, bx, ry + 2, fw, bw, 5, false);
            } else {
                ctx.fill(bx, ry + 2, bx + bw, ry + 7, Gfx.withAlpha(0x22E58A, 0xA0)); // готов
            }
            ry += rowH;
        }
    }

    private FtHelper() {}
}
