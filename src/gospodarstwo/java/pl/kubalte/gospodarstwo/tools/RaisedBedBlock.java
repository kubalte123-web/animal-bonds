package pl.kubalte.gospodarstwo.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Grządka podniesiona: a wooden box of good soil. Crops planted on it are always watered, can't be trampled, and grow
 * about a quarter faster.
 */
public class RaisedBedBlock extends Block {
    public RaisedBedBlock(Properties props) {
        super(props);
    }

    static boolean plant(BlockState plant) {
        return plant.getBlock() instanceof CropBlock || plant.getBlock() instanceof StemBlock || plant.is(BlockTags.SAPLINGS)
                || plant.is(BlockTags.FLOWERS);
    }

    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant) {
        return plant(plant) ? TriState.TRUE : TriState.DEFAULT;
    }

    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos up = pos.above();
        BlockState plant = level.getBlockState(up);
        if (plant(plant) && random.nextFloat() < 0.25F) plant.randomTick(level, up, random);
    }
}
