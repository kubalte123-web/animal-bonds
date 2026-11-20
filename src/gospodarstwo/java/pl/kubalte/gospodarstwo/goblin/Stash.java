package pl.kubalte.gospodarstwo.goblin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Putting things into containers: chests and barrels around, the goblin's own pack. */
public final class Stash {
    private Stash() {}

    /** Chests and barrels within r blocks of c (a few layers up and down). */
    public static List<BlockPos> containers(Level level, BlockPos c, int r) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -3, -r), c.offset(r, 3, r))) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) out.add(p.immutable());
        }
        out.sort((a, b) -> Double.compare(a.distSqr(c), b.distSqr(c)));
        return out;
    }

    public static boolean holds(Container c, ItemStack s) {
        for (int i = 0; i < c.getContainerSize(); i++) if (ItemStack.isSameItemSameComponents(c.getItem(i), s)) return true;
        return false;
    }

    /** How many of s still fit into c. */
    public static int room(Container c, ItemStack s) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack in = c.getItem(i);
            if (in.isEmpty()) n += s.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(in, s)) n += in.getMaxStackSize() - in.getCount();
        }
        return n;
    }

    public static boolean hasEmptySlot(Container c) {
        for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).isEmpty()) return true;
        return false;
    }

    /**
     * Where s should go: first a container that already holds the same thing (and has room), else one with an empty slot.
     */
    public static BlockPos target(Level level, List<BlockPos> containers, ItemStack s) {
        for (BlockPos p : containers) if (level.getBlockEntity(p) instanceof Container c && holds(c, s) && room(c, s) > 0) return p;
        for (BlockPos p : containers) if (level.getBlockEntity(p) instanceof Container c && hasEmptySlot(c)) return p;
        return null;
    }

    /** Puts as much of s into c as fits (merging into same stacks first); returns what is left. */
    public static ItemStack insert(Container c, ItemStack s) {
        ItemStack left = s.copy();
        for (int i = 0; i < c.getContainerSize() && !left.isEmpty(); i++) {
            ItemStack in = c.getItem(i);
            if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, left) && in.getCount() < in.getMaxStackSize()) {
                int m = Math.min(left.getCount(), in.getMaxStackSize() - in.getCount());
                in.grow(m);
                left.shrink(m);
            }
        }
        for (int i = 0; i < c.getContainerSize() && !left.isEmpty(); i++) {
            if (c.getItem(i).isEmpty() && c.canPlaceItem(i, left)) {
                c.setItem(i, left.copy());
                left = ItemStack.EMPTY;
            }
        }
        c.setChanged();
        return left;
    }
}
