package pl.kubalte.gospodarstwo.goblin;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** What a goblin needs for itself: food, sleep, fun (dancing by a campfire in the evening), company, and to stay away from what it fears. */
public final class GoblinNeeds {
    private GoblinNeeds() {}

    public static long time(Level level) {
        return Math.floorMod(level.getOverworldClockTime(), 24000L);
    }

    /** Evening: from sunset until about nine o'clock - dancing time. */
    public static boolean evening(Level level) {
        long t = time(level);
        return t >= 11800 && t < 15500;
    }

    /** Night: bedtime until dawn. */
    public static boolean bedtime(Level level) {
        long t = time(level);
        return t >= 15500 && t < 23400;
    }

    // ------------------------------------------------------------------ food
    /** Eats the best thing in its pack (its favourites first, never what it hates unless starving). */
    public static boolean eatSomething(Goblin g) {
        int best = -1;
        float score = -1;
        for (int i = 0; i < g.pack.getContainerSize(); i++) {
            ItemStack s = g.pack.getItem(i);
            if (s.isEmpty() || !Goblin.isOffer(s)) continue;
            if (g.traits().hates(s) && g.hunger > 10) continue;
            float v = g.traits().likes(s) ? 2F : Goblin.liking(s);
            if (v > score) {
                score = v;
                best = i;
            }
        }
        if (best < 0) {
            // nothing to eat: it lets you know it is hungry
            if (g.getOwner() instanceof Player p && g.tickCount % 400 < 20 && g.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, g.getX(), g.getEyeY() + 0.4, g.getZ(), 1, 0, 0, 0, 0);
                p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("goblin.gospodarstwo.hungry", g.getDisplayName()));
            }
            return false;
        }
        ItemStack food = g.pack.removeItem(best, 1);
        var fp = food.get(net.minecraft.core.component.DataComponents.FOOD);
        float fill = fp == null ? 10 : fp.nutrition() * 6;
        g.addNeeds(fill, 0, g.traits().likes(food) ? 20 : g.traits().hates(food) ? -20 : 3, 0);
        g.act(Goblin.A_EAT, 30);
        if (g.level() instanceof ServerLevel sl)
            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food.getItem()), g.getX(), g.getEyeY(), g.getZ(), 8, 0.15, 0.1, 0.15, 0.05);
        g.playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 1.4F);
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(food.getItem()).getPath();
        if (path.endsWith("_stew") || path.endsWith("_soup") || path.equals("bigos")) g.stow(new ItemStack(net.minecraft.world.item.Items.BOWL));
        return true;
    }

    // ------------------------------------------------------------------ fears
    public static boolean scaryEntity(Goblin g, Entity e) {
        Traits t = g.traits();
        var type = e.getType();
        if (type == net.minecraft.world.entity.EntityTypes.CREEPER) return t.fears(Traits.Fear.CREEPERS);
        if (type == net.minecraft.world.entity.EntityTypes.SPIDER || type == net.minecraft.world.entity.EntityTypes.CAVE_SPIDER)
            return t.fears(Traits.Fear.SPIDERS);
        if (net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(EntityTypeTags.UNDEAD) && e instanceof net.minecraft.world.entity.monster.Enemy) return t.fears(Traits.Fear.UNDEAD);
        if (type == net.minecraft.world.entity.EntityTypes.WOLF && e instanceof net.minecraft.world.entity.TamableAnimal w && !w.isTame())
            return t.fears(Traits.Fear.WOLVES);
        if (type == net.minecraft.world.entity.EntityTypes.BEE) return t.fears(Traits.Fear.BEES);
        return false;
    }

    /** Runs from things it fears - unless it holds a weapon and feels brave. */
    static final class FleeFears extends Goal {
        private final Goblin g;
        private Entity scary;
        private BlockPos fire;

        FleeFears(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        boolean brave() {
            return g.tool() == Work.Tool.SWORD && g.mood() > 60 && g.isTame();
        }

        @Override
        public boolean canUse() {
            if (g.tickCount % 5 != 0 || g.resting() || g.isSleeping() || brave()) return false;
            List<Entity> near = g.level().getEntities(g, g.getBoundingBox().inflate(7), e -> e.isAlive() && scaryEntity(g, e));
            scary = near.isEmpty() ? null : near.get(0);
            fire = null;
            if (scary == null && g.traits().fears(Traits.Fear.FIRE)) {
                for (BlockPos p : BlockPos.betweenClosed(g.blockPosition().offset(-3, -1, -3), g.blockPosition().offset(3, 1, 3))) {
                    BlockState s = g.level().getBlockState(p);
                    if (s.is(BlockTags.FIRE) || s.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) {
                        fire = p.immutable();
                        break;
                    }
                }
            }
            return scary != null || fire != null;
        }

        @Override
        public void start() {
            Vec3 from = scary != null ? scary.position() : Vec3.atCenterOf(fire);
            Vec3 away = DefaultRandomPos.getPosAway(g, 12, 6, from);
            if (away != null) g.getNavigation().moveTo(away.x, away.y, away.z, 1.5);
            g.act(Goblin.A_SCARED, 20);
            g.addNeeds(0, 0, -3, 0);
        }

        @Override
        public boolean canContinueToUse() {
            return !g.getNavigation().isDone();
        }
    }

    // ------------------------------------------------------------------ sleep
    static final class Sleep extends Goal {
        private final Goblin g;
        private BlockPos bed;
        private Vec3 spot;
        private boolean lying;

        Sleep(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        boolean sleepy() {
            if (!g.isTame()) return bedtime(g.level()) && g.getTarget() == null;
            if (g.getTarget() != null || g.depositing) return false;
            if (g.mode() == Goblin.MODE_FOLLOW) return g.getOwner() instanceof Player p && p.isSleeping() || g.energy < 8;
            return bedtime(g.level()) || g.energy < 10;
        }

        @Override
        public boolean canUse() {
            if (g.tickCount % 20 != 0 || g.resting() || g.isSleeping() || !sleepy()) return false;
            bed = null;
            spot = null;
            if (g.isTame() && g.mode() != Goblin.MODE_FOLLOW && g.hut != null && GoblinHut.isHut(g.level(), g.hut)) {
                spot = GoblinHut.bedSpot(g.level(), g.hut);
            } else if (g.isTame()) {
                BlockPos c = g.mode() == Goblin.MODE_BASE && g.home() != null ? g.home() : g.blockPosition();
                bed = freeBed(g.level(), c, 10);
            }
            if (spot == null && bed == null) spot = g.position();
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (g.getTarget() != null) return false;
            if (lying) return g.isSleeping() ? sleepy() || g.energy < 95 && bedtime(g.level()) : g.work() == 6 && (sleepy() || g.energy < 60);
            return true;
        }

        @Override
        public void start() {
            lying = false;
            if (bed != null) g.getNavigation().moveTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 1.0);
            else g.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.0);
        }

        @Override
        public void tick() {
            if (lying) {
                if (g.tickCount % 60 == 0 && g.level() instanceof ServerLevel sl)
                    sl.sendParticles(ParticleTypes.WHITE_ASH, g.getX(), g.getEyeY() + 0.2, g.getZ(), 1, 0.1, 0.1, 0.1, 0);
                return;
            }
            Vec3 target = bed != null ? Vec3.atCenterOf(bed) : spot;
            if (g.position().distanceToSqr(target) < (bed != null ? 2.5 : 1.5) || g.getNavigation().isDone()) {
                g.getNavigation().stop();
                if (bed != null) {
                    BlockState s = g.level().getBlockState(bed);
                    if (s.getBlock() instanceof BedBlock && !s.getValue(BedBlock.OCCUPIED)) g.startSleeping(bed);
                    else g.setWork(6);
                } else {
                    g.setWork(6);
                }
                lying = true;
            }
        }

        @Override
        public void stop() {
            if (g.isSleeping() && !g.resting()) g.stopSleeping();
            if (g.work() == 6) g.setWork(0);
            lying = false;
        }
    }

    static BlockPos freeBed(Level level, BlockPos c, int r) {
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -3, -r), c.offset(r, 3, r))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof BedBlock && s.getValue(BedBlock.PART) == BedPart.HEAD && !s.getValue(BedBlock.OCCUPIED)) return p.immutable();
        }
        return null;
    }

    // ------------------------------------------------------------------ dancing by the fire
    static BlockPos litCampfire(Level level, BlockPos c, int r) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -3, -r), c.offset(r, 3, r))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT) && p.distSqr(c) < bd) {
                bd = p.distSqr(c);
                best = p.immutable();
            }
        }
        return best;
    }

    static final class Dance extends Goal {
        private final Goblin g;
        private BlockPos fire;
        private int time, length;
        private Vec3 spot;

        Dance(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        boolean wouldDance() {
            int t = g.tickCount;
            g.tickCount = 0;
            boolean r = canUse();
            g.tickCount = t;
            return r;
        }

        @Override
        public boolean canUse() {
            if (g.tickCount % 40 != 0 || !evening(g.level()) || g.resting() || g.getTarget() != null || g.depositing || g.isSleeping()) return false;
            if (g.isTame() && g.mode() == Goblin.MODE_SIT) return false;
            boolean likes = g.traits().pastime == Traits.Pastime.DANCING;
            if (g.fun > (likes ? 95 : 80)) return false;
            if (g.isTame() && g.mode() == Goblin.MODE_FOLLOW && !(g.getOwner() instanceof Player p && p.distanceTo(g) < 12
                    && p.getDeltaMovement().horizontalDistanceSqr() < 0.01)) return false;
            fire = litCampfire(g.level(), g.blockPosition(), 16);
            return fire != null;
        }

        @Override
        public void start() {
            time = 0;
            length = (g.traits().pastime == Traits.Pastime.DANCING ? 600 : 300) + g.getRandom().nextInt(200);
            double a = (g.getId() * 1.7) % (Math.PI * 2);
            spot = Vec3.atBottomCenterOf(fire).add(Math.cos(a) * 2.4, 0, Math.sin(a) * 2.4);
            g.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.1);
        }

        @Override
        public boolean canContinueToUse() {
            BlockState s = g.level().getBlockState(fire);
            return time < length && s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT) && g.getTarget() == null
                    && (evening(g.level()) || time < 100);
        }

        @Override
        public void tick() {
            time++;
            if (g.position().distanceToSqr(spot) > 1.2) {
                if (g.getNavigation().isDone()) g.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.1);
                return;
            }
            g.getNavigation().stop();
            g.getLookControl().setLookAt(Vec3.atCenterOf(fire).add(0, 0.3, 0));
            // a new dance every few seconds; goblins next to each other pick the same one
            int dance = Goblin.A_DANCE1 + (int) ((g.level().getGameTime() / 120 + fire.hashCode()) % 3 + 3) % 3;
            if (g.action() != dance) g.act(dance, 0);
            boolean likes = g.traits().pastime == Traits.Pastime.DANCING;
            if (time % 20 == 0) {
                int others = g.level().getEntitiesOfClass(Goblin.class, new AABB(fire).inflate(5), o -> o != g && o.action() >= Goblin.A_DANCE1
                        && o.action() <= Goblin.A_DANCE3).size();
                g.addNeeds(0, -0.2F, likes ? 3 : 1.5F, others > 0 ? 1.5F : 0);
                if (g.level() instanceof ServerLevel sl && g.getRandom().nextInt(3) == 0)
                    sl.sendParticles(ParticleTypes.NOTE, g.getX(), g.getEyeY() + 0.5, g.getZ(), 1, 0.2, 0.1, 0.2, g.getRandom().nextFloat());
            }
            if (time % 40 == 0 && g.onGround() && g.getRandom().nextInt(3) == 0) g.getJumpControl().jump();
        }

        @Override
        public void stop() {
            if (g.action() >= Goblin.A_DANCE1 && g.action() <= Goblin.A_DANCE3) g.act(Goblin.A_NONE, 1);
        }
    }

    // ------------------------------------------------------------------ goblins together: petting, teasing, tag
    static final class Social extends Goal {
        private final Goblin g;
        private Goblin other;
        private int kind, time;

        Social(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        static boolean idle(Goblin x) {
            return x.isAlive() && x.getTarget() == null && x.work() == 0 && !x.resting() && !x.isSleeping() && x.socialLock <= 0 && !x.depositing
                    && (x.action() == Goblin.A_NONE || x.action() == Goblin.A_PLAY) && x.considering() <= 0;
        }

        @Override
        public boolean canUse() {
            if (g.tickCount % 40 != 0 || !idle(g) || g.isTame() && g.mode() == Goblin.MODE_SIT) return false;
            boolean wants = g.social < 60 || g.traits().pastime == Traits.Pastime.COMPANY ? g.getRandom().nextInt(3) == 0 : g.getRandom().nextInt(25) == 0;
            if (!wants) return false;
            List<Goblin> near = g.level().getEntitiesOfClass(Goblin.class, g.getBoundingBox().inflate(10), o -> o != g && idle(o)
                    && o.isTame() == g.isTame());
            if (near.isEmpty()) return false;
            other = near.get(g.getRandom().nextInt(near.size()));
            kind = g.getRandom().nextInt(3);
            return true;
        }

        @Override
        public void start() {
            time = 0;
            other.socialLock = 200;
            other.getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            return other.isAlive() && time < 200 && g.getTarget() == null && other.getTarget() == null;
        }

        @Override
        public void tick() {
            time++;
            other.socialLock = Math.max(other.socialLock, 5);
            g.getLookControl().setLookAt(other);
            other.getLookControl().setLookAt(g);
            double d = g.distanceToSqr(other);
            switch (kind) {
                case 0 -> { // pet on the head
                    if (d > 2.2) g.getNavigation().moveTo(other, 1.0);
                    else {
                        g.getNavigation().stop();
                        if (g.action() != Goblin.A_PET) {
                            g.act(Goblin.A_PET, 60);
                            other.act(Goblin.A_PETTED, 60);
                        }
                        if (time % 20 == 0 && g.level() instanceof ServerLevel sl)
                            sl.sendParticles(ParticleTypes.HEART, other.getX(), other.getEyeY() + 0.4, other.getZ(), 1, 0.1, 0.1, 0.1, 0);
                        if (g.action() == Goblin.A_NONE) time = 999;
                    }
                }
                case 1 -> { // poke - the other one gets annoyed and chases it
                    if (time < 100) {
                        if (d > 2.2) g.getNavigation().moveTo(other, 1.1);
                        else if (g.action() != Goblin.A_POKE) {
                            g.act(Goblin.A_POKE, 15);
                            other.act(Goblin.A_ANNOYED, 20);
                            other.playSound(SoundEvents.FOX_SNIFF, 0.8F, 1.7F);
                            time = 100;
                        }
                    } else {
                        runAround(g, other, 1.35);
                        other.getNavigation().moveTo(g, 1.25);
                    }
                }
                default -> { // tag: run around each other
                    runAround(g, other, 1.3);
                    if (time % 30 == 0) other.getNavigation().moveTo(g, 1.3);
                    if (time % 25 == 0 && g.onGround()) g.getJumpControl().jump();
                }
            }
        }

        static void runAround(Goblin runner, Goblin chaser, double speed) {
            if (runner.getNavigation().isDone()) {
                Vec3 away = DefaultRandomPos.getPosAway(runner, 6, 3, chaser.position());
                if (away != null) runner.getNavigation().moveTo(away.x, away.y, away.z, speed);
            }
        }

        @Override
        public void stop() {
            g.addNeeds(0, 0, 8, 15);
            other.addNeeds(0, 0, kind == 1 ? 3 : 8, 15);
            other.socialLock = 60;
            g.socialLock = 60;
        }
    }

    /** Noticed a mob that would scare this goblin within the radius. */
    static boolean anyScary(Goblin g, double r) {
        return !g.level().getEntities(g, g.getBoundingBox().inflate(r), e -> e instanceof LivingEntity && scaryEntity(g, e)).isEmpty();
    }

    /** Silences an unused import warning for Mob in some compilers. */
    static boolean isMob(Entity e) {
        return e instanceof Mob;
    }
}
