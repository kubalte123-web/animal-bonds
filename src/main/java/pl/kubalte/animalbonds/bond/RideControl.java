package pl.kubalte.animalbonds.bond;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import pl.kubalte.animalbonds.BondConfig;

/**
 * Lets a player steer any bonded creature they ride (server-authoritative), moving the way that creature moves:
 * <ul>
 *   <li>walkers walk and jump; goats, rabbits, frogs and slimes jump much higher</li>
 *   <li>spiders (and any modded wall climber) climb walls</li>
 *   <li>flyers (phantom, bat, bee, parrot, allay, ghasts, blaze, vex... and modded flyers) fly: look up/down to
 *       climb or dive, jump = rise, no input = hover</li>
 *   <li>swimmers (dolphin, axolotl, turtle, drowned, guardian, fish...) swim freely in 3D under water</li>
 * </ul>
 * While ridden the creature's own movement AI is suspended.
 */
public final class RideControl {
    private RideControl() {}

    public enum Mode { WALK, CLIMB, FLY, SWIM }

    /** Ridden mob -> its original "no gravity" flag (restored when the rider gets off). */
    private static final Map<Mob, Boolean> RIDDEN = Collections.synchronizedMap(new WeakHashMap<>());

    private static final Set<String> FLYERS = Set.of(
            "minecraft:phantom", "minecraft:bat", "minecraft:bee", "minecraft:parrot", "minecraft:allay",
            "minecraft:ghast", "minecraft:happy_ghast", "minecraft:blaze", "minecraft:vex", "minecraft:breeze",
            "minecraft:ender_dragon", "minecraft:wither");
    private static final Set<String> SWIMMERS = Set.of(
            "minecraft:dolphin", "minecraft:axolotl", "minecraft:turtle", "minecraft:drowned", "minecraft:guardian",
            "minecraft:elder_guardian", "minecraft:squid", "minecraft:glow_squid", "minecraft:cod", "minecraft:salmon",
            "minecraft:tropical_fish", "minecraft:pufferfish", "minecraft:tadpole", "minecraft:nautilus",
            "minecraft:zombie_nautilus", "minecraft:frog");

