package pl.kubalte.gospodarstwo.kitchen;

import com.mojang.serialization.MapCodec;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import pl.kubalte.gospodarstwo.block.StationBlock;
import pl.kubalte.gospodarstwo.block.StationBlockEntity;

/** A kitchen station block (quern, press, crock, smokehouse): the block entity does the work. */
public class KitchenBlock extends StationBlock {
    private final BiFunction<BlockPos, BlockState, ? extends StationBlockEntity> factory;
    private final Supplier<? extends BlockEntityType<? extends StationBlockEntity>> type;
    private final MapCodec<KitchenBlock> codec;

    public KitchenBlock(Properties props, VoxelShape shape, BiFunction<BlockPos, BlockState, ? extends StationBlockEntity> factory,
                        Supplier<? extends BlockEntityType<? extends StationBlockEntity>> type) {
        super(props, shape);
        this.factory = factory;
        this.type = type;
        this.codec = simpleCodec(p -> new KitchenBlock(p, shape, factory, type));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource r) {
        if (!(level.getBlockEntity(pos) instanceof StationBlockEntity be) || !be.working()) return;
        if (be instanceof SmokehouseBlockEntity) {
            if (r.nextInt(2) == 0) level.addParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5 + r.nextGaussian() * 0.15,
                    pos.getY() + 1.2, pos.getZ() + 0.5 + r.nextGaussian() * 0.15, 0, 0.04, 0);
        } else if (be instanceof QuernBlockEntity && r.nextInt(3) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_ASH, pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 0.5,
                    pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0, 0);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        return t == type.get() ? (l, p, s, be) -> StationBlockEntity.tick(l, p, s, (StationBlockEntity) be) : null;
    }
}
