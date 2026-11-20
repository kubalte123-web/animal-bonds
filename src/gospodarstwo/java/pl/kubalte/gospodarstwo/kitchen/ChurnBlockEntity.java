package pl.kubalte.gospodarstwo.kitchen;

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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** Maselnica: pour in a bucket of milk, then churn it by hand (right-clicks) until it turns into butter. */
public class ChurnBlockEntity extends BlockEntity {
    private boolean milk;
    private int strokes, butter;
    /** Game time of the last stroke (client: plunger animation). */
    public long lastStroke = -100;

    public ChurnBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.CHURN_BE.get(), pos, state);
    }

    public boolean milk() {
        return milk;
    }

    public int butter() {
        return butter;
    }

    public float progress() {
        return strokes / (float) Math.max(1, GConfig.CHURN_STROKES.get());
    }

    public InteractionResult use(Player player, ItemStack stack) {
        if (level == null) return InteractionResult.PASS;
        if (stack.is(Items.MILK_BUCKET)) {
            if (milk || butter > 0) return InteractionResult.SUCCESS;
            if (!level.isClientSide()) {
                milk = true;
                strokes = 0;
                if (!player.getAbilities().instabuild) player.setItemInHand(player.getMainHandItem() == stack ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET));
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1F, 1F);
                sync();
            }
            return InteractionResult.SUCCESS;
        }
        if (!stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (butter > 0) {
            ItemStack b = new ItemStack(Gospodarstwo.BUTTER.get(), butter);
            butter = 0;
            if (!player.getInventory().add(b)) player.drop(b, false);
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1F);
            sync();
            return InteractionResult.SUCCESS;
        }
        if (!milk) {
            player.sendOverlayMessage(Component.translatable("churn.gospodarstwo.empty"));
            return InteractionResult.SUCCESS;
        }
        churn();
        return InteractionResult.SUCCESS;
    }

    /** One stroke of the plunger. */
    public void churn() {
        if (level == null || !milk) return;
        strokes++;
        lastStroke = level.getGameTime();
        level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.4F, 1.6F + level.getRandom().nextFloat() * 0.2F);
        if (strokes >= GConfig.CHURN_STROKES.get()) {
            milk = false;
            strokes = 0;
            butter = 2;
            level.playSound(null, worldPosition, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 1F, 1F);
        }
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && butter > 0) Block.popResource(level, pos, new ItemStack(Gospodarstwo.BUTTER.get(), butter));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        milk = in.getBooleanOr("milk", false);
        strokes = in.getIntOr("strokes", 0);
        butter = in.getIntOr("butter", 0);
        lastStroke = in.getLongOr("last_stroke", -100L);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("milk", milk);
        out.putInt("strokes", strokes);
        out.putInt("butter", butter);
        out.putLong("last_stroke", lastStroke);
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
