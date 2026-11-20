package pl.kubalte.gospodarstwo.kitchen;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A simple station recipe: N of an input -> M of a product; colour is what the renderer draws for it. */
public record Rec(Predicate<ItemStack> input, int count, Supplier<? extends Item> result, int resultCount, int color) {
    public static Rec find(List<Rec> list, ItemStack s) {
        if (s.isEmpty()) return null;
        for (Rec r : list) if (r.input().test(s)) return r;
        return null;
    }

    public static Rec byResult(List<Rec> list, ItemStack out) {
        if (out.isEmpty()) return null;
        for (Rec r : list) if (out.is(r.result().get())) return r;
        return null;
    }

    public static Supplier<Item> of(Item item) {
        return () -> item;
    }
}
