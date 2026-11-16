package zukvisual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

/** Круг фраз: наведи на сектор и кликни, фраза уйдёт в чат. Esc закрывает без отправки. */
public class PhraseWheelScreen extends Screen {
    static final int RADIUS = 80;

    public PhraseWheelScreen() {
        super(Text.literal("Фразы"));
    }

    private int hovered(double mx, double my) {
        List<String> ph = PhraseWheel.PHRASES;
        int n = ph.size();
        double dx = mx - width / 2.0, dy = my - height / 2.0;
        if (n == 0 || dx * dx + dy * dy < 20 * 20) return -1;
        double ang = Math.atan2(dy, dx) + Math.PI / 2; // 0 = вверх
        double sector = 2 * Math.PI / n;
        int i = (int) Math.round(ang / sector);
        return ((i % n) + n) % n;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x80000000);
        List<String> ph = PhraseWheel.PHRASES;
        int n = ph.size(), cx = width / 2, cy = height / 2;
        int hover = hovered(mouseX, mouseY);

        Gfx.gradientText(ctx, textRenderer, "Фразы", cx - textRenderer.getWidth("Фразы") / 2, cy - 4, 0f);
        for (int i = 0; i < n; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / n;
            int px = cx + (int) (Math.cos(a) * RADIUS), py = cy + (int) (Math.sin(a) * RADIUS * 0.8);
            String s = ph.get(i);
            int w = textRenderer.getWidth(s) + 14;
            Gfx.box(ctx, px - w / 2, py - 8, w, 16);
            if (i == hover) ctx.fill(px - w / 2, py + 8, px + w / 2, py + 10, Gfx.wave(i * 0.1f));
            ctx.drawTextWithShadow(textRenderer, s, px - w / 2 + 7, py - 4, i == hover ? 0xFF22E58A : 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int i = hovered(click.x(), click.y());
        if (click.button() == 0 && i >= 0) {
            PhraseWheel.send(MinecraftClient.getInstance(), PhraseWheel.PHRASES.get(i));
            onClose();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() { return false; }
}
