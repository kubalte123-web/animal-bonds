package pl.kubalte.gospodarstwo.kitchen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.block.StationBlockEntity;

/** Kamionkowy garnek: everything put in ferments together over about a day (cabbage -> sauerkraut, juice -> vinegar...). */
public class CrockBlockEntity extends StationBlockEntity {
    public static final List<Rec> RECIPES = new ArrayList<>();

    static {
        RECIPES.add(new Rec(s -> s.is(Gospodarstwo.CABBAGE.get()), 1, Gospodarstwo.SAUERKRAUT, 1, 0xD8D49A));
        RECIPES.add(new Rec(s -> s.is(Items.BEETROOT), 1, Gospodarstwo.PICKLED_BEETS, 1, 0x8E1E3A));
        RECIPES.add(new Rec(s -> s.is(Gospodarstwo.APPLE_JUICE.get()), 1, Gospodarstwo.VINEGAR, 1, 0xC89848));
        RECIPES.add(new Rec(s -> s.is(Gospodarstwo.ONION.get()), 1, Gospodarstwo.PICKLED_ONIONS, 1, 0xE8DCB8));
    }

    public CrockBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.CROCK_BE.get(), pos, state);
    }

    @Override
    public Job job(ItemStack in) {
        Rec r = Rec.find(RECIPES, in);
        if (r == null) return null;
        int n = Math.min(in.getCount(), 16);
        return new Job(n, new ItemStack(r.result().get(), n * r.resultCount()), GConfig.FERMENT_TICKS.get());
    }

    public int contentColor() {
        Rec r = Rec.find(RECIPES, input);
        if (r == null) r = Rec.byResult(RECIPES, output);
        return r == null ? -1 : r.color();
    }

    public float fraction() {
        return Math.min(1F, progress / (float) Math.max(1, GConfig.FERMENT_TICKS.get()));
    }

    @Override
    protected int syncEvery() {
        return 400;
    }

    @Override
    protected SoundEvent workSound() {
        return SoundEvents.BUBBLE_COLUMN_BUBBLE_POP;
    }

    @Override
    protected String emptyKey() {
        return "crock.gospodarstwo.empty";
    }
}