    private static String id(Mob mob) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
    }

    public static boolean canFly(Mob mob) {
        return FLYERS.contains(id(mob)) || mob.getNavigation() instanceof FlyingPathNavigation || mob.getMoveControl() instanceof FlyingMoveControl;
    }

    public static boolean canClimb(Mob mob) {
        return mob.getNavigation() instanceof WallClimberNavigation;
    }

    public static boolean canSwim(Mob mob) {
        return SWIMMERS.contains(id(mob)) || mob.getNavigation() instanceof WaterBoundPathNavigation
                || mob.getNavigation() instanceof AmphibiousPathNavigation;
    }

    public static Mode mode(Mob mob) {
        if (canFly(mob)) return Mode.FLY;
        if (canSwim(mob) && mob.isInWater()) return Mode.SWIM;
        if (canClimb(mob)) return Mode.CLIMB;
        return Mode.WALK;
    }

    /** Initial upward speed of a ridden jump (0.42 = normal mob jump, ~1.25 blocks). */
    public static double jumpPower(Mob mob) {
        return switch (id(mob)) {
            case "minecraft:goat" -> 0.9;          // ~4 blocks: mountain goat
            case "minecraft:rabbit", "minecraft:frog" -> 0.75;
            case "minecraft:slime", "minecraft:magma_cube", "minecraft:sulfur_cube" -> 0.8;
            case "minecraft:camel", "minecraft:camel_husk", "minecraft:fox", "minecraft:cat", "minecraft:ocelot", "minecraft:wolf" -> 0.6;
            case "minecraft:spider", "minecraft:cave_spider" -> 0.55;
            default -> 0.5;
        };
    }

    private static boolean isRidden(Mob mob) {
        return mob.getFirstPassenger() instanceof ServerPlayer;
    }

    /** Called before the mob's AI runs. */
    public static void preTick(Mob mob) {
        boolean ridden = isRidden(mob);
        boolean was = RIDDEN.containsKey(mob);
        if (ridden && !was) {
            RIDDEN.put(mob, mob.isNoGravity());
            mob.goalSelector.disableControlFlag(Goal.Flag.MOVE);
            mob.goalSelector.disableControlFlag(Goal.Flag.JUMP);
            mob.goalSelector.disableControlFlag(Goal.Flag.LOOK);
            mob.targetSelector.disableControlFlag(Goal.Flag.TARGET);
            if (canFly(mob)) mob.setNoGravity(true);
        } else if (!ridden && was) {
            Boolean noGravity = RIDDEN.remove(mob);
            mob.goalSelector.enableControlFlag(Goal.Flag.MOVE);
            mob.goalSelector.enableControlFlag(Goal.Flag.JUMP);
            mob.goalSelector.enableControlFlag(Goal.Flag.LOOK);
            mob.targetSelector.enableControlFlag(Goal.Flag.TARGET);
            mob.setNoGravity(noGravity != null && noGravity);
        }
        if (ridden) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            if (mob.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            if (canFly(mob)) mob.setNoGravity(true);
        }
    }

    /** True while a player rides this creature (used to cancel fall damage from big jumps). */
    public static boolean isPlayerRidden(Mob mob) {
        return RIDDEN.containsKey(mob) && isRidden(mob);
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

        double attr = mob.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED) ? mob.getAttributeValue(Attributes.MOVEMENT_SPEED) : 0.25;
        double speed = (0.12 + Math.min(attr, 0.5) * 0.6) * BondConfig.RIDE_SPEED.get() * (in.sprint() ? 1.4 : 1.0);
        double yaw = Math.toRadians(yRot);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double lx = fz, lz = -fx;
        Vec3 flat = new Vec3(fx * forward + lx * strafe, 0, fz * forward + lz * strafe);
        if (flat.lengthSqr() > 1e-6) flat = flat.normalize();

        Mode mode = mode(mob);
        Vec3 v = mob.getDeltaMovement();
        switch (mode) {
            case FLY, SWIM -> {
                double s = mode == Mode.FLY ? Math.max(speed, 0.3) * 1.3 : Math.max(speed, 0.2);
                double pitch = Math.toRadians(rider.getXRot());
                double vy = forward != 0 ? -Math.sin(pitch) * s * forward : 0;
                double horiz = forward != 0 ? Math.cos(pitch) : 1;
                double vx = flat.x * s * horiz, vz = flat.z * s * horiz;
                if (in.jump()) vy = Math.max(vy, s * 0.8);
                if (forward == 0 && strafe == 0 && !in.jump()) {
                    v = new Vec3(v.x * 0.6, mode == Mode.FLY ? v.y * 0.5 : v.y, v.z * 0.6); // hover / drift
                } else {
                    v = new Vec3(vx, vy, vz);
                }
                mob.resetFallDistance();
            }
            case CLIMB -> {
                if (forward != 0 || strafe != 0) v = new Vec3(flat.x * speed, v.y, flat.z * speed);
                // walk up walls like a spider does
                if (mob.horizontalCollision && (forward != 0 || strafe != 0)) v = new Vec3(v.x, 0.25, v.z);
                if (in.jump() && mob.onGround()) v = new Vec3(v.x, jumpPower(mob), v.z);
                mob.resetFallDistance();
            }
            case WALK -> {
                if (forward != 0 || strafe != 0) {
                    double s = forward < 0 && strafe == 0 ? speed * 0.5 : speed;
                    v = new Vec3(flat.x * s, v.y, flat.z * s);
                }
                if (in.jump() && (mob.onGround() || mob.isInWater())) {
                    v = new Vec3(v.x * 1.2, mob.isInWater() ? 0.3 : jumpPower(mob), v.z * 1.2);
                }
            }
        }
        mob.setDeltaMovement(v);
        mob.hurtMarked = true;
    }
}
