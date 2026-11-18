package pl.kubalte.elementals;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** The six states of a creature (as in the "Owca Żywiołów" concept): none + five elements. */
public enum Element implements StringRepresentable {
    NONE("none", 0xBDB7A6),
    FIRE("fire", 0xE8572A),
    ICE("ice", 0x7CC6E4),
    DARK("dark", 0x8B5CF6),
    ELECTRIC("electric", 0xE9C32A),
    DRAGON("dragon", 0x2F9E78);

    public final String id;
    /** RGB */
    public final int color;

    Element(String id, int color) {
        this.id = id;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public Component displayName() {
        return Component.translatable("element.elementals." + id);
    }

    private static final Element[] VALUES = values();

    public static Element byOrdinal(int i) {
        return i >= 0 && i < VALUES.length ? VALUES[i] : NONE;
    }
}
