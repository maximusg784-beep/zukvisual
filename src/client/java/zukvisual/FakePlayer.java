package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.UUID;

/**
 * FakePlayer: твоя копия (с твоей экипировкой) стоит перед тобой, её можно бить для проверки урона и эффектов.
 * Существует только на твоём клиенте: серверу о ней ничего не отправляется, использовать её нельзя.
 * Урон примерный: по предмету в руке, перезарядке, критам и броне копии. Когда копия «умирает», она появляется заново.
 */
public final class FakePlayer {
    static OtherClientPlayerEntity fake;
    static ClientWorld spawnedIn;
    static int deadTicks = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null) { fake = null; spawnedIn = null; return; }
            if (fake != null && spawnedIn != mc.world) { fake = null; spawnedIn = null; } // сменился мир
            if (!Modules.FAKE_PLAYER.enabled) {
                if (fake != null) remove(mc.world);
                return;
            }
            if (fake == null) {
                spawn(mc.world, mc.player);
            } else if (fake.getHealth() <= 0f) {
                if (++deadTicks > 30) { remove(mc.world); } // следующий тик создаст новую
            } else {
                deadTicks = 0;
            }
        });

        // Регистрируется последним, чтобы остальные обработчики ударов (частицы, звук, счётчики) отработали раньше.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && fake != null && entity == fake) {
                hitFake(player);
                return ActionResult.SUCCESS; // удар обработан здесь, пакет на сервер не уходит
            }
            return ActionResult.PASS;
        });
    }

    static void spawn(ClientWorld world, PlayerEntity p) {
        OtherClientPlayerEntity e = new OtherClientPlayerEntity(world, p.getGameProfile());
        e.setUuid(UUID.randomUUID()); // чтобы не совпадать с твоим UUID
        e.setId(-4242);
        double yaw = Math.toRadians(p.getYaw());
        e.refreshPositionAndAngles(p.getX() - Math.sin(yaw) * 3, p.getY(), p.getZ() + Math.cos(yaw) * 3, p.getYaw() + 180f, 0f);
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            e.equipStack(s, p.getEquippedStack(s).copy());
        }
        e.setHealth(20f);
        e.setCustomName(Text.literal("Fake " + p.getName().getString()));
        e.setCustomNameVisible(true);
        world.addEntity(e);
        fake = e;
        spawnedIn = world;
        deadTicks = 0;
    }

    static void remove(ClientWorld world) {
        if (fake != null) world.removeEntity(fake.getId(), Entity.RemovalReason.DISCARDED);
        fake = null;
        spawnedIn = null;
        deadTicks = 0;
    }

    static float baseDamage(String id) {
        if (id.contains("sword")) {
            if (id.startsWith("netherite")) return 8; if (id.startsWith("diamond")) return 7;
            if (id.startsWith("iron")) return 6; if (id.startsWith("stone")) return 5;
            return 4;
        }
        if (id.contains("axe") && !id.contains("pickaxe")) {
            if (id.startsWith("netherite")) return 10; if (id.startsWith("diamond") || id.startsWith("iron") || id.startsWith("stone")) return 9;
            return 7;
        }
        if (id.equals("mace")) return 6;
        if (id.equals("trident")) return 9;
        return 1;
    }

    static void hitFake(PlayerEntity player) {
        ItemStack st = player.getMainHandStack();
        float dmg = baseDamage(Registries.ITEM.getId(st.getItem()).getPath());
        float cd = player.getAttackCooldownProgress(0.5f);
        dmg *= 0.2f + cd * cd * 0.8f;
        if (player.fallDistance > 0f && !player.isOnGround() && cd > 0.9f) dmg *= 1.5f; // крит
        dmg *= 1f - Math.min(20, fake.getArmor()) / 25f; // упрощённая защита брони копии
        fake.setHealth(Math.max(0f, fake.getHealth() - dmg));
        fake.maxHurtTime = 10;
        fake.hurtTime = 10;
    }

    private FakePlayer() {}
}
