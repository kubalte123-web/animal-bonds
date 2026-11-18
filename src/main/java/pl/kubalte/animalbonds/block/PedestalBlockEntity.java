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
    private PenAccess access = new PenAccess();
    /** Bonded creatures that lived in the pen at the last scan (not saved: rebuilt on the first scan). */
    private final java.util.Set<java.util.UUID> residents = new java.util.HashSet<>();

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.PEDESTAL_BE.get(), pos, state);
    }

    public PenAccess access() {
        return access;
    }

    public void setOwner(net.minecraft.world.entity.player.Player player) {
        access.owner = java.util.Optional.of(player.getUUID());
        access.ownerName = player.getName().getString();
        setChanged();
    }

    public void accessChanged() {
        setChanged();
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        out.store("access", PenAccess.CODEC, access);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        in.read("access", PenAccess.CODEC).ifPresent(a -> access = a);
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
        pen.storage = pl.kubalte.animalbonds.work.PenStorage.find(level, worldPosition);
        java.util.Set<java.util.UUID> nearby = returnRunaways(level, pen);
        List<Mob> mobs = new java.util.ArrayList<>(pen.mobs(level));
        // workers and ball players busy just outside the fence still belong to the pen
        for (Mob m : level.getEntitiesOfClass(Mob.class, pen.box().inflate(16, 4, 16), m -> residents.contains(m.getUUID())
                && !mobs.contains(m) && (pl.kubalte.animalbonds.work.PenWork.isBusy(m) || pl.kubalte.animalbonds.play.BallLogic.isBusy(m)))) {
            mobs.add(m);
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new pl.kubalte.animalbonds.api.PenTickEvent(level, worldPosition, pen, mobs));
        residents.retainAll(nearby); // creatures taken far away (or gone) stop being residents
        for (Mob mob : mobs) {
            BondData d = BondLogic.getOrAutoBond(mob);
            if (d != null) {
                if (d.owner.isEmpty() && access.owner.isPresent()) d.owner = access.owner;
                BondLogic.penTick(level, worldPosition, mob, d, pen, mobs);
                residents.add(mob.getUUID());
            }
        }
        pl.kubalte.animalbonds.work.PenWork.refillTroughs(level, pen);
        pl.kubalte.animalbonds.work.PenWork.tick(level, pen, mobs);
        return pen;
    }

    /**
     * Goats, spiders and others can sometimes get over a fence. Bonded residents found just outside the pen
     * (not ridden, not on a lead) are led back in. Creatures taken further away by the player stay free.
     */
    private java.util.Set<java.util.UUID> returnRunaways(ServerLevel level, PenScanner.Pen pen) {
        java.util.Set<java.util.UUID> nearby = new java.util.HashSet<>();
        if (residents.isEmpty()) return nearby;
        net.minecraft.world.phys.AABB near = pen.box().inflate(8, 2, 8);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, near, m -> residents.contains(m.getUUID()))) {
            nearby.add(mob.getUUID());
            if (pen.isInside(mob)) continue;
            if (pl.kubalte.animalbonds.work.PenWork.isBusy(mob) || pl.kubalte.animalbonds.play.BallLogic.isBusy(mob)) continue;
            if (mob.isPassenger() || mob.isVehicle() || mob.isLeashed()) continue;
            BondData d = BondLogic.get(mob);
            if (d == null || d.isFainted()) continue;
            net.minecraft.core.BlockPos dest = pen.randomCell(level.getRandom());
            mob.getNavigation().stop();
            mob.teleportTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
        }
        return nearby;
    }

    public void openBook(ServerPlayer player) {
        ServerLevel level = (ServerLevel) this.level;
        if (!access.isTrusted(player)) {
            player.sendOverlayMessage(Component.translatable("message.animalbonds.not_your_chronicle", access.ownerName));
            return;
        }
        PenScanner.Pen pen = PenScanner.scan(level, worldPosition);
        if (pen != null) pen.storage = pl.kubalte.animalbonds.work.PenStorage.find(level, worldPosition);
        if (pen == null) {
            player.sendOverlayMessage(Component.translatable("message.animalbonds.no_pen"));
            return;
        }
        List<Mob> mobs = pen.mobs(level);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new pl.kubalte.animalbonds.api.PenTickEvent(level, worldPosition, pen, mobs));
        for (Mob mob : mobs) {
            BondData d = BondLogic.getOrAutoBond(mob);
            if (d != null) {
                BondLogic.evaluateNeeds(mob, d, pen, mobs);
                BondLogic.sync(mob, d);
            }
        }
        List<BookEntry> entries = mobs.stream().map(m -> BookEntry.of(m, pen, level.getGameTime())).toList();
        PacketDistributor.sendToPlayer(player, new OpenBookPayload(worldPosition, pen.area(), entries,
                access.ownerName, List.copyOf(access.members.values()), access.isOwner(player)));
    }
}
