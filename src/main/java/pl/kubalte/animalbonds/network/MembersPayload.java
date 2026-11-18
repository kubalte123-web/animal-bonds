package pl.kubalte.animalbonds.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import pl.kubalte.animalbonds.AnimalBonds;

/** Client -> server: the Chronicle's owner adds or removes a trusted player (by nick). */
public record MembersPayload(BlockPos pedestal, boolean add, String name) implements CustomPacketPayload {
    public static final Type<MembersPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "members"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MembersPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pedestal);
                buf.writeBoolean(p.add);
                buf.writeUtf(p.name, 32);
            },
            buf -> new MembersPayload(buf.readBlockPos(), buf.readBoolean(), buf.readUtf(32)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
