package pl.kubalte.gospodarstwo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.kubalte.gospodarstwo.block.BonsaiData;
import pl.kubalte.gospodarstwo.block.BonsaiPotBlockEntity;
import pl.kubalte.gospodarstwo.block.FruitLeavesBlock;
import pl.kubalte.gospodarstwo.block.LoomBlockEntity;
import pl.kubalte.gospodarstwo.breed.Breed;
import pl.kubalte.gospodarstwo.breed.BreedEvents;

/** CI test (-Dgospodarstwo.selftest=true). Runs 200 ticks after start (test area loaded), logs GTEST lines. */
public final class GSelfTest {
    private GSelfTest() {}

    public static void register(IEventBus bus) {
        if (Boolean.getBoolean("gospodarstwo.selftest")) bus.addListener(GSelfTest::onTick);
    }

    private static final List<String> FAILS = new ArrayList<>();
    private static int checks, ticks;
    private static final BlockPos O = new BlockPos(-60, 250, -60);

    static void check(boolean ok, String what) {
        checks++;
        if (ok) Gospodarstwo.LOGGER.info("GTEST ok   {}", what);
        else {
            FAILS.add(what);
            Gospodarstwo.LOGGER.error("GTEST FAIL {}", what);
        }
    }

    static void onTick(ServerTickEvent.Post e) {
        ticks++;
        ServerLevel level = e.getServer().overworld();
        if (ticks == 5) {
            for (int cx = -5; cx <= -2; cx++) for (int cz = -5; cz <= -2; cz++) level.setChunkForced(cx, cz, true);
        }
        if (ticks != 200) return;
        try {
            run(level);
        } catch (Throwable t) {
            Gospodarstwo.LOGGER.error("GTEST FAIL exception", t);
            FAILS.add("exception " + t);
        }
        if (FAILS.isEmpty()) Gospodarstwo.LOGGER.info("GTEST RESULT: PASS ({} checks)", checks);
        else Gospodarstwo.LOGGER.error("GTEST RESULT: FAIL ({} of {}): {}", FAILS.size(), checks, FAILS);
    }

    private static boolean dropsContain(List<ItemStack> drops, Item item) {
        for (ItemStack s : drops) if (s.is(item)) return true;
        return false;
    }

