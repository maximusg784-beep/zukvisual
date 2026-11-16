package zukvisual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/** InventoryHUD: твой основной инвентарь (27 слотов) прямо на экране. Блок перетаскивается в HUD-редакторе. */
public final class InvHud {
    public static void render(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        if (!Modules.INVENTORY_HUD.enabled || mc.player == null) return;
        int w = 9 * 18 + 8, h = 3 * 18 + 8;
        int x = mc.getWindow().getScaledWidth() - w - 6 + Layout.dx("Inventory");
        int y = mc.getWindow().getScaledHeight() - h - 30 + Layout.dy("Inventory");
        Layout.reg("Inventory", x, y, w, h);
        Gfx.box(ctx, x, y, w, h);
        var inv = mc.player.getInventory();
        for (int i = 0; i < 27; i++) {
            ItemStack st = inv.getStack(9 + i);
            if (st.isEmpty()) continue;
            int sx = x + 4 + (i % 9) * 18 + 1, sy = y + 4 + (i / 9) * 18 + 1;
            ctx.drawItem(st, sx, sy);
            ctx.drawStackOverlay(tr, st, sx, sy);
        }
    }

    private InvHud() {}
}
