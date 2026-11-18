package pl.kubalte.animalbonds.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.kubalte.animalbonds.BondConfig;
import pl.kubalte.animalbonds.ModRegistry;
import pl.kubalte.animalbonds.bond.BondData;
import pl.kubalte.animalbonds.bond.BondLogic;
import pl.kubalte.animalbonds.bond.PenScanner;
import pl.kubalte.animalbonds.network.BookEntry;
import pl.kubalte.animalbonds.network.OpenBookPayload;

public class PedestalBlockEntity extends BlockEntity {
    private int timer;

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.PEDESTAL_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PedestalBlockEntity be) {
        if (++be.timer < BondConfig.SCAN_INTERVAL.get()) return;
        be.timer = 0;
        if (level instanceof ServerLevel sl) be.updatePen(sl);
    }

    /** Runs the periodic care simulation for every bonded creature in the pen. */
    public PenScanner.Pen updatePen(ServerLevel level) {
        PenScanner.Pen pen = PenScanner.scan(level, worldPosition);
        if (pen == null) return null;
        List<Mob> mobs = pen.mobs(level);
        for (Mob mob : mobs) {
            BondData d = BondLogic.getOrAutoBond(mob);
            if (d != null) BondLogic.penTick(level, worldPosition, mob, d, pen, mobs);
        }
        return pen;
    }

    public void openBook(ServerPlayer player) {
        ServerLevel level = (ServerLevel) this.level;
        PenScanner.Pen pen = PenScanner.scan(level, worldPosition);
        if (pen == null) {
            player.sendOverlayMessage(Component.translatable("message.animalbonds.no_pen"));
            return;
        }
        List<Mob> mobs = pen.mobs(level);
        for (Mob mob : mobs) {
            BondData d = BondLogic.getOrAutoBond(mob);
            if (d != null) {
                BondLogic.evaluateNeeds(mob, d, pen, mobs);
                BondLogic.sync(mob, d);
            }
        }
        List<BookEntry> entries = mobs.stream().map(m -> BookEntry.of(m, pen, level.getGameTime())).toList();
        PacketDistributor.sendToPlayer(player, new OpenBookPayload(worldPosition, pen.area(), entries));
    }
}
