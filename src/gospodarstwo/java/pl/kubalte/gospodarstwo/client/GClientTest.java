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
            case 1140 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %.1f %.1f 0 0", B.getX() + 127.5, B.getY() + 1.2,
                    B.getZ() - 3.6)));
            case 1220 -> shot(m + "items1");
            case 1380 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %.1f %.1f 0 0", B.getX() + 136.5, B.getY() + 1.2,
                    B.getZ() - 3.6)));
            case 1460 -> shot(m + "items2");
            case 1620 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %.1f %.1f 0 0", B.getX() + 147.5, B.getY() + 1.2,
                    B.getZ() - 3.6)));
            case 1700 -> shot(m + "items3");
            case 1860 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %.1f %.1f 0 0", B.getX() + 156.5, B.getY() + 1.2,
                    B.getZ() - 3.6)));
            case 1940 -> shot(m + "items4");
            case 2100 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 32", B.getX() + 111.5, B.getY() + 1,
                    B.getZ() - 3.0)));
            case 2180 -> shot(m + "workshop");
            case 2340 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 12", B.getX() + 193.5, B.getY() + 1,
                    B.getZ() - 3.4)));
            case 2420 -> shot(m + "clothes");
            case 2580 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 18", B.getX() + 179.5, B.getY() + 1,
                    B.getZ() - 3.5)));
            case 2660 -> shot(m + "home");
            case 2820 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 24", B.getX() + 256.5, B.getY() + 1,
                    B.getZ() - 3.6)));
            case 2900 -> shot(m + "kitchen");
            case 3060 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 26", B.getX() + 276.5, B.getY() + 1,
                    B.getZ() - 3.8)));
            case 3140 -> shot(m + "tools");
            case 3300 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 15", B.getX() + 299.5, B.getY() + 1,
                    B.getZ() - 3.2)));
            case 3380 -> shot(m + "goblins");
            case 3420 -> server.execute(() -> {
                var sp = server.getPlayerList().getPlayers().get(0);
                for (var gob : server.overworld().getEntitiesOfClass(pl.kubalte.gospodarstwo.goblin.Goblin.class,
                        new net.minecraft.world.phys.AABB(B.offset(296, 0, -2)).inflate(1)))
                    pl.kubalte.gospodarstwo.goblin.GoblinMenu.open(sp, gob);
            });
            case 3500 -> shot(m + "goblinmenu");
            case 3540 -> {
                if (mc.gui.screen() instanceof pl.kubalte.gospodarstwo.client.goblin.GoblinScreen gs)
                    mc.gui.setScreen(new pl.kubalte.gospodarstwo.client.goblin.GoblinInfoScreen(gs, gs.getMenu()));
            }
            case 3620 -> shot(m + "goblininfo");
            case 3660 -> server.execute(() -> server.getPlayerList().getPlayers().get(0).closeContainer());
            case 3700 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 18", B.getX() + 319.5, B.getY() + 1,
                    B.getZ() - 3.0)));
            case 3780 -> shot(m + "goblinraces");
            case 3820 -> server.execute(() -> cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 20", B.getX() + 331.0, B.getY() + 1,
                    B.getZ() - 4.0)));
            case 3900 -> shot(m + "goblinhut");
            case 3940 -> server.execute(() -> {
                cmd(server, "time set 14000");
                cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 24", B.getX() + 340.5, B.getY() + 1, B.getZ() - 4.0));
            });
            case 4040 -> shot(m + "goblindance");
            case 4050 -> {
                server.execute(() -> {
                    for (var g : server.overworld().getEntitiesOfClass(pl.kubalte.gospodarstwo.goblin.Goblin.class,
                            new net.minecraft.world.phys.AABB(B).inflate(400)))
                        Gospodarstwo.LOGGER.info("GOBLIN SERVER id={} action={} seq={} hiding={} race={} noai={} class={}", g.getId(), g.action(),
                                g.actionSeq(), g.hiding(), g.race(), g.isNoAi(), g.getClass().getSimpleName());
                });
                if (mc.level != null) for (var en : mc.level.entitiesForRendering())
                    if (en instanceof pl.kubalte.gospodarstwo.goblin.Goblin g)
                        Gospodarstwo.LOGGER.info("GOBLIN CLIENT id={} action={} seq={} hiding={} race={}", g.getId(), g.action(), g.actionSeq(),
                                g.hiding(), g.race());
            }
            case 4080 -> server.execute(() -> {
                cmd(server, "time set 6000");
                cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 16", B.getX() + 349.5, B.getY() + 1, B.getZ() - 3.5));
            });
            case 4160 -> shot(m + "goblinfight");
            case 4300 -> {
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
        for (int x = 108; x <= 310; x++) for (int z = -8; z <= 6; z++) for (int y = 0; y < 9; y++)
            level.setBlockAndUpdate(B.offset(x, y, z), Blocks.AIR.defaultBlockState());
        BlockPos wheel = B.offset(114, 0, 0), vat = B.offset(116, 0, 0), loom = B.offset(118, 0, 0);
        level.setBlockAndUpdate(wheel, Gospodarstwo.SPINNING_WHEEL.get().defaultBlockState().setValue(pl.kubalte.gospodarstwo.block.StationBlock.FACING,
                net.minecraft.core.Direction.WEST));
        level.setBlockAndUpdate(vat, Gospodarstwo.DYE_VAT.get().defaultBlockState());
        level.setBlockAndUpdate(loom, Gospodarstwo.WEAVING_LOOM.get().defaultBlockState());
        if (level.getBlockEntity(wheel) instanceof pl.kubalte.gospodarstwo.block.StationBlockEntity be) be.insert(p, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_wool")), 64));
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
        // stage 2 workshop: a hide half tanned on the rack, a kettle bubbling on a campfire
        BlockPos rackPos = B.offset(110, 0, 0), kettlePos = B.offset(112, 1, 0);
        level.setBlockAndUpdate(rackPos, Gospodarstwo.TANNING_RACK.get().defaultBlockState());
        if (level.getBlockEntity(rackPos) instanceof pl.kubalte.gospodarstwo.block.StationBlockEntity be) {
            be.insert(p, new ItemStack(Gospodarstwo.RAW_HIDE.get(), 4));
            be.debugProgress(pl.kubalte.gospodarstwo.GConfig.TAN_TICKS.get() / 2);
        }
        level.setBlockAndUpdate(kettlePos.below(), Blocks.CAMPFIRE.defaultBlockState());
        level.setBlockAndUpdate(kettlePos, Gospodarstwo.KETTLE.get().defaultBlockState());
        if (level.getBlockEntity(kettlePos) instanceof pl.kubalte.gospodarstwo.block.StationBlockEntity be) be.insert(p, new ItemStack(Items.HONEYCOMB, 64));
        BlockPos cold = B.offset(113, 0, 1);
        level.setBlockAndUpdate(cold, Gospodarstwo.KETTLE.get().defaultBlockState());
        if (level.getBlockEntity(cold) instanceof pl.kubalte.gospodarstwo.block.StationBlockEntity be) be.insert(p, new ItemStack(Items.HONEYCOMB, 16));
        cmd(server, "kill @e[type=minecraft:slime]");
        cmd(server, "gamerule spawn_mobs false");
        cmd(server, "gamerule doMobSpawning false");

        home(level, p);
        kitchen(level, p);
        tools(level, p);
        goblins(level, p);
        goblins2(level, p);

        int perRow = 18;
        for (int i = 0; i < items.size(); i++) {
            int wall = i / (perRow * 5), k = i % (perRow * 5);
            int col = k % perRow + wall * 20, row = k / perRow;
            BlockPos at = B.offset(123 + col, 4 - row, 1);
            level.setBlockAndUpdate(at.south(), Blocks.SPRUCE_PLANKS.defaultBlockState());
            net.minecraft.world.entity.decoration.ItemFrame f = new net.minecraft.world.entity.decoration.ItemFrame(level, at, net.minecraft.core.Direction.NORTH);
            f.setItem(items.get(i), false);
            level.addFreshEntity(f);
        }
    }

    static ItemStack dyed(net.minecraft.world.level.ItemLike item, int rgb) {
        ItemStack s = new ItemStack(item);
        if (rgb >= 0) pl.kubalte.gospodarstwo.craft.Dyes.setColor(s, rgb);
        return s;
    }

    static void dress(ServerLevel level, BlockPos pos, ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
        Mob m = put(level, EntityTypes.HUSK, pos, null, false);
        if (m == null) return;
        m.setPersistenceRequired();
        m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, head);
        m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
        m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, legs);
        m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, feet);
    }

    /** Stage 3: people in the new clothes, and a room with curtains, rugs and horn lanterns. */
    static void home(ServerLevel level, net.minecraft.server.level.ServerPlayer p) {
        dress(level, B.offset(190, 0, 0), dyed(Gospodarstwo.WOOL_CAP.get(), 0xB02E26), dyed(Gospodarstwo.WOOL_SWEATER.get(), 0x3C44AA),
                dyed(Gospodarstwo.WOOL_TROUSERS.get(), -1), new ItemStack(Gospodarstwo.FELT_BOOTS.get()));
        dress(level, B.offset(192, 0, 0), new ItemStack(Gospodarstwo.FUR_HAT.get()), new ItemStack(Gospodarstwo.SHEEPSKIN_COAT.get()),
                dyed(Gospodarstwo.WOOL_TROUSERS.get(), 0x5E7C16), new ItemStack(Gospodarstwo.FELT_BOOTS.get()));
        dress(level, B.offset(194, 0, 0), ItemStack.EMPTY, dyed(Gospodarstwo.LINEN_SHIRT.get(), -1), dyed(Gospodarstwo.LINEN_TROUSERS.get(), 0x3C44AA),
                ItemStack.EMPTY);
        dress(level, B.offset(196, 0, 0), dyed(Gospodarstwo.WOOL_CAP.get(), 0xF9D849), dyed(Gospodarstwo.LINEN_SHIRT.get(), 0xD8506A),
                dyed(Gospodarstwo.WOOL_TROUSERS.get(), 0x6A2C70), new ItemStack(Gospodarstwo.FELT_BOOTS.get()));
        // back wall with two windows
        for (int x = 172; x <= 187; x++) for (int y = 0; y <= 3; y++) {
            boolean window = y >= 1 && y <= 2 && (x >= 174 && x <= 176 || x >= 182 && x <= 184);
            level.setBlockAndUpdate(B.offset(x, y, 5), window ? Blocks.GLASS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
        }
        BlockState cur = Gospodarstwo.CURTAIN.get().defaultBlockState();
        for (int x = 174; x <= 176; x++) for (int y = 1; y <= 2; y++) {
            level.setBlockAndUpdate(B.offset(x, y, 4), cur);
            if (level.getBlockEntity(B.offset(x, y, 4)) instanceof pl.kubalte.gospodarstwo.block.ColoredBlockEntity be) be.setColor(0xB02E26);
        }
        for (int x = 182; x <= 184; x++) for (int y = 1; y <= 2; y++) {
            level.setBlockAndUpdate(B.offset(x, y, 4), cur.setValue(pl.kubalte.gospodarstwo.block.CurtainBlock.OPEN, true));
            if (level.getBlockEntity(B.offset(x, y, 4)) instanceof pl.kubalte.gospodarstwo.block.ColoredBlockEntity be) be.setColor(0x3C6E9A);
        }
        // rugs: a purple 3x2 rug, a natural one, a rag rug
        for (int x = 177; x <= 179; x++) for (int z = 1; z <= 2; z++) {
            level.setBlockAndUpdate(B.offset(x, 0, z), Gospodarstwo.WOOL_RUG.get().defaultBlockState());
            if (level.getBlockEntity(B.offset(x, 0, z)) instanceof pl.kubalte.gospodarstwo.block.ColoredBlockEntity be) be.setColor(0x6A2C70);
        }
        level.setBlockAndUpdate(B.offset(181, 0, 2), Gospodarstwo.WOOL_RUG.get().defaultBlockState());
        level.setBlockAndUpdate(B.offset(175, 0, 2), Gospodarstwo.RAG_RUG.get().defaultBlockState());
        level.setBlockAndUpdate(B.offset(174, 0, 2), Gospodarstwo.RAG_RUG.get().defaultBlockState());
        // horn lanterns: one on a fence post, one hanging from a beam
        level.setBlockAndUpdate(B.offset(180, 0, 4), Blocks.SPRUCE_FENCE.defaultBlockState());
        level.setBlockAndUpdate(B.offset(180, 1, 4), Gospodarstwo.HORN_LANTERN.get().defaultBlockState());
        level.setBlockAndUpdate(B.offset(178, 3, 4), Blocks.SPRUCE_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(B.offset(178, 2, 4), Gospodarstwo.HORN_LANTERN.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
    }

    /** Stage 4: the kitchen stations at work. */
    static void kitchen(ServerLevel level, net.minecraft.server.level.ServerPlayer p) {
        BlockState north = Gospodarstwo.QUERN.get().defaultBlockState();
        BlockPos q = B.offset(251, 0, 0), pr = B.offset(253, 0, 0), cr = B.offset(255, 0, 0), sm = B.offset(258, 1, 0), ch = B.offset(260, 0, 0);
        level.setBlockAndUpdate(q, north);
        if (level.getBlockEntity(q) instanceof pl.kubalte.gospodarstwo.block.StationBlockEntity be) {
            be.insert(p, new ItemStack(Items.WHEAT, 64));
            be.debugProgress(1);
        }
        level.setBlockAndUpdate(pr, Gospodarstwo.FRUIT_PRESS.get().defaultBlockState());
        if (level.getBlockEntity(pr) instanceof pl.kubalte.gospodarstwo.kitchen.PressBlockEntity be) be.insert(p, new ItemStack(Gospodarstwo.PLUM.get(), 64));
        level.setBlockAndUpdate(cr, Gospodarstwo.CROCK.get().defaultBlockState());
        if (level.getBlockEntity(cr) instanceof pl.kubalte.gospodarstwo.block.StationBlockEntity be) be.insert(p, new ItemStack(Gospodarstwo.CABBAGE.get(), 16));
        level.setBlockAndUpdate(sm.below(), Blocks.CAMPFIRE.defaultBlockState());
        level.setBlockAndUpdate(sm, Gospodarstwo.SMOKEHOUSE.get().defaultBlockState());
        if (level.getBlockEntity(sm) instanceof pl.kubalte.gospodarstwo.block.StationBlockEntity be) {
            be.insert(p, new ItemStack(Gospodarstwo.RAW_SAUSAGE.get(), 6));
            be.debugProgress(pl.kubalte.gospodarstwo.GConfig.SMOKE_TICKS.get() * 2 / 3);
        }
        level.setBlockAndUpdate(ch, Gospodarstwo.BUTTER_CHURN.get().defaultBlockState());
        if (level.getBlockEntity(ch) instanceof pl.kubalte.gospodarstwo.kitchen.ChurnBlockEntity be) {
            ItemStack milk = new ItemStack(Items.MILK_BUCKET);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, milk);
            be.use(p, milk);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        // a finished churn of butter and a crock that is done, next to them
        BlockPos ch2 = B.offset(261, 0, 1);
        level.setBlockAndUpdate(ch2, Gospodarstwo.BUTTER_CHURN.get().defaultBlockState());
        if (level.getBlockEntity(ch2) instanceof pl.kubalte.gospodarstwo.kitchen.ChurnBlockEntity be) {
            ItemStack milk = new ItemStack(Items.MILK_BUCKET);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, milk);
            be.use(p, milk);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            for (int i = 0; i < pl.kubalte.gospodarstwo.GConfig.CHURN_STROKES.get(); i++) be.churn();
        }
    }

    /** Stage 5: raised beds with crops, an incubator with eggs, a fishing net in a pond. */
    static void tools(ServerLevel level, net.minecraft.server.level.ServerPlayer p) {
        CropBlock[] crops = {(CropBlock) Blocks.CARROTS, Gospodarstwo.CABBAGES.get(), (CropBlock) Blocks.WHEAT};
        for (int i = 0; i < 3; i++) {
            BlockPos b = B.offset(270 + i, 0, 1);
            level.setBlockAndUpdate(b, Gospodarstwo.RAISED_BED.get().defaultBlockState());
            level.setBlockAndUpdate(b.above(), crops[i].getStateForAge(crops[i].getMaxAge() - i));
            level.setBlockAndUpdate(b.south(), Gospodarstwo.RAISED_BED.get().defaultBlockState());
            level.setBlockAndUpdate(b.south().above(), crops[i].getStateForAge(i + 2));
        }
        BlockPos inc = B.offset(275, 0, 0);
        level.setBlockAndUpdate(inc, Gospodarstwo.INCUBATOR.get().defaultBlockState());
        if (level.getBlockEntity(inc) instanceof pl.kubalte.gospodarstwo.tools.IncubatorBlockEntity be) be.use(p, new ItemStack(Gospodarstwo.DUCK_EGG.get(), 3));
        BlockPos inc2 = B.offset(276, 0, 1);
        level.setBlockAndUpdate(inc2, Gospodarstwo.INCUBATOR.get().defaultBlockState());
        if (level.getBlockEntity(inc2) instanceof pl.kubalte.gospodarstwo.tools.IncubatorBlockEntity be) be.use(p, new ItemStack(Gospodarstwo.GREEN_EGG.get(), 4));
        // a little pond (one block deep) with a net that already caught something
        for (int x = 277; x <= 282; x++) for (int z = -2; z <= 3; z++) level.setBlockAndUpdate(B.offset(x, -1, z), Blocks.WATER.defaultBlockState());
        BlockPos net = B.offset(279, -1, 0);
        level.setBlockAndUpdate(net, Gospodarstwo.FISHING_NET.get().defaultBlockState().setValue(pl.kubalte.gospodarstwo.tools.FishingNetBlock.WATERLOGGED, true));
        if (level.getBlockEntity(net) instanceof pl.kubalte.gospodarstwo.tools.FishingNetBlockEntity be) for (int i = 0; i < 4; i++) be.catchOne();
    }

    /** The goblin helper: a wild one peeking from behind a tree, one thinking over a treat, a tame one in gear, one asleep. */
    static void goblins(ServerLevel level, net.minecraft.server.level.ServerPlayer p) {
        // a small oak with the wild goblin behind it
        BlockPos tree = B.offset(302, 0, 2);
        for (int y = 0; y < 5; y++) level.setBlockAndUpdate(tree.above(y), Blocks.OAK_LOG.defaultBlockState());
        for (BlockPos l : BlockPos.betweenClosed(tree.offset(-2, 3, -2), tree.offset(2, 5, 2)))
            if (level.getBlockState(l).isAir() && l.distManhattan(tree.above(4)) <= 3)
                level.setBlockAndUpdate(l, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        var wild = Gospodarstwo.GOBLIN.get().spawn(level, tree.offset(-1, 0, 1), EntitySpawnReason.COMMAND);
        if (wild != null) {
            wild.setNoAi(true);
            wild.setHiding(true);
            wild.setYRot(180);
            wild.yHeadRot = 180;
            wild.yBodyRot = 180;
        }
        var hungry = Gospodarstwo.GOBLIN.get().spawn(level, B.offset(300, 0, 0), EntitySpawnReason.COMMAND);
        if (hungry != null) {
            hungry.setNoAi(true);
            hungry.debugConsider(new ItemStack(Gospodarstwo.PIEROGI.get()));
            hungry.setYRot(200);
            hungry.yBodyRot = 200;
        }
        var tame = Gospodarstwo.GOBLIN.get().spawn(level, B.offset(296, 0, -2), EntitySpawnReason.COMMAND);
        if (tame != null) {
            tame.tame(p);
            tame.setNoAi(true);
            tame.setPack(true);
            tame.pack.addItem(new ItemStack(Items.COBBLESTONE, 40));
            tame.pack.addItem(new ItemStack(Gospodarstwo.FLOUR.get(), 12));
            tame.pack.addItem(new ItemStack(Items.WHEAT, 20));
            tame.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            tame.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, dyed(Gospodarstwo.WOOL_SWEATER.get(), 0xB02E26));
            tame.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            tame.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Gospodarstwo.SICKLE.get()));
            tame.setYRot(150);
            tame.yHeadRot = 150;
            tame.yBodyRot = 150;
        }
        var tame2 = Gospodarstwo.GOBLIN.get().spawn(level, B.offset(298, 0, -1), EntitySpawnReason.COMMAND);
        if (tame2 != null) {
            tame2.tame(p);
            tame2.setNoAi(true);
            tame2.setPack(true);
            tame2.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Gospodarstwo.WATERING_CAN.get()));
            tame2.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Gospodarstwo.FUR_HAT.get()));
            tame2.setYRot(210);
            tame2.yHeadRot = 210;
            tame2.yBodyRot = 210;
        }
        // one asleep in a bed
        BlockPos foot = B.offset(294, 0, 1);
        BlockState bed = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_bed")).defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.SOUTH);
        level.setBlock(foot.south(), bed.setValue(net.minecraft.world.level.block.BedBlock.PART,
                net.minecraft.world.level.block.state.properties.BedPart.HEAD), 18);
        level.setBlock(foot, bed.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT), 18);
        var sleepy = Gospodarstwo.GOBLIN.get().spawn(level, foot.south(), EntitySpawnReason.COMMAND);
        if (sleepy != null) {
            sleepy.tame(p);
            sleepy.setNoAi(true);
            sleepy.debugRest(100000);
            sleepy.startSleeping(foot.south());
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

    static pl.kubalte.gospodarstwo.goblin.Goblin gob(ServerLevel level, net.minecraft.server.level.ServerPlayer p, BlockPos at, float yaw,
            pl.kubalte.gospodarstwo.goblin.Race race) {
        var g = Gospodarstwo.GOBLIN.get().spawn(level, at, EntitySpawnReason.COMMAND);
        if (g == null) return null;
        g.tame(p);
        g.setNoAi(true);
        g.setRace(race);
        g.setYRot(yaw);
        g.yHeadRot = yaw;
        g.yBodyRot = yaw;
        return g;
    }

    /** The overhaul: the four races, a hut, dancing round a campfire at night, a fight and a shield. */
    static void goblins2(ServerLevel level, net.minecraft.server.level.ServerPlayer p) {
        for (int x = 311; x <= 360; x++) for (int z = -8; z <= 8; z++) for (int y = 0; y < 9; y++)
            level.setBlockAndUpdate(B.offset(x, y, z), Blocks.AIR.defaultBlockState());
        var R = pl.kubalte.gospodarstwo.goblin.Race.values();
        for (int i = 0; i < 4; i++) gob(level, p, B.offset(316 + i * 2, 0, 1), 180, R[i]);
        // the hut, finished, with its owner in front
        pl.kubalte.gospodarstwo.goblin.GoblinHutBlock.place(level, B.offset(330, 0, 2), net.minecraft.core.Direction.NORTH, 3);
        var builder = gob(level, p, B.offset(332, 0, 0), 160, R[0]);
        if (builder != null) {
            builder.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
            builder.act(pl.kubalte.gospodarstwo.goblin.Goblin.A_CHEER, 0);
        }
        // a campfire and three dancers
        BlockPos fire = B.offset(340, 0, 2);
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
        int[] dances = {pl.kubalte.gospodarstwo.goblin.Goblin.A_DANCE1, pl.kubalte.gospodarstwo.goblin.Goblin.A_DANCE2,
                pl.kubalte.gospodarstwo.goblin.Goblin.A_DANCE3};
        for (int i = 0; i < 3; i++) {
            double a = Math.PI * (0.15 + i * 0.35) + Math.PI;
            BlockPos at = BlockPos.containing(fire.getX() + 0.5 + Math.cos(a) * 2.2, fire.getY(), fire.getZ() + 0.5 + Math.sin(a) * 2.2);
            float yaw = (float) Math.toDegrees(Math.atan2(fire.getZ() - at.getZ(), fire.getX() - at.getX())) - 90F;
            var d = gob(level, p, at, yaw, R[i]);
            if (d != null) d.act(dances[i], 0);
        }
        // a fight: one leaping at a zombie, one blocking with a shield
        var zombie = EntityTypes.ZOMBIE.spawn(level, B.offset(350, 0, 3), EntitySpawnReason.COMMAND);
        if (zombie != null) {
            zombie.setNoAi(true);
            zombie.setYRot(0);
            zombie.yBodyRot = 0;
        }
        var fighter = gob(level, p, B.offset(350, 0, 1), 0, R[1]);
        if (fighter != null) {
            fighter.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            fighter.act(pl.kubalte.gospodarstwo.goblin.Goblin.A_WINDUP, 0);
        }
        var guard = gob(level, p, B.offset(347, 0, 1), 200, R[3]);
        if (guard != null) {
            guard.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            guard.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            guard.act(pl.kubalte.gospodarstwo.goblin.Goblin.A_BLOCK, 0);
        }
    }
}
