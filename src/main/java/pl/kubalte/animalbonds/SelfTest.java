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

    static void onStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        try {
            run(server.overworld());
        } catch (Throwable t) {
            AnimalBonds.LOGGER.error("ABTEST FAIL exception", t);
            FAILS.add("exception: " + t);
        }
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
        for (int cx = -1; cx <= 2; cx++) for (int cz = -1; cz <= 2; cz++) level.setChunkForced(cx, cz, true);

        // ---- Pen detection ----
        BlockPos pedestal = buildPen(level, origin);
        PenScanner.Pen pen = PenScanner.scan(level, pedestal);
        check(pen != null, "pen detected next to fence");
        check(pen != null && pen.area() == 36, "pen area is 36 (was " + (pen == null ? -1 : pen.area()) + ")");
        level.setBlockAndUpdate(origin.offset(7, 0, 4), Blocks.AIR.defaultBlockState());
        check(PenScanner.scan(level, pedestal) == null, "open pen is not detected");
        level.setBlockAndUpdate(origin.offset(7, 0, 4), Blocks.OAK_FENCE.defaultBlockState());
        check(level.getBlockEntity(pedestal) instanceof PedestalBlockEntity, "pedestal has block entity");

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
