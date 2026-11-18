package pl.kubalte.animalbonds.bond;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import pl.kubalte.animalbonds.BondConfig;
import pl.kubalte.animalbonds.block.NestBlock;
import pl.kubalte.animalbonds.block.PedestalBlock;
import pl.kubalte.animalbonds.block.TroughBlock;

/**
 * Finds the fenced pen connected to a pedestal. The flood fill follows the terrain: every column of the pen
 * has its own floor height, so pens may contain hills and pits (up to {@code maxStep} blocks between neighbours).
 * The pen is bounded by anything a creature can't stand in or on: fences, walls, gates, solid walls.
 * The pedestal may stand inside the pen or directly next to its fence.
 */
public final class PenScanner {
    private PenScanner() {}

    public static final class Pen {
        /** column (x,z) -> floor y (the block a creature stands in). */
        public final Long2IntOpenHashMap cells;
        public final int minX, minZ, maxX, maxZ, minY, maxY;
        /** Height of the pedestal-side start cell (kept for reference). */
        public final int y;
        public final List<BlockPos> troughs = new ArrayList<>();
        /** Troughs, beds etc. at the pen's edge/inside: not walkable, but a creature on them is in the pen. */
        public final LongOpenHashSet features = new LongOpenHashSet();
        public int beds;
        public boolean water, hay, flowers, snow, warmth;
        public int grassCells, sandCells;
        // ---- bonuses given by other mods through PenTickEvent ----
        /** Needs satisfied for every creature in the pen. */
        public final java.util.EnumSet<Need> forcedNeeds = java.util.EnumSet.noneOf(Need.class);
        public float xpMultiplier = 1F;
        /** >1 = gifts come more often. */
        public float giftSpeed = 1F;
        /** Extra items added to every gift. */
        public int giftBonus = 0;
        /** <1 = trough food lasts longer. */
        public float troughUse = 1F;

        Pen(int y, Long2IntOpenHashMap cells, int minX, int minZ, int maxX, int maxZ, int minY, int maxY) {
            this.y = y;
            this.cells = cells;
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
            this.minY = minY;
            this.maxY = maxY;
        }

        public int area() {
            return cells.size();
        }

        public boolean contains(int x, int z) {
            long k = pack(x, z);
            return cells.containsKey(k) || features.contains(k);
        }

        /** Is the entity inside the pen (right column and roughly at floor height)? */
        public boolean isInside(Entity e) {
            long k = pack(e.getBlockX(), e.getBlockZ());
            if (cells.containsKey(k)) return Math.abs(e.getBlockY() - cells.get(k)) <= 2;
            return features.contains(k) && e.getBlockY() >= minY - 1 && e.getBlockY() <= maxY + 2;
        }

        public AABB box() {
            return new AABB(minX, minY - 2, minZ, maxX + 1, maxY + 5, maxZ + 1);
        }

        public List<Mob> mobs(Level level) {
            return level.getEntitiesOfClass(Mob.class, box(), m -> m.isAlive() && isInside(m));
        }

        public BlockPos randomCell(RandomSource random) {
            long[] keys = cells.keySet().toLongArray();
            long c = keys[random.nextInt(keys.length)];
            return new BlockPos(unpackX(c), cells.get(c), unpackZ(c));
        }
    }

    static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    static int unpackX(long v) {
        return (int) (v >> 32);
    }

    static int unpackZ(long v) {
        return (int) v;
    }

    private static boolean passable(Level level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        return st.getCollisionShape(level, pos).isEmpty();
    }

    /** Blocks you can't count as "ground": standing on top of the pen boundary means being outside of it. */
    private static boolean isBoundaryBlock(BlockState st) {
        return st.is(BlockTags.FENCES) || st.is(BlockTags.WALLS) || st.is(BlockTags.FENCE_GATES)
                || st.getBlock() instanceof TroughBlock || st.getBlock() instanceof PedestalBlock;
    }

    /** Can a creature stand at this position? */
    private static boolean standable(Level level, BlockPos.MutableBlockPos p) {
        if (!level.isLoaded(p) || !passable(level, p)) return false;
        p.move(Direction.DOWN);
        BlockState below = level.getBlockState(p);
        boolean ok = !isBoundaryBlock(below)
                && (!below.getCollisionShape(level, p).isEmpty() || !below.getFluidState().isEmpty());
        p.move(Direction.UP);
        return ok;
    }

