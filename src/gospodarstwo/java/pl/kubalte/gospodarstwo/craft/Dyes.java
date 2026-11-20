package pl.kubalte.gospodarstwo.craft;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** Colours of yarn and cloth: vanilla dyes plus natural dyes from plants. */
public final class Dyes {
    private Dyes() {}

    /** Natural dyes: plants and other things that colour the dye vat. */
    private static final Map<Item, Integer> NATURAL = new java.util.HashMap<>();

    public static void init() {
        NATURAL.put(Gospodarstwo.ONION.get(), 0xC28A2E);      // onion skins: yellow-brown
        NATURAL.put(Gospodarstwo.PLUM.get(), 0x6A2C70);       // plum: purple
        NATURAL.put(Gospodarstwo.STRAWBERRY.get(), 0xD8506A); // strawberry: pink-red
        NATURAL.put(Gospodarstwo.GOOSEBERRIES.get(), 0x8DAF4A);
        NATURAL.put(Gospodarstwo.CABBAGE.get(), 0x5E9E3A);
        NATURAL.put(Items.BEETROOT, 0x8E1F2F);
        NATURAL.put(Items.SWEET_BERRIES, 0xA02838);
        NATURAL.put(Items.GLOW_BERRIES, 0xE8A030);
        NATURAL.put(Items.COCOA_BEANS, 0x6B4226);
        NATURAL.put(Items.INK_SAC, 0x1D1C21);
        NATURAL.put(Items.GLOW_INK_SAC, 0x2DB8A8);
        NATURAL.put(Items.LAPIS_LAZULI, 0x1F4FB0);
        NATURAL.put(Items.CACTUS, 0x4E8A2A);
        NATURAL.put(Items.KELP, 0x3E6E3A);
    }

    /** RGB of a dye or natural dye item, or -1. */
    public static int dyeColor(ItemStack s) {
        if (s.getItem() instanceof DyeItem) {
            DyeColor c = dyeOf(s.getItem());
            if (c != null) return c.getTextureDiffuseColor() & 0xFFFFFF;
        }
        Integer n = NATURAL.get(s.getItem());
        return n == null ? -1 : n;
    }

    /** The dye colour of a vanilla dye item, from its id (white_dye -> WHITE). */
    public static DyeColor dyeOf(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        int cut = path.lastIndexOf('_');
        return cut < 0 ? null : DyeColor.byName(path.substring(0, cut), null);
    }

    public static int colorOf(ItemStack s) {
        DyedItemColor c = s.get(DataComponents.DYED_COLOR);
        return c == null ? -1 : c.rgb() & 0xFFFFFF;
    }

    public static void setColor(ItemStack s, int rgb) {
        if (rgb < 0) s.remove(DataComponents.DYED_COLOR);
        else s.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb));
    }

    /** Mixes a colour into another like leather armour does (average, keeping brightness). */
    public static int mix(int a, int b, int weightA) {
        if (a < 0) return b;
        int r = (((a >> 16) & 255) * weightA + ((b >> 16) & 255)) / (weightA + 1);
        int g = (((a >> 8) & 255) * weightA + ((b >> 8) & 255)) / (weightA + 1);
        int bl = ((a & 255) * weightA + (b & 255)) / (weightA + 1);
        return (r << 16) | (g << 8) | bl;
    }
}
