package pl.kubalte.animalbonds.bond;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Per-mob bond state. Stored as a NeoForge data attachment on the mob and synced to clients
 * (the synced part is small: xp, happiness, needs, faint and bags flags).
 */
public final class BondData {
    public int xp;
    public long lastPet = Long.MIN_VALUE / 2;
    public long lastTreat = Long.MIN_VALUE / 2;
    public long lastGift;
    /** 0..100, -1 = never evaluated in a pen. */
    public int happiness = -1;
    /** Bit i set = i-th need of the species profile is satisfied. */
    public int needMask;
    /** Game time until which the mob is fainted; 0 = not fainted. */
    public long faintedUntil;
    public boolean bags;
    public Optional<GlobalPos> home = Optional.empty();
    /** The player who earned the creature's trust. Others may use it only if trusted at its pen's Chronicle. */
    public Optional<UUID> owner = Optional.empty();
    /** Game time of the last splicing this creature took part in. */
    public long lastSplice = Long.MIN_VALUE / 2;

    public BondData() {}

    private BondData(int xp, long lastPet, long lastTreat, long lastGift, int happiness, int needMask, long faintedUntil, boolean bags, Optional<GlobalPos> home, Optional<UUID> owner, long lastSplice) {
        this.xp = xp;
        this.lastPet = lastPet;
        this.lastTreat = lastTreat;
        this.lastGift = lastGift;
        this.happiness = happiness;
        this.needMask = needMask;
        this.faintedUntil = faintedUntil;
        this.bags = bags;
        this.home = home;
        this.owner = owner;
        this.lastSplice = lastSplice;
    }

    public int level() {
        return BondLevels.levelFor(xp);
    }

    public boolean isFainted() {
        return faintedUntil > 0;
    }

    public static final MapCodec<BondData> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("xp", 0).forGetter(d -> d.xp),
            Codec.LONG.optionalFieldOf("last_pet", Long.MIN_VALUE / 2).forGetter(d -> d.lastPet),
            Codec.LONG.optionalFieldOf("last_treat", Long.MIN_VALUE / 2).forGetter(d -> d.lastTreat),
            Codec.LONG.optionalFieldOf("last_gift", 0L).forGetter(d -> d.lastGift),
            Codec.INT.optionalFieldOf("happiness", -1).forGetter(d -> d.happiness),
            Codec.INT.optionalFieldOf("needs", 0).forGetter(d -> d.needMask),
            Codec.LONG.optionalFieldOf("fainted_until", 0L).forGetter(d -> d.faintedUntil),
            Codec.BOOL.optionalFieldOf("bags", false).forGetter(d -> d.bags),
            GlobalPos.CODEC.optionalFieldOf("home").forGetter(d -> d.home),
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(d -> d.owner),
            Codec.LONG.optionalFieldOf("last_splice", Long.MIN_VALUE / 2).forGetter(d -> d.lastSplice)
    ).apply(i, BondData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BondData> STREAM_CODEC = StreamCodec.of(
            (buf, d) -> {
                buf.writeVarInt(d.xp);
                buf.writeVarInt(d.happiness + 1);
                buf.writeVarInt(d.needMask);
                buf.writeLong(d.faintedUntil);
                buf.writeBoolean(d.bags);
            },
            buf -> {
                BondData d = new BondData();
                d.xp = buf.readVarInt();
                d.happiness = buf.readVarInt() - 1;
                d.needMask = buf.readVarInt();
                d.faintedUntil = buf.readLong();
                d.bags = buf.readBoolean();
                return d;
            });
}
