package pl.kubalte.gospodarstwo.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.craft.Dyes;

public class SpinningWheelBlockEntity extends StationBlockEntity {
    /** N pieces of fibre -> M pieces of yarn; fibreColor = colour of the fibre drawn on the distaff. */
    public record Spin(Predicate<ItemStack> input, int count, Supplier<? extends Item> yarn, int yarnCount, int fibreColor) {}

    private static final List<Spin> SPINS = new ArrayList<>();

    public static void add(Spin s) {
        SPINS.add(s);
    }

    static {
        add(new Spin(s -> s.is(ItemTags.WOOL), 1, Gospodarstwo.YARN_WOOL, 2, 0xEDEDED));
        add(new Spin(s -> s.is(Gospodarstwo.FLAX_FIBER.get()), 2, Gospodarstwo.YARN_LINEN, 1, 0xD8CBA2));
        add(new Spin(s -> s.is(Gospodarstwo.MERINO_WOOL.get()), 1, Gospodarstwo.YARN_MERINO, 2, 0xF4ECD6));
        add(new Spin(s -> s.is(Gospodarstwo.ANGORA_WOOL.get()), 1, Gospodarstwo.YARN_ANGORA, 2, 0xFFFFFF));
        add(new Spin(s -> s.is(Gospodarstwo.CASHMERE.get()), 1, Gospodarstwo.YARN_CASHMERE, 1, 0xE8DCCB));
        add(new Spin(s -> s.is(Gospodarstwo.LLAMA_WOOL.get()) || s.is(Gospodarstwo.CAMEL_WOOL.get()), 1, Gospodarstwo.YARN_LLAMA, 2, 0xD9C2A0));
        add(new Spin(s -> s.is(Gospodarstwo.HIGHLAND_HAIR.get()) || s.is(Gospodarstwo.MANGALICA_WOOL.get()) || s.is(Gospodarstwo.DOG_HAIR.get())
                || s.is(Gospodarstwo.HORSE_HAIR.get()), 2, Gospodarstwo.YARN_COARSE, 1, 0xA88A64));
    }

    public static Spin find(ItemStack s) {
        if (s.isEmpty()) return null;
        for (Spin r : SPINS) if (r.input().test(s)) return r;
        return null;
    }

    public SpinningWheelBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.SPINNING_WHEEL_BE.get(), pos, state);
    }

    @Override
    public Job job(ItemStack in) {
        Spin r = find(in);
        if (r == null) return null;
        ItemStack out = new ItemStack(r.yarn().get(), r.yarnCount());
        // coloured wool keeps its colour in the yarn
        String path = BuiltInRegistries.ITEM.getKey(in.getItem()).getPath();
        if (path.endsWith("_wool") && !path.equals("white_wool") && in.is(ItemTags.WOOL)) {
            DyeColor c = DyeColor.byName(path.substring(0, path.length() - 5), null);
            if (c != null) Dyes.setColor(out, c.getTextureDiffuseColor() & 0xFFFFFF);
        }
        int dyed = Dyes.colorOf(in);
        if (dyed >= 0) Dyes.setColor(out, dyed);
        return new Job(r.count(), out, Math.max(1, GConfig.LOOM_TICKS.get() * 3 / 5));
    }

    @Override
    protected String emptyKey() {
        return "spinning.gospodarstwo.empty";
    }

    @Override
    protected net.minecraft.sounds.SoundEvent workSound() {
        return net.minecraft.sounds.SoundEvents.WOODEN_BUTTON_CLICK_ON;
    }
}
