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
            default -> {
                if (ticks == 1500) server.execute(() -> hybridShowcase(server));
                if (ticks == 1600) shot("hybrids");
                if (ticks == 1700) server.execute(() -> {
                    ServerPlayer sp = player(server);
                    pl.kubalte.animalbonds.block.SplicingAltarBlock.open(sp, (ServerLevel) sp.level(), altarPos);
                });
                if (ticks == 1740 && mc.gui.screen() instanceof AltarScreen a) a.testChoose(true, true, false, true);
                if (ticks == 1800) shot("altar");
                if (ticks == 1860 && mc.gui.screen() instanceof AltarScreen a) a.testSplice();
                if (ticks == 1920) {
                    mc.gui.setScreen(null);
                    server.execute(() -> {
                        cmd(server, "gamemode creative @a");
                        ServerPlayer sp = player(server);
                        long n = ((ServerLevel) sp.level()).getEntitiesOfClass(Mob.class, sp.getBoundingBox().inflate(16),
                                m -> pl.kubalte.animalbonds.bond.Hybrids.isHybrid(m)).size();
                        AnimalBonds.LOGGER.info("ABCLIENT RIDETEST altar_splice {} hybrids={}", n >= 7 ? "PASS" : "FAIL", n);
                        cmd(server, String.format("tp @p %d %d %d 0 25", altarPos.getX(), altarPos.getY(), altarPos.getZ() - 4));
                    });
                }
                if (ticks == 2000) shot("spliced");
                if (ticks >= 2100) rideTests(mc, server);
            }
        }
    }

    private static BlockPos altarPos;

    private static void hybrid(ServerLevel level, net.minecraft.world.entity.EntityType<? extends Mob> body, String donor, BlockPos pos,
                               boolean head, boolean legs, boolean extras, int colors) {
        Mob m = body.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (m == null) return;
        m.setData(pl.kubalte.animalbonds.ModRegistry.HYBRID.get(), new pl.kubalte.animalbonds.bond.HybridData(donor, head, legs, extras, colors, false, "test"));
        m.setNoAi(true);
        m.setYRot(150);
        m.yBodyRot = 150;
        m.yHeadRot = 150;
    }

    /** A row of hybrids in front of the player, plus an altar with two bonded parents. */
    private static void hybridShowcase(MinecraftServer server) {
        ServerPlayer sp = player(server);
        ServerLevel level = (ServerLevel) sp.level();
        cmd(server, "time set 1000");
        BlockPos b = new BlockPos(800, -60, 800);
        for (int x = -6; x <= 14; x++) for (int z = -6; z <= 10; z++) for (int y = 0; y < 6; y++)
            level.setBlockAndUpdate(b.offset(x, y, z), Blocks.AIR.defaultBlockState());
        hybrid(level, EntityTypes.COW, "minecraft:zombie", b.offset(0, 0, 4), true, false, false, 2);
        hybrid(level, EntityTypes.PIG, "minecraft:chicken", b.offset(2, 0, 4), false, true, true, 2);
        hybrid(level, EntityTypes.SHEEP, "minecraft:spider", b.offset(4, 0, 4), false, true, false, 2);
        hybrid(level, EntityTypes.CHICKEN, "minecraft:cow", b.offset(6, 0, 4), true, false, false, 0);
        hybrid(level, EntityTypes.WOLF, "minecraft:phantom", b.offset(8, 0, 4), false, false, true, 2);
        hybrid(level, EntityTypes.HORSE, "minecraft:goat", b.offset(10, 0, 4), true, false, false, 2);
        altarPos = b.offset(4, 0, -3);
        level.setBlockAndUpdate(altarPos, pl.kubalte.animalbonds.ModRegistry.ALTAR.get().defaultBlockState());
        Mob cow = spawn(level, EntityTypes.COW, b.offset(2, 0, -3), 6, false);
        Mob spider = spawn(level, EntityTypes.SPIDER, b.offset(6, 0, -3), 6, false);
        for (Mob m : new Mob[] {cow, spider}) if (m != null) m.setNoAi(true);
        sp.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE, 2));
        cmd(server, "gamemode survival @a");
        cmd(server, String.format("tp @p %d %d %d 0 15", b.getX() + 5, b.getY(), b.getZ() - 2));
    }

    // ---------------- riding movement tests ----------------
    private record RideCase(String name, net.minecraft.world.entity.EntityType<? extends Mob> type, boolean forward, int jumpTicks,
                            float pitch, boolean wall, boolean night, double minRise, double maxRise, boolean hover) {}

    private static final java.util.List<RideCase> RIDE_CASES = java.util.List.of(
            new RideCase("cow_jump", EntityTypes.COW, false, 4, 0, false, false, 0.8, 1.8, false),
            new RideCase("goat_high_jump", EntityTypes.GOAT, false, 4, 0, false, false, 2.5, 99, false),
            new RideCase("spider_wall_climb", EntityTypes.SPIDER, true, 0, 0, true, false, 3.0, 99, false),
            new RideCase("bee_flight", EntityTypes.BEE, true, 0, -35, false, false, 3.0, 99, true),
            new RideCase("phantom_flight", EntityTypes.PHANTOM, true, 0, -35, false, true, 3.0, 99, true));

    private static int rideCase = 0, rideT = 0;
    private static double rideStartY, rideMaxY, rideYAtRelease;
    private static java.util.UUID rideMob;

    private static void rideTests(Minecraft mc, MinecraftServer server) {
        if (rideCase >= RIDE_CASES.size()) {
            if (rideT++ == 20) {
                AnimalBonds.LOGGER.info("ABCLIENT DONE");
                mc.stop();
            }
            return;
        }
        RideCase c = RIDE_CASES.get(rideCase);
        int t = rideT++;
        var vehicle = mc.player.getVehicle();
        if (t == 0) {
            server.execute(() -> {
                ServerPlayer sp = player(server);
                ServerLevel level = (ServerLevel) sp.level();
                cmd(server, c.night() ? "time set 18000" : "time set 1000");
                BlockPos base = new BlockPos(700 + rideCase * 20, -60, 700);
                // clear the air around and build a stone wall 3 blocks ahead (+Z) for climbers
                for (int x = -3; x <= 3; x++) for (int z = -3; z <= 8; z++) for (int y = 0; y < 12; y++)
                    level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                if (c.wall()) for (int x = -3; x <= 3; x++) for (int y = 0; y < 6; y++)
                    level.setBlockAndUpdate(base.offset(x, y, 3), Blocks.STONE.defaultBlockState());
                Mob m = spawn(level, c.type(), base, 6, false);
                if (m == null) return;
                if (c.type() == EntityTypes.PHANTOM) m.teleportTo(base.getX() + 0.5, base.getY() + 2, base.getZ() + 0.5);
                rideMob = m.getUUID();
                cmd(server, String.format("tp @p %d %d %d 0 0", base.getX(), base.getY(), base.getZ() - 2));
                sp.startRiding(m, true, true);
            });
        } else if (t == 20) {
            mc.player.setYRot(0);
            mc.player.setXRot(c.pitch());
            rideStartY = vehicle != null ? vehicle.getY() : 0;
            rideMaxY = rideStartY;
            if (c.forward()) mc.options.keyUp.setDown(true);
            if (c.jumpTicks() > 0) mc.options.keyJump.setDown(true);
        } else if (t > 20 && t <= 90) {
            mc.player.setYRot(0);
            mc.player.setXRot(c.pitch());
            if (t == 20 + c.jumpTicks()) mc.options.keyJump.setDown(false);
            if (vehicle != null) rideMaxY = Math.max(rideMaxY, vehicle.getY());
            if (t == 90) {
                mc.options.keyUp.setDown(false);
                mc.options.keyJump.setDown(false);
                rideYAtRelease = vehicle != null ? vehicle.getY() : 0;
            }
        } else if (t == 110) {
            double rise = rideMaxY - rideStartY;
            boolean ok = vehicle != null && rise >= c.minRise() && rise <= c.maxRise();
            String hoverInfo = "";
            if (c.hover() && vehicle != null) {
                double drop = rideYAtRelease - vehicle.getY();
                hoverInfo = String.format(" hoverDrop=%.2f", drop);
                ok &= drop < 1.0;
            }
            AnimalBonds.LOGGER.info("ABCLIENT RIDETEST {} {} rise={}{} (expected {}..{}) riding={}", c.name(), ok ? "PASS" : "FAIL",
                    String.format("%.2f", rise), hoverInfo, c.minRise(), c.maxRise(), vehicle != null);
            if (rideCase == 3) shot("flying");
        } else if (t == 115) {
            server.execute(() -> {
                ServerPlayer sp = player(server);
                sp.stopRiding();
                Mob m = find(sp, rideMob);
                if (m != null) m.discard();
            });
        } else if (t >= 125) {
            rideCase++;
            rideT = 0;
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
        // GeckoLib test creatures (CI test mod): unique textures, saddlebags, fainting
        var geo = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(net.minecraft.resources.Identifier.parse("abtestgeo:cubeling"));
        AnimalBonds.LOGGER.info("ABCLIENT geckolib test creature available: {}", geo.isPresent());
        if (geo.isPresent()) {
            for (int i = 0; i < 4; i++) {
                net.minecraft.world.entity.Entity ent = geo.get().spawn(level, new BlockPos(596 + i * 3, -60, 627), EntitySpawnReason.COMMAND);
                if (ent instanceof Mob m) {
                    m.setNoAi(true);
                    m.setYRot(i == 3 ? 90 : 180);
                    m.yBodyRot = m.getYRot();
                    if (i >= 2) {
                        BondData d = BondLogic.bond(m);
                        BondLogic.addXp(m, d, BondLevels.xpForLevel(8));
                        d.bags = true;
                        BondLogic.sync(m, d);
                        if (i == 2) BondEvents.faint(level, m, d);
                    }
                }
            }
        }
        cmd(server, "tp @p 600 -60 621 0 15");
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
