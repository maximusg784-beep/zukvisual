package zukvisual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Меню Zukvisual. Страница 1: модули и тема. Страница 2 (Binds): все клавиши мода,
 * включая клавиши включения и выключения каждого модуля. Список прокручивается колёсиком.
 * Клик по строке, затем нажми любую клавишу или кнопку мыши (в том числе боковую).
 * Esc отменяет, Backspace снимает назначение.
 */
public class ClickGuiScreen extends Screen {
    static final int PW = 280, HEAD = 28;

    boolean bindsPage = false;
    boolean configsPage = false;
    KeyBinding waiting = null;
    int scroll = 0;
    final long opened = System.currentTimeMillis();

    static class Row {
        String name, desc;
        Module module;            // если не null, рисуем переключатель
        Supplier<String> value;   // иначе текст справа
        Runnable click;
    }

    public ClickGuiScreen() {
        super(Text.literal("Zukvisual"));
        Sfx.toggle(true);
    }

    private Row row(String name, String desc, Module m, Supplier<String> value, Runnable click) {
        Row r = new Row();
        r.name = name; r.desc = desc; r.module = m; r.value = value; r.click = click;
        return r;
    }

    private void setPage(int page) {
        bindsPage = page == 1;
        configsPage = page == 2;
        waiting = null;
        scroll = 0;
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        if (configsPage) {
            rows.add(row("Назад", "К модулям", null, () -> "<", () -> setPage(0)));
            for (int n = 1; n <= 3; n++) {
                final int k = n;
                rows.add(row("Сохранить профиль " + k, "Записать текущие настройки", null, () -> "save", () -> {
                    Modules.saveProfile(k);
                    HudExtras.notify("Профиль " + k + " сохранён");
                }));
                rows.add(row("Загрузить профиль " + k, "Вернуть сохранённые настройки", null, () -> "load", () ->
                        HudExtras.notify(Modules.loadProfile(k) ? "Профиль " + k + " загружен" : "Профиль " + k + " пуст")));
            }
        } else if (!bindsPage) {
            for (Module m : Modules.ALL) {
                rows.add(row(m.name, m.desc, m, null, () -> {
                    m.enabled = !m.enabled;
                    Modules.save();
                    HudExtras.notify(m.name + (m.enabled ? " включён" : " выключен"));
                    Sfx.toggle(m.enabled);
                }));
            }
            rows.add(row("Theme", "Клик: сменить цвета", null, () -> Gfx.THEME_NAMES[Gfx.themeIndex], () -> {
                Gfx.setTheme(Gfx.themeIndex + 1);
                Modules.save();
                HudExtras.notify("Тема: " + Gfx.THEME_NAMES[Gfx.themeIndex]);
            }));
            rows.add(row("Hit style", "Клик: сменить частицы удара", null, () -> ZukEffects.STYLES[ZukEffects.hitStyle], () -> {
                ZukEffects.hitStyle = (ZukEffects.hitStyle + 1) % ZukEffects.STYLES.length;
                Modules.save();
                HudExtras.notify("Частицы удара: " + ZukEffects.STYLES[ZukEffects.hitStyle]);
            }));
            rows.add(row("Kill style", "Клик: сменить анимацию убийства", null, () -> Cosmetics.KILL_STYLES[Cosmetics.killStyle], () -> {
                Cosmetics.killStyle = (Cosmetics.killStyle + 1) % Cosmetics.KILL_STYLES.length;
                Modules.save();
                HudExtras.notify("Анимация убийства: " + Cosmetics.KILL_STYLES[Cosmetics.killStyle]);
            }));
            rows.add(row("Particle quality", "Клик: Low / Medium / High (чем ниже, тем больше FPS)", null, () -> Fx.QUALITY_NAMES[Fx.quality], () -> {
                Fx.quality = (Fx.quality + 1) % Fx.QUALITY_NAMES.length;
                Modules.save();
                HudExtras.notify("Качество частиц: " + Fx.QUALITY_NAMES[Fx.quality]);
            }));
            rows.add(row("Cosmetic color", "Клик: цвет темы или радуга", null, () -> Settings.COLOR[Settings.colorMode], () -> {
                Settings.colorMode = (Settings.colorMode + 1) % Settings.COLOR.length;
                Modules.save();
            }));
            rows.add(row("Particle size", "Клик: размер частиц косметики", null, () -> Settings.SIZE[Settings.sizeMode], () -> {
                Settings.sizeMode = (Settings.sizeMode + 1) % Settings.SIZE.length;
                Modules.save();
            }));
            rows.add(row("Panel opacity", "Клик: прозрачность панелей", null, () -> Settings.OPACITY[Settings.opacity], () -> {
                Settings.opacity = (Settings.opacity + 1) % Settings.OPACITY.length;
                Settings.applyOpacity();
                Modules.save();
            }));
            rows.add(row("Snow density", "Клик: густота снега", null, () -> Settings.SNOW[Settings.snow], () -> {
                Settings.snow = (Settings.snow + 1) % Settings.SNOW.length;
                Modules.save();
            }));
            rows.add(row("Holiday", "Клик: Auto по дате / Halloween / NewYear / Birthday / Women", null, () -> Settings.HOLIDAY[Settings.holidayMode], () -> {
                Settings.holidayMode = (Settings.holidayMode + 1) % Settings.HOLIDAY.length;
                Modules.save();
                HudExtras.notify("Праздник: " + Settings.HOLIDAY[Settings.holidayMode]);
            }));
            rows.add(row("World particles", "Клик: стиль частиц мира", null, () -> Settings.WORLD[Settings.worldStyle], () -> {
                Settings.worldStyle = (Settings.worldStyle + 1) % Settings.WORLD.length;
                Modules.save();
            }));
            rows.add(row("HUD editor", "Перетащи блоки HUD мышью", null, () -> ">", () ->
                    MinecraftClient.getInstance().setScreen(new HudEditorScreen())));
            rows.add(row("Профили", "Сохранить и загрузить настройки", null, () -> ">", () -> setPage(2)));
            rows.add(row("Binds", "Клавиши и кнопки мыши", null, () -> ">", () -> setPage(1)));
        } else {
            rows.add(row("Назад", "К модулям", null, () -> "<", () -> setPage(0)));
            for (KeyBinding kb : Binds.ALL) {
                rows.add(row(Binds.label(kb),
                        waiting == kb ? "Нажми клавишу или кнопку мыши (Esc отмена)" : "Клик, затем нажми клавишу или кнопку мыши",
                        null, () -> waiting == kb ? "..." : Binds.keyName(kb), () -> waiting = kb));
            }
        }
        return rows;
    }

