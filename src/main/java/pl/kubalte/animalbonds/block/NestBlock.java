package pl.kubalte.animalbonds.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** "Legowisko": a soft animal bed. One bed is enough for two creatures. */
public class NestBlock extends Block {
    public static final MapCodec<NestBlock> CODEC = simpleCodec(NestBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 4, 15);

    public NestBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
