package pl.kubalte.golebie;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.kubalte.golebie.bird.Bird;

/** CI test (-Dgolebie.selftest=true): taming, diapers, stroking, perching, nods. Logs PTEST lines. */
public final class PSelfTest {
    private PSelfTest() {}

    public static void register(IEventBus bus) {
        if (Boolean.getBoolean("golebie.selftest")) bus.addListener(PSelfTest::onTick);
    }

    private static final List<String> FAILS = new ArrayList<>();
    private static int checks, ticks;
    private static final BlockPos O = new BlockPos(100, 250, 100);

    static void check(boolean ok, String what) {
        checks++;
        if (ok) Golebie.LOGGER.info("PTEST ok   {}", what);
        else {
            FAILS.add(what);
            Golebie.LOGGER.error("PTEST FAIL {}", what);
        }
    }

    static void onTick(ServerTickEvent.Post e) {
        ticks++;
        ServerLevel level = e.getServer().overworld();
        if (ticks == 5) for (int cx = 5; cx <= 7; cx++) for (int cz = 5; cz <= 7; cz++) level.setChunkForced(cx, cz, true);
        if (ticks != 210) return;
        try {
            run(level);
        } catch (Throwable t) {
            Golebie.LOGGER.error("PTEST FAIL exception", t);
            FAILS.add("exception " + t);
        }
        if (FAILS.isEmpty()) Golebie.LOGGER.info("PTEST RESULT: PASS ({} checks)", checks);
        else Golebie.LOGGER.error("PTEST RESULT: FAIL ({} of {}): {}", FAILS.size(), checks, FAILS);
    }

    static void run(ServerLevel level) {
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            level.setBlockAndUpdate(O.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
            for (int y = 0; y < 4; y++) level.setBlockAndUpdate(O.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        FakePlayer player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("ptest".getBytes()), "PTest"));
        player.setPos(O.getX() + 0.5, O.getY(), O.getZ() + 0.5);
        Bird pigeon = Golebie.PIGEON.get().spawn(level, O.offset(2, 0, 0), EntitySpawnReason.COMMAND);
        Bird fantail = Golebie.FANTAIL.get().spawn(level, O.offset(-2, 0, 0), EntitySpawnReason.COMMAND);
        check(pigeon != null && fantail != null, "both birds spawn");
        if (pigeon == null || fantail == null) return;

        // taming with seeds
        for (Bird b : List.of(pigeon, fantail)) {
            for (int i = 0; i < 60 && !b.isTame(); i++) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 8));
                b.mobInteract(player, InteractionHand.MAIN_HAND);
            }
            check(b.isTame() && b.isOwnedBy(player), b.species() + " is tamed with seeds");
        }

        // diapers
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Golebie.DIAPERS.get(1).get()));
        pigeon.mobInteract(player, InteractionHand.MAIN_HAND);
        check(pigeon.diaper() == 2 && player.getMainHandItem().isEmpty(), "a diaper goes on");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Golebie.DIAPERS.get(2).get()));
        pigeon.mobInteract(player, InteractionHand.MAIN_HAND);
        check(pigeon.diaper() == 3 && dropped(level, pigeon, Golebie.DIAPERS.get(1).get()), "another diaper swaps it, the old one drops");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        pigeon.mobInteract(player, InteractionHand.MAIN_HAND);
        check(pigeon.diaper() == 0 && dropped(level, pigeon, Golebie.DIAPERS.get(2).get()), "shears take it off");

        // stroking
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        fantail.mobInteract(player, InteractionHand.MAIN_HAND);
        check(fantail.petTicks() > 0 && fantail.action() == Bird.A_PETTED, "stroked, it leans into the hand");

        // onto the shoulder and the head
        check(pigeon.perchNow(player, Bird.PERCH_LEFT) && pigeon.getVehicle() == player, "the pigeon sits on the left shoulder");
        pigeon.rideTick();
        Vec3 want = Bird.perchPos(player, Bird.PERCH_LEFT);
        check(pigeon.position().distanceTo(want) < 0.05 && Math.abs(pigeon.getY() - (player.getY() + player.getBbHeight() * 0.775)) < 0.05,
                "at shoulder height beside the head (" + pigeon.position() + " vs " + want + ")");
        check(fantail.perchNow(player, Bird.PERCH_HEAD) && Math.abs(Bird.perchPos(player, Bird.PERCH_HEAD).y - player.getY() - player.getBbHeight()) < 0.1,
                "the fantail sits on the head");
        check(player.getPassengers().size() == 2, "two birds on one player");
        check(pigeon.startPerchFlight(player, 0) == false || true, "room check does not crash");
        pigeon.hopOff(player);
        check(!pigeon.isPassenger() && pigeon.isLanding(), "it hops off and flutters down");
        fantail.hopOff(player);

        // nods
        float[] nod = {0, 0, 0, 0, 0, 0, 5, 12, 20, 24, 18, 8, 2, 0, 0, 0};
        float[] lookDown = {0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30};
        float[] still = new float[16];
        check(Nods.nodded(nod), "a quick nod is noticed");
        check(!Nods.nodded(lookDown) && !Nods.nodded(still), "looking down slowly is not a nod");
        int n = Nods.answer(player);
        check(n >= 2 && pigeon.pendingBow() > 0 && fantail.pendingBow() > 0, "tame birds around answer the nod (" + n + ")");

        // running up when you crouch
        pigeon.setPos(O.getX() + 6.5, O.getY(), O.getZ() + 0.5);
        player.setShiftKeyDown(true);
        check(pigeon.wouldRunToOwner(), "crouching calls it over");
        player.setShiftKeyDown(false);
        check(!pigeon.wouldRunToOwner(), "standing does not");

        // flying onto you
        check(pigeon.startPerchFlight(player, 0) && pigeon.isFlyingTowardsPerch(), "it takes off towards a shoulder");

        // tame parents, tame young
        var child = pigeon.getBreedOffspring(level, pigeon);
        check(child instanceof Bird c && c.isTame() && c.isOwnedBy(player), "young of tame birds are tame");
        check(!pigeon.hurtServer(level, level.damageSources().fall(), 4F), "no fall damage");
        pigeon.discard();
        fantail.discard();
    }

    static boolean dropped(ServerLevel level, Bird b, net.minecraft.world.item.Item item) {
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, new AABB(b.blockPosition()).inflate(3)))
            if (ie.getItem().is(item)) return true;
        return false;
    }
}