    private static int itemsAround(ServerLevel level, BlockPos pos, Item item) {
        int n = 0;
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3))) if (ie.getItem().is(item)) n += ie.getItem().getCount();
        return n;
    }

    static void run(ServerLevel level) {
        for (int x = -2; x <= 40; x++) for (int z = -2; z <= 30; z++) {
            level.setBlockAndUpdate(O.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
            for (int y = 0; y < 8; y++) level.setBlockAndUpdate(O.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        FakePlayer player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("gtest".getBytes()), "GTest"));
        player.setPos(O.getX() + 0.5, O.getY(), O.getZ() - 2.5);

        // ---- crops: ripe crops drop their produce and seeds ----
        Object[][] crops = {{Gospodarstwo.FLAX.get(), Gospodarstwo.FLAX_FIBER.get(), Gospodarstwo.FLAX_SEEDS.get()},
                {Gospodarstwo.CABBAGES.get(), Gospodarstwo.CABBAGE.get(), Gospodarstwo.CABBAGE_SEEDS.get()},
                {Gospodarstwo.ONIONS.get(), Gospodarstwo.ONION.get(), Gospodarstwo.ONION.get()},
                {Gospodarstwo.STRAWBERRIES.get(), Gospodarstwo.STRAWBERRY.get(), Gospodarstwo.STRAWBERRY_RUNNER.get()}};
        int i = 0;
        for (Object[] c : crops) {
            CropBlock crop = (CropBlock) c[0];
            BlockPos p = O.offset(i * 2, 0, 0);
            level.setBlockAndUpdate(p.below(), Blocks.FARMLAND.defaultBlockState());
            level.setBlockAndUpdate(p, crop.defaultBlockState());
            check(level.getBlockState(p).is(crop), crop + " can be planted on farmland");
            for (int k = 0; k < 20 && !crop.isMaxAge(level.getBlockState(p)); k++) crop.performBonemeal(level, level.getRandom(), p, level.getBlockState(p));
            BlockState ripe = level.getBlockState(p);
            check(crop.isMaxAge(ripe), crop + " grows");
            List<ItemStack> drops = Block.getDrops(ripe, level, p, null);
            check(dropsContain(drops, (Item) c[1]) && dropsContain(drops, (Item) c[2]), crop + " drops " + c[1] + " and " + c[2] + ": " + drops);
            i++;
        }
        check(Gospodarstwo.FLAX_SEEDS.get() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() == Gospodarstwo.FLAX.get(),
                "flax seeds plant flax");

        // ---- gooseberry bush ----
        BlockPos bush = O.offset(10, 0, 0);
        level.setBlockAndUpdate(bush, Gospodarstwo.GOOSEBERRY_BUSH.get().defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(bush), Direction.UP, bush, false);
        level.getBlockState(bush).useWithoutItem(level, player, hit);
        check(itemsAround(level, bush, Gospodarstwo.GOOSEBERRIES.get()) > 0, "picking a ripe gooseberry bush gives gooseberries");
        check(level.getBlockState(bush).getValue(SweetBerryBushBlock.AGE) == 1, "picked bush starts over");

        // ---- fruit trees ----
        BlockPos sap = O.offset(14, 0, 4);
        level.setBlockAndUpdate(sap, Gospodarstwo.APPLE_SAPLING.get().defaultBlockState());
        BlockState sapState = level.getBlockState(sap);
        Gospodarstwo.APPLE_GROWER.growTree(level, level.getChunkSource().getGenerator(), sap, sapState, level.getRandom());
        int leaves = 0;
        for (BlockPos q : BlockPos.betweenClosed(sap.offset(-3, 0, -3), sap.offset(3, 8, 3))) if (level.getBlockState(q).is(Gospodarstwo.APPLE_LEAVES.get())) leaves++;
        check(level.getBlockState(sap).is(Blocks.OAK_LOG) && leaves > 10, "apple sapling grows into an apple tree (" + leaves + " leaves)");
        BlockPos plumSap = O.offset(22, 0, 4);
        level.setBlockAndUpdate(plumSap, Gospodarstwo.PLUM_SAPLING.get().defaultBlockState());
        Gospodarstwo.PLUM_GROWER.growTree(level, level.getChunkSource().getGenerator(), plumSap, level.getBlockState(plumSap), level.getRandom());
        check(!level.getBlockState(plumSap).is(Gospodarstwo.PLUM_SAPLING.get()), "plum sapling grows");

        BlockPos leaf = O.offset(30, 2, 0);
        level.setBlockAndUpdate(leaf, Gospodarstwo.PLUM_LEAVES.get().defaultBlockState().setValue(FruitLeavesBlock.FRUIT, 3)
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        level.getBlockState(leaf).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(leaf), Direction.DOWN, leaf, false));
        check(itemsAround(level, leaf, Gospodarstwo.PLUM.get()) > 0 && level.getBlockState(leaf).getValue(FruitLeavesBlock.FRUIT) == 0,
                "ripe plum leaves give plums");
        for (int k = 0; k < 200; k++) level.getBlockState(leaf).randomTick(level, leaf, level.getRandom());
        check(level.getBlockState(leaf).getValue(FruitLeavesBlock.FRUIT) == 3, "fruit ripens again (blossom -> fruit)");

        // ---- bonsai ----
        BlockPos pot = O.offset(0, 0, 8);
        level.setBlockAndUpdate(pot, Gospodarstwo.BONSAI_POT.get().defaultBlockState());
        BonsaiPotBlockEntity bonsai = (BonsaiPotBlockEntity) level.getBlockEntity(pot);
        ItemStack sapling = new ItemStack(Items.OAK_SAPLING);
        player.setItemInHand(InteractionHand.MAIN_HAND, sapling);
        bonsai.use(player, InteractionHand.MAIN_HAND, sapling);
        check(bonsai.hasTree() && sapling.isEmpty(), "oak sapling planted in the bonsai pot");
        for (int k = 0; k < 100; k++) bonsai.step(level, 20);
        check(bonsai.data().growth() == 0, "without water the bonsai doesn't grow");
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        bonsai.use(player, InteractionHand.MAIN_HAND, bucket);
        int need = GConfig.BONSAI_TICKS.get() / 20 + 5;
        for (int k = 0; k < need * 2; k++) {
            bonsai.step(level, 20);
            if (bonsai.data().water() < 1000) bonsai.use(player, InteractionHand.MAIN_HAND, bucket);
        }
        check(bonsai.data().growth() == BonsaiPotBlockEntity.WILD_CAP, "untrimmed tree stops as a shaggy bush (" + bonsai.data().growth() + ")");
        ItemStack shears = new ItemStack(Items.SHEARS);
        bonsai.setData(new BonsaiData(bonsai.data().sapling(), 300, 0, 1L, 20000, 0));
        bonsai.use(player, InteractionHand.MAIN_HAND, shears);
        bonsai.setData(new BonsaiData(bonsai.data().sapling(), 600, bonsai.data().trims(), 1L, 20000, 300));
        bonsai.use(player, InteractionHand.MAIN_HAND, shears);
        check(bonsai.data().trims() == 2, "trimming twice while it grows (" + bonsai.data().trims() + ")");
        for (int k = 0; k < need * 2; k++) {
            bonsai.step(level, 20);
            if (bonsai.data().water() < 1000) bonsai.use(player, InteractionHand.MAIN_HAND, bucket);
        }
        check(BonsaiPotBlockEntity.stage(bonsai.data()) == 3, "cared-for tree becomes a finished bonsai");
        List<ItemStack> potDrops = Block.getDrops(level.getBlockState(pot), level, pot, bonsai);
        check(!potDrops.isEmpty() && potDrops.get(0).get(Gospodarstwo.BONSAI_DATA.get()) != null,
                "picking up the pot keeps the bonsai on the item (" + potDrops + ")");

        // ---- loom ----
        BlockPos loomPos = O.offset(4, 0, 8);
        level.setBlockAndUpdate(loomPos, Gospodarstwo.WEAVING_LOOM.get().defaultBlockState());
        LoomBlockEntity loom = (LoomBlockEntity) level.getBlockEntity(loomPos);
        ItemStack string = new ItemStack(Items.STRING, 8);
        loom.insert(player, string);
        check(loom.input().getCount() == 8 && string.isEmpty(), "string goes into the loom");
        for (int k = 0; k < GConfig.LOOM_TICKS.get() * 2 + 10; k++) LoomBlockEntity.tick(level, loomPos, level.getBlockState(loomPos), loom);
        check(loom.output().is(Gospodarstwo.CANVAS.get()) && loom.output().getCount() == 2, "loom weaves 8 string into 2 canvas (" + loom.output() + ")");
        loom.take(player);
        check(player.getInventory().countItem(Gospodarstwo.CANVAS.get()) == 2, "cloth taken from the loom");
        ItemStack linenYarn = new ItemStack(Gospodarstwo.YARN_LINEN.get(), 3);
        loom.insert(player, linenYarn);
        for (int k = 0; k < GConfig.LOOM_TICKS.get() + 5; k++) LoomBlockEntity.tick(level, loomPos, level.getBlockState(loomPos), loom);
        check(loom.output().is(Gospodarstwo.LINEN.get()), "linen yarn becomes linen");
        loom.take(player);

        // ---- textiles: spinning wheel, dye vat, dyed cloth ----
        BlockPos wheelPos = O.offset(6, 0, 8);
        level.setBlockAndUpdate(wheelPos, Gospodarstwo.SPINNING_WHEEL.get().defaultBlockState());
        var wheel = (pl.kubalte.gospodarstwo.block.SpinningWheelBlockEntity) level.getBlockEntity(wheelPos);
        wheel.insert(player, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_wool")), 2));
        for (int k = 0; k < GConfig.LOOM_TICKS.get() * 2 + 10; k++) pl.kubalte.gospodarstwo.block.StationBlockEntity.tick(level, wheelPos, level.getBlockState(wheelPos), wheel);
        check(wheel.output().is(Gospodarstwo.YARN_WOOL.get()) && wheel.output().getCount() == 4, "spinning 2 red wool gives 4 wool yarn (" + wheel.output() + ")");
        check(pl.kubalte.gospodarstwo.craft.Dyes.colorOf(wheel.output()) >= 0, "yarn from red wool is red");
        wheel.take(player);
        wheel.insert(player, new ItemStack(Gospodarstwo.FLAX_FIBER.get(), 4));
        for (int k = 0; k < GConfig.LOOM_TICKS.get() * 2 + 10; k++) pl.kubalte.gospodarstwo.block.StationBlockEntity.tick(level, wheelPos, level.getBlockState(wheelPos), wheel);
        check(wheel.output().is(Gospodarstwo.YARN_LINEN.get()) && wheel.output().getCount() == 2, "flax fibre spins into linen yarn");

        BlockPos vatPos = O.offset(8, 0, 8);
        level.setBlockAndUpdate(vatPos, Gospodarstwo.DYE_VAT.get().defaultBlockState());
        var vat = (pl.kubalte.gospodarstwo.block.DyeVatBlockEntity) level.getBlockEntity(vatPos);
        ItemStack dye = new ItemStack(Gospodarstwo.PLUM.get());
        vat.use(player, InteractionHand.MAIN_HAND, dye);
        check(vat.charges() == 0, "no dye without water");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        vat.use(player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        check(vat.hasWater(), "vat filled with water");
        vat.use(player, InteractionHand.MAIN_HAND, dye);
        check(vat.charges() == 8 && vat.color() == 0x6A2C70, "plums dye the water purple");
        ItemStack yarn = new ItemStack(Gospodarstwo.YARN_MERINO.get(), 6);
        vat.use(player, InteractionHand.MAIN_HAND, yarn);
        check(pl.kubalte.gospodarstwo.craft.Dyes.colorOf(yarn) == 0x6A2C70 && vat.charges() == 2, "merino yarn dipped in the vat turns purple");
        loom.insert(player, yarn);
        for (int k = 0; k < GConfig.LOOM_TICKS.get() * 2 + 10; k++) LoomBlockEntity.tick(level, loomPos, level.getBlockState(loomPos), loom);
        check(loom.output().is(Gospodarstwo.MERINO_CLOTH.get()) && pl.kubalte.gospodarstwo.craft.Dyes.colorOf(loom.output()) == 0x6A2C70,
                "purple yarn weaves into purple merino cloth (" + loom.output() + ")");
        vat.use(player, InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("yellow_dye"))));
        check(vat.charges() == 10 && vat.color() != 0x6A2C70, "a second dye mixes into the bath");

        // ---- animals ----
        Mob duck = Gospodarstwo.DUCK.get().spawn(level, O.offset(10, 0, 12), EntitySpawnReason.COMMAND);
        Mob goose = Gospodarstwo.GOOSE.get().spawn(level, O.offset(12, 0, 12), EntitySpawnReason.COMMAND);
        Mob turkey = Gospodarstwo.TURKEY.get().spawn(level, O.offset(14, 0, 12), EntitySpawnReason.COMMAND);
        Mob snail = Gospodarstwo.SNAIL.get().spawn(level, O.offset(16, 0, 12), EntitySpawnReason.COMMAND);
        check(duck != null && goose != null && turkey != null && snail != null, "duck, goose, turkey and snail spawn");
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
        snail.interact(player, InteractionHand.MAIN_HAND, snail.position());
        check(player.getInventory().countItem(Gospodarstwo.SNAIL_SLIME.get()) == 1, "snail slime collected with a bottle");

        // ---- breeds ----
        Mob cow = EntityTypes.COW.spawn(level, O.offset(20, 0, 16), EntitySpawnReason.COMMAND);
        check(cow.hasData(Gospodarstwo.BREED.get()), "new cows get a breed roll");
        BreedEvents.setBreed(cow, Breed.HIGHLAND);
        player.setPos(cow.getX() + 1, cow.getY(), cow.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        player.interactOn(cow, InteractionHand.MAIN_HAND, cow.position());
        check(itemsAround(level, cow.blockPosition(), Gospodarstwo.HIGHLAND_HAIR.get()) > 0, "shearing a highland cow gives highland hair");
        int before = itemsAround(level, cow.blockPosition(), Gospodarstwo.HIGHLAND_HAIR.get());
        player.interactOn(cow, InteractionHand.MAIN_HAND, cow.position());
        check(itemsAround(level, cow.blockPosition(), Gospodarstwo.HIGHLAND_HAIR.get()) == before, "hair needs time to grow back");

        Mob sheep = EntityTypes.SHEEP.spawn(level, O.offset(24, 0, 16), EntitySpawnReason.COMMAND);
        BreedEvents.setBreed(sheep, Breed.MERINO);
        player.setPos(sheep.getX() + 1, sheep.getY(), sheep.getZ());
        player.interactOn(sheep, InteractionHand.MAIN_HAND, sheep.position());
        check(itemsAround(level, sheep.blockPosition(), Gospodarstwo.MERINO_WOOL.get()) > 0, "merino gives merino wool when sheared");

        Mob goat = EntityTypes.GOAT.spawn(level, O.offset(28, 0, 16), EntitySpawnReason.COMMAND);
        player.setPos(goat.getX() + 1, goat.getY(), goat.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Gospodarstwo.CURRY_COMB.get()));
        player.interactOn(goat, InteractionHand.MAIN_HAND, goat.position());
        check(itemsAround(level, goat.blockPosition(), Gospodarstwo.CASHMERE.get()) > 0, "brushing a goat gives cashmere");
        Mob llama = EntityTypes.LLAMA.spawn(level, O.offset(32, 0, 16), EntitySpawnReason.COMMAND);
        player.setPos(llama.getX() + 1, llama.getY(), llama.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        player.interactOn(llama, InteractionHand.MAIN_HAND, llama.position());
        check(itemsAround(level, llama.blockPosition(), Gospodarstwo.LLAMA_WOOL.get()) > 0, "shearing a llama gives llama wool");
        goat.discard();
        llama.discard();
        check(Breed.byIndex(Breed.ANGORA.index()) == Breed.ANGORA && BreedEvents.breedsOf(cow).contains(Breed.HIGHLAND), "breed lookup");
        for (Mob m : new Mob[] {duck, goose, turkey, snail, cow, sheep}) if (m != null) m.discard();
    }
}
