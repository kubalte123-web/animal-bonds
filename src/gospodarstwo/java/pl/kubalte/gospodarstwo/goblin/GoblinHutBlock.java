package pl.kubalte.gospodarstwo.goblin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/**
 * Szałas goblina: a 2x2x2 lean-to of sticks, thatch and wool. Eight blocks of this one type make it up (PART says
 * which one: bit 0 = right, bit 1 = back, bit 2 = top, seen from the door); the model of the whole hut is drawn by the
 * front-left bottom part. STAGE shows how far the goblin has built it.
 */
public class GoblinHutBlock extends Block {
    public static final MapCodec<GoblinHutBlock> CODEC = simpleCodec(GoblinHutBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 7);
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);

    public GoblinHutBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(PART, 0).setValue(STAGE, 3));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, PART, STAGE);
    }

    /** "right" of someone standing in the door looking in... we use the door's direction: door faces FACING. */
    static Direction right(Direction door) {
        return door.getCounterClockWise();
    }

    /** World position of a part, from the hut's origin (front-left bottom). */
    public static BlockPos partPos(BlockPos origin, Direction door, int part) {
        BlockPos p = origin;
        if ((part & 1) != 0) p = p.relative(right(door));
        if ((part & 2) != 0) p = p.relative(door.getOpposite());
        if ((part & 4) != 0) p = p.above();
        return p;
    }

    public static BlockPos origin(BlockPos pos, BlockState s) {
        Direction door = s.getValue(FACING);
        int part = s.getValue(PART);
        BlockPos p = pos;
        if ((part & 1) != 0) p = p.relative(right(door).getOpposite());
        if ((part & 2) != 0) p = p.relative(door);
        if ((part & 4) != 0) p = p.below();
        return p;
    }

    /** Can a hut with this door stand with its origin here? */
    public static boolean fits(LevelReader level, BlockPos origin, Direction door) {
        for (int i = 0; i < 8; i++) {
            BlockPos p = partPos(origin, door, i);
            if (!level.getBlockState(p).canBeReplaced()) return false;
            if (i < 4 && !level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) return false;
        }
        return true;
    }

    public static void place(Level level, BlockPos origin, Direction door, int stage) {
        BlockState base = Gospodarstwo.GOBLIN_HUT.get().defaultBlockState().setValue(FACING, door).setValue(STAGE, stage);
        for (int i = 0; i < 8; i++) level.setBlock(partPos(origin, door, i), base.setValue(PART, i), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        for (int i = 0; i < 8; i++) level.updateNeighborsAt(partPos(origin, door, i), base.getBlock());
    }

    public static void setStage(Level level, BlockPos origin, int stage) {
        BlockState s = level.getBlockState(origin);
        if (!(s.getBlock() instanceof GoblinHutBlock)) return;
        Direction door = s.getValue(FACING);
        for (int i = 0; i < 8; i++) {
            BlockPos p = partPos(origin, door, i);
            BlockState q = level.getBlockState(p);
            if (q.getBlock() instanceof GoblinHutBlock) level.setBlock(p, q.setValue(STAGE, stage), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction door = ctx.getHorizontalDirection().getOpposite();
        BlockPos origin = ctx.getClickedPos();
        return fits(ctx.getLevel(), origin, door) ? defaultBlockState().setValue(FACING, door) : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide()) place(level, pos, state.getValue(FACING), 3);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos origin = origin(pos, state);
        Direction door = state.getValue(FACING);
        if (!level.isClientSide()) {
            for (int i = 0; i < 8; i++) {
                BlockPos p = partPos(origin, door, i);
                if (!p.equals(pos) && level.getBlockState(p).getBlock() instanceof GoblinHutBlock) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR
                        .defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            }
            if (!player.getAbilities().instabuild && state.getValue(STAGE) == 3)
                Block.popResource(level, pos, new ItemStack(Gospodarstwo.GOBLIN_HUT_ITEM.get()));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // walls: thin on the sides and the back, the roof slopes in steps; the door side stays open
    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape(s);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape(s);
    }

    static VoxelShape shape(BlockState s) {
        if (s.getValue(STAGE) < 2) return Shapes.empty();
        Direction door = s.getValue(FACING);
        int part = s.getValue(PART);
        boolean right = (part & 1) != 0, back = (part & 2) != 0, top = (part & 4) != 0;
        VoxelShape v = Shapes.empty();
        Direction side = right ? right(door) : right(door).getOpposite();
        v = Shapes.or(v, wall(side));
        if (back) v = Shapes.or(v, wall(door.getOpposite()));
        if (top) v = Shapes.or(v, Block.box(0, 12, 0, 16, 16, 16));
        return v;
    }

    static VoxelShape wall(Direction d) {
        return switch (d) {
            case NORTH -> Block.box(0, 0, 0, 16, 16, 2);
            case SOUTH -> Block.box(0, 0, 14, 16, 16, 16);
            case WEST -> Block.box(0, 0, 0, 2, 16, 16);
            default -> Block.box(14, 0, 0, 16, 16, 16);
        };
    }
}
