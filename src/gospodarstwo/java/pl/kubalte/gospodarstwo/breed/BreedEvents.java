package pl.kubalte.gospodarstwo.breed;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** Rolling breeds for new animals, inheritance, shearing and the breeds' own products. */
public final class BreedEvents {
    private BreedEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(BreedEvents::onJoin);
        bus.addListener(BreedEvents::onBaby);
        bus.addListener(BreedEvents::onInteract);
        bus.addListener(BreedEvents::onTick);
    }

    public static Breed breed(Entity e) {
        return e.hasData(Gospodarstwo.BREED.get()) ? Breed.byIndex(e.getData(Gospodarstwo.BREED.get())) : null;
    }

    public static void setBreed(Entity e, Breed b) {
        e.setData(Gospodarstwo.BREED.get(), b == null ? 0 : b.index());
    }

    public static List<Breed> breedsOf(Entity e) {
        List<Breed> out = new ArrayList<>();
        for (Breed b : Breed.values()) if (b.species.get() == e.getType()) out.add(b);
        return out;
    }

    static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob mob) || mob.hasData(Gospodarstwo.BREED.get())) return;
        List<Breed> options = breedsOf(mob);
        if (options.isEmpty()) return;
        Breed pick = null;
        if (!Boolean.getBoolean("gospodarstwo.noRolls") && mob.getRandom().nextDouble() < GConfig.BREED_CHANCE.get()) {
            pick = pickFor(mob, (ServerLevel) e.getLevel(), options, mob.getRandom());
        }
        setBreed(mob, pick);
    }

    /** Highland cows prefer cold places, merinos warm grasslands; otherwise any breed of the species. */
    static Breed pickFor(Mob mob, ServerLevel level, List<Breed> options, RandomSource rnd) {
        float temp = level.getBiome(mob.blockPosition()).value().getBaseTemperature();
        if (options.contains(Breed.HIGHLAND) && temp > 1.0F && rnd.nextBoolean()) return null;
        if (options.contains(Breed.MERINO) && temp < 0.3F && rnd.nextBoolean()) return null;
        return options.get(rnd.nextInt(options.size()));
    }

    static void onBaby(BabyEntitySpawnEvent e) {
        if (!(e.getChild() instanceof Mob child) || breedsOf(child).isEmpty()) return;
        Breed a = breed(e.getParentA()), b = breed(e.getParentB());
        RandomSource r = child.getRandom();
        Breed pick;
        if (a == b) pick = a; // purebred
        else if (a == null || b == null) pick = r.nextBoolean() ? (a != null ? a : b) : null; // half chance
        else pick = r.nextBoolean() ? a : b;
        setBreed(child, pick);
    }

    static void onInteract(PlayerInteractEvent.EntityInteract e) {
        Player p = e.getEntity();
        Entity target = e.getTarget();
        ItemStack stack = e.getItemStack();
        if (stack.is(Items.SHEARS) && (target.getType() == net.minecraft.world.entity.EntityTypes.LLAMA
                || target.getType() == net.minecraft.world.entity.EntityTypes.TRADER_LLAMA) && target instanceof Mob llama) {
            e.setCancellationResult(pl.kubalte.gospodarstwo.craft.Grooming.shearLlama(stack, p, llama));
            e.setCanceled(true);
            return;
        }
        Breed b = breed(target);
        if (b == null) return;
        // sneak + empty hand: tell the breed
        if (stack.isEmpty() && p.isShiftKeyDown()) {
            if (!p.level().isClientSide()) p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("breed.gospodarstwo.is", b.displayName()));
            e.setCancellationResult(InteractionResult.SUCCESS);
            e.setCanceled(true);
            return;
        }
        if (!stack.is(Items.SHEARS) || !(target instanceof Mob mob) || mob.isBaby()) return;
        if (b == Breed.MERINO) {
            // vanilla shears the sheep; the merino also gives fine wool
            if (target instanceof Sheep sheep && !sheep.isSheared() && !p.level().isClientSide()) {
                mob.spawnAtLocation((ServerLevel) p.level(), new ItemStack(b.product.get(), 1 + mob.getRandom().nextInt(2)));
            }
            return;
        }
        if (!b.shearable) return;
        long now = p.level().getGameTime();
        if (now - mob.getData(Gospodarstwo.LAST_HARVEST.get()) < GConfig.HARVEST_COOLDOWN.get()) {
            if (!p.level().isClientSide()) p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("breed.gospodarstwo.regrowing"));
            e.setCancellationResult(InteractionResult.SUCCESS);
            e.setCanceled(true);
            return;
        }
        if (!p.level().isClientSide()) {
            ServerLevel level = (ServerLevel) p.level();
            mob.setData(Gospodarstwo.LAST_HARVEST.get(), now);
            mob.spawnAtLocation(level, new ItemStack(b.product.get(), 1 + mob.getRandom().nextInt(3)));
            level.playSound(null, mob, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (p instanceof net.minecraft.server.level.ServerPlayer sp) stack.hurtAndBreak(1, level, sp, i -> {});
        }
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
    }

    /** Silkies drop down and green-legged hens lay green eggs now and then (about every 5-10 minutes). */
    static void onTick(EntityTickEvent.Post e) {
        Entity ent = e.getEntity();
        if (ent.level().isClientSide() || ent.tickCount % 20 != 0 || !(ent instanceof Mob mob) || mob.isBaby()) return;
        Breed b = breed(ent);
        if (b != Breed.SILKIE && b != Breed.GREENLEG) return;
        if (mob.getRandom().nextInt(450) != 0) return;
        ServerLevel level = (ServerLevel) ent.level();
        mob.spawnAtLocation(level, new ItemStack(b.product.get()));
        if (b == Breed.GREENLEG) level.playSound(null, mob, SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1.0F, 1.0F);
        BlockPos pos = mob.blockPosition();
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0);
    }
}
