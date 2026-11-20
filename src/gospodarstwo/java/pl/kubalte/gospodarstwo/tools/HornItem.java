package pl.kubalte.gospodarstwo.tools;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Róg myśliwski calls your animals (tamed pets and horses) from 32 blocks; róg bojowy (from a ravager horn) scares
 * monsters away for a while.
 */
public class HornItem extends Item {
    private final boolean war;

    public HornItem(Properties props, boolean war) {
        super(props);
        this.war = war;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            int n = war ? scare(sl, player) : call(sl, player);
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.PLAYERS, 3F, war ? 0.5F : 0.9F);
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.PLAYERS, 2F, war ? 0.53F : 1.2F);
            player.getCooldowns().addCooldown(stack, war ? 600 : 100);
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.SUCCESS;
    }

    /** Pets and horses of the player come running; returns how many heard it. */
    public static int call(ServerLevel level, Player player) {
        int n = 0;
        for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(player.blockPosition()).inflate(32),
                m -> m instanceof OwnableEntity o && o.getOwner() == player)) {
            if (m instanceof TamableAnimal t && t.isOrderedToSit()) {
                t.setOrderedToSit(false);
                t.setInSittingPose(false);
            }
            // too far to find a path (beyond the animal's follow range): it comes running out of the bushes next to you
            if (!m.getNavigation().moveTo(player, 1.4)) {
                double a = level.getRandom().nextDouble() * Math.PI * 2;
                m.teleportTo(player.getX() + Math.cos(a) * 2.5, player.getY(), player.getZ() + Math.sin(a) * 2.5);
                m.getNavigation().stop();
            }
            level.sendParticles(ParticleTypes.NOTE, m.getX(), m.getEyeY() + 0.5, m.getZ(), 1, 0, 0, 0, 0.5);
            n++;
        }
        return n;
    }

    /** Monsters close by lose their target, get weak and slow and are pushed away. */
    public static int scare(ServerLevel level, Player player) {
        int n = 0;
        for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(player.blockPosition()).inflate(16), m -> m instanceof Enemy)) {
            m.setTarget(null);
            m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            m.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
            net.minecraft.world.phys.Vec3 away = m.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(1.1);
            m.push(away.x, 0.35, away.z);
            m.hurtMarked = true;
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, m.getX(), m.getEyeY() + 0.3, m.getZ(), 2, 0.2, 0.2, 0.2, 0);
            n++;
        }
        return n;
    }
}
