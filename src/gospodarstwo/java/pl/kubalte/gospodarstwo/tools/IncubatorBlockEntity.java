package pl.kubalte.gospodarstwo.tools;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
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
import pl.kubalte.gospodarstwo.breed.Breed;
import pl.kubalte.gospodarstwo.breed.BreedEvents;

/** Wylęgarnia: up to 4 eggs of one kind are kept warm under a lamp; after half a day every egg hatches into a chick. */
public class IncubatorBlockEntity extends BlockEntity {
    public static final int MAX = 4;
    private ItemStack eggs = ItemStack.EMPTY;
    private int progress;

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.INCUBATOR_BE.get(), pos, state);
    }

    /** What hatches from an egg: entity type, breed (or null), shell colour; null if it is not an egg we hatch. */
    public record Hatch(Supplier<? extends EntityType<? extends Mob>> type, Breed breed, int color) {}

    public static Hatch hatchOf(ItemStack s) {
        if (s.isEmpty()) return null;
        if (s.is(Gospodarstwo.DUCK_EGG.get())) return new Hatch(Gospodarstwo.DUCK, null, 0xDDE8D8);
        if (s.is(Gospodarstwo.GOOSE_EGG.get())) return new Hatch(Gospodarstwo.GOOSE, null, 0xF4F2EA);
        if (s.is(Gospodarstwo.GREEN_EGG.get())) return new Hatch(() -> EntityTypes.CHICKEN, Breed.GREENLEG, 0x8FBF7F);
        if (s.is(Items.EGG)) return new Hatch(() -> EntityTypes.CHICKEN, null, 0xF0E0C0);
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        if (id.equals("minecraft:blue_egg")) return new Hatch(() -> EntityTypes.CHICKEN, null, 0xB8D8E8);
        if (id.equals("minecraft:brown_egg")) return new Hatch(() -> EntityTypes.CHICKEN, null, 0xB0784C);
        return null;
    }

    public ItemStack eggs() {
        return eggs;
    }

    public float fraction() {
        return Math.min(1F, progress / (float) Math.max(1, GConfig.INCUBATE_TICKS.get()));
    }

    public int eggColor() {
        Hatch h = hatchOf(eggs);
        return h == null ? -1 : h.color();
    }

    /** For helpers: puts eggs in; returns how many were used. */
    public int offerEggs(ItemStack stack) {
        if (hatchOf(stack) == null || (!eggs.isEmpty() && !ItemStack.isSameItemSameComponents(eggs, stack))) return 0;
        int move = Math.min(MAX - eggs.getCount(), stack.getCount());
        if (move <= 0) return 0;
        if (eggs.isEmpty()) eggs = stack.copyWithCount(move);
        else eggs.grow(move);
        sync();
        return move;
    }

    public InteractionResult use(Player player, ItemStack stack) {
        if (level == null) return InteractionResult.PASS;
        if (!stack.isEmpty()) {
            if (hatchOf(stack) == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!eggs.isEmpty() && !ItemStack.isSameItemSameComponents(eggs, stack)) {
                player.sendOverlayMessage(Component.translatable("incubator.gospodarstwo.other"));
                return InteractionResult.SUCCESS;
            }
            int move = Math.min(MAX - eggs.getCount(), stack.getCount());
            if (move <= 0) return InteractionResult.SUCCESS;
            if (eggs.isEmpty()) eggs = stack.copyWithCount(move);
            else eggs.grow(move);
            if (!player.getAbilities().instabuild) stack.shrink(move);
            level.playSound(null, worldPosition, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
            sync();
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (eggs.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("incubator.gospodarstwo.empty"));
        } else if (player.isShiftKeyDown()) {
            if (!player.getInventory().add(eggs)) player.drop(eggs, false);
            eggs = ItemStack.EMPTY;
            progress = 0;
            sync();
        } else {
            player.sendOverlayMessage(Component.translatable("incubator.gospodarstwo.status", Math.round(fraction() * 100)));
        }
        return InteractionResult.SUCCESS;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IncubatorBlockEntity be) {
        if (level.isClientSide() || be.eggs.isEmpty()) return;
        be.progress++;
        if (be.progress % 400 == 0) be.sync();
        if (be.progress >= GConfig.INCUBATE_TICKS.get()) be.hatch((ServerLevel) level);
        else be.setChanged();
    }

    /** All eggs hatch now; returns how many chicks came out. */
    public int hatch(ServerLevel level) {
        Hatch h = hatchOf(eggs);
        int n = 0;
        if (h != null) for (int i = 0; i < eggs.getCount(); i++) {
            Mob m = h.type().get().create(level, EntitySpawnReason.BREEDING);
            if (m == null) continue;
            m.setPos(worldPosition.getX() + 0.5 + (i % 2 - 0.5) * 0.4, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5 + (i / 2 - 0.5) * 0.4);
            m.setYRot(level.getRandom().nextFloat() * 360F);
            if (m instanceof AgeableMob a) a.setAge(-24000);
            if (h.breed() != null) BreedEvents.setBreed(m, h.breed());
            level.addFreshEntity(m);
            level.sendParticles(ParticleTypes.HEART, m.getX(), m.getY() + 0.5, m.getZ(), 1, 0, 0, 0, 0);
            n++;
        }
        level.playSound(null, worldPosition, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 1F, 1.2F);
        eggs = ItemStack.EMPTY;
        progress = 0;
        sync();
        return n;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !eggs.isEmpty()) Block.popResource(level, pos, eggs);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        eggs = in.read("eggs", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        progress = in.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("eggs", ItemStack.OPTIONAL_CODEC, eggs);
        out.putInt("progress", progress);
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
