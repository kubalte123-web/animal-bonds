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
    /** Bonded creatures that lived in the pen at the last scan (not saved: rebuilt on the first scan). */
    private final java.util.Set<java.util.UUID> residents = new java.util.HashSet<>();

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
        returnRunaways(level, pen);
        List<Mob> mobs = pen.mobs(level);
        residents.clear();
        for (Mob mob : mobs) {
            BondData d = BondLogic.getOrAutoBond(mob);
            if (d != null) {
                BondLogic.penTick(level, worldPosition, mob, d, pen, mobs);
                residents.add(mob.getUUID());
            }
        }
        return pen;
    }

    /**
     * Goats, spiders and others can sometimes get over a fence. Bonded residents found just outside the pen
     * (not ridden, not on a lead) are led back in. Creatures taken further away by the player stay free.
     */
    private void returnRunaways(ServerLevel level, PenScanner.Pen pen) {
        if (residents.isEmpty()) return;
        net.minecraft.world.phys.AABB near = pen.box().inflate(8, 2, 8);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, near, m -> residents.contains(m.getUUID()))) {
            if (pen.contains(mob.getBlockX(), mob.getBlockZ()) && mob.getBlockY() <= pen.y + 1) continue;
            if (mob.isPassenger() || mob.isVehicle() || mob.isLeashed()) continue;
            BondData d = BondLogic.get(mob);
            if (d == null || d.isFainted()) continue;
            net.minecraft.core.BlockPos dest = pen.randomCell(level.getRandom());
            mob.getNavigation().stop();
            mob.teleportTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
        }
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
