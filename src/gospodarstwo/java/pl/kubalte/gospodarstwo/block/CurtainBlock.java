package pl.kubalte.gospodarstwo.block;

import com.mojang.serialization.MapCodec;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Zasłona: hangs on the far side of the block (against the window). Right-click opens / closes the whole curtain (all
 * connected pieces facing the same way); a dye colours it. Drawn by {@code CurtainRenderer}.
 */
public class CurtainBlock extends BaseEntityBlock {
    public static final MapCodec<CurtainBlock> CODEC = simpleCodec(CurtainBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape N = Block.box(0, 0, 13, 16, 16, 16), S = Block.box(0, 0, 0, 16, 16, 3), W = Block.box(13, 0, 0, 16, 16, 16),
            E = Block.box(0, 0, 0, 3, 16, 16);

    public CurtainBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // the curtain faces the player, the cloth hangs on the far side
        Direction f = ctx.getHorizontalDirection().getOpposite();
        BlockState below = ctx.getLevel().getBlockState(ctx.getClickedPos().above());
        boolean open = below.getBlock() instanceof CurtainBlock && below.getValue(FACING) == f && below.getValue(OPEN);
        return defaultBlockState().setValue(FACING, f).setValue(OPEN, open);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> S;
            case WEST -> W;
            case EAST -> E;
            default -> N;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CurtainBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (ColoredBlocks.dye(level, pos, player, stack)) return InteractionResult.SUCCESS;
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) toggle(level, pos);
        return InteractionResult.SUCCESS;
    }

    /** Opens or closes every connected piece of the curtain (same facing, touching up/down or sideways). */
    public static int toggle(Level level, BlockPos start) {
        BlockState s0 = level.getBlockState(start);
        if (!(s0.getBlock() instanceof CurtainBlock)) return 0;
        Direction f = s0.getValue(FACING);
        boolean open = !s0.getValue(OPEN);
        Direction side = f.getClockWise();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(start);
        while (!todo.isEmpty() && seen.size() < 32) {
            BlockPos p = todo.poll();
            if (!seen.add(p)) continue;
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof CurtainBlock) || s.getValue(FACING) != f) {
                seen.remove(p);
                continue;
            }
            level.setBlock(p, s.setValue(OPEN, open), Block.UPDATE_ALL);
            for (BlockPos n : new BlockPos[] {p.above(), p.below(), p.relative(side), p.relative(side.getOpposite())}) if (!seen.contains(n)) todo.add(n);
        }
        level.playSound(null, start, open ? SoundEvents.WOOL_HIT : SoundEvents.WOOL_STEP, SoundSource.BLOCKS, 1F, open ? 1.2F : 0.9F);
        return seen.size();
    }
}
