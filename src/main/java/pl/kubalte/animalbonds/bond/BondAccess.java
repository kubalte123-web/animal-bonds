package pl.kubalte.animalbonds.bond;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import pl.kubalte.animalbonds.block.PedestalBlockEntity;

/** Who may ride, feed, name or open the bags of a bonded creature. */
public final class BondAccess {
    private BondAccess() {}

    /** Owner of the bond, or a player trusted at the Chronicle of the creature's home pen. */
    public static boolean canUse(ServerPlayer player, Mob mob, BondData d) {
        if (d == null) return true;
        if (d.owner.isEmpty() || d.owner.get().equals(player.getUUID())) return true;
        if (d.home.isPresent()) {
            ServerLevel lvl = player.level().getServer().getLevel(d.home.get().dimension());
            BlockPos pos = d.home.get().pos();
            if (lvl != null && lvl.isLoaded(pos) && lvl.getBlockEntity(pos) instanceof PedestalBlockEntity be) {
                return be.access().isTrusted(player) && be.access().owner.isPresent();
            }
        }
        return false;
    }
}
