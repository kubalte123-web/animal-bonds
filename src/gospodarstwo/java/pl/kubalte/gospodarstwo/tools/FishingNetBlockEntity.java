package pl.kubalte.gospodarstwo.tools;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** Sieć rybacka: set in water with open water around, it catches a fish now and then (up to 6). Right-click to empty it. */
public class FishingNetBlockEntity extends BlockEntity {
    public static final int MAX = 6;
    private final List<ItemStack> catches = new ArrayList<>();

    public FishingNetBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.FISHING_NET_BE.get(), pos, state);
    }

    public List<ItemStack> catches() {
        return catches;
    }

    /** Water blocks in the 5x5 around (the net needs open water to catch anything). */
    public int water() {
        if (level == null) return 0;
        int n = 0;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
            if (level.getFluidState(worldPosition.offset(dx, 0, dz)).is(FluidTags.WATER)) n++;
        return n;
    }

    static Item pick(RandomSource r) {
        int x = r.nextInt(100);
        if (x < 50) return Items.COD;
        if (x < 75) return Items.SALMON;
        if (x < 85) return Items.PUFFERFISH;
        if (x < 88) return Items.TROPICAL_FISH;
        if (x < 94) return Items.KELP;
        if (x < 97) return Items.INK_SAC;
        return Items.NAUTILUS_SHELL;
    }

    /** Catches one thing if there is room; returns whether it did. */
    public boolean catchOne() {
        if (level == null || catches.size() >= MAX) return false;
        catches.add(new ItemStack(pick(level.getRandom())));
        sync();
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FishingNetBlockEntity be) {
        if (level.isClientSide() || level.getGameTime() % 100 != pos.asLong() % 100) return;
        if (be.water() < 12) return;
        if (level.getRandom().nextFloat() < GConfig.NET_CHANCE.get()) {
            if (be.catchOne()) level.playSound(null, pos, SoundEvents.FISH_SWIM, SoundSource.BLOCKS, 0.6F, 1F);
        }
    }

    /** For helpers: everything caught so far. */
    public java.util.List<ItemStack> takeAll() {
        java.util.List<ItemStack> l = new ArrayList<>(catches);
        catches.clear();
        if (!l.isEmpty()) sync();
        return l;
    }

    public InteractionResult take(Player player) {
        if (level == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (catches.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(water() < 12 ? "net.gospodarstwo.no_water" : "net.gospodarstwo.empty"));
            return InteractionResult.SUCCESS;
        }
        for (ItemStack s : catches) if (!player.getInventory().add(s)) player.drop(s, false);
        catches.clear();
        level.playSound(null, worldPosition, SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.BLOCKS, 1F, 1F);
        sync();
        return InteractionResult.SUCCESS;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) for (ItemStack s : catches) Block.popResource(level, pos, s);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        catches.clear();
        in.read("catches", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l -> l.forEach(s -> {
            if (!s.isEmpty()) catches.add(s);
        }));
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("catches", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(catches));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
