package pl.kubalte.gospodarstwo.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/**
 * Konewka: fill it from water, then water a 3x3 patch: crops often grow a stage, saplings and bushes sometimes grow,
 * farmland gets wet. It also waters bonsai pots.
 */
public class WateringCanItem extends Item {
    public WateringCanItem(Properties props) {
        super(props);
    }

    public static int water(ItemStack s) {
        Integer w = s.get(Gospodarstwo.WATER.get());
        return w == null ? 0 : w;
    }

    public static void setWater(ItemStack s, int w) {
        s.set(Gospodarstwo.WATER.get(), Math.max(0, w));
    }

    public static int max() {
        return GConfig.CAN_USES.get();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            setWater(stack, max());
            level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        Player player = ctx.getPlayer();
        if (water(stack) <= 0) {
            if (player != null && !level.isClientSide()) player.sendOverlayMessage(Component.translatable("can.gospodarstwo.empty"));
            return InteractionResult.PASS;
        }
        BlockPos c = ctx.getClickedPos();
        if (!(level.getBlockState(c).getBlock() instanceof CropBlock) && !(level.getBlockState(c).getBlock() instanceof BonemealableBlock)) c = c.above();
        if (level instanceof ServerLevel sl) {
            waterArea(sl, c, sl.getRandom());
            if (player == null || !player.getAbilities().instabuild) setWater(stack, water(stack) - 1);
            level.playSound(null, c, SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 0.6F, 1.6F);
            if (player != null) player.getCooldowns().addCooldown(stack, 8);
        }
        return InteractionResult.SUCCESS;
    }

    /** Waters the 3x3 around c (the layer of the plants, with the soil below). */
    public static void waterArea(ServerLevel level, BlockPos c, RandomSource r) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos p = c.offset(dx, 0, dz);
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof CropBlock crop) {
                if (!crop.isMaxAge(s) && r.nextFloat() < 0.4F) level.setBlock(p, crop.getStateForAge(crop.getAge(s) + 1), 2);
            } else if (s.getBlock() instanceof BonemealableBlock b && r.nextFloat() < 0.12F && b.isValidBonemealTarget(level, p, s)) {
                b.performBonemeal(level, r, p, s);
            }
            BlockState soil = level.getBlockState(p.below());
            if (soil.getBlock() instanceof FarmBlock) level.setBlock(p.below(), soil.setValue(FarmBlock.MOISTURE, 7), 2);
            level.sendParticles(ParticleTypes.SPLASH, p.getX() + 0.5, p.getY() + 0.3, p.getZ() + 0.5, 6, 0.3, 0.05, 0.3, 0.1);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13F * water(stack) / Math.max(1, max())), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3F76E4;
    }
}
