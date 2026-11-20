package pl.kubalte.gospodarstwo.kitchen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class Heat {
    private Heat() {}

    /** A fire under the block: lit campfire, fire, magma or lava. */
    public static boolean below(Level level, BlockPos pos) {
        if (level == null) return false;
        BlockState b = level.getBlockState(pos.below());
        if (b.getBlock() instanceof CampfireBlock) return b.getValue(CampfireBlock.LIT);
        return b.is(BlockTags.FIRE) || b.is(Blocks.MAGMA_BLOCK) || b.is(Blocks.LAVA);
    }
}
