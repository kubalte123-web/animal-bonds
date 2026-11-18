package pl.kubalte.elementals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import pl.kubalte.elementals.Element;
import pl.kubalte.elementals.ElementEvents;
import pl.kubalte.elementals.Elementals;

/** CI-only visual test (-Delementals.clienttest=true): a line-up of creatures and elements, logs "ELCLIENT SHOT name". */
public final class ElementalsClientTest {
    private ElementalsClientTest() {}

    private static int ticks = -1;
    private static final BlockPos BASE = new BlockPos(1000, -60, 1000);

    public static void register(IEventBus modBus) {
        if (Boolean.getBoolean("elementals.clienttest")) NeoForge.EVENT_BUS.addListener(ElementalsClientTest::onTick);
    }

    private static void shot(String name) {
        Elementals.LOGGER.info("ELCLIENT SHOT {}", name);
    }

    static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return;
        ticks++;
        String mode = ElementalsClient.useGecko() ? "gecko" : "plain";
        switch (ticks) {
            case 40 -> server.execute(() -> {
                cmd(server, "gamemode creative @a");
                cmd(server, "time set 6000");
                cmd(server, "weather clear");
                cmd(server, String.format("tp @p %d %d %d", BASE.getX(), BASE.getY(), BASE.getZ() - 6));
            });
            case 120 -> server.execute(() -> lineup(server));
            case 220 -> shot(mode + "creatures");
            case 240 -> server.execute(() -> cmd(server, String.format("tp @p %d %d %d 0 20", BASE.getX() + 4, BASE.getY() + 1, BASE.getZ() + 1)));
            case 320 -> shot(mode + "elements");
            case 340 -> server.execute(() -> cmd(server, String.format("tp @p %d %d %d 0 10", BASE.getX() + 4, BASE.getY(), BASE.getZ() + 8)));
            case 400 -> shot(mode + "variants");
            case 420 -> server.execute(() -> cmd(server, "time set 18000"));
            case 480 -> shot(mode + "night");
            case 500 -> {
                if (ModList.get().isLoaded("animalbonds")) server.execute(() -> pen(server));
            }
            case 620 -> {
                if (ModList.get().isLoaded("animalbonds")) shot(mode + "book");
            }
            case 680 -> {
                mc.gui.setScreen(null);
                Elementals.LOGGER.info("ELCLIENT DONE");
                mc.stop();
            }
            default -> {}
        }
    }

    private static void cmd(MinecraftServer server, String c) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    private static Mob put(ServerLevel level, EntityType<? extends Mob> type, BlockPos pos, Element el, boolean baby) {
        Mob m = type.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (m == null) return null;
        m.setNoAi(true);
        m.setYRot(180);
        m.yBodyRot = 180;
        m.yHeadRot = 180;
        if (baby && m instanceof net.minecraft.world.entity.AgeableMob a) a.setBaby(true);
        if (m instanceof net.minecraft.world.entity.monster.Monster) m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        ElementEvents.apply(m, el);
        return m;
    }

    /** Row 1: the four creatures (+ a baby). Row 2: vanilla mobs with each element. Row 3: Mchowiec in all six elements. */
    private static void lineup(MinecraftServer server) {
        ServerLevel level = (ServerLevel) player(server).level();
        for (int x = -4; x <= 14; x++) for (int z = -8; z <= 12; z++) for (int y = 0; y < 6; y++)
            level.setBlockAndUpdate(BASE.offset(x, y, z), Blocks.AIR.defaultBlockState());
        put(level, Elementals.MOSSLING.get(), BASE, Element.NONE, false);
        put(level, Elementals.PUFFOWL.get(), BASE.offset(2, 0, 0), Element.NONE, false);
        put(level, Elementals.EMBERLIZARD.get(), BASE.offset(4, 0, 0), Element.FIRE, false);
        put(level, Elementals.FROSTLING.get(), BASE.offset(6, 0, 0), Element.ICE, false);
        put(level, Elementals.MOSSLING.get(), BASE.offset(8, 0, 0), Element.NONE, true);
        put(level, Elementals.PUFFOWL.get(), BASE.offset(9, 0, 0), Element.ELECTRIC, false);

        put(level, EntityTypes.COW, BASE.offset(0, 0, 4), Element.FIRE, false);
        put(level, EntityTypes.SHEEP, BASE.offset(2, 0, 4), Element.ICE, false);
        put(level, EntityTypes.ZOMBIE, BASE.offset(4, 0, 4), Element.DARK, false);
        put(level, EntityTypes.CREEPER, BASE.offset(6, 0, 4), Element.ELECTRIC, false);
        put(level, EntityTypes.PIG, BASE.offset(8, 0, 4), Element.DRAGON, false);

        Element[] all = Element.values();
        for (int i = 0; i < all.length; i++) {
            put(level, i % 2 == 0 ? Elementals.MOSSLING.get() : Elementals.EMBERLIZARD.get(), BASE.offset(i * 2 - 1, 0, 11), all[i], false);
        }
    }

    /** A small pen (Animal Bonds) with an elemental creature, and the Chronicle opened on it. */
    private static void pen(MinecraftServer server) {
        ServerPlayer sp = player(server);
        ServerLevel level = (ServerLevel) sp.level();
        BlockPos o = BASE.offset(30, 0, 0);
        BlockPos ped = pl.kubalte.animalbonds.SelfTest.buildPen(level, o);
        Mob moss = put(level, Elementals.MOSSLING.get(), o.offset(3, 0, 3), Element.DRAGON, false);
        if (moss != null) {
            moss.setNoAi(false);
            pl.kubalte.animalbonds.bond.BondLogic.bond(moss);
        }
        if (level.getBlockEntity(ped) instanceof pl.kubalte.animalbonds.block.PedestalBlockEntity be) be.openBook(sp);
    }
}
