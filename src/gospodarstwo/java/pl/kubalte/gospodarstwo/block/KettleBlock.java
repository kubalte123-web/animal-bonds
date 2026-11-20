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

/** Kocioł rzemieślniczy: stand it on a campfire (or fire, magma, lava) to render wax, boil glue and lard. */
public class KettleBlock extends StationBlock {
    public static final MapCodec<KettleBlock> CODEC = simpleCodec(KettleBlock::new);

    public KettleBlock(Properties props) {
        super(props, Block.box(2, 0, 2, 14, 11, 14));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource r) {
        if (!(level.getBlockEntity(pos) instanceof KettleBlockEntity be) || !be.working()) return;
        double x = pos.getX() + 0.3 + r.nextDouble() * 0.4, z = pos.getZ() + 0.3 + r.nextDouble() * 0.4;
        level.addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP, x, pos.getY() + 0.6, z, 0, 0.02, 0);
        if (r.nextInt(3) == 0) level.addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE, x, pos.getY() + 0.7, z, 0, 0.03, 0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KettleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, Gospodarstwo.KETTLE_BE.get(), StationBlockEntity::tick);
    }
}
