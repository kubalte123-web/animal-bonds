package pl.kubalte.animalbonds;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import pl.kubalte.animalbonds.block.PedestalBlockEntity;
import pl.kubalte.animalbonds.block.TroughBlock;
import pl.kubalte.animalbonds.bond.BondData;
import pl.kubalte.animalbonds.bond.BondLevels;
import pl.kubalte.animalbonds.bond.BondLogic;
import pl.kubalte.animalbonds.bond.Need;
import pl.kubalte.animalbonds.bond.PenScanner;
import pl.kubalte.animalbonds.bond.SpeciesProfiles;
import pl.kubalte.animalbonds.network.BookEntry;

/**
 * Automated server-side test, run in CI with -Danimalbonds.selftest=true.
 * Builds a pen in the sky, spawns creatures and checks the bond rules, then stops the server.
 */
public final class SelfTest {
    private SelfTest() {}

    public static void register(IEventBus bus) {
        if (Boolean.getBoolean("animalbonds.selftest")) bus.addListener(SelfTest::onStarted);
    }

    private static final List<String> FAILS = new ArrayList<>();
    private static int checks;

    private static void check(boolean ok, String what) {
        checks++;
        if (ok) {
            AnimalBonds.LOGGER.info("ABTEST ok   {}", what);
        } else {
            FAILS.add(what);
            AnimalBonds.LOGGER.error("ABTEST FAIL {}", what);
        }
    }

    private static int waitTicks = -1;
    private static BlockPos escapePedestal;
    private static List<Mob> escapeMobs = List.of();

