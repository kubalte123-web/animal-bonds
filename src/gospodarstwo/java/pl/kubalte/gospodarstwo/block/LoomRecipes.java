package pl.kubalte.gospodarstwo.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** What the weaving loom makes: N pieces of yarn of one kind -> one piece of cloth. Other mods can add their own. */
public final class LoomRecipes {
    private LoomRecipes() {}

    /** @param yarnColor colour of the warp threads drawn on the loom, @param clothColor colour of the woven cloth. */
    public record Recipe(Predicate<ItemStack> input, int count, Supplier<? extends Item> output, int yarnColor, int clothColor) {}

    private static final List<Recipe> RECIPES = new ArrayList<>();

    public static void add(Recipe r) {
        RECIPES.add(r);
    }

    static {
        add(new Recipe(s -> s.is(Items.STRING), 4, Gospodarstwo.CANVAS, 0xF2F0E8, 0xD9D2BC));
        add(new Recipe(s -> s.is(Gospodarstwo.FLAX_FIBER.get()), 3, Gospodarstwo.LINEN, 0xD8CBA2, 0xE6DCC0));
        add(new Recipe(s -> s.is(Gospodarstwo.MERINO_WOOL.get()), 3, Gospodarstwo.MERINO_CLOTH, 0xF4ECD6, 0xEFE3C4));
        add(new Recipe(s -> s.is(Gospodarstwo.ANGORA_WOOL.get()), 3, Gospodarstwo.ANGORA_CLOTH, 0xFFFFFF, 0xF8F4F8));
        add(new Recipe(s -> s.is(Gospodarstwo.MANGALICA_WOOL.get()), 3, Gospodarstwo.FELT, 0xC9B79A, 0x9E8C70));
        add(new Recipe(s -> s.is(Gospodarstwo.HIGHLAND_HAIR.get()), 3, Gospodarstwo.FELT, 0xB0582A, 0x8A4A2A));
        add(new Recipe(s -> s.is(ItemTags.WOOL), 2, Gospodarstwo.WOOLEN_CLOTH, 0xE8E8E8, 0xC8C2B8));
    }

    public static Recipe find(ItemStack s) {
        if (s.isEmpty()) return null;
        for (Recipe r : RECIPES) if (r.input().test(s)) return r;
        return null;
    }

    public static List<Recipe> all() {
        return RECIPES;
    }
}
