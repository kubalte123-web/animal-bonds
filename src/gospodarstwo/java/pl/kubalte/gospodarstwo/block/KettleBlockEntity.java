package pl.kubalte.gospodarstwo.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;

public class KettleBlockEntity extends StationBlockEntity {
    /** N of one ingredient -> M of a product; colour = colour of the brew drawn in the pot. */
    public record Brew(Predicate<ItemStack> input, int count, Supplier<? extends Item> result, int resultCount, int color) {}

    private static final List<Brew> BREWS = new ArrayList<>();

    public static void add(Brew b) {
        BREWS.add(b);
    }

    static {
        add(new Brew(s -> s.is(Items.HONEYCOMB), 1, Gospodarstwo.BEESWAX, 2, 0xE8B23A));
        add(new Brew(s -> s.is(Items.BONE), 3, Gospodarstwo.GLUE, 1, 0xC9B48A));
        add(new Brew(s -> s.is(Gospodarstwo.BONE_MARROW.get()) || s.is(Gospodarstwo.POULTRY_BONE.get()), 2, Gospodarstwo.GLUE, 1, 0xC9B48A));
        add(new Brew(s -> s.is(Gospodarstwo.SWIM_BLADDER.get()), 1, Gospodarstwo.GLUE, 2, 0xD8D0B0));
        add(new Brew(s -> s.is(Gospodarstwo.FATBACK.get()), 2, Gospodarstwo.LARD, 1, 0xF2EBDD));
    }

    public static Brew find(ItemStack s) {
        if (s.isEmpty()) return null;
        for (Brew b : BREWS) if (b.input().test(s)) return b;
        return null;
    }

    /** Colour of what is in the pot: the brew being cooked, or the finished product; -1 = empty. */
    public int liquidColor() {
        Brew b = find(input);
        if (b != null) return b.color();
        if (!output.isEmpty()) for (Brew x : BREWS) if (output.is(x.result().get())) return x.color();
        return -1;
    }

    public KettleBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.KETTLE_BE.get(), pos, state);
    }

    /** A fire under the kettle: lit campfire, fire, magma or lava. */
    public boolean heated() {
        if (level == null) return false;
        BlockState below = level.getBlockState(worldPosition.below());
        if (below.getBlock() instanceof CampfireBlock) return below.getValue(CampfireBlock.LIT);
        return below.is(BlockTags.FIRE) || below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.LAVA);
    }

    @Override
    public Job job(ItemStack in) {
        Brew b = find(in);
        if (b == null || !heated()) return null;
        return new Job(b.count(), new ItemStack(b.result().get(), b.resultCount()), GConfig.KETTLE_TICKS.get());
    }

    @Override
    public boolean insert(Player player, ItemStack stack) {
        if (find(stack) == null) return false;
        if (!heated() && level != null && !level.isClientSide()) player.sendOverlayMessage(Component.translatable("kettle.gospodarstwo.cold"));
        return super.insertAny(player, stack);
    }

    @Override
    protected String emptyKey() {
        return "kettle.gospodarstwo.empty";
    }

    @Override
    protected net.minecraft.sounds.SoundEvent workSound() {
        return net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT;
    }
}
