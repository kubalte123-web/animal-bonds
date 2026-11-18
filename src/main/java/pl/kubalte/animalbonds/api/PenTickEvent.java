package pl.kubalte.animalbonds.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.Event;
import pl.kubalte.animalbonds.bond.PenScanner;

/**
 * Fired on the NeoForge event bus every time a Chronicle (pedestal) runs its pen simulation, before the creatures
 * are evaluated. Other mods can give the pen bonuses through {@link PenScanner.Pen}: satisfy needs for everyone
 * ({@code forcedNeeds}), multiply bond xp ({@code xpMultiplier}), speed up gifts ({@code giftSpeed}),
 * add to gift amounts ({@code giftBonus}) or make trough food last longer ({@code troughUse}).
 */
public class PenTickEvent extends Event {
    private final ServerLevel level;
    private final BlockPos pedestal;
    private final PenScanner.Pen pen;
    private final List<Mob> creatures;

    public PenTickEvent(ServerLevel level, BlockPos pedestal, PenScanner.Pen pen, List<Mob> creatures) {
        this.level = level;
        this.pedestal = pedestal;
        this.pen = pen;
        this.creatures = creatures;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getPedestal() {
        return pedestal;
    }

    public PenScanner.Pen getPen() {
        return pen;
    }

    /** All creatures in the pen (bonded or not). */
    public List<Mob> getCreatures() {
        return creatures;
    }
}
