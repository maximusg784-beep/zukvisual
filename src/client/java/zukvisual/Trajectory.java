package zukvisual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Prediction: показывает траекторию и точку падения.
 *  - твоего броска, пока держишь в руке жемчуг, снежок, яйцо, зелье, бутылку опыта, трезубец или натягиваешь лук;
 *  - уже летящих снарядов (стрелы, жемчуг и т.п.), у чужих линия оранжевая.
 * Считается по физике игры, блоки учитываются, сущности нет. Рисуется частицами.
 */
public final class Trajectory {
    static int tick = 0;

    record Spec(double speed, double gravity, double rollDeg) {}

    static Spec specFor(MinecraftClient mc, ItemStack st) {
        if (st.isEmpty()) return null;
        if (st.isOf(Items.ENDER_PEARL) || st.isOf(Items.SNOWBALL) || st.isOf(Items.EGG)) return new Spec(1.5, 0.03, 0);
        if (st.isOf(Items.SPLASH_POTION) || st.isOf(Items.LINGERING_POTION)) return new Spec(0.5, 0.05, -20);
        if (st.isOf(Items.EXPERIENCE_BOTTLE)) return new Spec(0.7, 0.07, -20);
        if (st.isOf(Items.TRIDENT)) return new Spec(2.5, 0.05, 0);
        if (st.isOf(Items.BOW) && mc.player.isUsingItem()) {
            float f = mc.player.getItemUseTime() / 20f;
            f = (f * f + f * 2f) / 3f;
            if (f < 0.1f) return null;
            return new Spec(Math.min(1f, f) * 3.0, 0.05, 0);
        }
        return null;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!Modules.PREDICTION.enabled || mc.player == null || mc.world == null) return;
            if (++tick % 2 != 0) return;

            Spec sp = specFor(mc, mc.player.getMainHandStack());
            if (sp == null) sp = specFor(mc, mc.player.getOffHandStack());
            if (sp != null) own(mc, sp);
            flying(mc);
        });
    }

    static void own(MinecraftClient mc, Spec sp) {
        var p = mc.player;
        double yaw = Math.toRadians(p.getYaw()), pitch = Math.toRadians(p.getPitch()), roll = Math.toRadians(sp.rollDeg());
        double dx = -Math.sin(yaw) * Math.cos(pitch), dy = -Math.sin(pitch + roll), dz = Math.cos(yaw) * Math.cos(pitch);
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= len; dy /= len; dz /= len;
        var pv = p.getVelocity();
        simulate(mc, p.getX(), p.getEyeY() - 0.1, p.getZ(),
                dx * sp.speed() + pv.x, dy * sp.speed() + (p.isOnGround() ? 0 : pv.y), dz * sp.speed() + pv.z,
                sp.gravity(), true, 100);
    }

    static void flying(MinecraftClient mc) {
        int n = 0;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ProjectileEntity pe)) continue;
            double g;
            if (e instanceof PersistentProjectileEntity) g = 0.05;
            else if (e instanceof ThrownItemEntity) g = 0.03;
            else continue;
            var v = e.getVelocity();
            if (v.lengthSquared() < 0.04) continue; // стоит или воткнулась
            if (e.squaredDistanceTo(mc.player) > 64 * 64) continue;
            boolean mine = pe.getOwner() == mc.player;
            simulate(mc, e.getX(), e.getY(), e.getZ(), v.x, v.y, v.z, g, mine, 60);
            if (++n >= 6) break;
        }
    }

    static void simulate(MinecraftClient mc, double x, double y, double z, double vx, double vy, double vz,
                         double gravity, boolean mine, int steps) {
        Vec3d prev = new Vec3d(x, y, z);
        for (int i = 0; i < steps; i++) {
            x += vx; y += vy; z += vz;
            vx *= 0.99; vy *= 0.99; vz *= 0.99;
            vy -= gravity;
            Vec3d next = new Vec3d(x, y, z);

            BlockHitResult hit = mc.world.raycast(new RaycastContext(prev, next,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            boolean landed = hit.getType() != HitResult.Type.MISS;
            Vec3d pt = landed ? hit.getPos() : next;

            if (i % 2 == 0 || landed) {
                int color = mine ? Settings.fxColor(i / (float) steps) : 0xFF9A3B;
                Fx.addKeep(mc.world, new DustParticleEffect(color, 0.8f), pt.x, pt.y, pt.z, 0, 0, 0);
            }
            if (landed) { // точка падения: зелёное кольцо
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    Fx.addKeep(mc.world, new DustParticleEffect(0x22E58A, 1.0f),
                            pt.x + Math.cos(a) * 0.3, pt.y + 0.05, pt.z + Math.sin(a) * 0.3, 0, 0, 0);
                }
                return;
            }
            prev = next;
            if (y < mc.world.getBottomY() - 10) return;
        }
    }

    private Trajectory() {}
}
