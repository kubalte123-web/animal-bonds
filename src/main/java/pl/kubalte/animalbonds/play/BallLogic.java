package pl.kubalte.animalbonds.play;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.kubalte.animalbonds.BondConfig;
import pl.kubalte.animalbonds.bond.BondAccess;
import pl.kubalte.animalbonds.bond.BondData;
import pl.kubalte.animalbonds.bond.BondLogic;

/**
 * Ball games: a thrown ball (an ordinary item on the ground) is chased by a bonded creature of the thrower, carried
 * in its mouth and brought back. Fetching counts as petting for the day and gives some bond.
 */
public final class BallLogic {
    private BallLogic() {}

    enum Phase { FLYING, CHASE, CARRY }

    static final class Game {
        final WeakReference<ItemEntity> ball;
        final WeakReference<ServerPlayer> owner;
        Phase phase = Phase.FLYING;
        Mob chaser;
        int age, phaseAge;

        Game(ItemEntity ball, ServerPlayer owner) {
            this.ball = new WeakReference<>(ball);
            this.owner = new WeakReference<>(owner);
        }
    }

    private static final List<Game> GAMES = new ArrayList<>();
    private static final Map<Mob, Long> LAST_REWARD = new WeakHashMap<>();
    /** Number of fetches since start (for the self test). */
    public static int fetches;

    public static void register(IEventBus bus) {
        bus.addListener(BallLogic::tick);
    }

    public static boolean isBall(ItemEntity e) {
        for (Game g : GAMES) if (g.ball.get() == e) return true;
        return false;
    }

    public static boolean isBusy(Mob mob) {
        for (Game g : GAMES) if (g.chaser == mob) return true;
        return false;
    }

    /** Throws one ball from the player's hand. */
    public static ItemEntity throwBall(ServerPlayer player, ItemStack stack) {
        ServerLevel level = (ServerLevel) player.level();
        ItemEntity ball = new ItemEntity(level, player.getX(), player.getEyeY() - 0.3, player.getZ(), stack.copyWithCount(1));
        Vec3 look = player.getLookAngle();
        ball.setDeltaMovement(look.x * 0.9, look.y * 0.9 + 0.25, look.z * 0.9);
        ball.setPickUpDelay(60);
        level.addFreshEntity(ball);
        GAMES.add(new Game(ball, player));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6F, 1.2F);
        return ball;
    }

    static void tick(ServerTickEvent.Post e) {
        Iterator<Game> it = GAMES.iterator();
        while (it.hasNext()) {
            Game g = it.next();
            ItemEntity ball = g.ball.get();
            ServerPlayer owner = g.owner.get();
            if (ball == null || !ball.isAlive() || owner == null || owner.isRemoved() && !(owner instanceof net.neoforged.neoforge.common.util.FakePlayer)
                    || owner.level() != ball.level() || !step(g, ball, owner)) {
                if (ball != null && ball.isAlive()) release(ball);
                it.remove();
            }
        }
    }

    private static void release(ItemEntity ball) {
        ball.setNoGravity(false);
        ball.setPickUpDelay(10);
    }

    /** Returns false when the game is over. */
    private static boolean step(Game g, ItemEntity ball, ServerPlayer owner) {
        ServerLevel level = (ServerLevel) ball.level();
        g.age++;
        g.phaseAge++;
        if (g.age > 1600) return false;
        switch (g.phase) {
            case FLYING -> {
                if (g.age < 8) return true;
                Mob m = findPlayer(level, ball, owner);
                if (m == null) return g.age < 200; // nobody to play with: the ball just lies there
                g.chaser = m;
                g.phase = Phase.CHASE;
                g.phaseAge = 0;
                ball.setPickUpDelay(32767);
            }
            case CHASE -> {
                Mob m = g.chaser;
                if (m == null || !m.isAlive() || g.phaseAge > 400) return false;
                if (m.distanceToSqr(ball) < 1.7 * 1.7 + m.getBbWidth()) {
                    g.phase = Phase.CARRY;
                    g.phaseAge = 0;
                    ball.setNoGravity(true);
                    level.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.5F, 1.4F);
                } else if (g.phaseAge % 10 == 1) {
                    m.getNavigation().moveTo(ball.getX(), ball.getY(), ball.getZ(), 1.4);
                }
            }
            case CARRY -> {
                Mob m = g.chaser;
                if (m == null || !m.isAlive() || g.phaseAge > 600) return false;
                float rot = m.yBodyRot * Mth.DEG_TO_RAD;
                double reach = m.getBbWidth() * 0.6 + 0.15;
                ball.setPos(m.getX() - Mth.sin(rot) * reach, m.getY() + m.getBbHeight() * 0.6, m.getZ() + Mth.cos(rot) * reach);
                ball.setDeltaMovement(Vec3.ZERO);
                if (m.distanceToSqr(owner) < 2.6 * 2.6 + m.getBbWidth()) {
                    deliver(level, g, ball, owner);
                    return false;
                }
                if (g.phaseAge % 10 == 1) m.getNavigation().moveTo(owner, 1.25);
            }
        }
        return true;
    }

    /** A bonded creature of the thrower, from the ball-playing level, not busy, closest to the ball. */
    private static Mob findPlayer(ServerLevel level, ItemEntity ball, ServerPlayer owner) {
        Mob best = null;
        double bestD = Double.MAX_VALUE;
        for (Mob m : level.getEntitiesOfClass(Mob.class, ball.getBoundingBox().inflate(20, 6, 20), Mob::isAlive)) {
            BondData d = BondLogic.get(m);
            if (d == null || d.isFainted() || d.level() < BondConfig.BALL_LEVEL.get() || m.isNoAi()) continue;
            if (m.isPassenger() || m.isVehicle() || m.isLeashed() || isBusy(m) || !BondAccess.canUse(owner, m, d)) continue;
            double dd = m.distanceToSqr(ball);
            if (dd >= bestD) continue;
            // only creatures that can actually get there (not the ones behind the fence)
            net.minecraft.world.level.pathfinder.Path path = m.getNavigation().createPath(ball, 1);
            if (dd > 2.5 * 2.5 && (path == null || !path.canReach())) continue;
            if (dd < bestD) {
                bestD = dd;
                best = m;
            }
        }
        return best;
    }

    private static void deliver(ServerLevel level, Game g, ItemEntity ball, ServerPlayer owner) {
        Mob m = g.chaser;
        ball.setNoGravity(false);
        ball.setPos(owner.getX(), owner.getY() + 0.3, owner.getZ());
        ball.setDeltaMovement(Vec3.ZERO);
        ball.setPickUpDelay(0);
        m.getNavigation().stop();
        fetches++;
        BondData d = BondLogic.get(m);
        long now = level.getGameTime();
        if (d != null) {
            d.lastPet = now; // playing counts as petting for today
            Long last = LAST_REWARD.get(m);
            if (last == null || now - last >= BondConfig.BALL_COOLDOWN.get()) {
                LAST_REWARD.put(m, now);
                BondLogic.addXp(m, d, BondConfig.BALL_XP.get());
            }
            BondLogic.sync(m, d);
        }
        BondLogic.hearts(m, 5);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, m.getX(), m.getY() + m.getBbHeight(), m.getZ(), 6, 0.3, 0.3, 0.3, 0);
    }

    static Map<String, Integer> debug() {
        Map<String, Integer> out = new HashMap<>();
        out.put("games", GAMES.size());
        out.put("fetches", fetches);
        return out;
    }
}
