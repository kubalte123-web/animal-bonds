package pl.kubalte.animalbonds.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Mob;
import pl.kubalte.animalbonds.BondConfig;
import pl.kubalte.animalbonds.bond.BondData;
import pl.kubalte.animalbonds.bond.BondLevels;
import pl.kubalte.animalbonds.bond.BondLogic;
import pl.kubalte.animalbonds.bond.Need;
import pl.kubalte.animalbonds.bond.PenScanner;
import pl.kubalte.animalbonds.bond.SpeciesProfiles;

/** One creature page of the book, as sent to the client. */
public record BookEntry(int entityId, String typeId, String customName, boolean bonded, int level, int xp,
                        int xpFrom, int xpTo, int happiness, List<Integer> needs, int faintTicks, boolean bags,
                        int giftTicks, List<String> favourites, String hybrid) {

    /** needs list encoding: ordinal * 2 + (satisfied ? 1 : 0) */
    public static int encodeNeed(Need need, boolean ok) {
        return need.ordinal() * 2 + (ok ? 1 : 0);
    }

    public static BookEntry of(Mob mob, PenScanner.Pen pen, long now) {
        BondData d = BondLogic.get(mob);
        SpeciesProfiles.Profile profile = SpeciesProfiles.get(mob);
        List<Integer> needs = new ArrayList<>();
        for (int i = 0; i < profile.needs().size(); i++) {
            boolean ok = d != null && (d.needMask & (1 << i)) != 0;
            needs.add(encodeNeed(profile.needs().get(i), ok));
        }
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        String name = mob.hasCustomName() ? mob.getCustomName().getString() : "";
        pl.kubalte.animalbonds.bond.HybridData hy = pl.kubalte.animalbonds.bond.Hybrids.get(mob);
        String hybrid = hy == null ? "" : hy.parents().isEmpty() ? hy.donorType() : hy.parents();
        if (d == null) {
            return new BookEntry(mob.getId(), type, name, false, 0, 0, 0, 1, -1, needs, 0, false, -1, profile.favourites(), hybrid);
        }
        int lvl = d.level();
        int from = BondLevels.xpForLevel(lvl);
        int to = lvl >= BondLevels.MAX_LEVEL ? from : BondLevels.xpForLevel(lvl + 1);
        int faint = d.isFainted() ? (int) Math.max(0, d.faintedUntil - now) : 0;
        int gift = lvl < BondConfig.GIFT_LEVEL.get() ? -1 : (int) Math.max(0, BondLogic.giftInterval(d) - (now - d.lastGift));
        return new BookEntry(mob.getId(), type, name, true, lvl, d.xp, from, to, d.happiness, needs, faint, d.bags, gift, profile.favourites(), hybrid);
    }

    public static final StreamCodec<FriendlyByteBuf, BookEntry> STREAM_CODEC = StreamCodec.of(
            (buf, e) -> {
                buf.writeVarInt(e.entityId);
                buf.writeUtf(e.typeId);
                buf.writeUtf(e.customName);
                buf.writeBoolean(e.bonded);
                buf.writeVarInt(e.level);
                buf.writeVarInt(e.xp);
                buf.writeVarInt(e.xpFrom);
                buf.writeVarInt(e.xpTo);
                buf.writeVarInt(e.happiness + 1);
                buf.writeVarInt(e.needs.size());
                for (int n : e.needs) buf.writeVarInt(n);
                buf.writeVarInt(e.faintTicks);
                buf.writeBoolean(e.bags);
                buf.writeVarInt(e.giftTicks + 1);
                buf.writeVarInt(e.favourites.size());
                for (String f : e.favourites) buf.writeUtf(f);
                buf.writeUtf(e.hybrid);
            },
            buf -> {
                int id = buf.readVarInt();
                String type = buf.readUtf();
                String name = buf.readUtf();
                boolean bonded = buf.readBoolean();
                int level = buf.readVarInt();
                int xp = buf.readVarInt();
                int from = buf.readVarInt();
                int to = buf.readVarInt();
                int happy = buf.readVarInt() - 1;
                int n = buf.readVarInt();
                List<Integer> needs = new ArrayList<>(n);
                for (int i = 0; i < n; i++) needs.add(buf.readVarInt());
                int faint = buf.readVarInt();
                boolean bags = buf.readBoolean();
                int gift = buf.readVarInt() - 1;
                int fc = buf.readVarInt();
                List<String> fav = new ArrayList<>(fc);
                for (int i = 0; i < fc; i++) fav.add(buf.readUtf());
                return new BookEntry(id, type, name, bonded, level, xp, from, to, happy, needs, faint, bags, gift, fav, buf.readUtf());
            });
}
