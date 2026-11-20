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

/** Żarna: grinds grain into flour (and sugar cane into sugar, bones into plenty of bone meal). */
public class QuernBlockEntity extends StationBlockEntity {
    public static final List<Rec> RECIPES = new ArrayList<>();

    static {
        RECIPES.add(new Rec(s -> s.is(Items.WHEAT), 1, Gospodarstwo.FLOUR, 2, 0xF2ECDC));
        RECIPES.add(new Rec(s -> s.is(Items.SUGAR_CANE), 1, Rec.of(Items.SUGAR), 2, 0xFAFAFA));
        RECIPES.add(new Rec(s -> s.is(Items.BONE), 1, Rec.of(Items.BONE_MEAL), 5, 0xEEEAE0));
        RECIPES.add(new Rec(s -> s.is(Gospodarstwo.POULTRY_BONE.get()), 1, Rec.of(Items.BONE_MEAL), 3, 0xEEEAE0));
        RECIPES.add(new Rec(s -> s.is(Gospodarstwo.HORN.get()) || s.is(Gospodarstwo.HOGLIN_TUSK.get()), 1, Rec.of(Items.BONE_MEAL), 6, 0xE8DCC0));
    }

    public QuernBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.QUERN_BE.get(), pos, state);
    }

    @Override
    public Job job(ItemStack in) {
        Rec r = Rec.find(RECIPES, in);
        return r == null ? null : new Job(r.count(), new ItemStack(r.result().get(), r.resultCount()), GConfig.QUERN_TICKS.get());
    }

    @Override
    protected SoundEvent workSound() {
        return SoundEvents.GRINDSTONE_USE;
    }

    @Override
    protected String emptyKey() {
        return "quern.gospodarstwo.empty";
    }
}
