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

/** Rama garbarska: stretch raw hides on it and after about half a day they are leather. */
public class TanningRackBlock extends StationBlock {
    public static final MapCodec<TanningRackBlock> CODEC = simpleCodec(TanningRackBlock::new);

    public TanningRackBlock(Properties props) {
        super(props, Block.box(0, 0, 6, 16, 16, 10));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TanningRackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, Gospodarstwo.TANNING_RACK_BE.get(), StationBlockEntity::tick);
    }
}
