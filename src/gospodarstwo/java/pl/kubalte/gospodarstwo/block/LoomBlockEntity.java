package pl.kubalte.gospodarstwo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;

public class LoomBlockEntity extends BlockEntity {
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private boolean weaving;
    /** Client only: ticks of weaving animation. */
    public float animTicks;

    public LoomBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.LOOM_BE.get(), pos, state);
    }

    public ItemStack input() {
        return input;
    }

    public ItemStack output() {
        return output;
    }

    public boolean weaving() {
        return weaving;
    }

    public int progress() {
        return progress;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LoomBlockEntity be) {
        if (level.isClientSide()) {
            if (be.weaving) be.animTicks += 1F;
            return;
        }
        LoomRecipes.Recipe r = LoomRecipes.find(be.input);
        boolean can = r != null && be.input.getCount() >= r.count()
                && (be.output.isEmpty() || be.output.is(r.output().get()) && be.output.getCount() < be.output.getMaxStackSize());
        if (can != be.weaving) {
            be.weaving = can;
            if (!can) be.progress = 0;
            be.sync();
        }
        if (!can) return;
        be.progress++;
        if (be.progress % 20 == 10) level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.25F, 1.6F);
        if (be.progress >= GConfig.LOOM_TICKS.get()) {
            be.progress = 0;
            be.input.shrink(r.count());
            if (be.input.isEmpty()) be.input = ItemStack.EMPTY;
            if (be.output.isEmpty()) be.output = new ItemStack(r.output().get());
            else be.output.grow(1);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
            be.sync();
        } else {
            be.setChanged();
        }
    }

    /** Right-click with yarn: puts it in (one kind at a time). */
    public boolean insert(Player player, ItemStack stack) {
        LoomRecipes.Recipe r = LoomRecipes.find(stack);
        if (r == null) return false;
        if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, stack)) {
            if (level != null && !level.isClientSide()) player.sendOverlayMessage(Component.translatable("loom.gospodarstwo.other_yarn"));
            return true;
        }
        if (level != null && !level.isClientSide()) {
            int room = stack.getMaxStackSize() - input.getCount();
            int move = Math.min(room, stack.getCount());
            if (move <= 0) return true;
            if (input.isEmpty()) input = stack.copyWithCount(move);
            else input.grow(move);
            if (!player.getAbilities().instabuild) stack.shrink(move);
            level.playSound(null, worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.6F, 1.3F);
            sync();
        }
        return true;
    }

    /** Empty hand: takes the cloth; sneaking (or no cloth): takes back the yarn. */
    public InteractionResult take(Player player) {
        if (level == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack give;
        if (!output.isEmpty() && !player.isShiftKeyDown()) {
            give = output;
            output = ItemStack.EMPTY;
        } else if (!input.isEmpty()) {
            give = input;
            input = ItemStack.EMPTY;
            progress = 0;
        } else {
            player.sendOverlayMessage(Component.translatable("loom.gospodarstwo.empty"));
            return InteractionResult.SUCCESS;
        }
        if (!player.getInventory().add(give)) player.drop(give, false);
        level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
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
        if (level != null) {
            if (!input.isEmpty()) Block.popResource(level, pos, input);
            if (!output.isEmpty()) Block.popResource(level, pos, output);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        input = in.read("input", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        progress = in.getIntOr("progress", 0);
        weaving = in.getBooleanOr("weaving", false);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("input", ItemStack.OPTIONAL_CODEC, input);
        out.store("output", ItemStack.OPTIONAL_CODEC, output);
        out.putInt("progress", progress);
        out.putBoolean("weaving", weaving);
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
