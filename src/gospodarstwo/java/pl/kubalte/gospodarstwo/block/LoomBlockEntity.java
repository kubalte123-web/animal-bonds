package pl.kubalte.gospodarstwo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.craft.Dyes;

/** The weaving loom: yarn in, cloth out; dyed yarn gives cloth of the same colour. */
public class LoomBlockEntity extends StationBlockEntity {
    public LoomBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.LOOM_BE.get(), pos, state);
    }

    @Override
    public Job job(ItemStack in) {
        LoomRecipes.Recipe r = LoomRecipes.find(in);
        if (r == null) return null;
        ItemStack out = new ItemStack(r.output().get());
        int color = Dyes.colorOf(in);
        if (color >= 0) Dyes.setColor(out, color);
        return new Job(r.count(), out, GConfig.LOOM_TICKS.get());
    }

    @Override
    protected String emptyKey() {
        return "loom.gospodarstwo.empty";
    }

    public boolean weaving() {
        return working;
    }
}
