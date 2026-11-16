package zukvisual;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Zukvisual: HUD + эффекты + меню (Right Shift). Только косметика. */
public class ZukvisualClient implements ClientModInitializer {
    static KeyBinding openGui;

    @Override
    public void onInitializeClient() {
        Modules.load();
        Layout.load();
        Stats.init();
        Combat.init();
        WorldFx.init();
        PhraseWheel.init();
        Fx.init();
        FpsBoost.init();
        ScreenFx.init();
        ZukEffects.init();
        HudExtras.init();
        FtHelper.init();
        Zones.init();
        ExtraHud.init();
        Freelook.init();
        Waypoints.init();
        Atmosphere.init();
        Cosmetics.init();
        Binds.initToggles();
        Trajectory.init();
        FastExp.init();
        AucHelper.init();
        Holiday.init();
        FakePlayer.init(); // последним: его обработчик удара должен идти после остальных

        openGui = Binds.reg("key.zukvisual.gui", GLFW.GLFW_KEY_RIGHT_SHIFT);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openGui.wasPressed()) mc.setScreen(new ClickGuiScreen());
        });

        HudRenderCallback.EVENT.register((ctx, tick) -> render(ctx));
    }

    static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        Layout.RECTS.clear();
        if (Modules.WATERMARK.enabled) drawWatermark(ctx, mc, tr);
        drawModuleList(ctx, mc, tr);
        HudExtras.render(ctx, mc, tr);
        FtHelper.render(ctx, mc, tr);
        ExtraHud.render(ctx, mc, tr);
        Zones.renderHud(ctx, mc, tr);
        Crosshair.render(ctx, mc);
        Waypoints.render(ctx, mc, tr);
        Cosmetics.render(ctx, mc, tr);
        Stats.render(ctx, mc, tr);
        Combat.render(ctx, mc, tr);
        Holiday.render(ctx, mc, tr);
        InvHud.render(ctx, mc, tr);
        ScreenFx.renderLowHp(ctx, mc);
        ScreenFx.render(ctx, mc);
    }

    static void drawWatermark(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        String brand = "Zukvisual";
        String info = " | " + mc.getSession().getUsername() + " | " + mc.getCurrentFps() + " fps | " + ExtraHud.ping(mc) + " ms | "
                + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        int w = tr.getWidth(brand + info) + 12, x = 6 + Layout.dx("Watermark"), y = 6 + Layout.dy("Watermark");
        Layout.reg("Watermark", x, y, w, 16);
        Gfx.box(ctx, x, y, w, 16);
        Gfx.gradientText(ctx, tr, brand, x + 6, y + 4, 0f);
        ctx.drawTextWithShadow(tr, info, x + 6 + tr.getWidth(brand), y + 4, 0xFFB8B8C8);
    }

    static List<Module> SORTED;

    static List<Module> sortedModules(TextRenderer tr) {
        if (SORTED == null) {
            SORTED = new ArrayList<>(Modules.ALL);
            SORTED.sort(Comparator.comparingInt((Module m) -> m.width(tr)).reversed());
        }
        return SORTED;
    }

    static void drawModuleList(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        int screenW = mc.getWindow().getScaledWidth();
        int y = 6 + Layout.dy("ModuleList"), index = 0, maxW = 0, topY = y;

        List<Module> sorted = sortedModules(tr);

        for (Module m : sorted) {
            boolean show = m.enabled && Modules.MODULE_LIST.enabled && m != Modules.MODULE_LIST;
            m.anim += ((show ? 1f : 0f) - m.anim) * 0.15f;
            if (m.anim < 0.01f) continue;

            int w = m.width(tr) + 10, h = 14;
            maxW = Math.max(maxW, w);
            int x = screenW - (int) (w * m.anim) - 4 + Layout.dx("ModuleList");
            ctx.fillGradient(x, y, x + w, y + h, Gfx.BG_TOP, Gfx.BG_BOT);
            ctx.fill(x, y, x + 2, y + h, Gfx.wave(index * 0.12f));
            Gfx.gradientText(ctx, tr, m.name, x + 6, y + 3, index * 0.12f);
            y += h + 1;
            index++;
        }
        if (index > 0) Layout.reg("ModuleList", screenW - maxW - 4 + Layout.dx("ModuleList"), topY, maxW, y - topY);
    }
}
