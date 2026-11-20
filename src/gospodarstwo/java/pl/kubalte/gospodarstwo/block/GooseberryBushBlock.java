package pl.kubalte.gospodarstwo.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** Krzew agrestu: a prickly bush like sweet berries, but with gooseberries. */
public class GooseberryBushBlock extends SweetBerryBushBlock {
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final MapCodec<SweetBerryBushBlock> CODEC_G = (MapCodec) simpleCodec(GooseberryBushBlock::new);

    public GooseberryBushBlock(Properties props) {
        super(props);
    }

    @Override
    public MapCodec<SweetBerryBushBlock> codec() {
        return CODEC_G;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(Gospodarstwo.GOOSEBERRIES.get());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int age = state.getValue(AGE);
        if (age > 1) {
            int n = 1 + level.getRandom().nextInt(2);
            Block.popResource(level, pos, new ItemStack(Gospodarstwo.GOOSEBERRIES.get(), n + (age == 3 ? 1 : 0)));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.3F);
            BlockState picked = state.setValue(AGE, 1);
            level.setBlock(pos, picked, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
