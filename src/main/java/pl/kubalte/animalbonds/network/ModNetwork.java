package pl.kubalte.animalbonds.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import pl.kubalte.animalbonds.BondConfig;
import pl.kubalte.animalbonds.ModRegistry;
import pl.kubalte.animalbonds.block.PedestalBlockEntity;
import pl.kubalte.animalbonds.bond.BagData;
import pl.kubalte.animalbonds.bond.BondAccess;
import pl.kubalte.animalbonds.bond.BondData;
import pl.kubalte.animalbonds.bond.BondLogic;

public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        // Client handler is registered in the client-only class (RegisterClientPayloadHandlersEvent).
        r.playToClient(OpenBookPayload.TYPE, OpenBookPayload.STREAM_CODEC);
        r.playToServer(RenamePayload.TYPE, RenamePayload.STREAM_CODEC, ModNetwork::handleRename);
        r.playToServer(OpenBagsPayload.TYPE, OpenBagsPayload.STREAM_CODEC, ModNetwork::handleOpenBags);
        r.playToServer(MembersPayload.TYPE, MembersPayload.STREAM_CODEC, ModNetwork::handleMembers);
        r.playToClient(OpenAltarPayload.TYPE, OpenAltarPayload.STREAM_CODEC);
        r.playToServer(SplicePayload.TYPE, SplicePayload.STREAM_CODEC, ModNetwork::handleSplice);
    }

    private static boolean nearPedestal(ServerPlayer player, BlockPos pedestal) {
        return player.level().getBlockEntity(pedestal) instanceof PedestalBlockEntity be && be.access().isTrusted(player)
                && player.distanceToSqr(pedestal.getX() + 0.5, pedestal.getY() + 0.5, pedestal.getZ() + 0.5) < 10 * 10;
    }

    private static Mob mobNear(ServerPlayer player, BlockPos pedestal, int entityId) {
        Entity e = player.level().getEntity(entityId);
        if (!(e instanceof Mob mob) || !mob.isAlive()) return null;
        if (mob.distanceToSqr(pedestal.getX() + 0.5, pedestal.getY(), pedestal.getZ() + 0.5) > 64 * 64) return null;
        return mob;
    }

    static void handleRename(RenamePayload p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player) || !nearPedestal(player, p.pedestal())) return;
        Mob mob = mobNear(player, p.pedestal(), p.entityId());
        if (mob == null) return;
        BondData d = BondLogic.get(mob);
        if (d != null && !BondAccess.canUse(player, mob, d)) return;
        if (d == null || d.level() < BondConfig.NAME_LEVEL.get()) {
            player.sendOverlayMessage(Component.translatable("message.animalbonds.need_level", BondConfig.NAME_LEVEL.get()));
            return;
        }
        String name = p.name().strip();
        if (name.length() > 32) name = name.substring(0, 32);
        if (name.isEmpty()) {
            mob.setCustomName(null);
        } else {
            mob.setCustomName(Component.literal(name));
        }
        mob.setPersistenceRequired();
        BondLogic.hearts(mob, 5);
        player.sendOverlayMessage(Component.translatable("message.animalbonds.named", mob.getDisplayName()));
        if (player.level().getBlockEntity(p.pedestal()) instanceof PedestalBlockEntity be) be.openBook(player);
    }

    static void handleOpenBags(OpenBagsPayload p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        Mob mob;
        if (p.entityId() < 0) {
            mob = player.getVehicle() instanceof Mob v ? v : null;
        } else {
            mob = nearPedestal(player, p.pedestal()) ? mobNear(player, p.pedestal(), p.entityId()) : null;
        }
        if (mob == null) return;
        openBags(player, mob);
    }

    static void handleSplice(SplicePayload p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        var level = (net.minecraft.server.level.ServerLevel) player.level();
        if (!(level.getBlockState(p.altar()).getBlock() instanceof pl.kubalte.animalbonds.block.SplicingAltarBlock)) return;
        if (player.distanceToSqr(p.altar().getX() + 0.5, p.altar().getY() + 0.5, p.altar().getZ() + 0.5) > 100) return;
        Mob a = mobNear(player, p.altar(), p.entityA()), b = mobNear(player, p.altar(), p.entityB());
        if (a != null && a.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p.altar())) > 14 * 14) a = null;
        if (b != null && b.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p.altar())) > 14 * 14) b = null;
        pl.kubalte.animalbonds.bond.Hybrids.splice(level, player, p.altar(), a, b, p.choices());
        pl.kubalte.animalbonds.block.SplicingAltarBlock.open(player, level, p.altar());
    }

    static void handleMembers(MembersPayload p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (!(player.level().getBlockEntity(p.pedestal()) instanceof PedestalBlockEntity be)) return;
        if (player.distanceToSqr(p.pedestal().getX() + 0.5, p.pedestal().getY() + 0.5, p.pedestal().getZ() + 0.5) > 100) return;
        if (!be.access().isOwner(player)) {
            player.sendOverlayMessage(Component.translatable("message.animalbonds.only_owner"));
            return;
        }
        if (be.access().owner.isEmpty()) be.setOwner(player);
        String name = p.name().strip();
        if (p.add()) {
            ServerPlayer target = player.level().getServer().getPlayerList().getPlayerByName(name);
            if (target == null) {
                player.sendOverlayMessage(Component.translatable("message.animalbonds.player_offline", name));
            } else if (!target.getUUID().equals(player.getUUID())) {
                be.access().members.put(target.getUUID(), target.getName().getString());
                player.sendOverlayMessage(Component.translatable("message.animalbonds.member_added", target.getName().getString()));
            }
        } else {
            be.access().members.values().removeIf(n -> n.equalsIgnoreCase(name));
        }
        be.accessChanged();
        be.openBook(player);
    }

    public static void openBags(ServerPlayer player, Mob mob) {
        BondData d = BondLogic.get(mob);
        if (d == null || !d.bags) return;
        if (!BondAccess.canUse(player, mob, d)) {
            player.sendOverlayMessage(Component.translatable("message.animalbonds.not_yours", mob.getDisplayName()));
            return;
        }
        BagData bags = mob.getData(ModRegistry.BAGS.get());
        player.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ChestMenu(MenuType.GENERIC_9x2, id, inv, bags.container, 2) {
            @Override
            public boolean stillValid(Player who) {
                return mob.isAlive() && who.distanceToSqr(mob) < 80 * 80;
            }
        }, Component.translatable("container.animalbonds.bags", mob.getDisplayName())));
    }
}
