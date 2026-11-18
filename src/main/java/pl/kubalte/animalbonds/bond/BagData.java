package pl.kubalte.animalbonds.bond;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/** Contents of the saddlebags carried by a bonded mob (server only, not synced). */
public final class BagData {
    public static final int SIZE = 18;

    private record Slot(int slot, ItemStack stack) {
        static final Codec<Slot> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("slot").forGetter(Slot::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(Slot::stack)
        ).apply(i, Slot::new));
    }

    public final SimpleContainer container = new SimpleContainer(SIZE);

    public BagData() {}

    private BagData(List<Slot> slots) {
        for (Slot s : slots) {
            if (s.slot() >= 0 && s.slot() < SIZE) container.setItem(s.slot(), s.stack());
        }
    }

    private List<Slot> slots() {
        List<Slot> out = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            ItemStack st = container.getItem(i);
            if (!st.isEmpty()) out.add(new Slot(i, st.copy()));
        }
        return out;
    }

    public static final MapCodec<BagData> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Slot.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(BagData::slots)
    ).apply(i, BagData::new));
}
