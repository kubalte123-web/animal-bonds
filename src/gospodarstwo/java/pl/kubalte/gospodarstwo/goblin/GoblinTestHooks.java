package pl.kubalte.gospodarstwo.goblin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Lets the self test drive the goblin's jobs tick by tick without waiting for its AI to pick them. */
public final class GoblinTestHooks {
    private GoblinTestHooks() {}

    static void stepAction(Goblin g) {
        if (g.actionLeft() > 0) g.act(g.action(), g.actionLeft() - 1);
    }

    /** Fells the nearest allowed tree; returns how many logs were cut, or -1 if none was found. */
    public static int chop(Goblin g) {
        GoblinTools.Chop c = GoblinTools.Chop.find(g);
        if (c == null) return -1;
        int before = g.level().getBlockState(c.base).isAir() ? 0 : c.logs.size();
        c.start(g);
        for (int i = 0; i < 4000 && !c.done; i++) {
            g.teleportTo(c.base.getX() + 1.5, c.base.getY(), c.base.getZ() + 0.5);
            c.tick(g);
        }
        c.stop(g);
        return before;
    }

    /** Digs the given number of mine steps; returns the floor of the last one. */
    public static BlockPos mine(Goblin g, BlockPos entrance, int steps) {
        g.mine = entrance;
        g.mineDepth = 0;
        GoblinTools.Mine m = new GoblinTools.Mine();
        m.start(g);
        for (int i = 0; i < 20000 && g.mineDepth < steps && !m.done; i++) {
            BlockPos stand = m.at(g, Math.max(0, g.mineDepth - 1));
            g.teleportTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
            m.tick(g);
            if (m.time > 3000) m.time = 0;
        }
        m.stop(g);
        return m.at(g, Math.max(0, steps - 1));
    }

    /** Shears the nearest woolly sheep; true if it was shorn. */
    public static boolean shear(Goblin g) {
        GoblinTools.Shear s = GoblinTools.Shear.find(g);
        if (s == null) return false;
        s.start(g);
        for (int i = 0; i < 200 && !s.done; i++) {
            g.teleportTo(s.sheep.getX() + 1, s.sheep.getY(), s.sheep.getZ());
            stepAction(g);
            s.tick(g);
        }
        return s.done && !((net.minecraft.world.entity.Shearable) s.sheep).readyForShearing();
    }

    /** Builds the hut at a free spot near home from the pack; returns its origin or null. */
    public static BlockPos buildHut(Goblin g) {
        if (!GoblinHut.hasMaterials(g)) return null;
        Direction[] d = new Direction[1];
        BlockPos spot = GoblinHut.findSpot(g, d);
        if (spot == null) return null;
        GoblinHut.Build b = new GoblinHut.Build(g);
        b.forTest(spot, d[0]);
        b.start();
        for (int i = 0; i < 1300 && b.canContinueToUse(); i++) {
            if (i == 0) {
                Vec3 f = Vec3.atBottomCenterOf(spot.relative(d[0], 1));
                g.teleportTo(f.x, f.y, f.z);
            }
            b.tick();
        }
        b.stop();
        return g.hut;
    }

    public static BlockPos hut(Goblin g) {
        return g.hut;
    }

    public static boolean wantsToDance(Goblin g) {
        return new GoblinNeeds.Dance(g).wouldDance();
    }

    public static BlockPos campfire(Level level, BlockPos c) {
        return GoblinNeeds.litCampfire(level, c, 16);
    }

    public static boolean friendly(Goblin g, LivingEntity e) {
        return GoblinCombat.Guard.friendly(g, e);
    }

    /** One shield check with the given projectile or mob around; true if the shield went up. */
    public static boolean shield(Goblin g, ServerLevel sl) {
        g.tickCount = 0;
        GoblinCombat.shieldTick(g, sl);
        return g.isUsingItem() && g.action() == Goblin.A_BLOCK;
    }

    public static boolean scary(Goblin g, net.minecraft.world.entity.Entity e) {
        return GoblinNeeds.scaryEntity(g, e);
    }

    public static void setTraits(Goblin g, Traits t) {
        g.setTraitsForTest(t);
    }
}
