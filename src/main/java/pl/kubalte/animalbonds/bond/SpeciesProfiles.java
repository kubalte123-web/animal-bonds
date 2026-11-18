package pl.kubalte.animalbonds.bond;

import static pl.kubalte.animalbonds.bond.Need.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Needs, favourite food and gifts of every vanilla creature. Unknown (modded) mobs get a sensible fallback. */
public final class SpeciesProfiles {
    private SpeciesProfiles() {}

    /** A gift the creature can bring: item id, count range, minimum bond level. */
    public record Gift(String item, int min, int max, int minLevel) {}

    /** favourites: item ids or "#namespace:tag". space: pen blocks needed per creature. */
    public record Profile(List<Need> needs, List<String> favourites, List<Gift> gifts, int space) {
        public boolean isFavourite(ItemStack stack) {
            for (String f : favourites) {
                if (f.startsWith("#")) {
                    TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(f.substring(1)));
                    if (stack.is(tag)) return true;
                } else {
                    Optional<Item> item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(f));
                    if (item.isPresent() && stack.is(item.get())) return true;
                }
            }
            return false;
        }
    }

    private static final Map<String, Profile> PROFILES = new HashMap<>();

    private static Gift g(String item, int min, int max, int minLevel) {
        return new Gift(item.contains(":") ? item : "minecraft:" + item, min, max, minLevel);
    }

    private static List<String> f(String... items) {
        return java.util.Arrays.stream(items).map(s -> s.startsWith("#") || s.contains(":") ? s : "minecraft:" + s).toList();
    }

    private static void p(String id, List<Need> needs, List<String> fav, int space, Gift... gifts) {
        PROFILES.put("minecraft:" + id, new Profile(needs, fav, List.of(gifts), space));
    }

    static {
        // ---- Farm animals ----
        p("cow", List.of(TROUGH, GRASS, WATER, SPACE, COMPANY, ROOF, TREAT, AFFECTION), f("wheat", "apple"), 9,
                g("leather", 1, 2, 2), g("beef", 1, 1, 6));
        p("mooshroom", List.of(TROUGH, WATER, SPACE, COMPANY, DARKNESS, TREAT, AFFECTION), f("wheat", "red_mushroom", "brown_mushroom"), 9,
                g("red_mushroom", 1, 3, 2), g("brown_mushroom", 1, 3, 2), g("leather", 1, 1, 4));
        p("sheep", List.of(TROUGH, GRASS, SPACE, COMPANY, BED, TREAT, AFFECTION), f("wheat", "sweet_berries"), 6,
                g("white_wool", 1, 3, 2), g("string", 1, 2, 4), g("mutton", 1, 1, 7));
        p("pig", List.of(TROUGH, WATER, BED, SPACE, ROOF, TREAT, AFFECTION), f("carrot", "potato", "beetroot", "golden_carrot"), 6,
                g("porkchop", 1, 1, 5), g("carrot", 1, 2, 2), g("potato", 1, 2, 3));
        p("chicken", List.of(TROUGH, BED, ROOF, LIGHT, COMPANY, TREAT, AFFECTION), f("wheat_seeds", "melon_seeds", "pumpkin_seeds", "beetroot_seeds", "torchflower_seeds"), 3,
                g("egg", 1, 2, 2), g("feather", 1, 3, 2), g("chicken", 1, 1, 8));
        p("rabbit", List.of(TROUGH, GRASS, FLOWERS, BED, SPACE, TREAT, AFFECTION), f("carrot", "golden_carrot", "dandelion"), 4,
                g("rabbit_hide", 1, 2, 2), g("rabbit_foot", 1, 1, 6));
        p("goat", List.of(TROUGH, SPACE, SNOW, HAY, TREAT, AFFECTION), f("wheat"), 9,
                g("leather", 1, 1, 3), g("goat_horn", 1, 1, 9));
        p("turtle", List.of(WATER, SAND, SPACE, TREAT, AFFECTION), f("seagrass"), 9,
                g("turtle_scute", 1, 1, 3), g("seagrass", 1, 3, 2));
        p("armadillo", List.of(SAND, WARMTH, BED, TREAT, AFFECTION), f("spider_eye"), 6,
                g("armadillo_scute", 1, 1, 2));
        p("sniffer", List.of(TROUGH, GRASS, SPACE, WATER, TREAT, AFFECTION), f("torchflower_seeds"), 25,
                g("torchflower_seeds", 1, 1, 3), g("pitcher_pod", 1, 1, 4), g("moss_block", 1, 2, 2));
        // ---- Mounts ----
        for (String horse : List.of("horse", "donkey", "mule")) {
            p(horse, List.of(TROUGH, HAY, WATER, ROOF, SPACE, BED, TREAT, AFFECTION), f("apple", "golden_apple", "golden_carrot", "sugar", "wheat", "hay_block"), 16,
                    g("leather", 1, 2, 2), g("apple", 1, 1, 4));
        }
        p("skeleton_horse", List.of(DARKNESS, SPACE, TREAT, AFFECTION), f("bone", "golden_apple"), 16, g("bone", 1, 3, 2));
        p("zombie_horse", List.of(DARKNESS, SPACE, TREAT, AFFECTION), f("rotten_flesh", "golden_apple"), 16, g("rotten_flesh", 1, 3, 2));
        for (String llama : List.of("llama", "trader_llama")) {
            p(llama, List.of(TROUGH, HAY, SPACE, COMPANY, TREAT, AFFECTION), f("hay_block", "wheat"), 12,
                    g("white_wool", 1, 2, 2), g("leather", 1, 1, 3));
        }
        p("camel", List.of(SAND, WATER, SPACE, WARMTH, TREAT, AFFECTION), f("cactus"), 20,
                g("leather", 1, 1, 3), g("cactus", 1, 2, 2), g("dead_bush", 1, 1, 2));
        p("camel_husk", List.of(SAND, DARKNESS, SPACE, TREAT, AFFECTION), f("rabbit_foot", "cactus"), 20,
                g("rotten_flesh", 1, 2, 2), g("sand", 1, 3, 2));
        p("strider", List.of(WARMTH, SPACE, TREAT, AFFECTION), f("warped_fungus"), 9,
                g("string", 1, 3, 2), g("warped_fungus", 1, 1, 5));
        p("happy_ghast", List.of(SPACE, WATER, TREAT, AFFECTION), f("snowball"), 36,
                g("ghast_tear", 1, 1, 6), g("snowball", 1, 4, 2));
        // ---- Pets ----
        p("wolf", List.of(BED, ROOF, COMPANY, TREAT, AFFECTION), f("beef", "porkchop", "chicken", "mutton", "rabbit", "cooked_beef", "bone"), 6,
                g("bone", 1, 2, 2), g("rabbit_hide", 1, 1, 4), g("leather", 1, 1, 6));
        p("cat", List.of(BED, ROOF, LIGHT, TREAT, AFFECTION), f("cod", "salmon"), 4,
                g("string", 1, 2, 2), g("feather", 1, 1, 3), g("rabbit_foot", 1, 1, 7), g("phantom_membrane", 1, 1, 9));
        p("ocelot", List.of(GRASS, FLOWERS, SPACE, TREAT, AFFECTION), f("cod", "salmon"), 6, g("cocoa_beans", 1, 2, 2));
        p("parrot", List.of(FLOWERS, LIGHT, COMPANY, TREAT, AFFECTION), f("wheat_seeds", "melon_seeds", "pumpkin_seeds", "beetroot_seeds", "torchflower_seeds"), 3,
                g("feather", 1, 2, 2), g("cocoa_beans", 1, 1, 4));
        p("fox", List.of(BED, SNOW, GRASS, TREAT, AFFECTION), f("sweet_berries", "glow_berries"), 6,
                g("sweet_berries", 1, 3, 2), g("rabbit_hide", 1, 1, 4), g("emerald", 1, 1, 9));
        p("panda", List.of(TROUGH, GRASS, SPACE, BED, TREAT, AFFECTION), f("bamboo", "cake"), 12,
                g("bamboo", 2, 6, 2), g("slime_ball", 1, 1, 6));
        p("polar_bear", List.of(SNOW, WATER, SPACE, TREAT, AFFECTION), f("cod", "salmon"), 16,
                g("cod", 1, 2, 2), g("salmon", 1, 1, 3), g("ice", 1, 2, 4));
        p("bee", List.of(FLOWERS, LIGHT, COMPANY, TREAT, AFFECTION), f("#minecraft:flowers"), 3,
                g("honeycomb", 1, 2, 2), g("honey_bottle", 1, 1, 5));
        p("frog", List.of(WATER, GRASS, SPACE, TREAT, AFFECTION), f("slime_ball"), 4,
                g("slime_ball", 1, 1, 3), g("lily_pad", 1, 1, 2));
        p("axolotl", List.of(WATER, DARKNESS, TREAT, AFFECTION), f("tropical_fish_bucket"), 4, g("clay_ball", 1, 3, 2));
        p("allay", List.of(LIGHT, FLOWERS, TREAT, AFFECTION), f("amethyst_shard", "cookie"), 2,
                g("amethyst_shard", 1, 2, 3), g("glowstone_dust", 1, 2, 5));
        p("copper_golem", List.of(ROOF, LIGHT, TREAT, AFFECTION), f("honeycomb", "copper_ingot"), 2,
                g("copper_nugget", 2, 6, 2), g("copper_ingot", 1, 1, 6));
        p("iron_golem", List.of(LIGHT, SPACE, FLOWERS, TREAT, AFFECTION), f("iron_ingot", "poppy"), 9,
                g("iron_nugget", 2, 5, 2), g("poppy", 1, 1, 2), g("iron_ingot", 1, 1, 7));
        p("snow_golem", List.of(SNOW, ROOF, TREAT, AFFECTION), f("snowball", "carved_pumpkin"), 4, g("snowball", 2, 8, 2));
        p("dolphin", List.of(WATER, SPACE, COMPANY, TREAT, AFFECTION), f("cod", "salmon"), 25, g("cod", 1, 2, 3), g("prismarine_crystals", 1, 1, 7));
        p("nautilus", List.of(WATER, SPACE, TREAT, AFFECTION), f("pufferfish", "cod"), 16, g("nautilus_shell", 1, 1, 6));
        p("squid", List.of(WATER, DARKNESS, TREAT, AFFECTION), f("cod"), 9, g("ink_sac", 1, 2, 2));
        p("glow_squid", List.of(WATER, DARKNESS, TREAT, AFFECTION), f("cod"), 9, g("glow_ink_sac", 1, 2, 2));
        p("bat", List.of(DARKNESS, ROOF, TREAT, AFFECTION), f("sweet_berries", "glow_berries"), 4, g("string", 1, 1, 3));
        p("villager", List.of(BED, ROOF, LIGHT, COMPANY, TREAT, AFFECTION), f("bread", "carrot", "potato", "beetroot"), 4, g("emerald", 1, 1, 4), g("bread", 1, 1, 2));
        p("wandering_trader", List.of(BED, ROOF, TREAT, AFFECTION), f("bread", "milk_bucket"), 4, g("emerald", 1, 1, 5));
        // ---- Monsters (cared for in dark, roofed pens) ----
        for (String z : List.of("zombie", "husk", "zombie_villager")) {
            p(z, List.of(TROUGH, ROOF, DARKNESS, COMPANY, TREAT, AFFECTION), f("beef", "porkchop", "chicken", "rotten_flesh"), 4,
                    g("rotten_flesh", 1, 3, 2), g("iron_nugget", 1, 3, 4), g("carrot", 1, 1, 5), g("potato", 1, 1, 5), g("iron_ingot", 1, 1, 9));
        }
        p("drowned", List.of(WATER, DARKNESS, TREAT, AFFECTION), f("cod", "salmon", "rotten_flesh"), 6,
                g("rotten_flesh", 1, 2, 2), g("copper_ingot", 1, 1, 5), g("nautilus_shell", 1, 1, 9));
        for (String s : List.of("skeleton", "stray", "bogged", "parched")) {
            p(s, List.of(ROOF, DARKNESS, BED, TREAT, AFFECTION), f("bone", "bone_meal"), 4,
                    g("bone", 1, 3, 2), g("arrow", 2, 6, 3), g("bone_meal", 1, 3, 2));
        }
        p("wither_skeleton", List.of(DARKNESS, WARMTH, TREAT, AFFECTION), f("bone", "coal"), 4,
                g("coal", 1, 2, 2), g("bone", 1, 2, 2), g("wither_skeleton_skull", 1, 1, 10));
        p("creeper", List.of(ROOF, DARKNESS, GRASS, TREAT, AFFECTION), f("gunpowder", "melon_slice"), 6,
                g("gunpowder", 1, 3, 2), g("music_disc_13", 1, 1, 10));
        p("spider", List.of(ROOF, DARKNESS, BED, TREAT, AFFECTION), f("spider_eye", "chicken", "rotten_flesh"), 6,
                g("string", 2, 4, 2), g("spider_eye", 1, 1, 3), g("cobweb", 1, 1, 6));
        p("cave_spider", List.of(ROOF, DARKNESS, TREAT, AFFECTION), f("spider_eye"), 3, g("string", 1, 3, 2), g("fermented_spider_eye", 1, 1, 6));
        p("enderman", List.of(ROOF, DARKNESS, SPACE, TREAT, AFFECTION), f("chorus_fruit", "melon_slice"), 9,
                g("ender_pearl", 1, 1, 3), g("grass_block", 1, 1, 2), g("ender_eye", 1, 1, 9));
        p("endermite", List.of(DARKNESS, TREAT, AFFECTION), f("chorus_fruit"), 1, g("ender_pearl", 1, 1, 8));
        p("silverfish", List.of(DARKNESS, ROOF, TREAT, AFFECTION), f("cobblestone"), 1, g("gravel", 1, 2, 2));
        p("slime", List.of(WATER, DARKNESS, SPACE, TREAT, AFFECTION), f("slime_ball", "sugar"), 6, g("slime_ball", 1, 3, 2));
        p("magma_cube", List.of(WARMTH, SPACE, TREAT, AFFECTION), f("magma_cream", "blaze_powder"), 6, g("magma_cream", 1, 2, 2));
        p("sulfur_cube", List.of(WARMTH, DARKNESS, SPACE, TREAT, AFFECTION), f("slime_ball", "gunpowder"), 6, g("gunpowder", 1, 2, 2), g("slime_ball", 1, 2, 3));
        p("blaze", List.of(WARMTH, ROOF, TREAT, AFFECTION), f("coal", "charcoal", "blaze_powder"), 6,
                g("blaze_powder", 1, 2, 2), g("blaze_rod", 1, 1, 5));
        p("ghast", List.of(WARMTH, SPACE, TREAT, AFFECTION), f("snowball"), 36, g("gunpowder", 1, 2, 2), g("ghast_tear", 1, 1, 6));
        p("witch", List.of(ROOF, DARKNESS, FLOWERS, TREAT, AFFECTION), f("sugar", "spider_eye", "glowstone_dust"), 4,
                g("redstone", 1, 3, 2), g("glowstone_dust", 1, 2, 3), g("sugar", 1, 2, 2), g("glass_bottle", 1, 1, 2));
        for (String pg : List.of("piglin", "piglin_brute")) {
            p(pg, List.of(WARMTH, ROOF, COMPANY, TREAT, AFFECTION), f("gold_ingot", "gold_nugget", "porkchop", "cooked_porkchop"), 4,
                    g("gold_nugget", 2, 5, 2), g("leather", 1, 1, 3), g("quartz", 1, 2, 4), g("gold_ingot", 1, 1, 7));
        }
        p("zombified_piglin", List.of(WARMTH, COMPANY, TREAT, AFFECTION), f("gold_nugget", "rotten_flesh"), 4,
                g("rotten_flesh", 1, 2, 2), g("gold_nugget", 1, 3, 3), g("gold_ingot", 1, 1, 8));
        p("hoglin", List.of(TROUGH, WARMTH, SPACE, TREAT, AFFECTION), f("crimson_fungus"), 12, g("leather", 1, 2, 2), g("porkchop", 1, 1, 6));
        p("zoglin", List.of(DARKNESS, SPACE, TREAT, AFFECTION), f("rotten_flesh"), 12, g("rotten_flesh", 1, 3, 2));
        p("phantom", List.of(DARKNESS, SPACE, TREAT, AFFECTION), f("cod", "salmon"), 16, g("phantom_membrane", 1, 1, 3));
        p("breeze", List.of(SPACE, LIGHT, TREAT, AFFECTION), f("wind_charge", "breeze_rod"), 9, g("wind_charge", 1, 2, 3), g("breeze_rod", 1, 1, 7));
        p("guardian", List.of(WATER, SPACE, TREAT, AFFECTION), f("cod", "pufferfish"), 16, g("prismarine_shard", 1, 2, 2), g("prismarine_crystals", 1, 1, 4));
        p("shulker", List.of(ROOF, DARKNESS, TREAT, AFFECTION), f("chorus_fruit"), 2, g("shulker_shell", 1, 1, 8), g("purpur_block", 1, 1, 3));
        p("zombie_nautilus", List.of(WATER, DARKNESS, TREAT, AFFECTION), f("rotten_flesh", "pufferfish"), 16, g("nautilus_shell", 1, 1, 7));
        for (String ill : List.of("pillager", "vindicator", "evoker")) {
            p(ill, List.of(BED, ROOF, COMPANY, TREAT, AFFECTION), f("bread", "cooked_beef", "emerald"), 4, g("emerald", 1, 1, 5), g("arrow", 1, 3, 2));
        }
        p("vex", List.of(LIGHT, TREAT, AFFECTION), f("glowstone_dust"), 1, g("iron_nugget", 1, 2, 3));
        p("ravager", List.of(TROUGH, HAY, SPACE, TREAT, AFFECTION), f("hay_block", "wheat"), 25, g("leather", 1, 3, 2), g("saddle", 1, 1, 9));
        p("creaking", List.of(DARKNESS, TREAT, AFFECTION), f("pale_oak_sapling", "resin_clump"), 4, g("resin_clump", 1, 2, 2), g("pale_oak_log", 1, 1, 4));
        p("warden", List.of(DARKNESS, ROOF, SPACE, TREAT, AFFECTION), f("sculk", "echo_shard"), 25, g("sculk", 1, 3, 2), g("echo_shard", 1, 1, 8));
    }

    private static final Profile FALLBACK_ANIMAL = new Profile(List.of(TROUGH, SPACE, COMPANY, TREAT, AFFECTION), List.of(), List.of(), 6);
    private static final Profile FALLBACK_MONSTER = new Profile(List.of(ROOF, DARKNESS, TREAT, AFFECTION), List.of(), List.of(), 4);
    private static final Profile FALLBACK_OTHER = new Profile(List.of(SPACE, TREAT, AFFECTION), List.of(), List.of(), 4);

    public static Profile get(Entity entity) {
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        Profile p = PROFILES.get(key.toString());
        if (p == null) p = entity instanceof Enemy ? FALLBACK_MONSTER : entity instanceof Animal ? FALLBACK_ANIMAL : FALLBACK_OTHER;
        HybridData h = Hybrids.get(entity);
        return h == null ? p : Hybrids.mergedProfile(p, key.toString(), h.donorType());
    }

    /** Profile of a creature type by id (used for a hybrid's donor parent). */
    public static Profile byId(String typeId) {
        Profile p = PROFILES.get(typeId);
        return p != null ? p : FALLBACK_OTHER;
    }

    /** Is this stack a food the mob loves (used for hand feeding and bonding)? */
    public static boolean isFavourite(Mob mob, ItemStack stack) {
        if (stack.isEmpty()) return false;
        Profile p = get(mob);
        if (p.isFavourite(stack)) return true;
        // Modded animals: anything they normally eat/breed with.
        return p.favourites().isEmpty() && mob instanceof Animal animal && animal.isFood(stack);
    }

    public static boolean hasProfile(Entity entity) {
        return PROFILES.containsKey(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }
}
