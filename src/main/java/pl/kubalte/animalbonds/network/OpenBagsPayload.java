package pl.kubalte.animalbonds.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import pl.kubalte.animalbonds.AnimalBonds;

/** Client -> server: open the saddlebags of a creature (from the book, or the one being ridden: entityId = -1). */
public record OpenBagsPayload(BlockPos pedestal, int entityId) implements CustomPacketPayload {
    public static final Type<OpenBagsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "open_bags"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBagsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pedestal);
                buf.writeVarInt(p.entityId + 1);
            },
            buf -> new OpenBagsPayload(buf.readBlockPos(), buf.readVarInt() - 1));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
