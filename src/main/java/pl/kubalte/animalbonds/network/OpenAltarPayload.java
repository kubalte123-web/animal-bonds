package pl.kubalte.animalbonds.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import pl.kubalte.animalbonds.AnimalBonds;

/** Server -> client: open the Splicing Altar with the bonded creatures around it. */
public record OpenAltarPayload(BlockPos altar, List<Candidate> candidates, String cost) implements CustomPacketPayload {
    /** problem: translation key why it can't be a parent, or "" when it can. */
    public record Candidate(int entityId, String typeId, String name, int level, String problem) {}

    public static final Type<OpenAltarPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "open_altar"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAltarPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.altar);
                buf.writeVarInt(p.candidates.size());
                for (Candidate c : p.candidates) {
                    buf.writeVarInt(c.entityId());
                    buf.writeUtf(c.typeId());
                    buf.writeUtf(c.name());
                    buf.writeVarInt(c.level());
                    buf.writeUtf(c.problem());
                }
                buf.writeUtf(p.cost);
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                int n = buf.readVarInt();
                List<Candidate> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) list.add(new Candidate(buf.readVarInt(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readUtf()));
                return new OpenAltarPayload(pos, list, buf.readUtf());
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
