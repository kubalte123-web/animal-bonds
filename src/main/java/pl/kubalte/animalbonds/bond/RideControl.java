package pl.kubalte.animalbonds.bond;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import pl.kubalte.animalbonds.BondConfig;

/**
 * Lets a player steer any bonded creature they ride (server-authoritative).
 * While ridden, the creature's own movement AI is suspended and its velocity follows the rider's keys.
 */
public final class RideControl {
    private RideControl() {}

    private static final Map<Mob, Boolean> RIDDEN = Collections.synchronizedMap(new WeakHashMap<>());

    /** Called before the mob's AI runs. */
    public static void preTick(Mob mob) {
        boolean ridden = mob.getFirstPassenger() instanceof ServerPlayer;
        boolean was = RIDDEN.containsKey(mob);
        if (ridden && !was) {
            RIDDEN.put(mob, Boolean.TRUE);
            mob.goalSelector.disableControlFlag(Goal.Flag.MOVE);
            mob.goalSelector.disableControlFlag(Goal.Flag.JUMP);
            mob.goalSelector.disableControlFlag(Goal.Flag.LOOK);
            mob.targetSelector.disableControlFlag(Goal.Flag.TARGET);
        } else if (!ridden && was) {
            RIDDEN.remove(mob);
            mob.goalSelector.enableControlFlag(Goal.Flag.MOVE);
            mob.goalSelector.enableControlFlag(Goal.Flag.JUMP);
            mob.goalSelector.enableControlFlag(Goal.Flag.LOOK);
            mob.targetSelector.enableControlFlag(Goal.Flag.TARGET);
        }
        if (ridden) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            if (mob.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    /** Called after the mob ticked: apply the rider's input as velocity. */
    public static void postTick(Mob mob, ServerPlayer rider) {
        Input in = rider.getLastClientInput();
        float forward = (in.forward() ? 1 : 0) - (in.backward() ? 1 : 0);
        float strafe = (in.left() ? 1 : 0) - (in.right() ? 1 : 0);

        float yRot = rider.getYRot();
        mob.setYRot(yRot);
        mob.yBodyRot = yRot;
        mob.yHeadRot = yRot;

        Vec3 v = mob.getDeltaMovement();
        if (forward != 0 || strafe != 0) {
            double yaw = Math.toRadians(yRot);
            double fx = -Math.sin(yaw), fz = Math.cos(yaw);
            double lx = fz, lz = -fx;
            Vec3 dir = new Vec3(fx * forward + lx * strafe, 0, fz * forward + lz * strafe).normalize();
            double attr = mob.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED) ? mob.getAttributeValue(Attributes.MOVEMENT_SPEED) : 0.25;
            double speed = (0.12 + Math.min(attr, 0.5) * 0.6) * BondConfig.RIDE_SPEED.get() * (in.sprint() ? 1.4 : 1.0);
            if (forward < 0 && strafe == 0) speed *= 0.5;
            double vy = v.y;
            if (mob.isNoGravity()) vy = -Math.sin(Math.toRadians(rider.getXRot())) * speed * forward;
            v = new Vec3(dir.x * speed, vy, dir.z * speed);
        } else if (mob.isNoGravity()) {
            v = new Vec3(v.x * 0.5, 0, v.z * 0.5);
        }
        if (in.jump()) {
            if (mob.isNoGravity()) {
                v = new Vec3(v.x, 0.25, v.z);
            } else if (mob.onGround()) {
                v = new Vec3(v.x, 0.5, v.z);
            }
        }
        mob.setDeltaMovement(v);
        mob.hurtMarked = true;
    }
}
