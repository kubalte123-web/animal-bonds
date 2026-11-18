package pl.kubalte.animalbonds.bond;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * A creature made at the Splicing Altar. The creature itself is of its "body" parent's type; the other parent
 * (the donor) contributes the chosen parts and traits. Part flags: false = from the body parent, true = from the donor.
 *
 * @param donorType  entity type id of the donor parent
 * @param head       head from the donor
 * @param legs       legs from the donor
 * @param extras     tail / wings / fins from the donor
 * @param colors     0 = body parent's colours, 1 = donor's colours, 2 = each part keeps its own (mixed)
 * @param movement   moves (when ridden) like the donor: fly / climb / swim / jump
 * @param parents    display names of the parents, "A × B"
 */
public record HybridData(String donorType, boolean head, boolean legs, boolean extras, int colors, boolean movement, String parents) {
    public static final MapCodec<HybridData> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("donor").forGetter(HybridData::donorType),
            Codec.BOOL.optionalFieldOf("head", false).forGetter(HybridData::head),
            Codec.BOOL.optionalFieldOf("legs", false).forGetter(HybridData::legs),
            Codec.BOOL.optionalFieldOf("extras", false).forGetter(HybridData::extras),
            Codec.INT.optionalFieldOf("colors", 2).forGetter(HybridData::colors),
            Codec.BOOL.optionalFieldOf("movement", false).forGetter(HybridData::movement),
            Codec.STRING.optionalFieldOf("parents", "").forGetter(HybridData::parents)
    ).apply(i, HybridData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HybridData> STREAM_CODEC = StreamCodec.of(
            (buf, d) -> {
                buf.writeUtf(d.donorType);
                buf.writeBoolean(d.head);
                buf.writeBoolean(d.legs);
                buf.writeBoolean(d.extras);
                buf.writeVarInt(d.colors);
                buf.writeBoolean(d.movement);
                buf.writeUtf(d.parents);
            },
            buf -> new HybridData(buf.readUtf(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readBoolean(), buf.readUtf()));
}
