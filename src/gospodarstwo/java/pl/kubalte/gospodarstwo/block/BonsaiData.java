package pl.kubalte.gospodarstwo.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** What grows in a bonsai pot (kept on the item when the pot is picked up). */
public record BonsaiData(String sapling, int growth, int trims, long seed, int water, int lastTrimGrowth) {
    public static final BonsaiData EMPTY = new BonsaiData("", 0, 0, 0L, 0, 0);

    public static final Codec<BonsaiData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("sapling").forGetter(BonsaiData::sapling),
            Codec.INT.fieldOf("growth").forGetter(BonsaiData::growth),
            Codec.INT.fieldOf("trims").forGetter(BonsaiData::trims),
            Codec.LONG.fieldOf("seed").forGetter(BonsaiData::seed),
            Codec.INT.fieldOf("water").forGetter(BonsaiData::water),
            Codec.INT.optionalFieldOf("last_trim", 0).forGetter(BonsaiData::lastTrimGrowth)).apply(i, BonsaiData::new));

    public static final StreamCodec<ByteBuf, BonsaiData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BonsaiData::sapling,
            ByteBufCodecs.VAR_INT, BonsaiData::growth,
            ByteBufCodecs.VAR_INT, BonsaiData::trims,
            ByteBufCodecs.VAR_LONG, BonsaiData::seed,
            ByteBufCodecs.VAR_INT, BonsaiData::water,
            ByteBufCodecs.VAR_INT, BonsaiData::lastTrimGrowth,
            BonsaiData::new);
}
