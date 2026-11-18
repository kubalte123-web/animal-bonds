package pl.kubalte.animalbonds.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import pl.kubalte.animalbonds.AnimalBonds;
import pl.kubalte.animalbonds.ModRegistry;
import pl.kubalte.animalbonds.SelfTest;
import pl.kubalte.animalbonds.block.PedestalBlockEntity;
import pl.kubalte.animalbonds.block.TroughBlock;
import pl.kubalte.animalbonds.bond.BondData;
import pl.kubalte.animalbonds.bond.BondEvents;
import pl.kubalte.animalbonds.bond.BondLevels;
import pl.kubalte.animalbonds.bond.BondLogic;
import pl.kubalte.animalbonds.network.ModNetwork;

/**
 * CI-only visual test (-Danimalbonds.clienttest=true): builds a pen in single player, opens the book
 * and logs "ABCLIENT SHOT <name>" markers; the CI script takes a screenshot at each marker.
 */
public final class ClientSelfTest {
    private ClientSelfTest() {}

    private static int ticks = -1;
    private static BlockPos pedestal;

    public static void register(IEventBus modBus) {
        if (Boolean.getBoolean("animalbonds.clienttest")) NeoForge.EVENT_BUS.addListener(ClientSelfTest::onTick);
    }

    private static void shot(String name) {
        AnimalBonds.LOGGER.info("ABCLIENT SHOT {}", name);
    }

    static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return;
        ticks++;
        switch (ticks) {
            case 60 -> server.execute(() -> setup(server));
            case 200 -> shot("world");
            case 300 -> server.execute(() -> {
                ServerPlayer sp = player(server);
                logMobs(sp);
                if (sp.level().getBlockEntity(pedestal) instanceof PedestalBlockEntity be) be.openBook(sp);
            });
            case 380 -> shot("book1");
            case 440 -> flip();
            case 480 -> shot("book2");
            case 540 -> flip();
            case 580 -> shot("book3");
            case 640 -> flip();
            case 680 -> shot("book4");
            case 740 -> {
                mc.gui.setScreen(null);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                server.execute(() -> {
                    ServerPlayer sp = player(server);
                    Mob cow = find(sp, cowId);
                    if (cow != null) AnimalBonds.LOGGER.info("ABCLIENT ride started: {}", sp.startRiding(cow, true, true));
                });
            }
            case 840 -> shot("ride");
            case 900 -> server.execute(() -> {
                ServerPlayer sp = player(server);
                Mob cow = find(sp, cowId);
                if (cow != null) ModNetwork.openBags(sp, cow);
            });
            case 960 -> shot("bags");
            case 1000 -> {
                mc.gui.setScreen(null);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(() -> {
                    ServerPlayer sp = player(server);
                    sp.stopRiding();
                    faintZombie(server);
                });
            }
            case 1060 -> server.execute(() -> {
                ServerPlayer sp = player(server);
                Mob z = find(sp, zombieId);
                if (z != null) {
                    cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 50", z.getX() - 0.5, z.getBlockY() + 1, z.getZ() - 2.5));
                    AnimalBonds.LOGGER.info("ABCLIENT zombie fainted={} at {}", BondLogic.get(z) != null && BondLogic.get(z).isFainted(), z.blockPosition());
                }
            });
            case 1140 -> shot("faint");
            case 1200 -> {
                AnimalBonds.LOGGER.info("ABCLIENT DONE");
                mc.stop();
            }
            default -> {}
        }
    }

    private static java.util.UUID cowId, zombieId;

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    private static Mob find(ServerPlayer sp, java.util.UUID id) {
        return id != null && ((ServerLevel) sp.level()).getEntity(id) instanceof Mob m ? m : null;
    }

    private static void logMobs(ServerPlayer sp) {
        ServerLevel level = (ServerLevel) sp.level();
        var pen = pl.kubalte.animalbonds.bond.PenScanner.scan(level, pedestal);
        for (Mob m : level.getEntitiesOfClass(Mob.class, sp.getBoundingBox().inflate(24))) {
            AnimalBonds.LOGGER.info("ABCLIENT mob {} at {} inPen={}", m.getType(), m.blockPosition(), pen != null && pen.contains(m.getBlockX(), m.getBlockZ()));
        }
    }

    private static void flip() {
        if (Minecraft.getInstance().gui.screen() instanceof BondBookScreen book) book.flipPage(1);
    }

    private static void cmd(MinecraftServer server, String c) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
    }

    private static void setup(MinecraftServer server) {
        ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = (ServerLevel) sp.level();
        BlockPos base = sp.blockPosition();
        BlockPos origin = base.offset(-3, 0, 4);
        cmd(server, "gamemode creative @a");
        cmd(server, "time set 1000");
        cmd(server, "weather clear 99999");
        cmd(server, "gamerule doDaylightCycle false");
        cmd(server, "gamerule doMobSpawning false");
        pedestal = SelfTest.buildPen(level, origin);
        level.setBlockAndUpdate(origin.offset(1, 0, 6), ModRegistry.TROUGH.get().defaultBlockState().setValue(TroughBlock.FILL, 3));
        level.setBlockAndUpdate(origin.offset(6, 0, 6), ModRegistry.NEST.get().defaultBlockState());
        level.setBlockAndUpdate(origin.offset(5, -1, 1), Blocks.WATER.defaultBlockState());

        Mob cow = spawn(level, EntityTypes.COW, origin.offset(3, 0, 3), 6, true);
        if (cow != null) cowId = cow.getUUID();
        spawn(level, EntityTypes.SHEEP, origin.offset(4, 0, 5), 3, false);
        Mob zombie = spawn(level, EntityTypes.ZOMBIE, origin.offset(2, 0, 2), 8, false);
        if (zombie != null) zombieId = zombie.getUUID();
        if (zombie != null) zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
        spawn(level, EntityTypes.CHICKEN, origin.offset(5, 0, 2), 0, false);
        if (cow != null) cow.setCustomName(net.minecraft.network.chat.Component.literal("Mućka"));
        if (level.getBlockEntity(pedestal) instanceof PedestalBlockEntity be) be.updatePen(level);

        // Stand behind the pedestal looking at the pen.
        cmd(server, String.format("tp @p %d %d %d -60 25", pedestal.getX() - 3, pedestal.getY() + 1, pedestal.getZ() - 2));
    }

    private static Mob spawn(ServerLevel level, net.minecraft.world.entity.EntityType<? extends Mob> type, BlockPos pos, int lvl, boolean bags) {
        Mob mob = type.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (mob == null || lvl <= 0) return mob;
        BondData d = BondLogic.bond(mob);
        BondLogic.addXp(mob, d, BondLevels.xpForLevel(lvl) + 5);
        d.lastPet = level.getGameTime();
        d.bags = bags;
        BondLogic.sync(mob, d);
        return mob;
    }

    private static void faintZombie(MinecraftServer server) {
        ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = (ServerLevel) sp.level();
        for (Mob m : level.getEntitiesOfClass(Mob.class, sp.getBoundingBox().inflate(20), m -> m.getType() == EntityTypes.ZOMBIE)) {
            BondData d = BondLogic.get(m);
            if (d != null) BondEvents.faint(level, m, d);
        }
    }
}
