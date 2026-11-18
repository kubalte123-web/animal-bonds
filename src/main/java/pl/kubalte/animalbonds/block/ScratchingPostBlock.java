package pl.kubalte.animalbonds.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** "Drapak": a pen upgrade. Creatures rub against it and feel petted even when nobody visits them. */
public class ScratchingPostBlock extends Block {
    public static final MapCodec<ScratchingPostBlock> CODEC = simpleCodec(ScratchingPostBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 2, 14), Block.box(5, 2, 5, 11, 16, 11));
    /** Taller than it looks, so nobody uses it as a step over the fence. */
    private static final VoxelShape COLLISION = Block.box(4, 0, 4, 12, 24, 12);

    public ScratchingPostBlock(Properties props) {
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

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return COLLISION;
    }
}
