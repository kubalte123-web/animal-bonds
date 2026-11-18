package pl.kubalte.animalbonds.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import pl.kubalte.animalbonds.bond.Need;
import pl.kubalte.animalbonds.bond.SpeciesProfiles;

/** Small API for other mods (e.g. new creatures): their profiles and extra info lines in the Chronicle. */
public final class AnimalBondsApi {
    private AnimalBondsApi() {}

    private static final List<Function<Mob, Component>> BOOK_LINES = new CopyOnWriteArrayList<>();
    private static final java.util.Set<String> NO_VARIETY = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Creatures whose own textures carry meaning (e.g. element colours) keep them: no unique recolouring. */
    public static void excludeFromTextureVariety(String entityTypeId) {
        NO_VARIETY.add(entityTypeId);
    }

    public static boolean isExcludedFromTextureVariety(String entityTypeId) {
        return NO_VARIETY.contains(entityTypeId);
    }

    /**
     * Registers needs, favourite food (item ids or "#tag") and gifts for a creature type.
     * @param gifts entries "itemId,min,max,minLevel"
     */
    public static void registerProfile(String entityTypeId, List<Need> needs, List<String> favourites, List<String> gifts, int space) {
        List<SpeciesProfiles.Gift> g = gifts.stream().map(s -> {
            String[] p = s.split(",");
            return new SpeciesProfiles.Gift(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        }).toList();
        SpeciesProfiles.register(entityTypeId, new SpeciesProfiles.Profile(needs, favourites, g, space));
    }

    /** Adds a line under the creature's name in the Chronicle (return null for "nothing to show"). */
    public static void registerBookLine(Function<Mob, Component> line) {
        BOOK_LINES.add(line);
    }

    /** Extra lines as one JSON text component (translated on the client), "" when none. */
    public static String bookLine(Mob mob) {
        net.minecraft.network.chat.MutableComponent all = null;
        for (Function<Mob, Component> f : BOOK_LINES) {
            Component c = f.apply(mob);
            if (c != null) {
                if (all == null) all = Component.empty().append(c);
                else all.append("  ").append(c);
            }
        }
        if (all == null) return "";
        return net.minecraft.network.chat.ComponentSerialization.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, all)
                .result().map(Object::toString).orElse("");
    }

    public static Component parseBookLine(String json) {
        try {
            return net.minecraft.network.chat.ComponentSerialization.CODEC
                    .parse(com.mojang.serialization.JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(json))
                    .result().orElse(Component.empty());
        } catch (Exception e) {
            return Component.literal(json);
        }
    }
}
