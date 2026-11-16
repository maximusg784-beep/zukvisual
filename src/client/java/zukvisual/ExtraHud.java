package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** TargetHUD, Potions, ArmorHUD, скорость, CPS, Zoom, DeathPos, пинг. Всё косметика/удобство. */
public final class ExtraHud {
    static LivingEntity lastTarget;
    static long lastTime;

    public static double bps = 0;
    public static String lastDeath = null;
    static double prevX, prevZ;
    static boolean deadNotified = false;
    static boolean lmbDown, rmbDown;
    static boolean wasGround = true;
    static final boolean[] ARMOR_WARNED = new boolean[4];
    static final List<Long> LMB = new ArrayList<>(), RMB = new ArrayList<>();
    static KeyBinding zoomKey;
    static Integer savedFov = null;

    public static void init() {
        zoomKey = Binds.reg("key.zukvisual.zoom", GLFW.GLFW_KEY_Z);

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity le) {
                lastTarget = le;
                lastTime = System.currentTimeMillis();
                Cosmetics.onAttack(le);
                Combat.onAttack(le, player.distanceTo(le));
                if (Modules.HIT_SOUND.enabled) Sfx.play(Sfx.hit(), 1.3f + (float) Math.random() * 0.4f);
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            var p = mc.player;
            if (p == null) return;
            long now = System.currentTimeMillis();

            // скорость (блоков в секунду)
            double dx = p.getX() - prevX, dz = p.getZ() - prevZ;
            bps = Math.sqrt(dx * dx + dz * dz) * 20;
            prevX = p.getX();
            prevZ = p.getZ();

            // CPS: считаем нажатия кнопок мыши
            boolean a = mc.options.attackKey.isPressed(), u = mc.options.useKey.isPressed();
            if (a && !lmbDown) LMB.add(now);
            if (u && !rmbDown) RMB.add(now);
            lmbDown = a;
            rmbDown = u;
            LMB.removeIf(t -> now - t > 1000);
            RMB.removeIf(t -> now - t > 1000);

            // Zoom по удержанию клавиши (как в OptiFine)
            if (Modules.ZOOM.enabled && mc.currentScreen == null && zoomKey.isPressed()) {
                if (savedFov == null) savedFov = mc.options.getFov().getValue();
                mc.options.getFov().setValue(30);
            } else if (savedFov != null) {
                mc.options.getFov().setValue(savedFov);
                savedFov = null;
            }

            // кольцо под ногами при прыжке
            boolean ground = p.isOnGround();
            if (Modules.JUMP_CIRCLE.enabled && wasGround && !ground && p.getVelocity().y > 0.2 && mc.world != null) {
                for (int i = 0; i < 24; i++) {
                    double ang = i * Math.PI * 2 / 24;
                    Fx.add(mc.world, 
                            new DustParticleEffect(Gfx.mix(Gfx.A, Gfx.B, i / 23f) & 0xFFFFFF, 1.0f),
                            p.getX() + Math.cos(ang) * 0.6, p.getY() + 0.05, p.getZ() + Math.sin(ang) * 0.6,
                            Math.cos(ang) * 0.12, 0, Math.sin(ang) * 0.12);
                }
            }
            wasGround = ground;

            // кольцо вокруг цели, в которую бьёшь (частицы, не видны сквозь стены)
            if (Modules.TARGET_MARKER.enabled && lastTarget != null && lastTarget.isAlive()
                    && now - lastTime < 4000 && mc.world != null && p.age % 2 == 0) {
                double rad = lastTarget.getWidth() * 0.6 + 0.3;
                double base = now / 400.0;
                double ty = lastTarget.getY() + lastTarget.getHeight() * (0.5 + 0.5 * Math.sin(now / 350.0));
                for (int i = 0; i < 6; i++) {
                    double ang = base + i * Math.PI / 3;
                    Fx.add(mc.world, 
                            new DustParticleEffect(Gfx.mix(Gfx.A, Gfx.B, i / 5f) & 0xFFFFFF, 0.9f),
                            lastTarget.getX() + Math.cos(ang) * rad, ty, lastTarget.getZ() + Math.sin(ang) * rad, 0, 0, 0);
                }
            }

            // предупреждение: броня почти сломана
            if (Modules.ARMOR_HUD.enabled) {
                EquipmentSlot[] sl = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                String[] nm = {"Шлем", "Нагрудник", "Поножи", "Ботинки"};
                for (int i = 0; i < 4; i++) {
                    ItemStack st = p.getEquippedStack(sl[i]);
                    if (st.isEmpty() || !st.isDamageable()) { ARMOR_WARNED[i] = false; continue; }
                    float left = 1f - st.getDamage() / (float) st.getMaxDamage();
                    if (left < 0.15f && !ARMOR_WARNED[i]) {
                        ARMOR_WARNED[i] = true;
                        HudExtras.notify(nm[i] + " почти сломан");
                    } else if (left > 0.25f) {
                        ARMOR_WARNED[i] = false;
                    }
                }
            }

            // место смерти
            if (p.getHealth() <= 0f) {
                if (!deadNotified && Modules.DEATH_POS.enabled) {
                    lastDeath = p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ();
                    HudExtras.notify("Смерть: " + lastDeath);
                    Waypoints.setDeath(mc);
                }
                deadNotified = true;
            } else {
                deadNotified = false;
            }
        });
    }

    public static int cps(boolean left) { return (left ? LMB : RMB).size(); }

    public static int ping(MinecraftClient mc) {
        var h = mc.getNetworkHandler();
        if (h == null || mc.player == null) return 0;
        var e = h.getPlayerListEntry(mc.player.getUuid());
        return e == null ? 0 : e.getLatency();
    }

    public static void render(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        if (Modules.TARGET_HUD.enabled) drawTarget(ctx, mc, tr);
        if (Modules.POTIONS.enabled) drawPotions(ctx, mc, tr);
        if (Modules.ARMOR_HUD.enabled) drawArmor(ctx, mc, tr);
    }

    static void drawTarget(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        if (lastTarget == null || System.currentTimeMillis() - lastTime > 4000 || !lastTarget.isAlive()) return;
        int x = mc.getWindow().getScaledWidth() / 2 + 24 + Layout.dx("TargetHUD"), y = mc.getWindow().getScaledHeight() / 2 + 24 + Layout.dy("TargetHUD");
        Layout.reg("TargetHUD", x, y, 120, 30);
        Gfx.box(ctx, x, y, 120, 30);
        String name = lastTarget.getName().getString();
        if (name.length() > 16) name = name.substring(0, 16);
        ctx.drawTextWithShadow(tr, name, x + 6, y + 5, 0xFFFFFFFF);
        float hp = lastTarget.getHealth(), max = lastTarget.getMaxHealth();
        String hpText = String.format("%.1f", hp);
        ctx.drawTextWithShadow(tr, hpText, x + 114 - tr.getWidth(hpText), y + 5, 0xFFB8B8C8);
        int bx = x + 6, bw = 108;
        ctx.fill(bx, y + 18, bx + bw, y + 24, 0x40FFFFFF);
        int fw = (int) (bw * Math.max(0f, Math.min(1f, hp / max)));
        Gfx.gradBar(ctx, bx, y + 18, fw, bw, 6, false);
    }

    static String time(int ticks) {
        int s = ticks / 20;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    static void drawPotions(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        var effects = mc.player.getStatusEffects();
        if (effects.isEmpty()) return;
        int x = 6 + Layout.dx("Potions"), y = 58 + Layout.dy("Potions"), rows = 0, w = 60;
        String[] lines = new String[effects.size()];
        for (StatusEffectInstance e : effects) {
            String n = e.getEffectType().value().getName().getString();
            if (e.getAmplifier() > 0) n += " " + (e.getAmplifier() + 1);
            lines[rows] = n + "  " + (e.isInfinite() ? "∞" : time(e.getDuration()));
            w = Math.max(w, tr.getWidth(lines[rows]) + 12);
            rows++;
        }
        Layout.reg("Potions", x, y, w, rows * 12 + 6);
        Gfx.box(ctx, x, y, w, rows * 12 + 6);
        for (int i = 0; i < rows; i++) ctx.drawTextWithShadow(tr, lines[i], x + 6, y + 5 + i * 12, 0xFFE0E0F0);
    }

    static void drawArmor(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] labels = {"HEAD", "BODY", "LEGS", "FEET"};
        int x = 6 + Layout.dx("ArmorHUD"), y = mc.getWindow().getScaledHeight() - 124 + Layout.dy("ArmorHUD");
        Layout.reg("ArmorHUD", x, y, 112, 46);
        Gfx.box(ctx, x, y, 112, 46);
        for (int i = 0; i < 4; i++) {
            ItemStack st = mc.player.getEquippedStack(slots[i]);
            int ry = y + 4 + i * 10;
            if (st.isEmpty() || !st.isDamageable()) {
                ctx.drawTextWithShadow(tr, labels[i] + "  —", x + 4, ry, 0xFF6A6A80);
                continue;
            }
            int max = st.getMaxDamage();
            HudExtras.bar(ctx, tr, labels[i], max - st.getDamage(), max, x, ry);
        }
    }

    private ExtraHud() {}
}
