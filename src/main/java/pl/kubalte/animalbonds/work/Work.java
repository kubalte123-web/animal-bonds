package pl.kubalte.animalbonds.work;

import net.minecraft.network.chat.Component;

/** Jobs creatures do in their pen when work mode is on. */
public enum Work {
    /** Harvests ripe crops in and around the pen and replants them. */
    FARMER,
    /** Attacks wild monsters coming near the pen. */
    GUARD,
    /** Picks up items lying around the pen and puts them into the storage. */
    COLLECTOR,
    /** Smelts ores, raw food, sand... in the pen storage. */
    SMELTER;

    public Component displayName() {
        return Component.translatable("work.animalbonds." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
