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

        // ---- stage 2: extra drops, tanning rack, kettle, silk ----
        BlockPos rackPos = O.offset(10, 0, 8);
        level.setBlockAndUpdate(rackPos, Gospodarstwo.TANNING_RACK.get().defaultBlockState());
        var rack = (pl.kubalte.gospodarstwo.block.TanningRackBlockEntity) level.getBlockEntity(rackPos);
        rack.insert(player, new ItemStack(Gospodarstwo.RAW_HIDE.get(), 3));
        check(rack.input().getCount() == 3, "raw hides stretched on the tanning rack");
        for (int k = 0; k < GConfig.TAN_TICKS.get() / 2; k++) pl.kubalte.gospodarstwo.block.StationBlockEntity.tick(level, rackPos, level.getBlockState(rackPos), rack);
        check(rack.output().isEmpty() && rack.tanFraction() > 0.4F, "half way the hides are still tanning (" + rack.tanFraction() + ")");
        for (int k = 0; k < GConfig.TAN_TICKS.get() / 2 + 5; k++) pl.kubalte.gospodarstwo.block.StationBlockEntity.tick(level, rackPos, level.getBlockState(rackPos), rack);
        check(rack.output().is(Items.LEATHER) && rack.output().getCount() == 3, "all three hides become leather together (" + rack.output() + ")");
        rack.take(player);
        rack.insert(player, new ItemStack(Gospodarstwo.THICK_HIDE.get(), 1));
        for (int k = 0; k < GConfig.TAN_TICKS.get() + 5; k++) pl.kubalte.gospodarstwo.block.StationBlockEntity.tick(level, rackPos, level.getBlockState(rackPos), rack);
        check(rack.output().is(Gospodarstwo.THICK_LEATHER.get()), "thick hide becomes thick leather");

        BlockPos coldPos = O.offset(14, 0, 8);
        level.setBlockAndUpdate(coldPos, Gospodarstwo.KETTLE.get().defaultBlockState());
        var cold = (pl.kubalte.gospodarstwo.block.KettleBlockEntity) level.getBlockEntity(coldPos);
        cold.insert(player, new ItemStack(Items.HONEYCOMB, 2));
        for (int k = 0; k < GConfig.KETTLE_TICKS.get() * 3; k++) pl.kubalte.gospodarstwo.block.StationBlockEntity.tick(level, coldPos, level.getBlockState(coldPos), cold);
        check(cold.input().getCount() == 2 && cold.output().isEmpty(), "a kettle without fire does nothing");
        BlockPos hotPos = O.offset(16, 0, 8);
        level.setBlockAndUpdate(hotPos.below(), Blocks.CAMPFIRE.defaultBlockState());
        level.setBlockAndUpdate(hotPos, Gospodarstwo.KETTLE.get().defaultBlockState());
        var hot = (pl.kubalte.gospodarstwo.block.KettleBlockEntity) level.getBlockEntity(hotPos);
        hot.insert(player, new ItemStack(Items.HONEYCOMB, 2));
        for (int k = 0; k < GConfig.KETTLE_TICKS.get() * 2 + 5; k++) pl.kubalte.gospodarstwo.block.StationBlockEntity.tick(level, hotPos, level.getBlockState(hotPos), hot);
        check(hot.output().is(Gospodarstwo.BEESWAX.get()) && hot.output().getCount() == 4, "honeycomb renders into beeswax over a campfire (" + hot.output() + ")");
        hot.take(player);
        hot.insert(player, new ItemStack(Items.BONE, 3));
        for (int k = 0; k < GConfig.KETTLE_TICKS.get() + 5; k++) pl.kubalte.gospodarstwo.block.StationBlockEntity.tick(level, hotPos, level.getBlockState(hotPos), hot);
        check(hot.output().is(Gospodarstwo.GLUE.get()), "bones boil into glue");
        hot.take(player);
        hot.insert(player, new ItemStack(Gospodarstwo.FATBACK.get(), 2));
        for (int k = 0; k < GConfig.KETTLE_TICKS.get() + 5; k++) pl.kubalte.gospodarstwo.block.StationBlockEntity.tick(level, hotPos, level.getBlockState(hotPos), hot);
        check(hot.output().is(Gospodarstwo.LARD.get()), "fatback renders into lard");

        wheel.take(player);
        wheel.take(player);
        wheel.insert(player, new ItemStack(Gospodarstwo.SPINNERET.get(), 2));
        for (int k = 0; k < GConfig.LOOM_TICKS.get() + 10; k++) pl.kubalte.gospodarstwo.block.StationBlockEntity.tick(level, wheelPos, level.getBlockState(wheelPos), wheel);
        check(wheel.output().is(Gospodarstwo.YARN_SILK.get()), "spider spinnerets spin into silk yarn (" + wheel.output() + ")");

        check(!pl.kubalte.gospodarstwo.craft.MobDrops.dropsOf(EntityTypes.COW).isEmpty()
                && !pl.kubalte.gospodarstwo.craft.MobDrops.dropsOf(EntityTypes.WARDEN).isEmpty(), "extra drops are defined for vanilla mobs");
        BlockPos field = O.offset(26, 0, 22);
        for (int k = 0; k < 16; k++) {
            Mob c = EntityTypes.COW.spawn(level, field.offset(k % 4, 0, k / 4), EntitySpawnReason.COMMAND);
            if (c != null) c.kill(level);
        }
        check(itemsAround(level, field.offset(2, 0, 2), Gospodarstwo.RAW_HIDE.get()) > 0, "cows drop raw hide");
        check(itemsAround(level, field.offset(2, 0, 2), Gospodarstwo.TALLOW.get()) > 0, "cows drop tallow");
        Mob rav = EntityTypes.RAVAGER.spawn(level, O.offset(36, 0, 27), EntitySpawnReason.COMMAND);
        if (rav != null) rav.kill(level);
        check(itemsAround(level, O.offset(36, 0, 27), Gospodarstwo.RAVAGER_HORN.get()) > 0, "a ravager always drops its horn");

        // ---- stage 3: clothes, bedding, curtains, rugs ----
        ItemStack sweater = new ItemStack(Gospodarstwo.WOOL_SWEATER.get());
        var eq = sweater.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        check(eq != null && eq.slot() == net.minecraft.world.entity.EquipmentSlot.CHEST, "a sweater is worn on the chest");
        check(new ItemStack(Gospodarstwo.FELT_BOOTS.get()).get(net.minecraft.core.component.DataComponents.EQUIPPABLE).slot()
                == net.minecraft.world.entity.EquipmentSlot.FEET, "felt boots go on the feet");
        check(sweater.is(pl.kubalte.gospodarstwo.craft.Clothing.WARM) && sweater.is(net.minecraft.tags.ItemTags.FREEZE_IMMUNE_WEARABLES),
                "wool clothes are warm and protect from freezing");
        check(new ItemStack(Gospodarstwo.LINEN_SHIRT.get()).is(pl.kubalte.gospodarstwo.craft.Clothing.AIRY), "linen clothes are airy");
        check(Gospodarstwo.FELT_BOOTS.get().canWalkOnPowderedSnow(new ItemStack(Gospodarstwo.FELT_BOOTS.get()), player), "felt boots walk on powder snow");
        check(sweater.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.withDefaultNamespace("dyeable"))), "sweaters can be dyed");

        BlockPos bedFoot = O.offset(20, 0, 8);
        BlockState bed = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_bed")).defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.NORTH);
        level.setBlock(bedFoot.north(), bed.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD), 18);
        level.setBlock(bedFoot, bed.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT), 18);
        ItemStack pillow = new ItemStack(Gospodarstwo.PILLOW.get(), 2);
        pl.kubalte.gospodarstwo.craft.Bedding.add(level, bedFoot, player, pillow);
        check(pl.kubalte.gospodarstwo.craft.Bedding.comfort(level, bedFoot) == 1 && pillow.getCount() == 1, "a pillow is put on the bed");
        pl.kubalte.gospodarstwo.craft.Bedding.add(level, bedFoot.north(), player, pillow);
        check(pillow.getCount() == 1, "a second pillow is not used up");
        pl.kubalte.gospodarstwo.craft.Bedding.add(level, bedFoot, player, new ItemStack(Gospodarstwo.FEATHERBED.get()));
        check(pl.kubalte.gospodarstwo.craft.Bedding.comfort(level, bedFoot.north()) == 3, "pillow and featherbed on the bed");
        pl.kubalte.gospodarstwo.craft.Bedding.rest(player, pl.kubalte.gospodarstwo.craft.Bedding.comfort(level, bedFoot));
        var rested = player.getEffect(Gospodarstwo.RESTED);
        check(rested != null && rested.getAmplifier() == 1 && rested.getDuration() > 20000, "sleeping in a cosy bed makes you well rested (" + rested + ")");
        check(player.getMaxHealth() >= 27F, "rested gives extra hearts (" + player.getMaxHealth() + ")");
        pl.kubalte.gospodarstwo.craft.Bedding.onBreak(new net.neoforged.neoforge.event.level.block.BreakBlockEvent(level, bedFoot, level.getBlockState(bedFoot), player));
        check(itemsAround(level, bedFoot, Gospodarstwo.PILLOW.get()) == 1 && itemsAround(level, bedFoot, Gospodarstwo.FEATHERBED.get()) == 1,
                "breaking the bed gives back the pillow and featherbed");

        BlockPos cur = O.offset(24, 0, 8);
        BlockState curtain = Gospodarstwo.CURTAIN.get().defaultBlockState();
        level.setBlockAndUpdate(cur, curtain);
        level.setBlockAndUpdate(cur.above(), curtain);
        level.setBlockAndUpdate(cur.east(), curtain);
        level.setBlockAndUpdate(cur.east(3), curtain);
        pl.kubalte.gospodarstwo.block.CurtainBlock.toggle(level, cur);
        check(level.getBlockState(cur.above()).getValue(pl.kubalte.gospodarstwo.block.CurtainBlock.OPEN)
                && level.getBlockState(cur.east()).getValue(pl.kubalte.gospodarstwo.block.CurtainBlock.OPEN)
                && !level.getBlockState(cur.east(3)).getValue(pl.kubalte.gospodarstwo.block.CurtainBlock.OPEN), "the whole curtain opens, a separate one doesn't");
        pl.kubalte.gospodarstwo.block.ColoredBlocks.dye(level, cur, player, new ItemStack(Gospodarstwo.PLUM.get()));
        check(level.getBlockEntity(cur) instanceof pl.kubalte.gospodarstwo.block.ColoredBlockEntity cb && cb.color() == 0x6A2C70, "plums dye a curtain purple");
        BlockPos rugPos = O.offset(28, 0, 8);
        level.setBlockAndUpdate(rugPos, Gospodarstwo.WOOL_RUG.get().defaultBlockState());
        check(level.getBlockState(rugPos).is(Gospodarstwo.WOOL_RUG.get()), "a rug lies on the grass");
        pl.kubalte.gospodarstwo.block.ColoredBlocks.dye(level, rugPos, player, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.withDefaultNamespace("red_dye"))));
        check(level.getBlockEntity(rugPos) instanceof pl.kubalte.gospodarstwo.block.ColoredBlockEntity rb && rb.color() >= 0, "a rug can be dyed");

        // ---- stage 4: kitchen ----
        kitchen(level, player);

        // ---- stage 5: tools and farm equipment ----
        tools(level, player);
        check(Breed.byIndex(Breed.ANGORA.index()) == Breed.ANGORA && BreedEvents.breedsOf(cow).contains(Breed.HIGHLAND), "breed lookup");
        for (Mob m : new Mob[] {duck, goose, turkey, snail, cow, sheep}) if (m != null) m.discard();
    }

    static void work(ServerLevel level, BlockPos pos, pl.kubalte.gospodarstwo.block.StationBlockEntity be, int ticks) {
        for (int k = 0; k < ticks; k++) pl.kubalte.gospodarstwo.block.StationBlockEntity.tick(level, pos, level.getBlockState(pos), be);
    }

    static void kitchen(ServerLevel level, FakePlayer player) {
        BlockPos q = O.offset(2, 0, 4);
        level.setBlockAndUpdate(q, Gospodarstwo.QUERN.get().defaultBlockState());
        var quern = (pl.kubalte.gospodarstwo.kitchen.QuernBlockEntity) level.getBlockEntity(q);
        quern.insert(player, new ItemStack(Items.WHEAT, 2));
        work(level, q, quern, GConfig.QUERN_TICKS.get() * 2 + 5);
        check(quern.output().is(Gospodarstwo.FLOUR.get()) && quern.output().getCount() == 4, "the quern grinds 2 wheat into 4 flour (" + quern.output() + ")");

        BlockPos pr = O.offset(4, 0, 4);
        level.setBlockAndUpdate(pr, Gospodarstwo.FRUIT_PRESS.get().defaultBlockState());
        var press = (pl.kubalte.gospodarstwo.kitchen.PressBlockEntity) level.getBlockEntity(pr);
        press.insert(player, new ItemStack(Items.APPLE, 4));
        work(level, pr, press, GConfig.PRESS_TICKS.get() * 2 + 5);
        check(press.output().is(Gospodarstwo.APPLE_JUICE.get()) && press.output().getCount() == 2, "4 apples press into 2 bottles of juice");
        int before = player.getInventory().countItem(Gospodarstwo.APPLE_JUICE.get());
        player.setShiftKeyDown(false);
        press.take(player);
        check(player.getInventory().countItem(Gospodarstwo.APPLE_JUICE.get()) == before, "juice can't be taken without a bottle");
        ItemStack bottles = new ItemStack(Items.GLASS_BOTTLE, 3);
        press.insert(player, bottles);
        check(player.getInventory().countItem(Gospodarstwo.APPLE_JUICE.get()) == before + 1 && bottles.getCount() == 2, "a bottle is filled with juice");

        BlockPos cr = O.offset(6, 0, 4);
        level.setBlockAndUpdate(cr, Gospodarstwo.CROCK.get().defaultBlockState());
        var crock = (pl.kubalte.gospodarstwo.kitchen.CrockBlockEntity) level.getBlockEntity(cr);
        crock.insert(player, new ItemStack(Gospodarstwo.CABBAGE.get(), 8));
        work(level, cr, crock, GConfig.FERMENT_TICKS.get() / 2);
        check(crock.output().isEmpty(), "sauerkraut needs time");
        work(level, cr, crock, GConfig.FERMENT_TICKS.get() / 2 + 5);
        check(crock.output().is(Gospodarstwo.SAUERKRAUT.get()) && crock.output().getCount() == 8, "8 cabbages ferment into 8 sauerkraut (" + crock.output() + ")");

        BlockPos sm = O.offset(8, 0, 4);
        level.setBlockAndUpdate(sm, Gospodarstwo.SMOKEHOUSE.get().defaultBlockState());
        var smoke = (pl.kubalte.gospodarstwo.kitchen.SmokehouseBlockEntity) level.getBlockEntity(sm);
        smoke.insert(player, new ItemStack(Items.PORKCHOP, 3));
        work(level, sm, smoke, 200);
        check(smoke.output().isEmpty() && smoke.input().getCount() == 3, "without a fire nothing gets smoked");
        level.setBlockAndUpdate(sm.below(), Blocks.CAMPFIRE.defaultBlockState());
        work(level, sm, smoke, GConfig.SMOKE_TICKS.get() + 5);
        check(smoke.output().is(Gospodarstwo.SMOKED_HAM.get()) && smoke.output().getCount() == 3, "porkchops are smoked into ham over a campfire");

        BlockPos ch = O.offset(10, 0, 4);
        level.setBlockAndUpdate(ch, Gospodarstwo.BUTTER_CHURN.get().defaultBlockState());
        var churn = (pl.kubalte.gospodarstwo.kitchen.ChurnBlockEntity) level.getBlockEntity(ch);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
        churn.use(player, player.getMainHandItem());
        check(churn.milk() && player.getMainHandItem().is(Items.BUCKET), "milk poured into the churn, the bucket comes back");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        for (int k = 0; k < GConfig.CHURN_STROKES.get() - 1; k++) churn.use(player, ItemStack.EMPTY);
        check(churn.butter() == 0, "not butter yet");
        churn.use(player, ItemStack.EMPTY);
        check(churn.butter() == 2, "churning turns the milk into butter");
        int b = player.getInventory().countItem(Gospodarstwo.BUTTER.get());
        churn.use(player, ItemStack.EMPTY);
        check(player.getInventory().countItem(Gospodarstwo.BUTTER.get()) == b + 2, "butter taken out");

        BlockPos ke = O.offset(12, 0, 4);
        level.setBlockAndUpdate(ke.below(), Blocks.CAMPFIRE.defaultBlockState());
        level.setBlockAndUpdate(ke, Gospodarstwo.KETTLE.get().defaultBlockState());
        var kettle = (pl.kubalte.gospodarstwo.block.KettleBlockEntity) level.getBlockEntity(ke);
        kettle.insert(player, new ItemStack(Gospodarstwo.RAW_PIEROGI.get(), 4));
        work(level, ke, kettle, GConfig.KETTLE_TICKS.get() * 4 + 5);
        check(kettle.output().is(Gospodarstwo.PIEROGI.get()) && kettle.output().getCount() == 4, "pierogi boil in the kettle");
        check(Gospodarstwo.BIGOS.get().getDefaultInstance().get(net.minecraft.core.component.DataComponents.FOOD) != null, "bigos is food");
    }

    static void tools(ServerLevel level, FakePlayer player) {
        BlockPos f = O.offset(2, 0, 20);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            level.setBlockAndUpdate(f.offset(dx, -1, dz), Blocks.FARMLAND.defaultBlockState());
            level.setBlockAndUpdate(f.offset(dx, 0, dz), ((CropBlock) Blocks.WHEAT).getStateForAge(0));
        }
        ItemStack can = new ItemStack(Gospodarstwo.WATERING_CAN.get());
        check(pl.kubalte.gospodarstwo.tools.WateringCanItem.water(can) == 0, "a new watering can is empty");
        pl.kubalte.gospodarstwo.tools.WateringCanItem.setWater(can, 24);
        for (int k = 0; k < 6; k++) pl.kubalte.gospodarstwo.tools.WateringCanItem.waterArea(level, f, level.getRandom());
        int grown = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) grown += ((CropBlock) Blocks.WHEAT).getAge(level.getBlockState(f.offset(dx, 0, dz)));
        check(grown >= 9, "watering makes the crops grow (" + grown + " stages)");
        check(level.getBlockState(f.offset(1, -1, 1)).getValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE) == 7, "watering wets the farmland");

        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setBlockAndUpdate(f.offset(dx, 0, dz), ((CropBlock) Blocks.WHEAT).getStateForAge(7));
        int reaped = pl.kubalte.gospodarstwo.tools.ScytheItem.reap(level, f, player, new ItemStack(Gospodarstwo.SCYTHE.get()));
        check(reaped == 9, "the scythe reaps 9 ripe crops (" + reaped + ")");
        check(level.getBlockState(f).is(Blocks.WHEAT) && ((CropBlock) Blocks.WHEAT).getAge(level.getBlockState(f)) == 0, "and sows them again");
        check(itemsAround(level, f, Items.WHEAT) >= 9, "wheat lies on the field");

        BlockPos g = O.offset(8, 0, 20);
        BlockState grass = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("short_grass"))
                .defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setBlockAndUpdate(g.offset(dx, 0, dz), grass);
        int cut = pl.kubalte.gospodarstwo.tools.ToolEvents.cut(level, g, player);
        check(cut == 8, "the sickle cuts the grass around too (" + cut + ")");

        net.minecraft.world.entity.TamableAnimal wolf = (net.minecraft.world.entity.TamableAnimal) EntityTypes.WOLF.spawn(level, O.offset(20, 0, 24),
                EntitySpawnReason.COMMAND);
        wolf.tame(player);
        player.setPos(O.getX() + 4.5, O.getY(), O.getZ() + 24.5);
        int called = pl.kubalte.gospodarstwo.tools.HornItem.call(level, player);
        check(called == 1 && (!wolf.getNavigation().isDone() || wolf.distanceTo(player) < 5), "the hunting horn calls your wolf (" + called + ")");
        wolf.discard();
        Mob zombie = EntityTypes.ZOMBIE.spawn(level, O.offset(8, 0, 24), EntitySpawnReason.COMMAND);
        int scared = pl.kubalte.gospodarstwo.tools.HornItem.scare(level, player);
        check(scared >= 1 && zombie.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS), "the war horn scares monsters");
        zombie.discard();

        BlockPos inc = O.offset(14, 0, 20);
        level.setBlockAndUpdate(inc, Gospodarstwo.INCUBATOR.get().defaultBlockState());
        var incubator = (pl.kubalte.gospodarstwo.tools.IncubatorBlockEntity) level.getBlockEntity(inc);
        ItemStack eggs = new ItemStack(Gospodarstwo.GREEN_EGG.get(), 6);
        incubator.use(player, eggs);
        check(incubator.eggs().getCount() == 4 && eggs.getCount() == 2, "4 eggs fit in the incubator");
        for (int k = 0; k < GConfig.INCUBATE_TICKS.get() + 5; k++)
            pl.kubalte.gospodarstwo.tools.IncubatorBlockEntity.tick(level, inc, level.getBlockState(inc), incubator);
        var chicks = level.getEntitiesOfClass(Mob.class, new AABB(inc).inflate(2), m -> m.getType() == EntityTypes.CHICKEN);
        check(chicks.size() == 4 && chicks.stream().allMatch(c -> c.isBaby() && BreedEvents.breed(c) == Breed.GREENLEG),
                "green eggs hatch into 4 greenleg chicks (" + chicks.size() + ")");
        chicks.forEach(Mob::discard);

        BlockPos pond = O.offset(20, 0, 20);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) level.setBlockAndUpdate(pond.offset(dx, 0, dz), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(pond, Gospodarstwo.FISHING_NET.get().defaultBlockState().setValue(pl.kubalte.gospodarstwo.tools.FishingNetBlock.WATERLOGGED, true));
        var net = (pl.kubalte.gospodarstwo.tools.FishingNetBlockEntity) level.getBlockEntity(pond);
        check(net.water() >= 24, "the net stands in open water (" + net.water() + ")");
        for (int k = 0; k < 8; k++) net.catchOne();
        check(net.catches().size() == 6, "the net holds up to 6 catches");
        net.take(player);
        check(net.catches().isEmpty(), "the catch is taken out");

        BlockPos bed = O.offset(26, 0, 20);
        level.setBlockAndUpdate(bed, Gospodarstwo.RAISED_BED.get().defaultBlockState());
        level.setBlockAndUpdate(bed.above(), ((CropBlock) Blocks.CARROTS).getStateForAge(0));
        check(level.getBlockState(bed.above()).canSurvive(level, bed.above()), "carrots grow on the raised bed");
        check(level.getBlockState(bed).isFertile(level, bed), "the raised bed is always watered");
    }
}
