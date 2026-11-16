package zukvisual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** HUD-редактор: перетаскивай блоки мышью. R сбрасывает всё, Esc сохраняет и выходит. */
public class HudEditorScreen extends Screen {
    String dragging = null;
    boolean wasDown = false;
    int lastX, lastY;

    public HudEditorScreen() {
        super(Text.literal("HUD"));
    }

    private String pick(int mx, int my) {
        String hit = null;
        for (var e : Layout.RECTS.entrySet()) {
            Layout.Rect r = e.getValue();
            if (mx >= r.x() && mx < r.x() + r.w() && my >= r.y() && my < r.y() + r.h()) hit = e.getKey();
        }
        return hit;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x50000000);

        // состояние левой кнопки отслеживаем сами: нажали, тянем, отпустили
        boolean down = MinecraftClient.getInstance().mouse.wasLeftButtonClicked();
        if (down && !wasDown) dragging = pick(mouseX, mouseY);
        if (down && dragging != null) Layout.move(dragging, mouseX - lastX, mouseY - lastY);
        if (!down && wasDown) {
            if (dragging != null) Layout.save();
            dragging = null;
        }
        wasDown = down;
        lastX = mouseX;
        lastY = mouseY;

        String hover = dragging != null ? dragging : pick(mouseX, mouseY);
        for (var e : Layout.RECTS.entrySet()) {
            Layout.Rect r = e.getValue();
            int c = e.getKey().equals(hover) ? 0xFF22E58A : Gfx.wave(0.1f);
            ctx.fill(r.x() - 1, r.y() - 1, r.x() + r.w() + 1, r.y(), c);
            ctx.fill(r.x() - 1, r.y() + r.h(), r.x() + r.w() + 1, r.y() + r.h() + 1, c);
            ctx.fill(r.x() - 1, r.y(), r.x(), r.y() + r.h(), c);
            ctx.fill(r.x() + r.w(), r.y(), r.x() + r.w() + 1, r.y() + r.h(), c);
            ctx.drawTextWithShadow(textRenderer, e.getKey(), r.x(), Math.max(0, r.y() - 10), c);
        }

        String hint = "Перетаскивай блоки мышью   R: сбросить всё   Esc: выйти";
        Gfx.gradientText(ctx, textRenderer, hint, (width - textRenderer.getWidth(hint)) / 2, height - 14, 0f);
    }

    // 1.21.9+: keyPressed принимает KeyInput. Если у тебя другие маппинги, поменяй параметры.
    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_R) {
            Layout.reset();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() { return false; }
}
