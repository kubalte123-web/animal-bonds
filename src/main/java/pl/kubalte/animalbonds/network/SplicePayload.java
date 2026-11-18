package pl.kubalte.animalbonds.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import pl.kubalte.animalbonds.AnimalBonds;
import pl.kubalte.animalbonds.bond.Hybrids;

/** Client -> server: splice parents A and B with the chosen parts. */
public record SplicePayload(BlockPos altar, int entityA, int entityB, Hybrids.Choices choices) implements CustomPacketPayload {
    public static final Type<SplicePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "splice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SplicePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.altar);
                buf.writeVarInt(p.entityA);
                buf.writeVarInt(p.entityB);
                Hybrids.Choices c = p.choices;
                buf.writeBoolean(c.bodyB());
                buf.writeBoolean(c.headB());
                buf.writeBoolean(c.legsB());
                buf.writeBoolean(c.extrasB());
                buf.writeVarInt(c.colors());
                buf.writeBoolean(c.movementB());
            },
            buf -> new SplicePayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(),
                    new Hybrids.Choices(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readBoolean())));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
