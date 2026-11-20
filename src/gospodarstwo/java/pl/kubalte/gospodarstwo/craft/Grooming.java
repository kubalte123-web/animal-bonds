package pl.kubalte.gospodarstwo.craft;

import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** Brushing with the curry comb and shearing llamas. */
public final class Grooming {
    private Grooming() {}

    /** What brushing gives: horse hair, cashmere (goats), camel wool, dog hair (tamed wolves). */
    public static Supplier<? extends Item> brushProduct(LivingEntity e) {
        EntityType<?> t = e.getType();
        if (t == EntityTypes.HORSE || t == EntityTypes.DONKEY || t == EntityTypes.MULE || t == EntityTypes.SKELETON_HORSE
                || t == EntityTypes.ZOMBIE_HORSE) return Gospodarstwo.HORSE_HAIR;
        if (t == EntityTypes.GOAT) return Gospodarstwo.CASHMERE;
        if (t == EntityTypes.CAMEL) return Gospodarstwo.CAMEL_WOOL;
        if (t == EntityTypes.WOLF && e instanceof TamableAnimal ta && ta.isTame()) return Gospodarstwo.DOG_HAIR;
        return null;
    }

    public static boolean ready(Mob mob) {
        return mob.level().getGameTime() - mob.getData(Gospodarstwo.LAST_HARVEST.get()) >= GConfig.HARVEST_COOLDOWN.get();
    }

    public static InteractionResult brush(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Supplier<? extends Item> product = brushProduct(target);
        if (product == null || !(target instanceof Mob mob) || mob.isBaby()) return InteractionResult.PASS;
        if (!(player.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() * 0.8, mob.getZ(), 3, 0.3, 0.2, 0.3, 0);
        level.playSound(null, mob, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1.0F, 1.1F);
        if (ready(mob)) {
            mob.setData(Gospodarstwo.LAST_HARVEST.get(), level.getGameTime());
            mob.spawnAtLocation(level, new ItemStack(product.get(), 1 + mob.getRandom().nextInt(2)));
        } else {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("groom.gospodarstwo.groomed"));
        }
        if (player instanceof ServerPlayer sp) stack.hurtAndBreak(1, level, sp, i -> {});
        return InteractionResult.SUCCESS;
    }

    /** Llamas can be sheared for llama wool. */
    public static InteractionResult shearLlama(ItemStack stack, Player player, Mob llama) {
        if (llama.isBaby()) return InteractionResult.PASS;
        if (!(player.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        if (!ready(llama)) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("breed.gospodarstwo.regrowing"));
            return InteractionResult.SUCCESS;
        }
        llama.setData(Gospodarstwo.LAST_HARVEST.get(), level.getGameTime());
        llama.spawnAtLocation(level, new ItemStack(Gospodarstwo.LLAMA_WOOL.get(), 1 + llama.getRandom().nextInt(3)));
        level.playSound(null, llama, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (player instanceof ServerPlayer sp) stack.hurtAndBreak(1, level, sp, i -> {});
        return InteractionResult.SUCCESS;
    }
}
