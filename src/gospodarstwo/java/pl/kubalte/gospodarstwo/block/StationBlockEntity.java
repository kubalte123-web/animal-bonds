package pl.kubalte.gospodarstwo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A work station that turns N of one input into one product over time (loom, spinning wheel...). Right-click with the
 * material puts it in; empty hand takes the product; sneaking with an empty hand takes the material back.
 */
public abstract class StationBlockEntity extends BlockEntity {
    /** A step of work: how many inputs it uses, what it makes and how long it takes. */
    public record Job(int count, ItemStack result, int ticks) {}

    protected ItemStack input = ItemStack.EMPTY;
    protected ItemStack output = ItemStack.EMPTY;
    protected int progress;
    protected boolean working;
    /** Client only: ticks of animation while working. */
    public float animTicks;

    protected StationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The job for this input, or null when it is not accepted. */
    public abstract Job job(ItemStack input);

    protected String emptyKey() {
        return "station.gospodarstwo.empty";
    }

    protected SoundEvent workSound() {
        return SoundEvents.WOOD_HIT;
    }

    public ItemStack input() {
        return input;
    }

    public ItemStack output() {
        return output;
    }

    public boolean working() {
        return working;
    }

    /** Tests: jump the work forward. */
    public void debugProgress(int p) {
        progress = p;
        sync();
    }

    public int progress() {
        return progress;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StationBlockEntity be) {
        if (level.isClientSide()) {
            if (be.working) be.animTicks += 1F;
            return;
        }
        Job j = be.job(be.input);
        boolean can = j != null && be.input.getCount() >= j.count()
                && (be.output.isEmpty() || ItemStack.isSameItemSameComponents(be.output, j.result())
                && be.output.getCount() + j.result().getCount() <= be.output.getMaxStackSize());
        if (can != be.working) {
            be.working = can;
            if (!can) be.progress = 0;
            be.sync();
        }
        if (!can) return;
        be.progress++;
        if (be.progress % 20 == 10) level.playSound(null, pos, be.workSound(), SoundSource.BLOCKS, 0.25F, 1.6F);
        if (be.progress >= j.ticks()) {
            be.progress = 0;
            be.input.shrink(j.count());
            if (be.input.isEmpty()) be.input = ItemStack.EMPTY;
            if (be.output.isEmpty()) be.output = j.result().copy();
            else be.output.grow(j.result().getCount());
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
            be.sync();
        } else if (be.syncEvery() > 0 && be.progress % be.syncEvery() == 0) {
            be.sync();
        } else {
            be.setChanged();
        }
    }

    /** How often (ticks of progress) to send the progress to clients; 0 = only when starting/stopping. */
    protected int syncEvery() {
        return 0;
    }

    /** Right-click with material: puts it in (one kind at a time). */
    public boolean insert(Player player, ItemStack stack) {
        if (job(stack) == null) return false;
        return insertAny(player, stack);
    }

    /** Puts the material in without checking whether it can be worked right now. */
    protected boolean insertAny(Player player, ItemStack stack) {
        if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, stack)) {
            if (level != null && !level.isClientSide()) player.sendOverlayMessage(Component.translatable("station.gospodarstwo.other_input"));
            return true;
        }
        if (level != null && !level.isClientSide()) {
            int move = Math.min(stack.getMaxStackSize() - input.getCount(), stack.getCount());
            if (move <= 0) return true;
            if (input.isEmpty()) input = stack.copyWithCount(move);
            else input.grow(move);
            if (!player.getAbilities().instabuild) stack.shrink(move);
            level.playSound(null, worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.6F, 1.3F);
            sync();
        }
        return true;
    }

    /** For helpers (the goblin): puts the material in without a player; returns how many were taken from s. */
    public int offer(ItemStack s) {
        if (!accepts(s)) return 0;
        if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, s)) return 0;
        int move = Math.min(s.getMaxStackSize() - input.getCount(), s.getCount());
        if (move <= 0) return 0;
        if (input.isEmpty()) input = s.copyWithCount(move);
        else input.grow(move);
        if (level != null) level.playSound(null, worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.6F, 1.3F);
        sync();
        return move;
    }

    /** For helpers: whether s is something this station works with (ignoring fire and the current contents). */
    public boolean accepts(ItemStack s) {
        return job(s) != null;
    }

    /** For helpers: takes the finished product out (empty if none). */
    public ItemStack takeOutput() {
        ItemStack o = output;
        output = ItemStack.EMPTY;
        if (!o.isEmpty()) sync();
        return o;
    }

    /** Empty hand: takes the product; sneaking (or no product): takes back the material. */
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
            player.sendOverlayMessage(Component.translatable(emptyKey()));
            return InteractionResult.SUCCESS;
        }
        if (!player.getInventory().add(give)) player.drop(give, false);
        level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
        sync();
        return InteractionResult.SUCCESS;
    }

    protected void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Whether the product drops when the block is broken (not for things that need a container to be taken). */
    protected boolean dropsOutput() {
        return true;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            if (!input.isEmpty()) Block.popResource(level, pos, input);
            if (!output.isEmpty() && dropsOutput()) Block.popResource(level, pos, output);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        input = in.read("input", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        progress = in.getIntOr("progress", 0);
        working = in.getBooleanOr("weaving", false);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("input", ItemStack.OPTIONAL_CODEC, input);
        out.store("output", ItemStack.OPTIONAL_CODEC, output);
        out.putInt("progress", progress);
        out.putBoolean("weaving", working);
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
