package pl.kubalte.gospodarstwo.kitchen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.block.StationBlockEntity;

/** Wędzarnia: hang meat and fish in it over a campfire; they come out smoked (more filling). */
public class SmokehouseBlockEntity extends StationBlockEntity {
    public static final List<Rec> RECIPES = new ArrayList<>();

    static {
        RECIPES.add(new Rec(s -> s.is(Items.PORKCHOP), 1, Gospodarstwo.SMOKED_HAM, 1, 0xE09A8A));
        RECIPES.add(new Rec(s -> s.is(Gospodarstwo.FATBACK.get()), 1, Gospodarstwo.SMOKED_FATBACK, 1, 0xF0E0D0));
        RECIPES.add(new Rec(s -> s.is(Items.COD) || s.is(Items.SALMON), 1, Gospodarstwo.SMOKED_FISH, 1, 0xC8A080));
        RECIPES.add(new Rec(s -> s.is(Gospodarstwo.RAW_SAUSAGE.get()), 1, Gospodarstwo.KIELBASA, 1, 0xD07A70));
        RECIPES.add(new Rec(s -> s.is(Items.CHICKEN) || s.is(Gospodarstwo.RAW_DUCK.get()) || s.is(Gospodarstwo.RAW_GOOSE.get())
                || s.is(Gospodarstwo.RAW_TURKEY.get()), 1, Gospodarstwo.SMOKED_POULTRY, 1, 0xE8B8A0));
    }

    public SmokehouseBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.SMOKEHOUSE_BE.get(), pos, state);
    }

    @Override
    public Job job(ItemStack in) {
        Rec r = Rec.find(RECIPES, in);
        if (r == null || !Heat.below(level, worldPosition)) return null;
        int n = Math.min(in.getCount(), 8);
        return new Job(n, new ItemStack(r.result().get(), n * r.resultCount()), GConfig.SMOKE_TICKS.get());
    }

    @Override
    public boolean insert(Player player, ItemStack stack) {
        if (Rec.find(RECIPES, stack) == null) return false;
        if (!Heat.below(level, worldPosition) && level != null && !level.isClientSide())
            player.sendOverlayMessage(Component.translatable("smokehouse.gospodarstwo.cold"));
        return insertAny(player, stack);
    }

    /** What hangs inside: raw (input) or smoked (output); colour of the raw meat. */
    public int hangingColor() {
        Rec r = Rec.find(RECIPES, input);
        if (r == null) r = Rec.byResult(RECIPES, output);
        return r == null ? -1 : r.color();
    }

    public int hangingCount() {
        return Math.min(6, Math.max(input.getCount(), output.getCount()));
    }

    public float fraction() {
        return input.isEmpty() ? 1F : Math.min(1F, progress / (float) Math.max(1, GConfig.SMOKE_TICKS.get()));
    }

    @Override
    protected int syncEvery() {
        return 200;
    }

    @Override
    protected SoundEvent workSound() {
        return SoundEvents.CAMPFIRE_CRACKLE;
    }

    @Override
    protected String emptyKey() {
        return "smokehouse.gospodarstwo.empty";
    }
}
