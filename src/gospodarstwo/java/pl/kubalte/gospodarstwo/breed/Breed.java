package pl.kubalte.gospodarstwo.breed;

import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/**
 * Breeds of ordinary animals. They are the same vanilla animals (and breed with ordinary ones), only look different
 * and give their own product.
 * <ul>
 *   <li>Krowa górska (highland cow): long ginger hair - shear it for highland hair</li>
 *   <li>Merynos (merino sheep): when sheared, also gives fine merino wool</li>
 *   <li>Kura jedwabista (silkie): fluffy white hen, drops down now and then</li>
 *   <li>Zielononóżka: Polish breed, lays green eggs now and then</li>
 *   <li>Mangalica: curly woolly pig - shear it for curly wool</li>
 *   <li>Królik angorski (angora rabbit): very fluffy - shear it for angora wool</li>
 * </ul>
 */
public enum Breed {
    HIGHLAND("highland", () -> EntityTypes.COW, 0xB0582A, 0x55, 0xC06A34, 1.18F, Gospodarstwo.HIGHLAND_HAIR, true),
    MERINO("merino", () -> EntityTypes.SHEEP, 0xF0E6CC, 0x40, 0xF4ECD6, 1.14F, Gospodarstwo.MERINO_WOOL, false),
    SILKIE("silkie", () -> EntityTypes.CHICKEN, 0xFFFFFF, 0x30, 0xFFFFFF, 1.22F, Gospodarstwo.DOWN, false),
    GREENLEG("greenleg", () -> EntityTypes.CHICKEN, 0x7A4A22, 0x80, 0, 0F, Gospodarstwo.GREEN_EGG, false),
    MANGALICA("mangalica", () -> EntityTypes.PIG, 0xD9C39A, 0x70, 0xE2CFA6, 1.16F, Gospodarstwo.MANGALICA_WOOL, true),
    ANGORA("angora", () -> EntityTypes.RABBIT, 0xFFFFFF, 0x50, 0xFFFFFF, 1.35F, Gospodarstwo.ANGORA_WOOL, true);

    public final String id;
    public final Supplier<EntityType<?>> species;
    /** Colour washed over the model and its strength (alpha). */
    public final int wash, washAlpha;
    /** Colour and size of the fluffy coat drawn over the model (size 0 = no coat). */
    public final int furColor;
    public final float furScale;
    public final Supplier<? extends Item> product;
    /** True = harvested with shears; otherwise dropped/added by itself. */
    public final boolean shearable;

    Breed(String id, Supplier<EntityType<?>> species, int wash, int washAlpha, int furColor, float furScale, Supplier<? extends Item> product,
          boolean shearable) {
        this.id = id;
        this.species = species;
        this.wash = wash;
        this.washAlpha = washAlpha;
        this.furColor = furColor;
        this.furScale = furScale;
        this.product = product;
        this.shearable = shearable;
    }

    public Component displayName() {
        return Component.translatable("breed.gospodarstwo." + id);
    }

    public static Breed byIndex(int i) {
        Breed[] v = values();
        return i >= 1 && i <= v.length ? v[i - 1] : null;
    }

    public int index() {
        return ordinal() + 1;
    }
}
