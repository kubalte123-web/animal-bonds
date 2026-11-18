package pl.kubalte.animalbonds.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import pl.kubalte.animalbonds.AnimalBonds;

/** Client -> server: give a bonded creature a name from the book. */
public record RenamePayload(BlockPos pedestal, int entityId, String name) implements CustomPacketPayload {
    public static final Type<RenamePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "rename"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenamePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pedestal);
                buf.writeVarInt(p.entityId);
                buf.writeUtf(p.name, 64);
            },
            buf -> new RenamePayload(buf.readBlockPos(), buf.readVarInt(), buf.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
