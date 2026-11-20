package pl.kubalte.gospodarstwo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.craft.Dyes;

public class DyeVatBlockEntity extends BlockEntity {
    /** Things that take colour in the vat (yarn, cloth; other mods can add to the tag). */
    public static final TagKey<Item> DYEABLE = TagKey.create(net.minecraft.core.registries.Registries.ITEM, Gospodarstwo.id("vat_dyeable"));
    public static final int MAX_CHARGES = 64, PER_DYE = 8;

    private boolean water;
    private int color = -1;
    private int charges;

    public DyeVatBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.DYE_VAT_BE.get(), pos, state);
    }

    public boolean hasWater() {
        return water;
    }

    /** Colour of the dye bath, or -1 for clean water. */
    public int color() {
        return charges > 0 ? color : -1;
    }

    public int charges() {
        return charges;
    }

    public InteractionResult use(Player player, InteractionHand hand, ItemStack stack) {
        if (level == null) return InteractionResult.PASS;
        boolean server = !level.isClientSide();
        if (stack.is(Items.WATER_BUCKET)) {
            if (server) {
                water = true;
                color = -1;
                charges = 0;
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1F, 1F);
                sync();
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.BUCKET) && water) {
            if (server) {
                boolean clean = charges == 0;
                water = false;
                color = -1;
                charges = 0;
                if (clean && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                    ItemStack b = new ItemStack(Items.WATER_BUCKET);
                    if (!player.getInventory().add(b)) player.drop(b, false);
                }
                level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1F, 1F);
                sync();
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(DYEABLE)) {
            if (!water) {
                if (server) player.sendOverlayMessage(Component.translatable("vat.gospodarstwo.no_water"));
                return InteractionResult.SUCCESS;
            }
            if (server) {
                if (charges == 0) {
                    // clean water washes the colour out
                    if (Dyes.colorOf(stack) < 0) return InteractionResult.SUCCESS;
                    Dyes.setColor(stack, -1);
                } else {
                    int n = Math.min(stack.getCount(), charges);
                    if (n == stack.getCount()) Dyes.setColor(stack, color);
                    else {
                        ItemStack dyed = stack.split(n);
                        Dyes.setColor(dyed, color);
                        if (!player.getInventory().add(dyed)) player.drop(dyed, false);
                    }
                    charges -= n;
                }
                level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8F, 0.8F);
                ((ServerLevel) level).sendParticles(ParticleTypes.SPLASH, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8,
                        worldPosition.getZ() + 0.5, 10, 0.25, 0.05, 0.25, 0.05);
                sync();
            }
            return InteractionResult.SUCCESS;
        }
        int dye = Dyes.dyeColor(stack);
        if (dye >= 0) {
            if (!water) {
                if (server) player.sendOverlayMessage(Component.translatable("vat.gospodarstwo.no_water"));
                return InteractionResult.SUCCESS;
            }
            if (charges >= MAX_CHARGES) return InteractionResult.SUCCESS;
            if (server) {
                color = charges == 0 ? dye : Dyes.mix(color, dye, Math.max(1, charges / PER_DYE));
                charges = Math.min(MAX_CHARGES, charges + PER_DYE);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level.playSound(null, worldPosition, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1F, 1F);
                sync();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult inspect(Player player) {
        if (level != null && !level.isClientSide()) {
            String key = !water ? "vat.gospodarstwo.empty" : charges == 0 ? "vat.gospodarstwo.clean" : "vat.gospodarstwo.dye";
            player.sendOverlayMessage(Component.translatable(key, charges));
        }
        return InteractionResult.SUCCESS;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        water = in.getBooleanOr("water", false);
        color = in.getIntOr("color", -1);
        charges = in.getIntOr("charges", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("water", water);
        out.putInt("color", color);
        out.putInt("charges", charges);
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
