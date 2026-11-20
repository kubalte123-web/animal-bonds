package pl.kubalte.gospodarstwo.kitchen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.block.StationBlockEntity;

/** Prasa: presses fruit into juice (and flax seeds into oil). The juice is drawn off with a glass bottle. */
public class PressBlockEntity extends StationBlockEntity {
    public static final List<Rec> RECIPES = new ArrayList<>();

    static {
        RECIPES.add(new Rec(s -> s.is(Items.APPLE), 2, Gospodarstwo.APPLE_JUICE, 1, 0xE8B848));
        RECIPES.add(new Rec(s -> s.is(Gospodarstwo.PLUM.get()), 2, Gospodarstwo.PLUM_JUICE, 1, 0x7A2E6E));
        RECIPES.add(new Rec(s -> s.is(Gospodarstwo.STRAWBERRY.get()) || s.is(Items.SWEET_BERRIES) || s.is(Gospodarstwo.GOOSEBERRIES.get()), 3,
                Gospodarstwo.BERRY_JUICE, 1, 0xC83A48));
        RECIPES.add(new Rec(s -> s.is(Gospodarstwo.FLAX_SEEDS.get()), 4, Gospodarstwo.LINSEED_OIL, 1, 0xD8B640));
    }

    public PressBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.PRESS_BE.get(), pos, state);
    }

    @Override
    public Job job(ItemStack in) {
        Rec r = Rec.find(RECIPES, in);
        return r == null ? null : new Job(r.count(), new ItemStack(r.result().get(), r.resultCount()), GConfig.PRESS_TICKS.get());
    }

    /** Colour of the mash in the basket (input) and of the juice in the tray (output); -1 = none. */
    public int mashColor() {
        Rec r = Rec.find(RECIPES, input);
        return r == null ? -1 : r.color();
    }

    public int juiceColor() {
        Rec r = Rec.byResult(RECIPES, output);
        return r == null ? -1 : r.color();
    }

    @Override
    public boolean insert(Player player, ItemStack stack) {
        if (stack.is(Items.GLASS_BOTTLE)) {
            if (level == null || level.isClientSide()) return true;
            if (output.isEmpty()) {
                player.sendOverlayMessage(Component.translatable("press.gospodarstwo.no_juice"));
                return true;
            }
            ItemStack juice = output.split(1);
            if (output.isEmpty()) output = ItemStack.EMPTY;
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (!player.getInventory().add(juice)) player.drop(juice, false);
            level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1F, 1F);
            sync();
            return true;
        }
        return super.insert(player, stack);
    }

    @Override
    public InteractionResult take(Player player) {
        if (!output.isEmpty() && !player.isShiftKeyDown()) {
            if (level != null && !level.isClientSide()) player.sendOverlayMessage(Component.translatable("press.gospodarstwo.need_bottle"));
            return InteractionResult.SUCCESS;
        }
        return super.take(player);
    }

    @Override
    protected boolean dropsOutput() {
        return false;
    }

    @Override
    protected SoundEvent workSound() {
        return SoundEvents.WOOD_STEP;
    }

    @Override
    protected String emptyKey() {
        return "press.gospodarstwo.empty";
    }
}
