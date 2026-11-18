package pl.kubalte.animalbonds.bond;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import pl.kubalte.animalbonds.BondConfig;
import pl.kubalte.animalbonds.ModRegistry;

/** Game-bus event handlers: petting, hand feeding, riding, saddlebags, pacifying monsters, fainting. */
public final class BondEvents {
    private BondEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(BondEvents::onInteract);
        bus.addListener(BondEvents::onTame);
        bus.addListener(BondEvents::onChangeTarget);
        bus.addListener(BondEvents::onDeath);
        bus.addListener(BondEvents::onEntityTick);
        bus.addListener(BondEvents::onEntityTickPre);
    }

    private static void consume(PlayerInteractEvent.EntityInteract e) {
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(e.getTarget() instanceof Mob mob)) return;
        Player player = e.getEntity();
        ItemStack stack = e.getItemStack();
        boolean server = !player.level().isClientSide();
        BondData d = server ? BondLogic.getOrAutoBond(mob) : BondLogic.get(mob);

        if (d != null && d.isFainted()) {
            consume(e);
            if (server) player.sendOverlayMessage(Component.translatable("message.animalbonds.fainted", mob.getDisplayName()));
            return;
        }

        // Saddlebags
        if (stack.is(ModRegistry.SADDLEBAGS.get())) {
            consume(e);
            if (!server) return;
            if (d == null) {
                player.sendOverlayMessage(Component.translatable("message.animalbonds.not_bonded", mob.getDisplayName()));
            } else if (d.bags) {
                player.sendOverlayMessage(Component.translatable("message.animalbonds.bags_already"));
            } else if (d.level() < BondConfig.BAGS_LEVEL.get()) {
                player.sendOverlayMessage(Component.translatable("message.animalbonds.need_level", BondConfig.BAGS_LEVEL.get()));
            } else {
                d.bags = true;
                BondLogic.sync(mob, d);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                mob.level().playSound(null, mob.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                player.sendOverlayMessage(Component.translatable("message.animalbonds.bags_on", mob.getDisplayName()));
            }
            return;
        }

        // Sneak + favourite food: hand feeding (also how a bond starts)
        if (player.isShiftKeyDown() && SpeciesProfiles.isFavourite(mob, stack)) {
            consume(e);
            if (server) handFeed((ServerPlayer) player, mob, d, stack);
            return;
        }

        // Sneak + empty hand: ride
        if (player.isShiftKeyDown() && stack.isEmpty() && d != null && !(mob instanceof AbstractHorse)) {
            consume(e);
            if (!server) return;
            if (d.level() < BondConfig.RIDE_LEVEL.get()) {
                player.sendOverlayMessage(Component.translatable("message.animalbonds.need_level", BondConfig.RIDE_LEVEL.get()));
            } else if (mob.isVehicle()) {
                player.sendOverlayMessage(Component.translatable("message.animalbonds.occupied"));
            } else {
                player.startRiding(mob, true);
            }
            return;
        }

        // Empty hand (or brush): petting. Does not cancel vanilla behaviour (wolves still sit, horses still mount).
        if (server && d != null && (stack.isEmpty() || stack.is(Items.BRUSH)) && !player.isShiftKeyDown()) {
            pet((ServerPlayer) player, mob, d);
        }
    }

    static void pet(ServerPlayer player, Mob mob, BondData d) {
        long now = mob.level().getGameTime();
        BondLogic.hearts(mob, 2);
        if (now - d.lastPet >= BondConfig.PET_COOLDOWN.get()) {
            d.lastPet = now;
            if (BondLogic.addXp(mob, d, BondConfig.PET_XP.get())) levelUp(player, mob, d);
            mob.playAmbientSound();
        }
        BondLogic.sync(mob, d);
    }

    static void handFeed(ServerPlayer player, Mob mob, BondData d, ItemStack stack) {
        ServerLevel level = (ServerLevel) mob.level();
        long now = level.getGameTime();
        if (!player.getAbilities().instabuild) stack.shrink(1);
        mob.heal(2.0F);
        level.playSound(null, mob.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (d == null) {
            if (level.getRandom().nextInt(100) < BondConfig.TAME_CHANCE.get()) {
                d = BondLogic.bond(mob);
                d.lastTreat = now;
                BondLogic.sync(mob, d);
                BondLogic.hearts(mob, 7);
                player.sendOverlayMessage(Component.translatable("message.animalbonds.bonded", mob.getDisplayName()));
            } else {
                level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
                player.sendOverlayMessage(Component.translatable("message.animalbonds.wary", mob.getDisplayName()));
            }
            return;
        }
        BondLogic.hearts(mob, 4);
        if (now - d.lastTreat >= BondConfig.TREAT_COOLDOWN.get()) {
            d.lastTreat = now;
            if (BondLogic.addXp(mob, d, BondConfig.TREAT_XP.get())) levelUp(player, mob, d);
        } else {
            player.sendOverlayMessage(Component.translatable("message.animalbonds.full", mob.getDisplayName()));
        }
        BondLogic.sync(mob, d);
    }

    static void levelUp(Player player, Mob mob, BondData d) {
        player.sendOverlayMessage(Component.translatable("message.animalbonds.level_up", mob.getDisplayName(), d.level()));
        mob.level().playSound(null, mob.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6F, 1.4F);
    }

    static void onTame(AnimalTameEvent e) {
        if (e.isCanceled()) return;
        BondData d = BondLogic.bond(e.getAnimal());
        BondLogic.sync(e.getAnimal(), d);
    }

    static void onChangeTarget(LivingChangeTargetEvent e) {
        if (!(e.getEntity() instanceof Mob mob)) return;
        if (!(e.getNewAboutToBeSetTarget() instanceof Player)) return;
        BondData d = BondLogic.get(mob);
        if (d != null && d.level() >= BondConfig.PEACEFUL_LEVEL.get()) e.setCanceled(true);
    }

    static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        BondData d = BondLogic.get(mob);
        if (d == null) return;
        if (d.level() >= BondConfig.FAINT_LEVEL.get() && !e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            e.setCanceled(true);
            faint(level, mob, d);
            return;
        }
        // Real death: drop the saddlebags and their contents.
        if (d.bags) {
            BagData bags = mob.getData(ModRegistry.BAGS.get());
            for (int i = 0; i < bags.container.getContainerSize(); i++) {
                ItemStack st = bags.container.removeItemNoUpdate(i);
                if (!st.isEmpty()) mob.spawnAtLocation(level, st);
            }
            mob.spawnAtLocation(level, new ItemStack(ModRegistry.SADDLEBAGS.get()));
            d.bags = false;
        }
    }

    public static void faint(ServerLevel level, Mob mob, BondData d) {
        mob.setHealth(Math.max(1.0F, mob.getMaxHealth() * 0.1F));
        mob.clearFire();
        mob.removeAllEffects();
        mob.ejectPassengers();
        mob.stopRiding();
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setNoAi(true);
        mob.setInvulnerable(true);
        d.faintedUntil = level.getGameTime() + BondLogic.faintDuration(d);
        BondLogic.sync(mob, d);
        level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 0.5, mob.getZ(), 15, 0.4, 0.3, 0.4, 0.02);
        if (BondConfig.FAINT_TELEPORT.get() && d.home.isPresent()) teleportHome(level, mob, d.home.get());
    }

    private static void teleportHome(ServerLevel from, Mob mob, GlobalPos home) {
        ServerLevel target = from.getServer().getLevel(home.dimension());
        if (target == null) return;
        BlockPos ped = home.pos();
        target.getChunk(ped); // make sure the pen is loaded
        PenScanner.Pen pen = PenScanner.scan(target, ped);
        BlockPos dest = pen != null ? pen.randomCell(target.getRandom()) : ped.above();
        Vec3 v = Vec3.atBottomCenterOf(dest);
        if (target == from) {
            mob.teleportTo(v.x, v.y, v.z);
        } else {
            mob.teleport(new net.minecraft.world.level.portal.TeleportTransition(target, v, Vec3.ZERO, mob.getYRot(), mob.getXRot(),
                    net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        }
    }

    static void onEntityTick(EntityTickEvent.Post e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        if (!mob.hasData(ModRegistry.BOND.get())) return;
        BondData d = BondLogic.get(mob);

        if (d.isFainted()) {
            if (mob.tickCount % 20 == 0) {
                mob.heal(mob.getMaxHealth() / Math.max(1, BondLogic.faintDuration(d) / 20f));
                level.sendParticles(ParticleTypes.NOTE, mob.getX(), mob.getY() + mob.getBbHeight() + 0.2, mob.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
                if (level.getGameTime() >= d.faintedUntil) wake(mob, d);
            }
            return;
        }

        if (mob.getFirstPassenger() instanceof ServerPlayer rider) {
            RideControl.postTick(mob, rider);
        }
    }

    static void onEntityTickPre(EntityTickEvent.Pre e) {
        if (e.getEntity() instanceof Mob mob && !mob.level().isClientSide() && mob.hasData(ModRegistry.BOND.get())) {
            RideControl.preTick(mob);
        }
    }

    public static void wake(Mob mob, BondData d) {
        d.faintedUntil = 0;
        mob.setNoAi(false);
        mob.setInvulnerable(false);
        mob.setHealth(mob.getMaxHealth());
        BondLogic.sync(mob, d);
        BondLogic.hearts(mob, 5);
    }
}
