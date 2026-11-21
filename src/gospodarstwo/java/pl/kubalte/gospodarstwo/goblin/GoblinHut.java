package pl.kubalte.gospodarstwo.goblin;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A tame goblin at the base with the right things in its pack builds itself a hut, then sleeps in it. */
public final class GoblinHut {
    private GoblinHut() {}

    public static final int STICKS = 12, THATCH = 8, WOOL = 3;

    static int sticks(ItemStack s) {
        if (s.is(Items.STICK)) return 1;
        if (s.is(ItemTags.PLANKS)) return 2;
        if (s.is(ItemTags.LOGS)) return 8;
        return 0;
    }

    static int thatch(ItemStack s) {
        if (s.is(ItemTags.LEAVES) || s.is(ItemTags.SAPLINGS) || s.is(Items.WHEAT) || s.is(Items.SHORT_GRASS) || s.is(Items.FERN)) return 1;
        if (s.is(Items.HAY_BLOCK)) return 9;
        return 0;
    }

    static int wool(ItemStack s) {
        if (s.is(ItemTags.WOOL) || s.is(ItemTags.WOOL_CARPETS)) return 1;
        return 0;
    }

    interface Measure {
        int of(ItemStack s);
    }

    static int count(Goblin g, Measure m) {
        int n = 0;
        for (int i = 0; i < g.pack.getContainerSize(); i++) n += m.of(g.pack.getItem(i)) * g.pack.getItem(i).getCount();
        return n;
    }

    static void take(Goblin g, Measure m, int need) {
        for (int i = 0; i < g.pack.getContainerSize() && need > 0; i++) {
            ItemStack s = g.pack.getItem(i);
            int per = m.of(s);
            if (per == 0) continue;
            while (!s.isEmpty() && need > 0) {
                s.shrink(1);
                need -= per;
            }
        }
        g.pack.setChanged();
    }

    public static boolean hasMaterials(Goblin g) {
        return count(g, GoblinHut::sticks) >= STICKS && count(g, GoblinHut::thatch) >= THATCH && count(g, GoblinHut::wool) >= WOOL;
    }

    public static boolean isHut(Level level, BlockPos origin) {
        BlockState s = level.getBlockState(origin);
        return s.getBlock() instanceof GoblinHutBlock && s.getValue(GoblinHutBlock.PART) == 0 && s.getValue(GoblinHutBlock.STAGE) == 3;
    }

    /** Where a goblin lies down inside: the middle of the floor, a little towards the back. */
    public static Vec3 bedSpot(Level level, BlockPos origin) {
        BlockState s = level.getBlockState(origin);
        Direction door = s.getValue(GoblinHutBlock.FACING);
        BlockPos back = GoblinHutBlock.partPos(origin, door, 2);
        BlockPos backRight = GoblinHutBlock.partPos(origin, door, 3);
        Vec3 a = Vec3.atBottomCenterOf(back), b = Vec3.atBottomCenterOf(backRight);
        return a.add(b).scale(0.5);
    }

    /** A free 2x2x2 place on firm ground near home, door towards home. */
    static BlockPos findSpot(Goblin g, Direction[] door) {
        Level level = g.level();
        BlockPos h = g.home() != null ? g.home() : g.blockPosition();
        for (int r = 3; r <= 10; r++) for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8;
            BlockPos p = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    h.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r)));
            if (level.getBlockState(p.below()).is(Blocks.FARMLAND) || level.getBlockState(p.below()).is(BlockTags.LEAVES)) continue;
            int dx = h.getX() - p.getX(), dz = h.getZ() - p.getZ();
            Direction d = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
            if (GoblinHutBlock.fits(level, p, d)) {
                door[0] = d;
                return p.immutable();
            }
        }
        return null;
    }

    static final class Build extends Goal {
        private final Goblin g;
        private BlockPos spot;
        private Direction door;
        private int time, stage;
        private boolean started;

        Build(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        void forTest(BlockPos spot, Direction door) {
            this.spot = spot;
            this.door = door;
        }

        @Override
        public boolean canUse() {
            if (!g.isTame() || g.mode() != Goblin.MODE_BASE || g.tickCount % 100 != 0 || g.depositing || g.getTarget() != null) return false;
            if (g.hut != null && g.level().getBlockState(g.hut).getBlock() instanceof GoblinHutBlock) return false;
            if (!hasMaterials(g)) return false;
            Direction[] d = new Direction[1];
            spot = findSpot(g, d);
            door = d[0];
            return spot != null;
        }

        @Override
        public void start() {
            time = 0;
            stage = -1;
            started = false;
            g.setWork(10);
        }

        @Override
        public boolean canContinueToUse() {
            return time < 1200 && stage < 3 && g.getTarget() == null && g.mode() == Goblin.MODE_BASE;
        }

        @Override
        public void tick() {
            time++;
            Vec3 front = Vec3.atBottomCenterOf(spot.relative(door, 1));
            if (!started) {
                if (g.position().distanceToSqr(front) > 2.5) {
                    if (g.getNavigation().isDone() || time % 30 == 0) g.getNavigation().moveTo(front.x, front.y, front.z, 1.0);
                    if (time > 400) g.teleportTo(front.x, front.y, front.z);
                    return;
                }
                if (!GoblinHutBlock.fits(g.level(), spot, door) || !hasMaterials(g)) {
                    time = 9999;
                    return;
                }
                take(g, GoblinHut::sticks, STICKS);
                take(g, GoblinHut::thatch, THATCH);
                take(g, GoblinHut::wool, WOOL);
                started = true;
                time = 0;
            }
            g.getNavigation().stop();
            g.getLookControl().setLookAt(Vec3.atCenterOf(spot.above()));
            if (g.action() != Goblin.A_BUILD) g.act(Goblin.A_BUILD, 0);
            if (time % 12 == 0) {
                g.level().playSound(null, spot, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8F, 1.1F + g.getRandom().nextFloat() * 0.2F);
                if (g.level() instanceof ServerLevel sl) sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                        Blocks.HAY_BLOCK.defaultBlockState()), spot.getX() + 1, spot.getY() + 1, spot.getZ() + 1, 4, 0.5, 0.5, 0.5, 0.05);
            }
            int want = Math.min(3, time / 70);
            if (want > stage) {
                stage = want;
                if (stage == 0) GoblinHutBlock.place(g.level(), spot, door, 0);
                else GoblinHutBlock.setStage(g.level(), spot, stage);
                g.level().playSound(null, spot, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1F, 1F);
            }
        }

        @Override
        public void stop() {
            g.setWork(0);
            if (g.action() == Goblin.A_BUILD) g.act(Goblin.A_CHEER, 30);
            if (stage >= 3) {
                g.hut = spot;
                g.addNeeds(0, 0, 25, 0);
                if (g.getOwner() instanceof Player p) p.sendOverlayMessage(Component.translatable("goblin.gospodarstwo.hut_built", g.getDisplayName()));
            }
        }
    }

    /** Used by items: whether the hut item is a block item of the hut. */
    static boolean isHutItem(ItemStack s) {
        return s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof GoblinHutBlock;
    }
}
