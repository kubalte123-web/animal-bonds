package pl.kubalte.gospodarstwo.goblin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A goblin's character, rolled when it is born and shaped by its race: favourite and hated foods, a favourite pastime,
 * things that bother it and things it is afraid of. The AI reads these.
 */
public final class Traits {
    /** Pastimes: doing it is fun (fills the fun need faster) and it chooses it more often. */
    public enum Pastime { DANCING, ANIMALS, CHOPPING, DIGGING, FARMING, COOKING, FISHING, FIGHTING, COMPANY }

    /** Things that put it in a bad mood. */
    public enum Bother { RAIN, DARKNESS, WATER, SUN, NOISE, LONELINESS }

    /** Things it runs away from. */
    public enum Fear { CREEPERS, SPIDERS, UNDEAD, WATER, FIRE, DARKNESS, WOLVES, BEES }

    public final List<String> likedFoods = new ArrayList<>(), hatedFoods = new ArrayList<>();
    public Pastime pastime = Pastime.DANCING;
    public final List<Bother> bothers = new ArrayList<>();
    public final List<Fear> fears = new ArrayList<>();

    static final String[] DISHES = {"gospodarstwo:pierogi", "gospodarstwo:bigos", "gospodarstwo:kielbasa", "gospodarstwo:apple_pie",
            "gospodarstwo:pancakes", "gospodarstwo:smoked_ham", "gospodarstwo:bread_with_butter", "gospodarstwo:smoked_fish",
            "gospodarstwo:plum_jam", "gospodarstwo:sauerkraut"};
    static final String[] PLAIN = {"minecraft:apple", "minecraft:carrot", "minecraft:baked_potato", "minecraft:cooked_chicken", "minecraft:cooked_cod",
            "minecraft:bread", "minecraft:sweet_berries", "minecraft:cookie", "minecraft:mushroom_stew", "minecraft:beetroot",
            "gospodarstwo:plum", "gospodarstwo:strawberry", "gospodarstwo:cabbage", "minecraft:cooked_porkchop", "minecraft:rabbit_stew"};

    public static Traits roll(Race race, RandomSource r) {
        Traits t = new Traits();
        t.likedFoods.add(DISHES[r.nextInt(DISHES.length)]);
        String plain;
        do plain = PLAIN[r.nextInt(PLAIN.length)]; while (t.likedFoods.contains(plain));
        t.likedFoods.add(plain);
        String hated;
        do hated = r.nextBoolean() ? PLAIN[r.nextInt(PLAIN.length)] : DISHES[r.nextInt(DISHES.length)]; while (t.likedFoods.contains(hated));
        t.hatedFoods.add(hated);
        // the race tips the scales
        Pastime[] lean = switch (race) {
            case FOREST -> new Pastime[] {Pastime.CHOPPING, Pastime.ANIMALS, Pastime.DANCING};
            case STONE -> new Pastime[] {Pastime.DIGGING, Pastime.FIGHTING, Pastime.COOKING};
            case SWAMP -> new Pastime[] {Pastime.FISHING, Pastime.FARMING, Pastime.ANIMALS};
            case CAVE -> new Pastime[] {Pastime.DIGGING, Pastime.DANCING, Pastime.COMPANY};
        };
        t.pastime = r.nextInt(3) > 0 ? lean[r.nextInt(lean.length)] : Pastime.values()[r.nextInt(Pastime.values().length)];
        switch (race) {
            case CAVE -> t.bothers.add(Bother.SUN);
            case SWAMP -> t.bothers.add(r.nextBoolean() ? Bother.NOISE : Bother.LONELINESS);
            case STONE -> t.bothers.add(Bother.WATER);
            case FOREST -> t.bothers.add(r.nextBoolean() ? Bother.RAIN : Bother.DARKNESS);
        }
        if (r.nextInt(3) == 0) {
            Bother b = Bother.values()[r.nextInt(Bother.values().length)];
            if (!t.bothers.contains(b) && !(race == Race.CAVE && b == Bother.DARKNESS)) t.bothers.add(b);
        }
        // one fear for sure, sometimes two; races are spared the silly ones
        List<Fear> pool = new ArrayList<>(List.of(Fear.values()));
        if (race == Race.SWAMP) pool.remove(Fear.WATER);
        if (race == Race.CAVE) pool.remove(Fear.DARKNESS);
        if (race == Race.STONE) pool.remove(Fear.FIRE);
        t.fears.add(pool.remove(r.nextInt(pool.size())));
        if (r.nextInt(3) == 0) t.fears.add(pool.remove(r.nextInt(pool.size())));
        return t;
    }

    public boolean likes(ItemStack s) {
        return likedFoods.contains(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
    }

    public boolean hates(ItemStack s) {
        return hatedFoods.contains(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
    }

    public boolean fears(Fear f) {
        return fears.contains(f);
    }

    public boolean bothered(Bother b) {
        return bothers.contains(b);
    }

    public static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        likedFoods.forEach(s -> l.add(StringTag.valueOf(s)));
        t.put("liked", l);
        ListTag h = new ListTag();
        hatedFoods.forEach(s -> h.add(StringTag.valueOf(s)));
        t.put("hated", h);
        t.putString("pastime", pastime.name());
        t.putString("bothers", String.join(",", bothers.stream().map(Enum::name).toList()));
        t.putString("fears", String.join(",", fears.stream().map(Enum::name).toList()));
        return t;
    }

    public static Traits load(CompoundTag t) {
        Traits x = new Traits();
        t.getListOrEmpty("liked").forEach(e -> e.asString().ifPresent(x.likedFoods::add));
        t.getListOrEmpty("hated").forEach(e -> e.asString().ifPresent(x.hatedFoods::add));
        try {
            x.pastime = Pastime.valueOf(t.getStringOr("pastime", "DANCING"));
        } catch (IllegalArgumentException ignored) {}
        for (String s : t.getStringOr("bothers", "").split(",")) if (!s.isEmpty()) try {
            x.bothers.add(Bother.valueOf(s));
        } catch (IllegalArgumentException ignored) {}
        for (String s : t.getStringOr("fears", "").split(",")) if (!s.isEmpty()) try {
            x.fears.add(Fear.valueOf(s));
        } catch (IllegalArgumentException ignored) {}
        return x;
    }
}
