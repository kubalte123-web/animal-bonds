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

/** Kołowrotek: spins wool, flax fibre, cashmere and other fibres into yarn; the wheel turns while it works. */
public class SpinningWheelBlock extends StationBlock {
    public static final MapCodec<SpinningWheelBlock> CODEC = simpleCodec(SpinningWheelBlock::new);

    public SpinningWheelBlock(Properties props) {
        super(props, Block.box(1, 0, 3, 15, 16, 13));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpinningWheelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, Gospodarstwo.SPINNING_WHEEL_BE.get(), StationBlockEntity::tick);
    }
}
