package pl.kubalte.animalbonds.work;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Pen upgrade "magazyn": any chest or barrel standing next to the pedestal (up to 2 blocks away). */
public final class PenStorage {
    private PenStorage() {}

    public static BlockPos find(ServerLevel level, BlockPos pedestal) {
        for (int dy = 0; dy >= -1; dy--) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue;
                    BlockPos p = pedestal.offset(dx, dy, dz);
                    if (level.getBlockEntity(p) instanceof Container) return p;
                }
            }
        }
        return null;
    }

    public static Container get(ServerLevel level, BlockPos pos) {
        return pos != null && level.getBlockEntity(pos) instanceof Container c ? c : null;
    }

    /** Puts the stack in; returns what did not fit. */
    public static ItemStack insert(Container c, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = 0; i < c.getContainerSize() && !rest.isEmpty(); i++) {
            ItemStack in = c.getItem(i);
            if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, rest) && in.getCount() < in.getMaxStackSize()) {
                int move = Math.min(rest.getCount(), in.getMaxStackSize() - in.getCount());
                in.grow(move);
                rest.shrink(move);
            }
        }
        for (int i = 0; i < c.getContainerSize() && !rest.isEmpty(); i++) {
            if (c.getItem(i).isEmpty() && c.canPlaceItem(i, rest)) {
                c.setItem(i, rest.copy());
                rest = ItemStack.EMPTY;
            }
        }
        c.setChanged();
        return rest;
    }

    /** Takes one item matching the test; returns it (count 1) or EMPTY. */
    public static ItemStack takeOne(Container c, Predicate<ItemStack> test) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack in = c.getItem(i);
            if (!in.isEmpty() && test.test(in)) {
                ItemStack one = in.copyWithCount(1);
                in.shrink(1);
                c.setChanged();
                return one;
            }
        }
        return ItemStack.EMPTY;
    }

    public static int count(Container c, Predicate<ItemStack> test) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) if (test.test(c.getItem(i))) n += c.getItem(i).getCount();
        return n;
    }
}
