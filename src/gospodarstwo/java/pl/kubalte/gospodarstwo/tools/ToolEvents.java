package pl.kubalte.gospodarstwo.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** Sierp: cutting grass with it cuts the grass around too and finds more seeds. */
public final class ToolEvents {
    private ToolEvents() {}

    static boolean grass(BlockState s) {
        String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return id.equals("short_grass") || id.equals("tall_grass") || id.equals("fern") || id.equals("large_fern") || id.equals("short_dry_grass")
                || id.equals("tall_dry_grass") || id.equals("bush");
    }

    public static void onBreak(BreakBlockEvent e) {
        Player p = e.getPlayer();
        if (p == null || !(e.getLevel() instanceof Level level) || level.isClientSide()) return;
        ItemStack tool = p.getMainHandItem();
        if (!tool.is(Gospodarstwo.SICKLE.get()) || !grass(e.getState())) return;
        cut(level, e.getPos(), p);
        tool.hurtAndBreak(1, p, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    /** Cuts the grass around pos (3x3, two high) and maybe drops an extra seed. Returns how many were cut. */
    public static int cut(Level level, BlockPos pos, Player p) {
        int n = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 1; dy++) {
            BlockPos q = pos.offset(dx, dy, dz);
            if (q.equals(pos) || !grass(level.getBlockState(q))) continue;
            level.destroyBlock(q, true, p);
            n++;
        }
        if (level.getRandom().nextFloat() < 0.35F) {
            Item[] seeds = {Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Gospodarstwo.FLAX_SEEDS.get(), Gospodarstwo.CABBAGE_SEEDS.get()};
            Block.popResource(level, pos, new ItemStack(seeds[level.getRandom().nextInt(seeds.length)]));
        }
        return n;
    }
}
