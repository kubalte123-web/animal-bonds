package pl.kubalte.animalbonds.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.player.Player;

/** Who owns a pen Chronicle and which other players are trusted to care for and use its creatures. */
public final class PenAccess {
    public record Member(UUID id, String name) {
        static final Codec<Member> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Member::id),
                Codec.STRING.fieldOf("name").forGetter(Member::name)
        ).apply(i, Member::new));
    }

    public Optional<UUID> owner = Optional.empty();
    public String ownerName = "";
    public final Map<UUID, String> members = new LinkedHashMap<>();

    public static final Codec<PenAccess> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(a -> a.owner),
            Codec.STRING.optionalFieldOf("owner_name", "").forGetter(a -> a.ownerName),
            Member.CODEC.listOf().optionalFieldOf("members", List.of()).forGetter(a -> a.members.entrySet().stream().map(e -> new Member(e.getKey(), e.getValue())).toList())
    ).apply(i, (owner, name, list) -> {
        PenAccess a = new PenAccess();
        a.owner = owner;
        a.ownerName = name;
        for (Member m : list) a.members.put(m.id(), m.name());
        return a;
    }));

    /** Unowned Chronicles (e.g. from older versions) are open to everyone. */
    public boolean isTrusted(Player p) {
        return owner.isEmpty() || owner.get().equals(p.getUUID()) || members.containsKey(p.getUUID());
    }

    public boolean isTrusted(UUID id) {
        return owner.isEmpty() || owner.get().equals(id) || members.containsKey(id);
    }

    public boolean isOwner(Player p) {
        return owner.isEmpty() || owner.get().equals(p.getUUID());
    }
}
