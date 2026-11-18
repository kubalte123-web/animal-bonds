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
            // Move to a clean spot (the server self-test left its test pens near spawn), let chunks load, build the scene.
            case 40 -> server.execute(() -> cmd(server, "tp @p 600 -60 600"));
            case 120 -> server.execute(() -> setup(server));
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
            case 770 -> {
                server.execute(() -> logCow(server, "before"));
                mc.options.keyUp.setDown(true);
            }
            case 820 -> {
                mc.options.keyUp.setDown(false);
                server.execute(() -> logCow(server, "after"));
            }
            case 840 -> shot("ride");
            // Pressing the inventory key while riding must open the saddlebags instead.
            case 900 -> mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
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
            case 1160 -> server.execute(() -> showcase(server));
            case 1300 -> {
                shot("variety");
                AnimalBonds.LOGGER.info("ABCLIENT unique textures generated: {}", VariantTextures.generatedTotal());
            }
            case 1340 -> server.execute(() -> cmd(server, "tp @p 600 -60 652 180 15"));
            case 1440 -> shot("bagsfit");
            case 1500 -> {
                AnimalBonds.LOGGER.info("ABCLIENT DONE");
                mc.stop();
            }
            default -> {}
        }
    }

    private static java.util.UUID cowId, zombieId;
    private static net.minecraft.world.phys.Vec3 cowStart;

    private static void logCow(MinecraftServer server, String when) {
        Mob cow = find(player(server), cowId);
        if (cow == null) return;
        if (cowStart == null) cowStart = cow.position();
        AnimalBonds.LOGGER.info("ABCLIENT ride {} cow at {} moved {}", when, cow.position(), cow.position().distanceTo(cowStart));
    }

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

    /** Rows of creatures to show the colour/size variety, and a row with saddlebags on many body shapes. */
    private static void showcase(MinecraftServer server) {
        ServerPlayer sp = player(server);
        ServerLevel level = (ServerLevel) sp.level();
        var rows = java.util.List.of(EntityTypes.COW, EntityTypes.CHICKEN, EntityTypes.SPIDER, EntityTypes.ZOMBIE, EntityTypes.PIG);
        for (int r = 0; r < rows.size(); r++) {
            for (int i = 0; i < 6; i++) {
                Mob m = rows.get(r).spawn(level, new BlockPos(594 + i * 2, -60, 630 + r * 3), EntitySpawnReason.COMMAND);
                if (m != null) {
                    m.setNoAi(true);
                    m.setYRot(180);
                    m.yBodyRot = 180;
                    m.yHeadRot = 180;
                    if (m.getType() == EntityTypes.ZOMBIE)
                        m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
                }
            }
        }
        var bagged = java.util.List.of(EntityTypes.COW, EntityTypes.PIG, EntityTypes.HORSE, EntityTypes.WOLF, EntityTypes.CHICKEN, EntityTypes.ZOMBIE, EntityTypes.SPIDER, EntityTypes.CAMEL);
        for (int i = 0; i < bagged.size(); i++) {
            Mob m = spawn(level, bagged.get(i), new BlockPos(593 + i * 2, -60, 646), 5, true);
            if (m != null) {
                if (m.getType() == EntityTypes.ZOMBIE)
                    m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
                m.setNoAi(true);
                m.setYRot(90);
                m.yBodyRot = 90;
                m.yHeadRot = 90;
            }
        }
        cmd(server, "tp @p 600 -60 624 0 15");
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
