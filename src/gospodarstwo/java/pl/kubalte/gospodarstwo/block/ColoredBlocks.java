package pl.kubalte.gospodarstwo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import pl.kubalte.gospodarstwo.craft.Dyes;

/** Shared: right-clicking a curtain or rug with a dye (or a natural dye) colours it; a water bucket washes it. */
public final class ColoredBlocks {
    private ColoredBlocks() {}

    public static boolean dye(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof ColoredBlockEntity be)) return false;
        int dye = Dyes.dyeColor(stack);
        boolean wash = stack.is(net.minecraft.world.item.Items.WATER_BUCKET);
        if (dye < 0 && !wash) return false;
        if (level.isClientSide()) return true;
        if (wash) {
            be.setColor(-1);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 1.2F);
            return true;
        }
        be.setColor(be.color() < 0 ? dye : Dyes.mix(be.color(), dye, 1));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1F, 1F);
        return true;
    }
}
