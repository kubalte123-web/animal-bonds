package pl.kubalte.gospodarstwo.craft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/**
 * A pillow and a featherbed can be put on any bed (sneak + right-click). Sleeping through the night in such a bed gives
 * "Wypoczęty" (extra hearts): half a day with one of them, a whole day and stronger with both.
 */
public final class Bedding {
    private Bedding() {}

    public static final int PILLOW = 1, FEATHERBED = 2;

    public static BlockPos head(BlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof BedBlock)) return null;
        return state.getValue(BedBlock.PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(BedBlock.FACING));
    }

    private static int get(Level level, BlockPos head) {
        return level.getData(Gospodarstwo.BED_COMFORT.get()).getOrDefault(Long.toString(head.asLong()), 0);
    }

    private static void set(Level level, BlockPos head, int c) {
        java.util.Map<String, Integer> m = new java.util.HashMap<>(level.getData(Gospodarstwo.BED_COMFORT.get()));
        if (c == 0) m.remove(Long.toString(head.asLong()));
        else m.put(Long.toString(head.asLong()), c);
        level.setData(Gospodarstwo.BED_COMFORT.get(), m);
    }

    public static int comfort(Level level, BlockPos pos) {
        BlockPos h = head(level.getBlockState(pos), pos);
        return h == null ? 0 : get(level, h);
    }

    /** Puts the pillow / featherbed on the bed; true if it was used. */
    public static boolean add(Level level, BlockPos pos, Player player, ItemStack stack) {
        int bit = stack.is(Gospodarstwo.PILLOW.get()) ? PILLOW : stack.is(Gospodarstwo.FEATHERBED.get()) ? FEATHERBED : 0;
        BlockPos h = head(level.getBlockState(pos), pos);
        if (bit == 0 || h == null) return false;
        int c = get(level, h);
        if ((c & bit) != 0) {
            player.sendOverlayMessage(Component.translatable(bit == PILLOW ? "bed.gospodarstwo.has_pillow" : "bed.gospodarstwo.has_featherbed"));
            return true;
        }
        set(level, h, c | bit);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, h, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1F, 0.9F);
        player.sendOverlayMessage(Component.translatable((c | bit) == 3 ? "bed.gospodarstwo.cosy" : "bed.gospodarstwo.added"));
        return true;
    }

    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (!e.getEntity().isShiftKeyDown()) return;
        ItemStack s = e.getItemStack();
        if (!s.is(Gospodarstwo.PILLOW.get()) && !s.is(Gospodarstwo.FEATHERBED.get())) return;
        if (!(e.getLevel().getBlockState(e.getPos()).getBlock() instanceof BedBlock)) return;
        if (!e.getLevel().isClientSide()) add(e.getLevel(), e.getPos(), e.getEntity(), s);
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    public static void onBreak(BreakBlockEvent e) {
        if (!(e.getLevel() instanceof Level level)) return;
        BlockPos h = head(e.getState(), e.getPos());
        if (h == null) return;
        int c = get(level, h);
        if (c == 0) return;
        if ((c & PILLOW) != 0) Block.popResource(level, e.getPos(), new ItemStack(Gospodarstwo.PILLOW.get()));
        if ((c & FEATHERBED) != 0) Block.popResource(level, e.getPos(), new ItemStack(Gospodarstwo.FEATHERBED.get()));
        set(level, h, 0);
    }

    /** Gives the rested effect to a player who slept in a bed with the given comfort. */
    public static void rest(Player p, int c) {
        if (c == 0) return;
        int both = c == 3 ? 1 : 0;
        int ticks = (both == 1 ? 24000 : 12000) * GConfig.RESTED_PERCENT.get() / 100;
        p.addEffect(new MobEffectInstance(Gospodarstwo.RESTED, ticks, both, false, true, true));
        p.heal(p.getMaxHealth());
    }

    public static void onSleepFinished(SleepFinishedTimeEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        for (Player p : level.players()) {
            if (!p.isSleeping()) continue;
            p.getSleepingPos().ifPresent(pos -> rest(p, comfort(level, pos)));
        }
    }
}