    /** {сколько строк видно, высота строки} */
    private int[] layout(int n) {
        int avail = height - 24 - HEAD - 6;
        int fit = Math.max(4, avail / 22);
        int visible = Math.min(n, fit);
        int r = Math.max(12, Math.min(26, avail / Math.max(1, visible)));
        return new int[]{visible, r};
    }

    private void clampScroll(int n, int visible) {
        scroll = Math.max(0, Math.min(scroll, n - visible));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float t = Math.min(1f, (System.currentTimeMillis() - opened) / 200f);
        t = 1f - (1f - t) * (1f - t);
        ctx.fill(0, 0, width, height, Gfx.withAlpha(0x000000, (int) (0x90 * t)));

        List<Row> rows = rows();
        int n = rows.size();
        int[] lay = layout(n);
        int visible = lay[0], r = lay[1];
        clampScroll(n, visible);
        int ph = HEAD + visible * r + 6;
        int px = (width - PW) / 2, py = (height - ph) / 2 + (int) ((1f - t) * 14);
        boolean twoLines = r >= 24;

        Gfx.roundedGradient(ctx, px, py, PW, ph, 5, Gfx.BG_TOP, Gfx.BG_BOT);
        Gfx.gradientLine(ctx, px + 5, py, PW - 10, 2);
        Gfx.gradientText(ctx, textRenderer, configsPage ? "Zukvisual  Профили" : bindsPage ? "Zukvisual  Binds" : "Zukvisual", px + 10, py + 10, 0f);

        int y = py + HEAD;
        for (int i = scroll; i < scroll + visible; i++) {
            Row row = rows.get(i);
            boolean hover = mouseX >= px && mouseX < px + PW && mouseY >= y && mouseY < y + r;
            if (hover) ctx.fill(px + 4, y, px + PW - 8, y + r - 2, 0x28FFFFFF);

            ctx.drawTextWithShadow(textRenderer, row.name, px + 12, twoLines ? y + 4 : y + (r - 8) / 2, 0xFFFFFFFF);
            if (twoLines) ctx.drawTextWithShadow(textRenderer, row.desc, px + 12, y + 14, 0xFF8888A0);

            if (row.module != null) {
                Module m = row.module;
                m.toggle += ((m.enabled ? 1f : 0f) - m.toggle) * 0.25f;
                int sx = px + PW - 44, sy = y + (r - 10) / 2;
                ctx.fill(sx, sy, sx + 26, sy + 10, Gfx.mix(0x2A2A38, Gfx.wave(0.2f), m.toggle));
                int kx = sx + 2 + (int) (14 * m.toggle);
                ctx.fill(kx, sy + 2, kx + 8, sy + 8, 0xFFFFFFFF);
            } else {
                String v = row.value.get();
                Gfx.gradientText(ctx, textRenderer, v, px + PW - 14 - textRenderer.getWidth(v), y + (r - 8) / 2, 0f);
            }
            y += r;
        }

        // полоса прокрутки
        if (n > visible) {
            int trackH = visible * r;
            int thumbH = Math.max(10, trackH * visible / n);
            int thumbY = py + HEAD + (trackH - thumbH) * scroll / Math.max(1, n - visible);
            ctx.fill(px + PW - 5, py + HEAD, px + PW - 3, py + HEAD + trackH, 0x30FFFFFF);
            ctx.fill(px + PW - 5, thumbY, px + PW - 3, thumbY + thumbH, Gfx.wave(0.1f));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (int) Math.signum(verticalAmount);
        return true;
    }

    private void assign(InputUtil.Key key) {
        Binds.bind(waiting, key);
        HudExtras.notify(Binds.label(waiting) + ": " + Binds.keyName(waiting));
        waiting = null;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (waiting != null) {
            // любая кнопка мыши, включая боковые (Mouse 4 / Mouse 5)
            assign(InputUtil.Type.MOUSE.createFromCode(click.button()));
            return true;
        }
        if (click.button() == 0) {
            List<Row> rows = rows();
            int n = rows.size();
            int[] lay = layout(n);
            int visible = lay[0], r = lay[1];
            clampScroll(n, visible);
            int ph = HEAD + visible * r + 6;
            int px = (width - PW) / 2, top = (height - ph) / 2 + HEAD;
            if (click.x() >= px && click.x() < px + PW && click.y() >= top && click.y() < top + visible * r) {
                int idx = scroll + (int) ((click.y() - top) / r);
                if (idx >= 0 && idx < n) {
                    rows.get(idx).click.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    // 1.21.9+: keyPressed принимает KeyInput. Если у тебя другие маппинги, поменяй параметры.
    @Override
    public boolean keyPressed(KeyInput input) {
        if (waiting != null) {
            int code = input.key();
            if (code == GLFW.GLFW_KEY_ESCAPE) waiting = null;
            else if (code == GLFW.GLFW_KEY_BACKSPACE) assign(InputUtil.UNKNOWN_KEY);
            else assign(InputUtil.Type.KEYSYM.createFromCode(code));
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() { return false; }
}
