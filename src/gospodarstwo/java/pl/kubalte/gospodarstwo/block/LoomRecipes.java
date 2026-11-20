package pl.kubalte.gospodarstwo.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** What the weaving loom makes: N pieces of yarn of one kind -> one piece of cloth. Other mods can add their own. */
public final class LoomRecipes {
    private LoomRecipes() {}

    /** @param yarnColor natural colour of the warp threads, @param clothColor natural colour of the cloth. */
    public record Recipe(Predicate<ItemStack> input, int count, Supplier<? extends Item> output, int yarnColor, int clothColor) {}

    private static final List<Recipe> RECIPES = new ArrayList<>();

    public static void add(Recipe r) {
        RECIPES.add(r);
    }

    static {
        add(new Recipe(s -> s.is(Items.STRING), 4, Gospodarstwo.CANVAS, 0xF2F0E8, 0xD9D2BC));
        add(new Recipe(s -> s.is(Gospodarstwo.YARN_LINEN.get()), 3, Gospodarstwo.LINEN, 0xD8CBA2, 0xE6DCC0));
        add(new Recipe(s -> s.is(Gospodarstwo.YARN_WOOL.get()), 3, Gospodarstwo.WOOLEN_CLOTH, 0xEDEDED, 0xC8C2B8));
        add(new Recipe(s -> s.is(Gospodarstwo.YARN_MERINO.get()), 3, Gospodarstwo.MERINO_CLOTH, 0xF4ECD6, 0xEFE3C4));
        add(new Recipe(s -> s.is(Gospodarstwo.YARN_ANGORA.get()), 3, Gospodarstwo.ANGORA_CLOTH, 0xFFFFFF, 0xF8F4F8));
        add(new Recipe(s -> s.is(Gospodarstwo.YARN_CASHMERE.get()), 3, Gospodarstwo.CASHMERE_CLOTH, 0xE8DCCB, 0xDCCDB6));
        add(new Recipe(s -> s.is(Gospodarstwo.YARN_LLAMA.get()), 3, Gospodarstwo.LLAMA_CLOTH, 0xD9C2A0, 0xC8AE88));
        add(new Recipe(s -> s.is(Gospodarstwo.YARN_COARSE.get()), 3, Gospodarstwo.FELT, 0xA88A64, 0x9E8C70));
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
