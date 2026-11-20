package pl.kubalte.gospodarstwo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

public class BonsaiPotBlockEntity extends BlockEntity {
    /** Growth is kept in thousandths of the full bonsai. */
    public static final int FULL = 1000;
    /** Without at least this many trims the tree stops as a shaggy bush at {@link #WILD_CAP}. */
    public static final int TRIMS_NEEDED = 2;
    public static final int WILD_CAP = 750;

    private BonsaiData data = BonsaiData.EMPTY;

    public BonsaiPotBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.BONSAI_BE.get(), pos, state);
    }

    public BonsaiData data() {
        return data;
    }

    public void setData(BonsaiData d) {
        data = d;
        changed();
    }

    public boolean hasTree() {
        return !data.sapling().isEmpty();
    }

    /** 0 sapling, 1 young tree, 2 being shaped, 3 finished bonsai. */
    public static int stage(BonsaiData d) {
        if (d.sapling().isEmpty()) return -1;
        if (d.growth() >= FULL) return 3;
        if (d.growth() >= 400) return 2;
        if (d.growth() >= 120) return 1;
        return 0;
    }

    public static boolean isSapling(ItemStack s) {
        return s.is(ItemTags.SAPLINGS) || s.is(Items.MANGROVE_PROPAGULE) || s.is(Items.AZALEA) || s.is(Items.FLOWERING_AZALEA);
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BonsaiPotBlockEntity be) {
        if (level.getGameTime() % 20 == 0) be.step(level, 20);
    }

    /** Advances the tree by the given number of ticks (public for tests). */
    public void step(Level level, int ticks) {
        BonsaiPotBlockEntity be = this;
        if (!be.hasTree()) return;
        BonsaiData d = be.data;
        int water = Math.max(0, d.water() - ticks);
        int growth = d.growth();
        int cap = d.trims() >= TRIMS_NEEDED ? FULL : WILD_CAP;
        if (d.water() > 0 && growth < cap) {
            // 20 ticks of growth; a trimmed tree puts its strength into the shape and grows a bit faster
            float step = (float) ticks * FULL / GConfig.BONSAI_TICKS.get() * (1F + 0.25F * Math.min(d.trims(), 3));
            int add = (int) step + (level.getRandom().nextFloat() < step % 1F ? 1 : 0);
            growth = Math.min(cap, growth + add);
        }
        boolean visible = stage(d) != stage(new BonsaiData(d.sapling(), growth, d.trims(), d.seed(), water, d.lastTrimGrowth()))
                || (d.water() > 0) != (water > 0) || level.getGameTime() % 400 == 0;
        be.data = new BonsaiData(d.sapling(), growth, d.trims(), d.seed(), water, d.lastTrimGrowth());
        if (visible) be.changed();
        else be.setChanged();
    }

    public InteractionResult use(Player player, InteractionHand hand, ItemStack stack) {
        if (level == null) return InteractionResult.PASS;
        BonsaiData d = data;
        if (!hasTree()) {
            if (!isSapling(stack)) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                setData(new BonsaiData(id, 0, 0, level.getRandom().nextLong(), 0, 0));
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level.playSound(null, worldPosition, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1F, 1F);
            }
            return InteractionResult.SUCCESS;
        }
        boolean can = stack.is(Gospodarstwo.WATERING_CAN.get()) && pl.kubalte.gospodarstwo.tools.WateringCanItem.water(stack) > 0;
        if (stack.is(Items.WATER_BUCKET) || (stack.is(Items.POTION) && isWaterBottle(stack)) || can) {
            if (!level.isClientSide()) {
                if (can && !player.getAbilities().instabuild)
                    pl.kubalte.gospodarstwo.tools.WateringCanItem.setWater(stack, pl.kubalte.gospodarstwo.tools.WateringCanItem.water(stack) - 1);
                setData(new BonsaiData(d.sapling(), d.growth(), d.trims(), d.seed(), GConfig.BONSAI_WATER.get(), d.lastTrimGrowth()));
                level.playSound(null, worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1F, 1F);
                ((ServerLevel) level).sendParticles(ParticleTypes.SPLASH, worldPosition.getX() + 0.5, worldPosition.getY() + 0.4,
                        worldPosition.getZ() + 0.5, 12, 0.25, 0.05, 0.2, 0.1);
                if (stack.is(Items.POTION) && !player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.GLASS_BOTTLE));
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.SHEARS)) {
            int st = stage(d);
            if (st < 1 || d.growth() - d.lastTrimGrowth() < 150 || d.trims() >= 3) {
                if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("bonsai.gospodarstwo.no_trim"));
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                setData(new BonsaiData(d.sapling(), d.growth(), d.trims() + 1, d.seed(), d.water(), d.growth()));
                level.playSound(null, worldPosition, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.4F);
                ((ServerLevel) level).sendParticles(ParticleTypes.COMPOSTER, worldPosition.getX() + 0.5, worldPosition.getY() + 0.7,
                        worldPosition.getZ() + 0.5, 10, 0.25, 0.2, 0.25, 0.0);
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) stack.hurtAndBreak(1, (ServerLevel) level, sp, i -> {});
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.BONE_MEAL)) {
            if (!level.isClientSide()) {
                int cap = d.trims() >= TRIMS_NEEDED ? FULL : WILD_CAP;
                setData(new BonsaiData(d.sapling(), Math.min(cap, d.growth() + 60), d.trims(), d.seed(), d.water(), d.lastTrimGrowth()));
                if (!player.getAbilities().instabuild) stack.shrink(1);
                ((ServerLevel) level).sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6,
                        worldPosition.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static boolean isWaterBottle(ItemStack stack) {
        var c = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        return c != null && c.is(net.minecraft.world.item.alchemy.Potions.WATER);
    }

    /** Empty hand: tells how the tree is doing. */
    public InteractionResult inspect(Player player) {
        if (level == null || level.isClientSide()) return InteractionResult.SUCCESS;
        if (!hasTree()) {
            player.sendOverlayMessage(Component.translatable("bonsai.gospodarstwo.empty"));
            return InteractionResult.SUCCESS;
        }
        BonsaiData d = data;
        int pct = d.growth() * 100 / FULL;
        Component water = Component.translatable(d.water() > 0 ? "bonsai.gospodarstwo.watered" : "bonsai.gospodarstwo.dry");
        String key = d.growth() >= FULL ? "bonsai.gospodarstwo.done"
                : d.trims() < TRIMS_NEEDED && d.growth() >= WILD_CAP ? "bonsai.gospodarstwo.needs_trim" : "bonsai.gospodarstwo.status";
        player.sendOverlayMessage(Component.translatable(key, pct, water, d.trims(), TRIMS_NEEDED));
        return InteractionResult.SUCCESS;
    }

    // ---------------- saving & syncing ----------------

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        data = new BonsaiData(in.getStringOr("sapling", ""), in.getIntOr("growth", 0), in.getIntOr("trims", 0), in.getLongOr("seed", 0L),
                in.getIntOr("water", 0), in.getIntOr("last_trim", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putString("sapling", data.sapling());
        out.putInt("growth", data.growth());
        out.putInt("trims", data.trims());
        out.putLong("seed", data.seed());
        out.putInt("water", data.water());
        out.putInt("last_trim", data.lastTrimGrowth());
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        BonsaiData d = components.get(Gospodarstwo.BONSAI_DATA.get());
        if (d != null) data = d;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (hasTree()) builder.set(Gospodarstwo.BONSAI_DATA.get(), data);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput out) {
        super.removeComponentsFromTag(out);
        out.discard("sapling");
        out.discard("growth");
        out.discard("trims");
        out.discard("seed");
        out.discard("water");
        out.discard("last_trim");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
