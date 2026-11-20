package pl.kubalte.gospodarstwo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** All hides on the rack are tanned together: raw hide -> leather, thick hide -> thick leather. */
public class TanningRackBlockEntity extends StationBlockEntity {
    public TanningRackBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.TANNING_RACK_BE.get(), pos, state);
    }

    public static ItemStack tanned(ItemStack in) {
        if (in.is(Gospodarstwo.RAW_HIDE.get())) return new ItemStack(Items.LEATHER);
        if (in.is(Gospodarstwo.THICK_HIDE.get())) return new ItemStack(Gospodarstwo.THICK_LEATHER.get());
        return ItemStack.EMPTY;
    }

    @Override
    public Job job(ItemStack in) {
        ItemStack out = tanned(in);
        if (out.isEmpty() || in.isEmpty()) return null;
        int n = Math.min(in.getCount(), 16);
        return new Job(n, out.copyWithCount(n), GConfig.TAN_TICKS.get());
    }

    @Override
    protected int syncEvery() {
        return 200;
    }

    /** 0..1 how far the hides are tanned (client: from the last sync). */
    public float tanFraction() {
        return Math.min(1F, progress / (float) Math.max(1, GConfig.TAN_TICKS.get()));
    }

    @Override
    protected String emptyKey() {
        return "rack.gospodarstwo.empty";
    }
}
