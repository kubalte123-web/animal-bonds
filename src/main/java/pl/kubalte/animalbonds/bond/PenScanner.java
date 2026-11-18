package pl.kubalte.animalbonds.bond;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import pl.kubalte.animalbonds.BondConfig;
import pl.kubalte.animalbonds.block.NestBlock;
import pl.kubalte.animalbonds.block.TroughBlock;

/**
 * Finds the fenced pen connected to a pedestal: a horizontal flood fill at the pedestal's height,
 * bounded by anything with a collision shape (fences, walls, closed gates, solid blocks).
 * The pedestal may stand inside the pen or directly next to its fence.
 */
public final class PenScanner {
    private PenScanner() {}

    public static final class Pen {
        public final int y;
        public final LongOpenHashSet cells;
        public final int minX, minZ, maxX, maxZ;
        public final List<BlockPos> troughs = new ArrayList<>();
        public int beds;
        public boolean water, hay, flowers, snow, warmth;
        public int grassCells, sandCells;

        Pen(int y, LongOpenHashSet cells, int minX, int minZ, int maxX, int maxZ) {
            this.y = y;
            this.cells = cells;
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
        }

        public int area() {
            return cells.size();
        }

        public boolean contains(int x, int z) {
            return cells.contains(pack(x, z));
        }

        public AABB box() {
            return new AABB(minX, y - 1, minZ, maxX + 1, y + 5, maxZ + 1);
        }

        public List<Mob> mobs(Level level) {
            return level.getEntitiesOfClass(Mob.class, box(), m -> m.isAlive() && contains(m.getBlockX(), m.getBlockZ()));
        }

        public BlockPos randomCell(net.minecraft.util.RandomSource random) {
            long[] arr = cells.toLongArray();
            long c = arr[random.nextInt(arr.length)];
            return new BlockPos(unpackX(c), y, unpackZ(c));
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

    /** @return the pen, or null when the pedestal is not next to / inside a closed pen. */
    public static Pen scan(Level level, BlockPos pedestal) {
        int max = BondConfig.MAX_PEN_AREA.get();
        List<BlockPos> starts = new ArrayList<>();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = pedestal.relative(d);
            if (passable(level, n)) {
                starts.add(n);
            } else {
                BlockPos beyond = n.relative(d);
                if (passable(level, beyond)) starts.add(beyond);
            }
        }
        // Prefer the cells behind a fence (typical "pedestal outside the pen" setup) after trying direct neighbours.
        for (BlockPos start : starts) {
            Pen pen = fill(level, start, max);
            if (pen != null && pen.area() >= 4) {
                survey(level, pen);
                return pen;
            }
        }
        return null;
    }

    private static Pen fill(Level level, BlockPos start, int max) {
        int y = start.getY();
        LongOpenHashSet seen = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        long s = pack(start.getX(), start.getZ());
        seen.add(s);
        queue.enqueue(s);
        int minX = start.getX(), maxX = minX, minZ = start.getZ(), maxZ = minZ;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty()) {
            long c = queue.dequeueLong();
            int x = unpackX(c), z = unpackZ(c);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int nx = x + d.getStepX(), nz = z + d.getStepZ();
                long n = pack(nx, nz);
                if (seen.contains(n)) continue;
                m.set(nx, y, nz);
                if (!level.isLoaded(m)) return null;
                if (!passable(level, m)) continue;
                seen.add(n);
                if (seen.size() > max) return null; // not enclosed (or too big)
                queue.enqueue(n);
            }
        }
        return new Pen(y, seen, minX, minZ, maxX, maxZ);
    }

    /** Records what the pen contains (troughs, beds, water, ground types...). */
    private static void survey(Level level, Pen pen) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (long c : pen.cells) {
            int x = unpackX(c), z = unpackZ(c);
            m.set(x, pen.y, z);
            BlockState at = level.getBlockState(m);
            BlockState ground = level.getBlockState(m.below());
            if (at.getBlock() instanceof TroughBlock && at.getValue(TroughBlock.FILL) > 0) pen.troughs.add(m.immutable());
            if (at.getBlock() instanceof NestBlock) pen.beds++;
            if (at.getFluidState().is(FluidTags.WATER) || ground.getFluidState().is(FluidTags.WATER)) pen.water = true;
            if (at.getFluidState().is(FluidTags.LAVA) || ground.getFluidState().is(FluidTags.LAVA)
                    || ground.is(Blocks.MAGMA_BLOCK) || at.is(BlockTags.CAMPFIRES)) pen.warmth = true;
            if (at.is(Blocks.HAY_BLOCK) || ground.is(Blocks.HAY_BLOCK)) pen.hay = true;
            if (at.is(BlockTags.FLOWERS)) pen.flowers = true;
            if (at.is(Blocks.SNOW) || ground.is(Blocks.SNOW_BLOCK) || ground.is(Blocks.POWDER_SNOW) || ground.is(BlockTags.ICE)) pen.snow = true;
            if (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.MOSS_BLOCK) || ground.is(Blocks.PODZOL)) pen.grassCells++;
            if (ground.is(BlockTags.SAND)) pen.sandCells++;
        }
        // Troughs, beds, hay bales and campfires have collision, so the flood fill flows around them:
        // look for them in the whole bounding box plus the fence ring, next to a pen cell.
        for (int x = pen.minX - 1; x <= pen.maxX + 1; x++) {
            for (int z = pen.minZ - 1; z <= pen.maxZ + 1; z++) {
                if (pen.contains(x, z)) continue;
                if (!(pen.contains(x + 1, z) || pen.contains(x - 1, z) || pen.contains(x, z + 1) || pen.contains(x, z - 1))) continue;
                m.set(x, pen.y, z);
                BlockState st = level.getBlockState(m);
                if (st.getBlock() instanceof TroughBlock) {
                    if (st.getValue(TroughBlock.FILL) > 0) pen.troughs.add(m.immutable());
                } else if (st.getBlock() instanceof NestBlock) {
                    pen.beds++;
                } else if (st.is(Blocks.HAY_BLOCK)) {
                    pen.hay = true;
                } else if (st.is(BlockTags.CAMPFIRES) || st.is(Blocks.MAGMA_BLOCK)) {
                    pen.warmth = true;
                }
            }
        }
    }
}
