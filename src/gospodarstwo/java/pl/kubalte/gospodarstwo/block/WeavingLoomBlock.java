package pl.kubalte.gospodarstwo.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/**
 * Krosno tkackie: put in yarn (or string) and it weaves cloth by itself - you can watch the heddles go up and down and
 * the shuttle fly. Dyed yarn gives dyed cloth.
 */
public class WeavingLoomBlock extends StationBlock {
    public static final MapCodec<WeavingLoomBlock> CODEC = simpleCodec(WeavingLoomBlock::new);

    public WeavingLoomBlock(Properties props) {
        super(props, Block.box(0, 0, 1, 16, 15, 15));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LoomBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, Gospodarstwo.LOOM_BE.get(), StationBlockEntity::tick);
    }
}
