package pl.kubalte.golebie.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import pl.kubalte.golebie.Golebie;
import pl.kubalte.golebie.bird.Bird;

/** CI visual test (-Dgolebie.clienttest=true): logs "PCLIENT SHOT name" when a screenshot should be taken. */
public final class PClientTest {
    private PClientTest() {}

    private static int ticks = -1;
    private static final BlockPos B = new BlockPos(2000, -60, 2000);
    private static Bird pigeon, fantail;
    private static final List<Bird> lineup = new ArrayList<>();

    static void register(IEventBus modBus) {
        if (Boolean.getBoolean("golebie.clienttest")) NeoForge.EVENT_BUS.addListener(PClientTest::onTick);
    }

    private static void shot(String name) {
        Golebie.LOGGER.info("PCLIENT SHOT {}", name);
    }

    static void cmd(MinecraftServer server, String c) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
    }

    static ServerPlayer sp(MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return;
        ticks++;
        // a real nod of the head, done on the client like a player moving the mouse
        if (ticks >= 470 && ticks <= 482) {
            float[] path = {10, 16, 22, 28, 34, 38, 38, 32, 24, 16, 10, 10, 10};
            mc.player.setXRot(path[ticks - 470]);
            mc.player.xRotO = mc.player.getXRot();
        }
        switch (ticks) {
            case 40 -> server.execute(() -> {
                cmd(server, "gamemode creative @a");
                cmd(server, "time set 6000");
                cmd(server, "gamerule doDaylightCycle false");
                cmd(server, "weather clear");
                cmd(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %d %.1f 0 10", B.getX() + 0.5, B.getY(), B.getZ() + 0.5));
            });
            case 100 -> server.execute(() -> scene(server));
            case 180 -> shot("birds");
            case 220 -> server.execute(() -> {
                fantail.pet(sp(server));
                pigeon.act(Bird.A_BEG, 0);
            });
            case 255 -> shot("petted");
            case 300 -> server.execute(() -> {
                pigeon.act(Bird.A_RUNUP, 0);
                fantail.act(Bird.A_RUNUP, 0);
            });
            case 340 -> shot("runup");
            case 380 -> server.execute(() -> {
                pigeon.act(Bird.A_CIRCLE, 0);
                fantail.act(Bird.A_CIRCLE_CCW, 0);
            });
            case 425 -> shot("circle");
            case 460 -> server.execute(() -> {
                pigeon.act(Bird.A_NONE, 0);
                fantail.act(Bird.A_NONE, 0);
            });
            case 488 -> server.execute(() -> {
                boolean ok = (pigeon.pendingBow() > 0 || pigeon.action() == Bird.A_BOW) && (fantail.pendingBow() > 0 || fantail.action() == Bird.A_BOW);
                Golebie.LOGGER.info("PCLIENT NOD {}", ok ? "OK" : "FAIL");
            });
            case 492 -> shot("bow");
            case 540 -> {
                server.execute(() -> {
                    ServerPlayer p = sp(server);
                    pigeon.perchNow(p, Bird.PERCH_LEFT);
                    fantail.perchNow(p, Bird.PERCH_HEAD);
                    pigeon.holdPose();
                    fantail.holdPose();
                    pigeon.act(Bird.A_NONE, 0);
                    fantail.act(Bird.A_NONE, 0);
                });
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.player.setXRot(5);
            }
            case 600 -> shot("perchfront");
            case 620 -> server.execute(() -> pigeon.act(Golebie.NIBBLE_LEFT_SHOULDER, 0));
            case 668 -> shot("nibblefront");
            case 700 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
            case 740 -> shot("perchfp");
            case 760 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 800 -> shot("perchback");
            case 820 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(() -> {
                    ServerPlayer p = sp(server);
                    pigeon.hopOff(p);
                    fantail.hopOff(p);
                });
            }
            case 828 -> shot("hopoff");
            case 900 -> Golebie.LOGGER.info("PCLIENT DONE");
            default -> {}
        }
    }

    static Bird bird(ServerLevel level, EntityType<? extends Bird> type, BlockPos at, int diaper, ServerPlayer owner) {
        Bird b = type.spawn(level, at, EntitySpawnReason.COMMAND);
        if (b == null) return null;
        b.tame(owner);
        b.setNoAi(true);
        b.setDiaper(diaper);
        b.setYRot(180);
        b.yBodyRot = 180;
        b.yHeadRot = 180;
        b.holdPose();
        return b;
    }

    static void scene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (int x = -8; x <= 8; x++) for (int z = -2; z <= 12; z++) {
            level.setBlockAndUpdate(B.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
            for (int y = 0; y < 5; y++) level.setBlockAndUpdate(B.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        ServerPlayer p = sp(server);
        pigeon = bird(level, Golebie.PIGEON.get(), B.offset(1, 0, 2), 2, p);
        fantail = bird(level, Golebie.FANTAIL.get(), B.offset(-1, 0, 2), 3, p);
        for (int i = 0; i < 6; i++) {
            lineup.add(bird(level, Golebie.PIGEON.get(), B.offset(-5 + i * 2, 0, 6), i + 1, p));
            lineup.add(bird(level, Golebie.FANTAIL.get(), B.offset(-5 + i * 2, 0, 8), i + 1, p));
        }
        Golebie.LOGGER.info("PCLIENT scene ready");
    }
}
