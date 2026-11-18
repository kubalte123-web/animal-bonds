package pl.kubalte.animalbonds.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Feeding trough. Fill it with feed (wheat, seeds, vegetables, hay, meat...) and creatures in the pen eat from it. */
public class TroughBlock extends Block {
    public static final MapCodec<TroughBlock> CODEC = simpleCodec(TroughBlock::new);
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 4);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 24, 16);

    public TroughBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** How many fill levels this item adds (0 = not feed). */
    public static int feedValue(ItemStack stack) {
        if (stack.is(Items.HAY_BLOCK)) return 4;
        if (stack.is(Items.WHEAT) || stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT)
                || stack.is(Items.APPLE) || stack.is(Items.MELON_SLICE) || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.ROTTEN_FLESH) || stack.is(Items.BONE) || stack.is(Items.BAMBOO)
                || stack.is(Items.KELP) || stack.is(Items.CRIMSON_FUNGUS) || stack.is(Items.WARPED_FUNGUS)
                || stack.is(ItemTags.MEAT) || stack.is(ItemTags.FISHES)) return 1;
        if (stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS) || stack.is(Items.MELON_SEEDS) || stack.is(Items.PUMPKIN_SEEDS)) return 1;
        return 0;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int add = feedValue(stack);
        int fill = state.getValue(FILL);
        if (add <= 0) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (fill >= 4) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, state.setValue(FILL, Math.min(4, fill + add)));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    /** As tall as a fence, so creatures can't use it as a step to jump out of the pen. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return COLLISION;
    }
}
