package pl.kubalte.gospodarstwo.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/**
 * Doniczka bonsai: a shallow tray. Plant any sapling, water it (water bucket or bottle), trim it with shears now and
 * then - and the sapling slowly becomes a miniature tree. Without trimming it only grows into a shaggy bush.
 */
public class BonsaiPotBlock extends BaseEntityBlock {
    public static final MapCodec<BonsaiPotBlock> CODEC = simpleCodec(BonsaiPotBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 3, 15, 4, 13);

    public BonsaiPotBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BonsaiPotBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Gospodarstwo.BONSAI_BE.get(), BonsaiPotBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof BonsaiPotBlockEntity be) {
            InteractionResult r = be.use(player, hand, stack);
            if (r != InteractionResult.PASS) return r;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof BonsaiPotBlockEntity be) return be.inspect(player);
        return InteractionResult.PASS;
    }
}
