package pl.kubalte.gospodarstwo.goblin;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * How goblins fight: run in, crouch, leap with the weapon raised over the head and strike from above, then jump
 * aside or back before the enemy can answer. With a shield they block arrows and blasts.
 */
public final class GoblinCombat {
    private GoblinCombat() {}

    static final class Fight extends Goal {
        private final Goblin g;
        private int phase, timer, cooldown;
        private boolean struck;
        private static final int APPROACH = 0, WINDUP = 1, LEAP = 2, RECOVER = 3;

        Fight(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = g.getTarget();
            return t != null && t.isAlive() && !g.resting() && !g.isSleeping() && g.isTame() && g.mode() != Goblin.MODE_SIT;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = g.getTarget();
            return t != null && t.isAlive() && g.distanceToSqr(t) < 32 * 32 && !g.resting();
        }

        @Override
        public void start() {
            phase = APPROACH;
            timer = 0;
            g.setAggressive(true);
        }

        @Override
        public void stop() {
            g.setAggressive(false);
            g.getNavigation().stop();
            if (g.getTarget() == null || !g.getTarget().isAlive()) {
                if (g.action() != Goblin.A_CHEER) g.act(Goblin.A_CHEER, 30);
                g.addNeeds(0, -2, g.traits().pastime == Traits.Pastime.FIGHTING ? 15 : 2, 0);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = g.getTarget();
            if (t == null) return;
            g.getLookControl().setLookAt(t, 30F, 30F);
            double d = g.distanceTo(t);
            timer++;
            if (cooldown > 0) cooldown--;
            switch (phase) {
                case APPROACH -> {
                    // run at it; close in, then get ready to jump
                    if (timer % 8 == 0 || g.getNavigation().isDone()) g.getNavigation().moveTo(t, d > 6 ? 1.55 : 1.25);
                    if (d < 3.6 && g.onGround() && cooldown == 0) {
                        g.getNavigation().stop();
                        phase = WINDUP;
                        timer = 0;
                        g.act(Goblin.A_WINDUP, 8);
                    } else if (d < 1.6 && cooldown == 0) {
                        // too close for a leap: a quick jab
                        jab(t);
                    }
                }
                case WINDUP -> {
                    g.getNavigation().stop();
                    g.setDeltaMovement(g.getDeltaMovement().multiply(0.3, 1, 0.3));
                    if (timer >= 6) {
                        Vec3 dir = t.position().subtract(g.position()).multiply(1, 0, 1).normalize();
                        double reach = Math.min(1.0, d / 3.6);
                        g.setDeltaMovement(dir.x * 0.62 * reach + 0.05 * dir.x, 0.48, dir.z * 0.62 * reach + 0.05 * dir.z);
                        g.hurtMarked = true;
                        g.act(Goblin.A_LEAP, 14);
                        phase = LEAP;
                        timer = 0;
                        struck = false;
                    }
                }
                case LEAP -> {
                    if (!struck && g.distanceTo(t) < 1.9 + t.getBbWidth() / 2 && (timer > 4 || g.onGround())) {
                        strike(t, true);
                        struck = true;
                    }
                    if (timer > 3 && g.onGround()) {
                        if (struck) dodge(t);
                        phase = RECOVER;
                        timer = 0;
                        cooldown = 18 + g.getRandom().nextInt(14);
                    } else if (timer > 30) {
                        phase = APPROACH;
                        timer = 0;
                    }
                }
                case RECOVER -> {
                    // keep a little distance, circling, until ready again
                    if (timer % 10 == 0 && d < 3) {
                        Vec3 side = t.position().subtract(g.position()).multiply(1, 0, 1).normalize().yRot((float) Math.PI / 2);
                        Vec3 p = g.position().add(side.scale(g.getRandom().nextBoolean() ? 2 : -2));
                        g.getNavigation().moveTo(p.x, p.y, p.z, 1.2);
                    }
                    if (cooldown == 0) {
                        phase = APPROACH;
                        timer = 0;
                    }
                }
                default -> phase = APPROACH;
            }
        }

        void jab(LivingEntity t) {
            strike(t, false);
            cooldown = 14;
            if (g.getRandom().nextFloat() < g.race().dodge) dodge(t);
        }

        void strike(LivingEntity t, boolean fromAbove) {
            if (!(g.level() instanceof ServerLevel sl)) return;
            g.swing(InteractionHand.MAIN_HAND);
            g.act(fromAbove ? Goblin.A_STRIKE : Goblin.A_PUNCH, 8);
            boolean hit = g.doHurtTarget(sl, t);
            if (hit && fromAbove) {
                // the blow from above hits harder
                t.hurtServer(sl, g.damageSources().mobAttack(g), (float) g.race().attack * 0.6F);
                sl.sendParticles(ParticleTypes.CRIT, t.getX(), t.getEyeY(), t.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
            }
            if (hit && !g.getMainHandItem().isEmpty() && g.getMainHandItem().isDamageableItem())
                g.getMainHandItem().hurtAndBreak(1, g, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }

        /** Jumps away to the left, right or back. */
        void dodge(LivingEntity t) {
            Vec3 to = t.position().subtract(g.position()).multiply(1, 0, 1).normalize();
            int k = g.getRandom().nextInt(3);
            Vec3 dir = k == 0 ? to.yRot((float) Math.PI / 2) : k == 1 ? to.yRot((float) -Math.PI / 2) : to.scale(-1);
            double power = 0.45 + g.race().dodge * 0.4;
            g.setDeltaMovement(dir.x * power, 0.3, dir.z * power);
            g.hurtMarked = true;
            g.act(k == 0 ? Goblin.A_DODGE_L : k == 1 ? Goblin.A_DODGE_R : Goblin.A_DODGE_B, 10);
        }
    }

    /** With a weapon in hand (and guarding switched on) it attacks monsters that go after you, other goblins or your animals. */
    static final class Guard extends TargetGoal {
        private final Goblin g;
        private LivingEntity found;

        Guard(Goblin g) {
            super(g, false);
            this.g = g;
        }

        static boolean friendly(Goblin g, LivingEntity e) {
            if (e == g.getOwner() || e instanceof Goblin) return true;
            if (e instanceof OwnableEntity o && o.getOwner() == g.getOwner()) return true;
            return e instanceof Animal || e instanceof Npc;
        }

        @Override
        public boolean canUse() {
            if (!g.isTame() || g.tickCount % 10 != 0 || !g.settings().guard) return false;
            Work.Tool tool = g.tool();
            if (tool != Work.Tool.SWORD && tool != Work.Tool.AXE && g.traits().pastime != Traits.Pastime.FIGHTING) return false;
            List<Mob> m = g.level().getEntitiesOfClass(Mob.class, g.getBoundingBox().inflate(14), x -> x instanceof Enemy && x.isAlive()
                    && x.getTarget() != null && friendly(g, x.getTarget()));
            if (m.isEmpty()) return false;
            m.sort((a, b) -> Double.compare(a.distanceToSqr(g), b.distanceToSqr(g)));
            found = m.get(0);
            return true;
        }

        @Override
        public void start() {
            g.setTarget(found);
            super.start();
        }
    }

    // ------------------------------------------------------------------ shield
    /** With a shield in the off hand: raises it against arrows flying at it, swelling creepers and archers taking aim. */
    static void shieldTick(Goblin g, ServerLevel sl) {
        if (!g.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)) return;
        if (g.isUsingItem()) {
            if (g.blockTicks > 0 && --g.blockTicks == 0) g.stopUsingItem();
            return;
        }
        if (g.tickCount % 2 != 0) return;
        Entity threat = threat(g, sl);
        if (threat == null) return;
        g.getLookControl().setLookAt(threat, 90F, 90F);
        g.setYRot((float) (Math.atan2(threat.getZ() - g.getZ(), threat.getX() - g.getX()) * 180 / Math.PI) - 90F);
        g.yBodyRot = g.getYRot();
        g.yHeadRot = g.getYRot();
        g.startUsingItem(InteractionHand.OFF_HAND);
        g.blockTicks = 30;
        g.act(Goblin.A_BLOCK, 30);
    }

    static Entity threat(Goblin g, ServerLevel sl) {
        for (Projectile p : sl.getEntitiesOfClass(Projectile.class, g.getBoundingBox().inflate(10), p -> p.getOwner() != g.getOwner())) {
            Vec3 v = p.getDeltaMovement();
            if (v.lengthSqr() < 0.04) continue;
            Vec3 to = g.getEyePosition().subtract(p.position());
            if (to.lengthSqr() > 100) continue;
            if (v.normalize().dot(to.normalize()) > 0.92) return p;
        }
        for (Mob m : sl.getEntitiesOfClass(Mob.class, g.getBoundingBox().inflate(16), m -> m instanceof Enemy)) {
            if (m.getType() == net.minecraft.world.entity.EntityTypes.CREEPER && m.getTarget() != null && m.distanceTo(g) < 4.5) return m;
            if (m.getTarget() == g && m.isUsingItem() && m.distanceTo(g) < 16) return m;
        }
        return null;
    }
}
