package pl.kubalte.gospodarstwo.goblin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.goal.Goal;
import pl.kubalte.gospodarstwo.block.StationBlockEntity;
import pl.kubalte.gospodarstwo.kitchen.ChurnBlockEntity;
import pl.kubalte.gospodarstwo.kitchen.PressBlockEntity;
import pl.kubalte.gospodarstwo.tools.FishingNetBlockEntity;
import pl.kubalte.gospodarstwo.tools.IncubatorBlockEntity;

/** What a goblin does: hides from people, takes offered food, collects loose things, stores them, works at the base. */
public final class GoblinGoals {
    private GoblinGoals() {}

    public static final int RADIUS = 12;

    static boolean night(Level level) {
        return level.isDarkOutside();
    }

    static Player nearestPlayer(Goblin g, double r) {
        Player best = null;
        double bd = r * r;
        for (Player p : g.level().players()) {
            if (p.isSpectator()) continue;
            double d = p.distanceToSqr(g);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ wild: take thrown food when nobody is close
    static final class TakeOffer extends Goal {
        private final Goblin g;
        private ItemEntity target;

        TakeOffer(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!g.willingToTakeOffer() || g.tickCount % 10 != 0) return false;
            if (nearestPlayer(g, 5.5) != null) return false;
            List<ItemEntity> items = g.level().getEntitiesOfClass(ItemEntity.class, g.getBoundingBox().inflate(10),
                    i -> Goblin.isOffer(i.getItem()) && i.getOwner() instanceof Player);
            if (items.isEmpty()) return false;
            items.sort((a, b) -> Double.compare(a.distanceToSqr(g), b.distanceToSqr(g)));
            target = items.get(0);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return target.isAlive() && g.willingToTakeOffer() && nearestPlayer(g, 4) == null;
        }

        @Override
        public void start() {
            g.getNavigation().moveTo(target, 0.8);
        }

        @Override
        public void tick() {
            g.getLookControl().setLookAt(target);
            if (g.distanceToSqr(target) < 2.0) {
                g.takeOffer(target);
                g.getNavigation().stop();
            } else if (g.getNavigation().isDone()) {
                g.getNavigation().moveTo(target, 0.8);
            }
        }
    }

    // ------------------------------------------------------------------ wild: watch the player from behind a tree
    static final class Hide extends Goal {
        private final Goblin g;
        private Player watched;
        private BlockPos spot;
        private int time;

        Hide(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (g.isTame() || g.considering() > 0) return false;
            watched = nearestPlayer(g, 18);
            return watched != null && watched.distanceTo(g) > 6;
        }

        @Override
        public boolean canContinueToUse() {
            return !g.isTame() && watched.isAlive() && watched.distanceTo(g) < 22 && watched.distanceTo(g) > 5 && g.considering() <= 0;
        }

        @Override
        public void start() {
            time = 0;
            spot = findCover();
            if (spot != null) g.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 1.1);
        }

        @Override
        public void stop() {
            g.setHiding(false);
        }

        /** A place on the far side of a tree trunk, as seen from the player. */
        BlockPos findCover() {
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            BlockPos c = g.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-8, -2, -8), c.offset(8, 2, 8))) {
                if (!g.level().getBlockState(p).is(BlockTags.LOGS)) continue;
                Vec3 away = Vec3.atCenterOf(p).subtract(watched.position()).multiply(1, 0, 1).normalize();
                BlockPos behind = BlockPos.containing(p.getX() + 0.5 + away.x * 1.2, p.getY(), p.getZ() + 0.5 + away.z * 1.2);
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos b = behind.above(dy);
                    if (g.level().getBlockState(b).isAir() && !g.level().getBlockState(b.below()).isAir()) {
                        double d = b.distSqr(c);
                        if (d < bd) {
                            bd = d;
                            best = b.immutable();
                        }
                        break;
                    }
                }
            }
            return best;
        }

        @Override
        public void tick() {
            time++;
            g.getLookControl().setLookAt(watched, 30F, 30F);
            boolean there = spot != null && g.blockPosition().distSqr(spot) <= 2;
            g.setHiding(there);
            if (time % 60 == 0 && !there) {
                spot = findCover();
                if (spot != null) g.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 1.1);
            }
            // now and then it peeks out from behind the trunk
            if (there && time % 80 == 40) g.setHiding(false);
        }
    }

    // ------------------------------------------------------------------ tame: pick up loose things into the pack
    static final class Collect extends Goal {
        private final Goblin g;
        private ItemEntity target;

        Collect(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!g.isTame() || !g.hasPack() || g.mode() == Goblin.MODE_SIT || g.depositing || g.tickCount % 10 != 0) return false;
            double r = g.mode() == Goblin.MODE_FOLLOW ? 10 : GoblinGoals.RADIUS;
            List<ItemEntity> items = g.level().getEntitiesOfClass(ItemEntity.class, g.getBoundingBox().inflate(r),
                    i -> !i.hasPickUpDelay() && g.packHasRoom(i.getItem()) && (g.home() == null || g.mode() != Goblin.MODE_BASE
                            || i.blockPosition().distSqr(g.home()) < RADIUS * RADIUS));
            if (items.isEmpty()) return false;
            items.sort((a, b) -> Double.compare(a.distanceToSqr(g), b.distanceToSqr(g)));
            target = items.get(0);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return target.isAlive() && g.packHasRoom(target.getItem()) && (g.mode() != Goblin.MODE_FOLLOW || g.getOwner() == null
                    || g.getOwner().distanceTo(g) < 16);
        }

        @Override
        public void start() {
            g.setWork(4);
            g.getNavigation().moveTo(target, 1.2);
        }

        @Override
        public void stop() {
            g.setWork(0);
        }

        @Override
        public void tick() {
            if (g.distanceToSqr(target) < 2.2) {
                pickUp(g, target);
            } else if (g.getNavigation().isDone()) {
                g.getNavigation().moveTo(target, 1.2);
            }
        }
    }

    /** Puts an item lying on the ground into the pack. */
    public static void pickUp(Goblin g, ItemEntity item) {
        ItemStack left = g.stow(item.getItem().copy());
        if (left.isEmpty()) item.discard();
        else item.setItem(left);
        g.take(item, 1);
        g.playSound(SoundEvents.ITEM_PICKUP, 0.4F, 1.4F);
    }

    // ------------------------------------------------------------------ store the pack into chests around
    static final class Deposit extends Goal {
        private final Goblin g;
        private BlockPos chest;
        private int open, stuck;

        Deposit(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!g.depositing) return false;
            chest = next(g);
            if (chest == null) {
                g.depositing = false;
                if (g.getOwner() instanceof Player p && !g.pack.isEmpty()) p.sendOverlayMessage(Component.translatable("goblin.gospodarstwo.no_room"));
                else if (g.getOwner() instanceof Player p) p.sendOverlayMessage(Component.translatable("goblin.gospodarstwo.stored"));
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return g.depositing && chest != null && stuck < 200;
        }

        @Override
        public void start() {
            g.setWork(5);
            open = 0;
            stuck = 0;
            g.getNavigation().moveTo(chest.getX() + 0.5, chest.getY(), chest.getZ() + 0.5, 1.1);
        }

        @Override
        public void stop() {
            g.setWork(0);
            if (open > 0 && chest != null) shut(g.level(), chest);
        }

        @Override
        public void tick() {
            stuck++;
            g.getLookControl().setLookAt(Vec3.atCenterOf(chest));
            if (open > 0) {
                if (--open == 0) {
                    shut(g.level(), chest);
                    chest = next(g);
                    stuck = 0;
                    if (chest != null) g.getNavigation().moveTo(chest.getX() + 0.5, chest.getY(), chest.getZ() + 0.5, 1.1);
                    else {
                        g.depositing = false;
                        if (g.getOwner() instanceof Player p)
                            p.sendOverlayMessage(Component.translatable(g.pack.isEmpty() ? "goblin.gospodarstwo.stored" : "goblin.gospodarstwo.no_room"));
                    }
                }
                return;
            }
            if (g.position().distanceToSqr(Vec3.atCenterOf(chest)) < 5.5) {
                g.getNavigation().stop();
                lid(g.level(), chest, true);
                storeInto(g, chest);
                open = 20;
            } else if (g.getNavigation().isDone()) {
                g.getNavigation().moveTo(chest.getX() + 0.5, chest.getY(), chest.getZ() + 0.5, 1.1);
            }
        }
    }

    /** The next container to visit: the target of the first thing in the pack that has somewhere to go. */
    static BlockPos next(Goblin g) {
        List<BlockPos> cs = Stash.containers(g.level(), g.blockPosition(), 16);
        if (cs.isEmpty()) return null;
        for (int i = 0; i < g.pack.getContainerSize(); i++) {
            ItemStack s = g.pack.getItem(i);
            if (s.isEmpty()) continue;
            BlockPos t = Stash.target(g.level(), cs, s);
            if (t != null) return t;
        }
        return null;
    }

    /** Moves into this container everything from the pack whose best place is here. Returns how many stacks moved. */
    public static int storeInto(Goblin g, BlockPos pos) {
        if (!(g.level().getBlockEntity(pos) instanceof Container c)) return 0;
        List<BlockPos> cs = Stash.containers(g.level(), g.blockPosition(), 16);
        if (!cs.contains(pos)) cs.add(0, pos);
        int n = 0;
        for (int i = 0; i < g.pack.getContainerSize(); i++) {
            ItemStack s = g.pack.getItem(i);
            if (s.isEmpty() || !pos.equals(Stash.target(g.level(), cs, s))) continue;
            ItemStack left = Stash.insert(c, s);
            g.pack.setItem(i, left);
            n++;
        }
        return n;
    }

    static void lid(Level level, BlockPos pos, boolean open) {
        BlockState s = level.getBlockState(pos);
        level.blockEvent(pos, s.getBlock(), 1, open ? 1 : 0);
        level.playSound(null, pos, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1.1F);
    }

    static void shut(Level level, BlockPos pos) {
        lid(level, pos, false);
    }

    // ------------------------------------------------------------------ base: walk back home when it strayed
    static final class GoHome extends Goal {
        private final Goblin g;

        GoHome(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return g.isTame() && g.mode() == Goblin.MODE_BASE && g.home() != null && g.blockPosition().distSqr(g.home()) > RADIUS * RADIUS;
        }

        @Override
        public void start() {
            BlockPos h = g.home();
            if (!g.getNavigation().moveTo(h.getX() + 0.5, h.getY(), h.getZ() + 0.5, 1.0))
                g.teleportTo(h.getX() + 0.5, h.getY(), h.getZ() + 0.5);
        }

        @Override
        public boolean canContinueToUse() {
            return !g.getNavigation().isDone();
        }
    }

    // ------------------------------------------------------------------ base: jobs
    static final class BaseWork extends Goal {
        private final Goblin g;
        private Job job;
        private int time, cooldown;

        BaseWork(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!g.isTame() || g.mode() != Goblin.MODE_BASE || g.depositing || g.resting()) return false;
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            if (g.tickCount % 20 != 0) return false;
            job = pick(g);
            if (job == null) cooldown = 60;
            return job != null;
        }

        @Override
        public boolean canContinueToUse() {
            return job != null && time < job.timeout && g.mode() == Goblin.MODE_BASE && !g.depositing;
        }

        @Override
        public void start() {
            time = 0;
            g.setWork(job.kind);
            job.walk(g);
        }

        @Override
        public void stop() {
            g.setWork(0);
            job = null;
            cooldown = 40 + g.getRandom().nextInt(80);
        }

        @Override
        public void tick() {
            time++;
            job.look(g);
            if (job.near(g)) {
                g.getNavigation().stop();
                if (job.act(g, time)) job = null;
            } else if (g.getNavigation().isDone() || time % 40 == 0) {
                job.walk(g);
            }
        }
    }

    /** One piece of work at a place. */
    abstract static class Job {
        final int kind, timeout;
        final BlockPos at;

        Job(int kind, BlockPos at, int timeout) {
            this.kind = kind;
            this.at = at;
            this.timeout = timeout;
        }

        void walk(Goblin g) {
            g.getNavigation().moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 1.0);
        }

        void look(Goblin g) {
            g.getLookControl().setLookAt(Vec3.atCenterOf(at));
        }

        boolean near(Goblin g) {
            return g.position().distanceToSqr(Vec3.atCenterOf(at)) < 4.5;
        }

        /** Does the work; true when finished. */
        abstract boolean act(Goblin g, int time);
    }

    /** Picks something to do around home: sleep at night, cook at a station, harvest, empty the net, play with animals. */
    static Job pick(Goblin g) {
        Level level = g.level();
        BlockPos home = g.home() == null ? g.blockPosition() : g.home();
        if (night(level)) {
            BlockPos bed = freeBed(level, home);
            if (bed != null) return new SleepJob(bed);
        }
        List<Job> options = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(home.offset(-RADIUS, -3, -RADIUS), home.offset(RADIUS, 3, RADIUS))) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof StationBlockEntity st) {
                if (!st.output().isEmpty() && !(st instanceof PressBlockEntity) && g.hasPack() && g.packHasRoom(st.output()))
                    options.add(new StationJob(p.immutable(), false));
                else if (st.input().isEmpty() && g.hasPack() && hasFor(g, st)) options.add(new StationJob(p.immutable(), true));
            } else if (be instanceof ChurnBlockEntity ch) {
                if (ch.milk() || ch.butter() > 0) options.add(new ChurnJob(p.immutable()));
            } else if (be instanceof FishingNetBlockEntity net && !net.catches().isEmpty() && g.hasPack()) {
                options.add(new NetJob(p.immutable()));
            } else if (be instanceof IncubatorBlockEntity inc && inc.eggs().getCount() < IncubatorBlockEntity.MAX && g.hasPack() && hasEggs(g)) {
                options.add(new IncubatorJob(p.immutable()));
            }
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s) && options.size() < 40) options.add(new HarvestJob(p.immutable()));
        }
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, new AABB(home).inflate(RADIUS), a -> !(a instanceof Goblin) && a.isAlive());
        if (!animals.isEmpty()) {
            Animal a = animals.get(g.getRandom().nextInt(animals.size()));
            // playing is always tempting, but work comes first most of the time
            if (options.isEmpty() || g.getRandom().nextInt(4) == 0) return new PlayJob(a);
        }
        if (options.isEmpty()) return null;
        return options.get(g.getRandom().nextInt(options.size()));
    }

    static boolean hasFor(Goblin g, StationBlockEntity st) {
        for (int i = 0; i < g.pack.getContainerSize(); i++) if (!g.pack.getItem(i).isEmpty() && st.accepts(g.pack.getItem(i))) return true;
        return false;
    }

    static boolean hasEggs(Goblin g) {
        for (int i = 0; i < g.pack.getContainerSize(); i++) if (IncubatorBlockEntity.hatchOf(g.pack.getItem(i)) != null) return true;
        return false;
    }

    static BlockPos freeBed(Level level, BlockPos c) {
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-RADIUS, -3, -RADIUS), c.offset(RADIUS, 3, RADIUS))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof BedBlock && s.getValue(BedBlock.PART) == BedPart.HEAD && !s.getValue(BedBlock.OCCUPIED)) return p.immutable();
        }
        return null;
    }

    /** Puts what fits into a station, or takes the product out. */
    public static boolean workStation(Goblin g, StationBlockEntity st) {
        if (!st.output().isEmpty() && !(st instanceof PressBlockEntity)) {
            ItemStack out = st.takeOutput();
            ItemStack left = g.stow(out);
            if (!left.isEmpty()) Block.popResource(g.level(), st.getBlockPos().above(), left);
            return true;
        }
        if (!st.input().isEmpty()) return false;
        for (int i = 0; i < g.pack.getContainerSize(); i++) {
            ItemStack s = g.pack.getItem(i);
            if (s.isEmpty() || !st.accepts(s)) continue;
            int moved = st.offer(s);
            if (moved > 0) {
                s.shrink(moved);
                g.pack.setChanged();
                return true;
            }
        }
        return false;
    }

    /** Reaps a ripe crop and sows it again; the harvest goes into the pack (or onto the ground). */
    public static boolean harvest(Goblin g, BlockPos p) {
        if (!(g.level() instanceof ServerLevel level)) return false;
        BlockState s = level.getBlockState(p);
        if (!(s.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(s)) return false;
        List<ItemStack> drops = Block.getDrops(s, level, p, null, g, ItemStack.EMPTY);
        boolean replant = false;
        for (ItemStack d : drops) if (!replant && d.is(s.getBlock().asItem())) {
            d.shrink(1);
            replant = true;
        }
        level.setBlock(p, replant ? crop.getStateForAge(0) : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        level.playSound(null, p, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.8F, 1.2F);
        for (ItemStack d : drops) {
            ItemStack left = g.stow(d);
            if (!left.isEmpty()) Block.popResource(level, p, left);
        }
        g.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return true;
    }

    static final class StationJob extends Job {
        StationJob(BlockPos at, boolean put) {
            super(3, at, 300);
        }

        @Override
        boolean act(Goblin g, int time) {
            if (g.level().getBlockEntity(at) instanceof StationBlockEntity st) {
                workStation(g, st);
                g.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            return true;
        }
    }

    static final class HarvestJob extends Job {
        HarvestJob(BlockPos at) {
            super(2, at, 300);
        }

        @Override
        boolean act(Goblin g, int time) {
            harvest(g, at);
            return true;
        }
    }

    static final class ChurnJob extends Job {
        ChurnJob(BlockPos at) {
            super(3, at, 600);
        }

        @Override
        boolean act(Goblin g, int time) {
            if (!(g.level().getBlockEntity(at) instanceof ChurnBlockEntity ch)) return true;
            if (ch.butter() > 0) {
                ItemStack b = ch.takeButter();
                ItemStack left = g.stow(b);
                if (!left.isEmpty()) Block.popResource(g.level(), at.above(), left);
                return true;
            }
            if (time % 10 == 0) {
                ch.churn();
                g.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            return !ch.milk() && ch.butter() == 0;
        }
    }

    static final class NetJob extends Job {
        NetJob(BlockPos at) {
            super(4, at, 400);
        }

        @Override
        boolean act(Goblin g, int time) {
            if (g.level().getBlockEntity(at) instanceof FishingNetBlockEntity net) for (ItemStack s : net.takeAll()) {
                ItemStack left = g.stow(s);
                if (!left.isEmpty()) Block.popResource(g.level(), g.blockPosition(), left);
            }
            return true;
        }

        @Override
        boolean near(Goblin g) {
            return g.position().distanceToSqr(Vec3.atCenterOf(at)) < 9;
        }
    }

    static final class IncubatorJob extends Job {
        IncubatorJob(BlockPos at) {
            super(3, at, 300);
        }

        @Override
        boolean act(Goblin g, int time) {
            if (g.level().getBlockEntity(at) instanceof IncubatorBlockEntity inc) for (int i = 0; i < g.pack.getContainerSize(); i++) {
                ItemStack s = g.pack.getItem(i);
                if (IncubatorBlockEntity.hatchOf(s) == null) continue;
                int n = inc.offerEggs(s);
                if (n > 0) {
                    s.shrink(n);
                    g.pack.setChanged();
                    break;
                }
            }
            return true;
        }
    }

    static final class SleepJob extends Job {
        SleepJob(BlockPos bed) {
            super(0, bed, 600);
        }

        @Override
        boolean near(Goblin g) {
            return g.position().distanceToSqr(Vec3.atCenterOf(at)) < 3;
        }

        @Override
        boolean act(Goblin g, int time) {
            BlockState s = g.level().getBlockState(at);
            if (s.getBlock() instanceof BedBlock && !s.getValue(BedBlock.OCCUPIED)) g.startSleeping(at);
            return true;
        }
    }

    static final class PlayJob extends Job {
        final Animal animal;

        PlayJob(Animal a) {
            super(1, a.blockPosition(), 240);
            this.animal = a;
        }

        @Override
        void walk(Goblin g) {
            g.getNavigation().moveTo(animal, 1.15);
        }

        @Override
        void look(Goblin g) {
            g.getLookControl().setLookAt(animal);
        }

        @Override
        boolean near(Goblin g) {
            return animal.isAlive() && g.distanceToSqr(animal) < 4;
        }

        @Override
        boolean act(Goblin g, int time) {
            if (!animal.isAlive()) return true;
            animal.getLookControl().setLookAt(g);
            if (time % 25 == 0 && g.onGround()) g.getJumpControl().jump();
            if (time % 30 == 0 && g.level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.HEART, animal.getX(), animal.getEyeY() + 0.4, animal.getZ(), 1, 0.2, 0.1, 0.2, 0);
            if (time % 40 == 0) {
                // a little chase around the animal
                double a = g.getRandom().nextDouble() * Math.PI * 2;
                g.getNavigation().moveTo(animal.getX() + Math.cos(a) * 2, animal.getY(), animal.getZ() + Math.sin(a) * 2, 1.3);
            }
            return time > 200;
        }
    }
}
