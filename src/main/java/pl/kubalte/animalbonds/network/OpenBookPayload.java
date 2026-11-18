package pl.kubalte.animalbonds.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import pl.kubalte.animalbonds.AnimalBonds;

/** Server -> client: open the pen book with the creatures found in the pen. */
public record OpenBookPayload(BlockPos pedestal, int area, List<BookEntry> entries, String owner, List<String> members, boolean canManage) implements CustomPacketPayload {
    public static final Type<OpenBookPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "open_book"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBookPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pedestal);
                buf.writeVarInt(p.area);
                buf.writeVarInt(p.entries.size());
                for (BookEntry e : p.entries) BookEntry.STREAM_CODEC.encode(buf, e);
                buf.writeUtf(p.owner);
                buf.writeVarInt(p.members.size());
                for (String m : p.members) buf.writeUtf(m);
                buf.writeBoolean(p.canManage);
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                int area = buf.readVarInt();
                int n = buf.readVarInt();
                List<BookEntry> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) list.add(BookEntry.STREAM_CODEC.decode(buf));
                String owner = buf.readUtf();
                int mc = buf.readVarInt();
                List<String> members = new ArrayList<>(mc);
                for (int i = 0; i < mc; i++) members.add(buf.readUtf());
                return new OpenBookPayload(pos, area, list, owner, members, buf.readBoolean());
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
