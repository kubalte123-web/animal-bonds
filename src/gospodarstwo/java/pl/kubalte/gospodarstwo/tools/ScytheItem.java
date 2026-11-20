package pl.kubalte.gospodarstwo.tools;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Kosa: right-click reaps every ripe crop in a 3x3 and sows it again with one of its own seeds. */
public class ScytheItem extends Item {
    public ScytheItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockPos c = ctx.getClickedPos();
        if (!(ctx.getLevel().getBlockState(c).getBlock() instanceof CropBlock)) c = c.above();
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        int n = reap(level, c, ctx.getPlayer(), ctx.getItemInHand());
        if (n == 0) return InteractionResult.PASS;
        level.playSound(null, c, SoundEvents.CROP_BREAK, SoundSource.PLAYERS, 1F, 0.9F);
        if (ctx.getPlayer() != null)
            ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), ctx.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return InteractionResult.SUCCESS;
    }

    /** Harvests and replants; returns how many crops were reaped. */
    public static int reap(ServerLevel level, BlockPos c, Player player, ItemStack tool) {
        int n = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos p = c.offset(dx, 0, dz);
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(s)) continue;
            List<ItemStack> drops = Block.getDrops(s, level, p, null, player, tool);
            boolean replant = false;
            for (ItemStack d : drops) if (!replant && d.is(s.getBlock().asItem())) {
                d.shrink(1);
                replant = true;
            }
            level.setBlock(p, replant ? crop.getStateForAge(0) : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            for (ItemStack d : drops) if (!d.isEmpty()) Block.popResource(level, p, d);
            n++;
        }
        return n;
    }
}
