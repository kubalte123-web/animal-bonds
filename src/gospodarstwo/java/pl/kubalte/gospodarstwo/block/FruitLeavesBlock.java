package pl.kubalte.gospodarstwo.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Leaves of a fruit tree: 0 plain, 1 blossom, 2 unripe fruit, 3 ripe fruit (right-click to pick, then it starts over).
 */
public class FruitLeavesBlock extends TintedParticleLeavesBlock {
    public static final IntegerProperty FRUIT = IntegerProperty.create("fruit", 0, 3);
    private final Supplier<? extends Item> fruit;
    private final MapCodec<FruitLeavesBlock> codec;

    public FruitLeavesBlock(Properties props, Supplier<? extends Item> fruit) {
        super(0.01F, props);
        this.fruit = fruit;
        this.codec = simpleCodec(p -> new FruitLeavesBlock(p, fruit));
        registerDefaultState(defaultBlockState().setValue(FRUIT, 0));
    }

    public Item fruit() {
        return fruit.get();
    }

    @Override
    public MapCodec<? extends TintedParticleLeavesBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FRUIT);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        BlockState now = level.getBlockState(pos);
        if (now.getBlock() != this) return; // decayed
        int f = now.getValue(FRUIT);
        if (f < 3 && random.nextInt(5) == 0) level.setBlock(pos, now.setValue(FRUIT, f + 1), 2);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(FRUIT) < 3) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            Block.popResource(level, pos, new ItemStack(fruit.get(), 1 + level.getRandom().nextInt(2)));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.2F);
            level.setBlock(pos, state.setValue(FRUIT, 0), 2);
        }
        return InteractionResult.SUCCESS;
    }
}