    static void onStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        try {
            run(server.overworld());
            setupUpgrades(server.overworld());
            setupEscapeTest(server.overworld());
            setupBallTest(server.overworld());
            waitTicks = 1200; // let the creatures live for a minute
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(SelfTest::onServerTick);
            return;
        } catch (Throwable t) {
            AnimalBonds.LOGGER.error("ABTEST FAIL exception", t);
            FAILS.add("exception: " + t);
        }
        finish(server);
    }

    /** A pen with trough, bed and pedestal right next to the fence: nobody may jump out. */
    private static void setupEscapeTest(ServerLevel level) {
        BlockPos origin = new BlockPos(8, 250, 40);
        forceChunks(level, origin);
        escapePedestal = buildPen(level, origin);
        level.setBlockAndUpdate(origin.offset(1, 0, 3), ModRegistry.TROUGH.get().defaultBlockState().setValue(TroughBlock.FILL, 4));
        level.setBlockAndUpdate(origin.offset(6, 0, 3), ModRegistry.NEST.get().defaultBlockState());
        level.setBlockAndUpdate(origin.offset(3, 0, 1), ModRegistry.PEDESTAL.get().defaultBlockState());
        List<Mob> list = new ArrayList<>();
        var types = List.of(EntityTypes.COW, EntityTypes.SHEEP, EntityTypes.PIG, EntityTypes.CHICKEN, EntityTypes.GOAT, EntityTypes.RABBIT, EntityTypes.HORSE, EntityTypes.ZOMBIE);
        int i = 0;
        for (var t : types) {
            Mob m = t.spawn(level, origin.offset(2 + i % 4, 0, 2 + i / 4 * 2), EntitySpawnReason.COMMAND);
            if (m != null) {
                // no roof in this test pen: protect the zombie from burning in the sun
                m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                BondLogic.bond(m);
                list.add(m);
            }
            i++;
        }
        escapeMobs = list;
        if (level.getBlockEntity(escapePedestal) instanceof PedestalBlockEntity be) be.updatePen(level); // register residents
    }

    static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) {
        if (waitTicks < 0) return;
        if (waitTicks == 1100) {
            try {
                checkUpgrades(e.getServer().overworld());
                // the ball is thrown once the test area is loaded and ticking
                pl.kubalte.animalbonds.play.BallLogic.throwBall(ballThrower, new ItemStack(ModRegistry.BALL.get()));
            } catch (Throwable t) {
                AnimalBonds.LOGGER.error("ABTEST FAIL upgrades exception", t);
                FAILS.add("upgrades exception: " + t);
            }
        }
        if (--waitTicks > 0) return;
        waitTicks = -1;
        ServerLevel level = e.getServer().overworld();
        // a regular pedestal scan brings back any runaway (goats can jump fences in vanilla)
        if (level.getBlockEntity(escapePedestal) instanceof PedestalBlockEntity be) be.updatePen(level);
        PenScanner.Pen pen = PenScanner.scan(level, escapePedestal);
        check(pen != null, "escape-test pen still detected");
        if (pen != null) {
            for (Mob m : escapeMobs) {
                boolean in = m.isAlive() && pen.isInside(m);
                check(in, "after 60 s " + m.getType() + " still in pen (at " + m.blockPosition() + ", alive=" + m.isAlive() + ")");
            }
        }
        check(pl.kubalte.animalbonds.play.BallLogic.fetches >= 1, "a bonded wolf fetched the thrown ball ("
                + pl.kubalte.animalbonds.play.BallLogic.fetches + " fetches, wolf at " + (ballWolf == null ? null : ballWolf.blockPosition()) + ")");
        if (ballWolf != null) {
            BondData wd = BondLogic.get(ballWolf);
            check(wd != null && wd.lastPet > 0, "fetching counts as petting for the day");
        }
        finish(e.getServer());
    }

    private static Mob ballWolf;

    /** A wolf and a (fake) player in a pen; the player throws a ball, checked when the wait is over. */
    private static void setupBallTest(ServerLevel level) {
        BlockPos o = new BlockPos(8, 250, 160);
        forceChunks(level, o);
        buildPen(level, o);
        ballWolf = EntityTypes.WOLF.spawn(level, o.offset(5, 0, 6), EntitySpawnReason.COMMAND);
        BondData d = BondLogic.bond(ballWolf);
        BondLogic.addXp(ballWolf, d, BondLevels.xpForLevel(4));
        d.lastPet = Long.MIN_VALUE / 2;
        FakePlayer p = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("abtest-ball".getBytes()), "ABBall"));
        p.setPos(o.getX() + 2.5, o.getY(), o.getZ() + 1.5);
        p.setYRot(0F);
        p.setXRot(10F);
        ballThrower = p;
    }

    private static FakePlayer ballThrower;

    private static BlockPos upPed, upCrop, upTrough, upChest;
    private static Mob upCow, upStrider;

    /** Pen upgrades (scratching post, storage chest, trough refill) and work mode: built at start, checked 100 ticks later. */
    private static void setupUpgrades(ServerLevel level) {
        BlockPos o = new BlockPos(8, 250, 110);
        forceChunks(level, o);
        BlockPos ped = buildPen(level, o);
        BlockPos chestPos = ped.offset(-1, 0, 0);
        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        net.minecraft.world.Container chest = (net.minecraft.world.Container) level.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.WHEAT, 4));
        chest.setItem(1, new ItemStack(Items.RAW_IRON, 2));
        BlockPos trough = o.offset(1, 0, 3);
        level.setBlockAndUpdate(trough, ModRegistry.TROUGH.get().defaultBlockState().setValue(TroughBlock.FILL, 0));
        level.setBlockAndUpdate(o.offset(6, 0, 6), ModRegistry.SCRATCHING_POST.get().defaultBlockState());
        BlockPos crop = o.offset(5, 0, 2);
        level.setBlockAndUpdate(crop.below(), Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(crop, ((net.minecraft.world.level.block.CropBlock) Blocks.WHEAT).getStateForAge(7));

        Mob cow = EntityTypes.COW.spawn(level, o.offset(3, 0, 4), EntitySpawnReason.COMMAND);
        Mob strider = EntityTypes.STRIDER.spawn(level, o.offset(4, 0, 5), EntitySpawnReason.COMMAND);
        // a strider may spawn with a baby strider riding it: that one would work too and spoil the count
        for (var rider : java.util.List.copyOf(strider.getPassengers())) rider.discard();
        BondData cd = BondLogic.bond(cow), sd = BondLogic.bond(strider);
        BondLogic.addXp(cow, cd, BondLevels.xpForLevel(5));
        BondLogic.addXp(strider, sd, BondLevels.xpForLevel(5));
        cd.lastPet = Long.MIN_VALUE / 2;
        cd.lastGift = Long.MIN_VALUE / 2;

        upPed = ped;
        upCrop = crop;
        upTrough = trough;
        upChest = chestPos;
        upCow = cow;
        upStrider = strider;
    }

    private static void checkUpgrades(ServerLevel level) {
        BlockPos ped = upPed, crop = upCrop, trough = upTrough;
        Mob cow = upCow, strider = upStrider;
        BondData cd = BondLogic.get(cow), sd = BondLogic.get(strider);
        net.minecraft.world.Container chest = (net.minecraft.world.Container) level.getBlockEntity(upChest);
        PenScanner.Pen pen = PenScanner.scan(level, ped);
        check(pen != null && pen.posts == 1, "scratching post found in pen (" + (pen == null ? -1 : pen.posts) + ")");
        check(pl.kubalte.animalbonds.work.PenStorage.find(level, ped) != null, "chest next to the pedestal is the pen storage");

        PedestalBlockEntity be = (PedestalBlockEntity) level.getBlockEntity(ped);
        AnimalBonds.LOGGER.info("ABTEST debug upgrades: mobs in pen={} cow={} lvl={} strider={} lvl={} pet={}", pen == null ? null : pen.mobs(level),
                cow.blockPosition(), cd.level(), strider.blockPosition(), sd.level(), cd.lastPet);
        boolean wasOn = BondConfig.WORK_ENABLED.get();
        BondConfig.WORK_ENABLED.set(false);
        be.updatePen(level);
        check(level.getBlockState(crop).getValue(net.minecraft.world.level.block.CropBlock.AGE) == 7, "work mode off: nobody harvests");
        check(cd.lastPet >= 0, "scratching post makes the cow feel petted (" + cd.lastPet + ")");
        check(level.getBlockState(trough).getValue(TroughBlock.FILL) > 0, "empty trough refilled from the storage");
        check(cd.lastGift >= 0 && level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                pen.box().inflate(2)).isEmpty(), "gift went straight into the storage");
        check(pl.kubalte.animalbonds.work.PenStorage.count(chest, st -> st.is(Items.IRON_INGOT)) == 0, "work mode off: nothing smelted");

        BondConfig.WORK_ENABLED.set(true);
        for (int i = 0; i < 8; i++) be.updatePen(level);
        check(level.getBlockState(crop).getValue(net.minecraft.world.level.block.CropBlock.AGE) == 0, "farmer cow harvested and replanted the wheat");
        check(pl.kubalte.animalbonds.work.PenStorage.count(chest, st -> st.is(Items.IRON_INGOT)) == 2, "strider smelted the raw iron in the storage");
        check(pl.kubalte.animalbonds.work.PenWork.jobs(cow).contains(pl.kubalte.animalbonds.work.Work.FARMER)
                && pl.kubalte.animalbonds.work.PenWork.jobs(strider).contains(pl.kubalte.animalbonds.work.Work.SMELTER), "jobs by species");
        String line = pl.kubalte.animalbonds.api.AnimalBondsApi.bookLine(cow);
        check(line.contains("work.animalbonds.label"), "Chronicle shows the job (" + line + ")");
        BondConfig.WORK_ENABLED.set(wasOn);
        cow.discard();
        strider.discard();
    }

    private static long PenScanner_pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /** Keep the test area loaded and ticking (far away from spawn, so the client test world stays clean). */
    private static void forceChunks(ServerLevel level, BlockPos origin) {
        for (int cx = (origin.getX() >> 4) - 1; cx <= (origin.getX() >> 4) + 1; cx++)
            for (int cz = (origin.getZ() >> 4) - 1; cz <= (origin.getZ() >> 4) + 1; cz++)
                level.setChunkForced(cx, cz, true);
    }

    private static void finish(MinecraftServer server) {
        if (FAILS.isEmpty()) {
            AnimalBonds.LOGGER.info("ABTEST RESULT: PASS ({} checks)", checks);
        } else {
            AnimalBonds.LOGGER.error("ABTEST RESULT: FAIL ({} of {} failed): {}", FAILS.size(), checks, FAILS);
        }
        server.halt(false);
    }

    /** Builds a 6x6 fenced pen on a stone platform; pedestal outside, next to the west fence. Returns pedestal pos. */
    public static BlockPos buildPen(ServerLevel level, BlockPos origin) {
        for (int x = -3; x <= 10; x++) {
            for (int z = -3; z <= 10; z++) {
                level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = -6; y <= -2; y++) level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.DIRT.defaultBlockState());
                for (int y = 0; y < 4; y++) level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        }
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                if (x == 0 || x == 7 || z == 0 || z == 7) level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        BlockPos pedestal = origin.offset(-1, 0, 3);
        level.setBlockAndUpdate(pedestal, ModRegistry.PEDESTAL.get().defaultBlockState());
        return pedestal;
    }

    private static void run(ServerLevel level) {
        BlockPos origin = new BlockPos(8, 250, 8);
        forceChunks(level, origin);

        // ---- Pen detection ----
        BlockPos pedestal = buildPen(level, origin);
        PenScanner.Pen pen = PenScanner.scan(level, pedestal);
        check(pen != null, "pen detected next to fence");
        check(pen != null && pen.area() == 36, "pen area is 36 (was " + (pen == null ? -1 : pen.area()) + ")");
        level.setBlockAndUpdate(origin.offset(7, 0, 4), Blocks.AIR.defaultBlockState());
        check(PenScanner.scan(level, pedestal) == null, "open pen is not detected");
        level.setBlockAndUpdate(origin.offset(7, 0, 4), Blocks.OAK_FENCE.defaultBlockState());
        check(level.getBlockEntity(pedestal) instanceof PedestalBlockEntity, "pedestal has block entity");

        // ---- Hills and pits inside the pen ----
        BlockPos hp = new BlockPos(8, 250, 60);
        forceChunks(level, hp);
        BlockPos hillPed = buildPen(level, hp);
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) {
            int h = (x == 3 && z == 3) ? 3 : 2;
            for (int y = 0; y < h; y++) level.setBlockAndUpdate(hp.offset(x, y, z), Blocks.DIRT.defaultBlockState());
        }
        for (int x = 5; x <= 6; x++) for (int z = 5; z <= 6; z++)
            for (int y = -1; y >= -4; y--) level.setBlockAndUpdate(hp.offset(x, y, z), Blocks.AIR.defaultBlockState());
        for (int x = 5; x <= 6; x++) for (int z = 5; z <= 6; z++) level.setBlockAndUpdate(hp.offset(x, -5, z), Blocks.STONE.defaultBlockState());
        PenScanner.Pen hill = PenScanner.scan(level, hillPed);
        check(hill != null && hill.area() == 36, "hilly pen detected with area 36 (was " + (hill == null ? -1 : hill.area()) + ")");
        if (hill != null) {
            check(hill.cells.get(PenScanner_pack(hp.getX() + 3, hp.getZ() + 3)) == hp.getY() + 3, "hill top floor is 3 blocks up");
            check(hill.cells.get(PenScanner_pack(hp.getX() + 5, hp.getZ() + 5)) == hp.getY() - 4, "pit floor is 4 blocks down");
            Mob onHill = EntityTypes.PIG.spawn(level, hp.offset(3, 3, 3), EntitySpawnReason.COMMAND);
            Mob inPit = EntityTypes.PIG.spawn(level, hp.offset(5, -4, 6), EntitySpawnReason.COMMAND);
            check(hill.isInside(onHill) && hill.isInside(inPit), "creatures on the hill and in the pit count as inside");
            onHill.discard();
            inPit.discard();
        }

        // ---- Creatures ----
        Mob cow = EntityTypes.COW.spawn(level, origin.offset(3, 0, 3), EntitySpawnReason.COMMAND);
        Mob cow2 = EntityTypes.COW.spawn(level, origin.offset(4, 0, 4), EntitySpawnReason.COMMAND);
        Mob zombie = EntityTypes.ZOMBIE.spawn(level, origin.offset(5, 0, 2), EntitySpawnReason.COMMAND);
        check(cow != null && zombie != null && cow2 != null, "creatures spawned");
        pen = PenScanner.scan(level, pedestal);
        List<Mob> mobs = pen.mobs(level);
        check(mobs.size() == 3, "3 creatures in pen (was " + mobs.size() + ")");
        check(!BondLogic.isBonded(cow), "fresh cow not bonded");
        check(SpeciesProfiles.isFavourite(cow, new ItemStack(Items.WHEAT)), "wheat is cow's favourite");
        check(!SpeciesProfiles.isFavourite(cow, new ItemStack(Items.BONE)), "bone is not cow's favourite");

        BondData d = BondLogic.bond(cow);
        check(BondLogic.isBonded(cow) && d.level() == 1, "bonded cow starts at level 1");

        // ---- Needs ----
        BondLogic.evaluateNeeds(cow, d, pen, mobs);
        List<Need> needs = SpeciesProfiles.get(cow).needs();
        int troughIdx = needs.indexOf(Need.TROUGH);
        int grassIdx = needs.indexOf(Need.GRASS);
        int companyIdx = needs.indexOf(Need.COMPANY);
        int roofIdx = needs.indexOf(Need.ROOF);
        check((d.needMask & (1 << troughIdx)) == 0, "trough need unmet without trough");
        check((d.needMask & (1 << grassIdx)) != 0, "grass need met on grass");
        check((d.needMask & (1 << companyIdx)) != 0, "company need met with two cows");
        check((d.needMask & (1 << roofIdx)) == 0, "roof need unmet under open sky");
        int before = d.happiness;
        level.setBlockAndUpdate(origin.offset(1, 0, 1), ModRegistry.TROUGH.get().defaultBlockState().setValue(TroughBlock.FILL, 4));
        pen = PenScanner.scan(level, pedestal);
        check(pen.troughs.size() == 1, "trough found in pen");
        BondLogic.evaluateNeeds(cow, d, pen, pen.mobs(level));
        check((d.needMask & (1 << troughIdx)) != 0, "trough need met with filled trough");
        check(d.happiness > before, "happiness rises with trough (" + before + " -> " + d.happiness + ")");
        check(TroughBlock.feedValue(new ItemStack(Items.HAY_BLOCK)) == 4 && TroughBlock.feedValue(new ItemStack(Items.DIAMOND)) == 0, "trough feed values");

        // ---- Levels & xp ----
        BondLogic.addXp(cow, d, BondLevels.xpForLevel(5) - d.xp);
        check(d.level() == 5, "xp reaches level 5 (got " + d.level() + ")");
        check(BondLevels.levelFor(BondLevels.maxXp() + 999) == 10, "level caps at 10");

        // ---- Gifts ----
        d.lastGift = level.getGameTime() - 10_000_000L;
        ItemStack gift = BondLogic.tryGift(level, cow, d);
        check(!gift.isEmpty(), "cow gives a gift at level 5 (" + gift + ")");
        check(BondLogic.tryGift(level, cow, d).isEmpty(), "no second gift right away");

        // ---- Pedestal scan tick ----
        PedestalBlockEntity be = (PedestalBlockEntity) level.getBlockEntity(pedestal);
        int xpBefore = d.xp;
        be.updatePen(level);
        check(d.home.isPresent() && d.home.get().pos().equals(pedestal), "pen scan sets home");
        check(d.xp >= xpBefore, "pen scan never removes xp");

        // ---- Monster: peaceful + fainting ----
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setPos(origin.getX() + 3, origin.getY(), origin.getZ() - 2);
        BondData zd = BondLogic.bond(zombie);
        zombie.setTarget(fake);
        check(zombie.getTarget() == null, "bonded zombie does not target players");
        zombie.setTarget(cow2);
        check(zombie.getTarget() == null, "bonded zombie does not attack other creatures");
        Mob golem = EntityTypes.IRON_GOLEM.spawn(level, origin.offset(3, 0, -2), EntitySpawnReason.COMMAND);
        golem.setTarget(zombie);
        check(golem.getTarget() == null, "iron golem leaves a bonded zombie alone");
        golem.discard();
        BondLogic.addXp(zombie, zd, BondLevels.xpForLevel(7));
        check(zd.level() >= 7, "zombie at level 7");
        be.updatePen(level);
        check(zd.home.isPresent(), "zombie got a home pen");
        zombie.teleportTo(origin.getX() + 3.5, origin.getY(), origin.getZ() - 4.5);
        check(!pen.contains(zombie.getBlockX(), zombie.getBlockZ()), "zombie moved out of the pen");
        zombie.hurtServer(level, level.damageSources().generic(), 1000.0F);
        check(zombie.isAlive() && zd.isFainted(), "level 7 zombie faints instead of dying");
        check(zombie.isNoAi(), "fainted zombie has no AI");
        check(pen.contains(zombie.getBlockX(), zombie.getBlockZ()), "fainted zombie is back in the pen");

        Mob wild = EntityTypes.ZOMBIE.spawn(level, origin.offset(3, 0, -2), EntitySpawnReason.COMMAND);
        wild.hurtServer(level, level.damageSources().generic(), 1000.0F);
        check(!wild.isAlive(), "unbonded zombie dies normally");

        // ---- Riding ----
        cow2.setPos(origin.getX() + 3, origin.getY(), origin.getZ() - 1);
        BondData c2 = BondLogic.bond(cow2);
        BondLogic.addXp(cow2, c2, BondLevels.xpForLevel(5));
        check(c2.level() >= BondConfig.RIDE_LEVEL.get(), "cow2 reaches riding level");

        // ---- Petting & hand feeding (real handlers) ----
        Mob sheep = EntityTypes.SHEEP.spawn(level, origin.offset(2, 0, 5), EntitySpawnReason.COMMAND);
        boolean bonded = false;
        for (int i = 0; i < 60 && !bonded; i++) {
            pl.kubalte.animalbonds.bond.BondEvents.handFeed(fake, sheep, BondLogic.get(sheep), new ItemStack(Items.WHEAT, 64));
            bonded = BondLogic.isBonded(sheep);
        }
        check(bonded, "hand feeding wheat eventually bonds a sheep");
        BondData sd = BondLogic.get(sheep);
        sd.lastTreat = Long.MIN_VALUE / 2;
        int x0 = sd.xp;
        pl.kubalte.animalbonds.bond.BondEvents.handFeed(fake, sheep, sd, new ItemStack(Items.WHEAT, 64));
        check(sd.xp > x0, "treat gives xp (" + x0 + " -> " + sd.xp + ")");
        int x1 = sd.xp;
        pl.kubalte.animalbonds.bond.BondEvents.handFeed(fake, sheep, sd, new ItemStack(Items.WHEAT, 64));
        check(sd.xp == x1, "second treat right away gives no xp");
        pl.kubalte.animalbonds.bond.BondEvents.pet(fake, sheep, sd);
        int x2 = sd.xp;
        check(x2 > x1, "petting gives xp");
        pl.kubalte.animalbonds.bond.BondEvents.pet(fake, sheep, sd);
        check(sd.xp == x2, "petting has a cooldown");

        // ---- Ownership & trusted players ----
        FakePlayer alice = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.fromString("00000000-0000-0000-0000-0000000000a1"), "Alice"));
        FakePlayer bob = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.fromString("00000000-0000-0000-0000-0000000000b0"), "Bob"));
        PedestalBlockEntity ped = (PedestalBlockEntity) level.getBlockEntity(pedestal);
        ped.setOwner(alice);
        BondData ad = BondLogic.get(sheep);
        ad.owner = java.util.Optional.of(alice.getUUID());
        ad.home = java.util.Optional.of(net.minecraft.core.GlobalPos.of(level.dimension(), pedestal));
        check(pl.kubalte.animalbonds.bond.BondAccess.canUse(alice, sheep, ad), "owner can use her creature");
        check(!pl.kubalte.animalbonds.bond.BondAccess.canUse(bob, sheep, ad), "stranger cannot use someone else's creature");
        check(!ped.access().isTrusted(bob), "stranger is not trusted at the Chronicle");
        ped.access().members.put(bob.getUUID(), "Bob");
        check(pl.kubalte.animalbonds.bond.BondAccess.canUse(bob, sheep, ad), "trusted player can use the creature");
        int bx = ad.xp;
        ad.lastTreat = Long.MIN_VALUE / 2;
        ped.access().members.remove(bob.getUUID());
        pl.kubalte.animalbonds.bond.BondEvents.handFeed(bob, sheep, ad, new ItemStack(Items.WHEAT, 64));
        check(ad.xp == bx, "stranger's treat gives no bond xp");

        // ---- Size variety ----
        Mob sized = EntityTypes.COW.spawn(level, origin.offset(4, 0, -2), EntitySpawnReason.COMMAND);
        var sc = sized.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        double sv = sc.getValue();
        check(sc.hasModifier(net.minecraft.resources.Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "size")), "new creature got a random size");
        check(sv >= BondConfig.MIN_SCALE.get() - 1e-6 && sv <= BondConfig.MAX_SCALE.get() + 1e-6, "size within range (" + sv + ")");
        double minS = 9, maxS = 0;
        for (int i = 0; i < 30; i++) {
            Mob c = EntityTypes.CHICKEN.spawn(level, origin.offset(5, 0, -2), EntitySpawnReason.COMMAND);
            double v = c.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).getValue();
            minS = Math.min(minS, v);
            maxS = Math.max(maxS, v);
            c.discard();
        }
        check(maxS - minS > 0.08, "sizes vary between creatures (" + minS + " .. " + maxS + ")");
        sized.discard();

        // ---- Splicing (hybrids) ----
        Mob pa = EntityTypes.COW.spawn(level, origin.offset(2, 0, 2), EntitySpawnReason.COMMAND);
        Mob pb = EntityTypes.SPIDER.spawn(level, origin.offset(3, 0, 2), EntitySpawnReason.COMMAND);
        for (Mob m : new Mob[] {pa, pb}) {
            BondData md = BondLogic.bond(m);
            md.owner = java.util.Optional.of(alice.getUUID());
            BondLogic.addXp(m, md, BondLevels.xpForLevel(6));
        }
        alice.getInventory().add(new ItemStack(Items.GOLDEN_APPLE, 3));
        var ch = new pl.kubalte.animalbonds.bond.Hybrids.Choices(false, true, true, false, 2, true);
        Mob hybrid = pl.kubalte.animalbonds.bond.Hybrids.splice(level, alice, origin.offset(4, 0, 4), pa, pb, ch);
        check(hybrid != null, "splicing cow + spider creates a hybrid");
        if (hybrid != null) {
            var hd = pl.kubalte.animalbonds.bond.Hybrids.get(hybrid);
            check(hybrid.getType() == EntityTypes.COW, "hybrid has the cow's body");
            check(hd != null && hd.donorType().equals("minecraft:spider") && hd.head() && hd.legs() && !hd.extras() && hd.movement(),
                    "hybrid remembers the chosen parts (" + hd + ")");
            check(SpeciesProfiles.get(hybrid).needs().contains(Need.DARKNESS) && SpeciesProfiles.get(hybrid).needs().contains(Need.GRASS),
                    "hybrid has the needs of both parents");
            check(SpeciesProfiles.isFavourite(hybrid, new ItemStack(Items.WHEAT)) && SpeciesProfiles.isFavourite(hybrid, new ItemStack(Items.SPIDER_EYE)),
                    "hybrid likes the food of both parents");
            check(pl.kubalte.animalbonds.bond.RideControl.canClimb(pl.kubalte.animalbonds.bond.Hybrids.movementSource(hybrid)),
                    "cow-spider hybrid climbs walls like a spider");
            check(BondLogic.isBonded(hybrid) && BondLogic.get(hybrid).owner.equals(java.util.Optional.of(alice.getUUID())), "hybrid is bonded to its maker");
            check(pl.kubalte.animalbonds.bond.Hybrids.splice(level, alice, origin.offset(4, 0, 4), pa, pb, ch) == null, "parents must rest before splicing again");
            check(pl.kubalte.animalbonds.bond.Hybrids.problem(alice, hybrid, pa) != null, "hybrids can't be spliced again");
            var json2 = pl.kubalte.animalbonds.bond.HybridData.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, hd).getOrThrow();
            check(pl.kubalte.animalbonds.bond.HybridData.MAP_CODEC.codec().parse(JsonOps.INSTANCE, json2).getOrThrow().equals(hd), "hybrid data save/load");
            hybrid.discard();
        }
        Mob pc = EntityTypes.PIG.spawn(level, origin.offset(2, 0, 3), EntitySpawnReason.COMMAND);
        Mob pd = EntityTypes.PHANTOM.spawn(level, origin.offset(3, 0, 3), EntitySpawnReason.COMMAND);
        for (Mob m : new Mob[] {pc, pd}) {
            BondData md = BondLogic.bond(m);
            md.owner = java.util.Optional.of(alice.getUUID());
            BondLogic.addXp(m, md, BondLevels.xpForLevel(6));
        }
        Mob flyingPig = pl.kubalte.animalbonds.bond.Hybrids.splice(level, alice, origin.offset(4, 0, 4), pc, pd,
                new pl.kubalte.animalbonds.bond.Hybrids.Choices(false, false, false, true, 0, true));
        check(flyingPig != null && pl.kubalte.animalbonds.bond.RideControl.canFly(pl.kubalte.animalbonds.bond.Hybrids.movementSource(flyingPig)),
                "pig with phantom wings and movement flies");
        Mob noCost = pl.kubalte.animalbonds.bond.Hybrids.splice(level, bob, origin.offset(4, 0, 4), pa, pb, ch);
        check(noCost == null, "splicing is refused for strangers");
        for (Mob m : new Mob[] {pa, pb, pc, pd, flyingPig}) if (m != null) m.discard();

        // ---- Serialization ----
        var json = BondData.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, d).getOrThrow();
        BondData back = BondData.MAP_CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        check(back.xp == d.xp && back.home.equals(d.home), "bond data codec round trip");
        BookEntry entry = BookEntry.of(cow, pen, level.getGameTime());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        BookEntry.STREAM_CODEC.encode(buf, entry);
        BookEntry decoded = BookEntry.STREAM_CODEC.decode(buf);
        check(decoded.equals(entry), "book entry network round trip");
        check(entry.bonded() && entry.level() == d.level() && entry.needs().size() == needs.size(), "book entry content");

        // ---- Every vanilla creature profile resolves ----
        check(SpeciesProfiles.hasProfile(cow) && SpeciesProfiles.hasProfile(zombie), "profiles for cow and zombie");
    }
}