    /** Floor height of column (x,z) closest to refY within maxStep, or Integer.MIN_VALUE. */
    private static int findFloor(Level level, int x, int z, int refY, int maxStep) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int d = 0; d <= maxStep; d++) {
            p.set(x, refY + d, z);
            if (standable(level, p)) return refY + d;
            if (d > 0) {
                p.set(x, refY - d, z);
                if (standable(level, p)) return refY - d;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** @return the pen, or null when the pedestal is not next to / inside a closed pen. */
    public static Pen scan(Level level, BlockPos pedestal) {
        int max = BondConfig.MAX_PEN_AREA.get();
        int step = BondConfig.MAX_PEN_STEP.get();
        List<BlockPos> starts = new ArrayList<>();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = pedestal.relative(d);
            int f = findFloor(level, n.getX(), n.getZ(), pedestal.getY(), 1);
            if (f != Integer.MIN_VALUE) starts.add(new BlockPos(n.getX(), f, n.getZ()));
            // Behind the fence: the cell right behind it, or (if a trough/bed sits there) its neighbours.
            BlockPos beyond = n.relative(d);
            for (BlockPos c : new BlockPos[] {beyond, beyond.relative(d.getClockWise()), beyond.relative(d.getCounterClockWise()), beyond.relative(d)}) {
                int fc = findFloor(level, c.getX(), c.getZ(), pedestal.getY(), 2);
                if (fc != Integer.MIN_VALUE) starts.add(new BlockPos(c.getX(), fc, c.getZ()));
            }
        }
        for (BlockPos start : starts) {
            Pen pen = fill(level, start, max, step);
            if (pen != null && pen.area() >= 4) {
                survey(level, pen);
                return pen;
            }
        }
        return null;
    }

    private static Pen fill(Level level, BlockPos start, int max, int step) {
        Long2IntOpenHashMap seen = new Long2IntOpenHashMap();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        long s = pack(start.getX(), start.getZ());
        seen.put(s, start.getY());
        queue.enqueue(s);
        int minX = start.getX(), maxX = minX, minZ = start.getZ(), maxZ = minZ, minY = start.getY(), maxY = minY;
        while (!queue.isEmpty()) {
            long c = queue.dequeueLong();
            int x = unpackX(c), z = unpackZ(c), y = seen.get(c);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int nx = x + d.getStepX(), nz = z + d.getStepZ();
                long n = pack(nx, nz);
                if (seen.containsKey(n)) continue;
                if (!level.isLoaded(new BlockPos(nx, y, nz))) return null;
                int f = findFloor(level, nx, nz, y, step);
                if (f == Integer.MIN_VALUE) {
                    // No floor: either a fence/wall/rock face (boundary) or a drop deeper than allowed (open edge).
                    BlockPos np = new BlockPos(nx, y, nz);
                    if (passable(level, np) && !isBoundaryBlock(level.getBlockState(np.below()))) return null;
                    continue;
                }
                seen.put(n, f);
                if (seen.size() > max) return null; // not enclosed (or too big)
                queue.enqueue(n);
            }
        }
        return new Pen(start.getY(), seen, minX, minZ, maxX, maxZ, minY, maxY);
    }

    /** Records what the pen contains (troughs, beds, water, ground types...). */
    private static void survey(Level level, Pen pen) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        LongOpenHashSet checkedEdge = new LongOpenHashSet();
        for (Long2IntMap.Entry e : pen.cells.long2IntEntrySet()) {
            long c = e.getLongKey();
            int x = unpackX(c), z = unpackZ(c), fy = e.getIntValue();
            m.set(x, fy, z);
            BlockState at = level.getBlockState(m);
            BlockState ground = level.getBlockState(m.below());
            if (at.getBlock() instanceof NestBlock) pen.beds++;
            if (at.getFluidState().is(FluidTags.WATER) || ground.getFluidState().is(FluidTags.WATER)) pen.water = true;
            if (at.getFluidState().is(FluidTags.LAVA) || ground.getFluidState().is(FluidTags.LAVA)
                    || ground.is(Blocks.MAGMA_BLOCK) || at.is(BlockTags.CAMPFIRES)) pen.warmth = true;
            if (ground.is(Blocks.HAY_BLOCK)) pen.hay = true;
            if (at.is(BlockTags.FLOWERS)) pen.flowers = true;
            if (at.is(Blocks.SNOW) || ground.is(Blocks.SNOW_BLOCK) || ground.is(Blocks.POWDER_SNOW) || ground.is(BlockTags.ICE)) pen.snow = true;
            if (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.MOSS_BLOCK) || ground.is(Blocks.PODZOL)) pen.grassCells++;
            if (ground.is(BlockTags.SAND)) pen.sandCells++;

            // Neighbouring non-cell columns: troughs, hay bales, campfires... standing at this cell's height.
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int nx = x + d.getStepX(), nz = z + d.getStepZ();
                long n = pack(nx, nz);
                if (pen.cells.containsKey(n)) continue;
                for (int dy = 0; dy <= 1; dy++) {
                    m.set(nx, fy + dy, nz);
                    long key = n ^ ((long) (fy + dy) * 0x9E3779B97F4A7C15L);
                    if (!checkedEdge.add(key)) continue;
                    BlockState st = level.getBlockState(m);
                    if (st.getBlock() instanceof TroughBlock) {
                        if (st.getValue(TroughBlock.FILL) > 0) pen.troughs.add(m.immutable());
                        pen.features.add(n);
                    } else if (st.getBlock() instanceof NestBlock) {
                        pen.beds++;
                        pen.features.add(n);
                    } else if (st.is(Blocks.WATER_CAULDRON)) {
                        pen.water = true;
                    } else if (st.is(Blocks.HAY_BLOCK)) {
                        pen.hay = true;
                    } else if (st.is(BlockTags.CAMPFIRES) || st.is(Blocks.MAGMA_BLOCK)) {
                        pen.warmth = true;
                    }
                }
            }
        }
    }
}
