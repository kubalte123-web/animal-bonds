package pl.kubalte.gospodarstwo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.block.BonsaiData;
import pl.kubalte.gospodarstwo.block.BonsaiPotBlockEntity;
import pl.kubalte.gospodarstwo.block.FruitLeavesBlock;
import pl.kubalte.gospodarstwo.block.LoomBlockEntity;
import pl.kubalte.gospodarstwo.block.WeavingLoomBlock;
import pl.kubalte.gospodarstwo.breed.Breed;
import pl.kubalte.gospodarstwo.breed.BreedEvents;

/** CI visual test (-Dgospodarstwo.clienttest=true): logs "GCLIENT SHOT name" when a screenshot should be taken. */
public final class GClientTest {
    private GClientTest() {}

    private static int ticks = -1;
    private static final BlockPos B = new BlockPos(1300, -60, 1300);

    public static void register(IEventBus modBus) {
        if (Boolean.getBoolean("gospodarstwo.clienttest")) NeoForge.EVENT_BUS.addListener(GClientTest::onTick);
    }

    private static void shot(String name) {
        Gospodarstwo.LOGGER.info("GCLIENT SHOT {}", name);
    }

    static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return;
        ticks++;
        String m = GClient.useGecko() ? "gecko" : "plain";
        switch (ticks) {
            case 40 -> server.execute(() -> {
                cmd(server, "gamemode creative @a");
                cmd(server, "time set 6000");
                cmd(server, "gamerule doDaylightCycle false");
                cmd(server, "weather clear");
                cmd(server, "kill @e[type=!player,distance=..300]");
                view(server, 0, 0);
            });
            case 120 -> server.execute(() -> scene(server));
            case 220 -> shot(m + "animals");
            case 260 -> server.execute(() -> view(server, 20, 0));
            case 340 -> shot(m + "breeds");
            case 380 -> server.execute(() -> view(server, 44, 0));
            case 460 -> shot(m + "plants");
            case 500 -> server.execute(() -> cmd(server, String.format("tp @p %d %d %d 0 5", B.getX() + 65, B.getY() + 1, B.getZ() - 7)));
            case 580 -> shot(m + "trees");
            case 620 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 40", B.getX() + 84.5, B.getY() + 1,
                    B.getZ() - 1.6)));
            case 700 -> shot(m + "bonsai");
            case 740 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 30 35", B.getX() + 101.3, B.getY() + 1,
                    B.getZ() - 1.2)));
            case 800 -> shot(m + "loom");
            case 830 -> shot(m + "loom2");
            case 860 -> server.execute(() -> textiles(server));
            case 900 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 30", B.getX() + 116.0, B.getY() + 1,
                    B.getZ() - 2.6)));
            case 980 -> shot(m + "textiles");
            case 1000 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %.1f %.1f 0 0", B.getX() + 127.5, B.getY() + 1.2,
                    B.getZ() - 3.6)));
            case 1080 -> shot(m + "items1");
            case 1100 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %.1f %.1f 0 0", B.getX() + 136.5, B.getY() + 1.2,
                    B.getZ() - 3.6)));
            case 1180 -> shot(m + "items2");
            case 1220 -> {
                Gospodarstwo.LOGGER.info("GCLIENT DONE");
                mc.stop();
            }
            default -> {}
        }
    }

    static void cmd(MinecraftServer server, String c) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
    }

    /** Stands in front of a row (things face north, the player looks south at them). */
    static void view(MinecraftServer server, int x, int z) {
        cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 22", B.getX() + x + 4.5, B.getY() + 1, B.getZ() + z - 5.5));
    }

    static Mob put(ServerLevel level, EntityType<? extends Mob> type, BlockPos pos, Breed breed, boolean baby) {
        Mob mob = type.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (mob == null) return null;
        mob.setNoAi(true);
        mob.setYRot(180);
        mob.yBodyRot = 180;
        mob.yHeadRot = 180;
        if (baby && mob instanceof AgeableMob a) a.setBaby(true);
        if (breed != null || BreedEvents.breedsOf(mob).size() > 0) BreedEvents.setBreed(mob, breed);
        return mob;
    }

    /** Spinning wheel, dye vat and a loom at work, and a wall of item frames with every item of the mod. */
    static void textiles(MinecraftServer server) {
        ServerLevel level = server.overworld();
        var p = server.getPlayerList().getPlayers().get(0);
        for (int x = 108; x <= 150; x++) for (int z = -8; z <= 6; z++) for (int y = 0; y < 6; y++)
            level.setBlockAndUpdate(B.offset(x, y, z), Blocks.AIR.defaultBlockState());
        BlockPos wheel = B.offset(114, 0, 0), vat = B.offset(116, 0, 0), loom = B.offset(118, 0, 0);
        level.setBlockAndUpdate(wheel, Gospodarstwo.SPINNING_WHEEL.get().defaultBlockState().setValue(pl.kubalte.gospodarstwo.block.StationBlock.FACING,
                net.minecraft.core.Direction.WEST));
        level.setBlockAndUpdate(vat, Gospodarstwo.DYE_VAT.get().defaultBlockState());
        level.setBlockAndUpdate(loom, Gospodarstwo.WEAVING_LOOM.get().defaultBlockState());
        if (level.getBlockEntity(wheel) instanceof pl.kubalte.gospodarstwo.block.StationBlockEntity be) be.insert(p, new ItemStack(Items.RED_WOOL, 64));
        if (level.getBlockEntity(vat) instanceof pl.kubalte.gospodarstwo.block.DyeVatBlockEntity be) {
            be.use(p, net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            be.use(p, net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Gospodarstwo.PLUM.get()));
        }
        ItemStack purple = new ItemStack(Gospodarstwo.YARN_MERINO.get(), 60);
        pl.kubalte.gospodarstwo.craft.Dyes.setColor(purple, 0x6A2C70);
        if (level.getBlockEntity(loom) instanceof pl.kubalte.gospodarstwo.block.StationBlockEntity be) be.insert(p, purple);

        // item wall: planks with frames in front, facing north
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (var h : Gospodarstwo.ITEMS.getEntries()) items.add(new ItemStack(h.get()));
        int[] colours = {0xB02E26, 0x3C44AA, 0xF9D849, 0x5E7C16, 0x6A2C70};
        for (int c : colours) {
            ItemStack y = new ItemStack(Gospodarstwo.YARN_WOOL.get());
            pl.kubalte.gospodarstwo.craft.Dyes.setColor(y, c);
            items.add(y);
            ItemStack cl = new ItemStack(Gospodarstwo.WOOLEN_CLOTH.get());
            pl.kubalte.gospodarstwo.craft.Dyes.setColor(cl, c);
            items.add(cl);
        }
        int perRow = 9;
        for (int i = 0; i < items.size(); i++) {
            int col = i % (perRow * 2), row = i / (perRow * 2);
            BlockPos at = B.offset(123 + col, 4 - row, 1);
            level.setBlockAndUpdate(at.south(), Blocks.SPRUCE_PLANKS.defaultBlockState());
            net.minecraft.world.entity.decoration.ItemFrame f = new net.minecraft.world.entity.decoration.ItemFrame(level, at, net.minecraft.core.Direction.NORTH);
            f.setItem(items.get(i), false);
            level.addFreshEntity(f);
        }
    }

    static void scene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (int x = -6; x <= 110; x++) for (int z = -10; z <= 8; z++) {
            for (int y = 0; y < 12; y++) level.setBlockAndUpdate(B.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        // row 1: the new animals
        put(level, Gospodarstwo.DUCK.get(), B.offset(0, 0, 0), null, false);
        put(level, Gospodarstwo.DUCK.get(), B.offset(1, 0, -1), null, true);
        put(level, Gospodarstwo.GOOSE.get(), B.offset(3, 0, 0), null, false);
        put(level, Gospodarstwo.TURKEY.get(), B.offset(6, 0, 0), null, false);
        put(level, Gospodarstwo.SNAIL.get(), B.offset(9, 0, 0), null, false);
        // row 2: breeds next to the ordinary animal
        put(level, EntityTypes.COW, B.offset(17, 0, 0), null, false);
        put(level, EntityTypes.COW, B.offset(19, 0, 0), Breed.HIGHLAND, false);
        put(level, EntityTypes.SHEEP, B.offset(21, 0, 0), Breed.MERINO, false);
        put(level, EntityTypes.CHICKEN, B.offset(23, 0, 0), Breed.SILKIE, false);
        put(level, EntityTypes.CHICKEN, B.offset(24, 0, 0), Breed.GREENLEG, false);
        put(level, EntityTypes.PIG, B.offset(26, 0, 0), Breed.MANGALICA, false);
        put(level, EntityTypes.RABBIT, B.offset(28, 0, 0), Breed.ANGORA, false);
        // row 3: crops in stages, gooseberries
        CropBlock[] crops = {Gospodarstwo.FLAX.get(), Gospodarstwo.CABBAGES.get(), Gospodarstwo.ONIONS.get(), Gospodarstwo.STRAWBERRIES.get()};
        for (int c = 0; c < crops.length; c++) {
            for (int a = 0; a < 4; a++) {
                BlockPos p = B.offset(42 + a, 0, c - 1);
                level.setBlockAndUpdate(p.below(), Blocks.FARMLAND.defaultBlockState());
                level.setBlockAndUpdate(p, crops[c].getStateForAge(new int[] {1, 3, 5, 7}[a]));
            }
        }
        for (int a = 0; a < 4; a++) level.setBlockAndUpdate(B.offset(47 + a, 0, 0),
                Gospodarstwo.GOOSEBERRY_BUSH.get().defaultBlockState().setValue(SweetBerryBushBlock.AGE, a));
        // fruit trees with ripe fruit
        BlockPos apple = B.offset(62, 0, 3), plum = B.offset(68, 0, 3);
        level.setBlockAndUpdate(apple, Gospodarstwo.APPLE_SAPLING.get().defaultBlockState());
        Gospodarstwo.APPLE_GROWER.growTree(level, level.getChunkSource().getGenerator(), apple, level.getBlockState(apple), level.getRandom());
        level.setBlockAndUpdate(plum, Gospodarstwo.PLUM_SAPLING.get().defaultBlockState());
        Gospodarstwo.PLUM_GROWER.growTree(level, level.getChunkSource().getGenerator(), plum, level.getBlockState(plum), level.getRandom());
        int f = 0;
        for (BlockPos p : BlockPos.betweenClosed(B.offset(58, 0, -1), B.offset(72, 9, 7))) {
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof FruitLeavesBlock) level.setBlock(p, st.setValue(FruitLeavesBlock.FRUIT, (f++ % 3 == 0) ? 1 : 3)
                    .setValue(LeavesBlock.PERSISTENT, true), 2);
        }
        // bonsai pots: sapling, young, untrimmed bush, finished bonsai of a few trees
        String[] trees = {"minecraft:oak_sapling", "minecraft:birch_sapling", "minecraft:oak_sapling", "minecraft:spruce_sapling",
                "minecraft:cherry_sapling", "minecraft:acacia_sapling", "gospodarstwo:apple_sapling"};
        int[] growth = {60, 300, 750, 1000, 1000, 1000, 1000};
        int[] trims = {0, 0, 0, 2, 3, 2, 2};
        for (int i = 0; i < trees.length; i++) {
            BlockPos p = B.offset(82 + (i % 4), 0, i / 4);
            level.setBlockAndUpdate(p, Gospodarstwo.BONSAI_POT.get().defaultBlockState());
            if (level.getBlockEntity(p) instanceof BonsaiPotBlockEntity be) be.setData(new BonsaiData(trees[i], growth[i], trims[i], 1234L + i * 77, 20000, 0));
        }
        // looms: one weaving string, one with merino wool and finished cloth
        BlockPos l1 = B.offset(100, 0, 0), l2 = B.offset(102, 0, 0);
        level.setBlockAndUpdate(l1, Gospodarstwo.WEAVING_LOOM.get().defaultBlockState().setValue(WeavingLoomBlock.FACING, net.minecraft.core.Direction.NORTH));
        level.setBlockAndUpdate(l2, Gospodarstwo.WEAVING_LOOM.get().defaultBlockState().setValue(WeavingLoomBlock.FACING, net.minecraft.core.Direction.NORTH));
        var p = server.getPlayerList().getPlayers().get(0);
        if (level.getBlockEntity(l1) instanceof LoomBlockEntity be) be.insert(p, new ItemStack(Items.STRING, 64));
        if (level.getBlockEntity(l2) instanceof LoomBlockEntity be) be.insert(p, new ItemStack(Gospodarstwo.MERINO_WOOL.get(), 30));
        level.setBlockAndUpdate(B.offset(97, 0, 0), Blocks.BARREL.defaultBlockState());
    }
}
