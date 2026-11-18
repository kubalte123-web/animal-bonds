package pl.kubalte.elementals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** CI test of the elements and creatures (-Delementals.selftest=true). Logs ELTEST lines; does not stop the server. */
public final class ElementalsSelfTest {
    private ElementalsSelfTest() {}

    public static void register(IEventBus bus) {
        if (Boolean.getBoolean("elementals.selftest")) bus.addListener(ElementalsSelfTest::onStarted);
    }

    private static final List<String> FAILS = new ArrayList<>();
    private static int checks;

    public static void check(boolean ok, String what) {
        checks++;
        if (ok) Elementals.LOGGER.info("ELTEST ok   {}", what);
        else {
            FAILS.add(what);
            Elementals.LOGGER.error("ELTEST FAIL {}", what);
        }
    }

    static void onStarted(ServerStartedEvent e) {
        try {
            run(e.getServer().overworld());
        } catch (Throwable t) {
            Elementals.LOGGER.error("ELTEST FAIL exception", t);
            FAILS.add("exception " + t);
        }
        if (FAILS.isEmpty()) Elementals.LOGGER.info("ELTEST RESULT: PASS ({} checks)", checks);
        else Elementals.LOGGER.error("ELTEST RESULT: FAIL ({} of {}): {}", FAILS.size(), checks, FAILS);
    }

    private static Mob spawn(ServerLevel level, net.minecraft.world.entity.EntityType<? extends Mob> type, BlockPos pos) {
        return type.spawn(level, pos, EntitySpawnReason.COMMAND);
    }

    static void run(ServerLevel level) {
        BlockPos o = new BlockPos(-40, 250, 8);
        for (int cx = -4; cx <= 0; cx++) for (int cz = 0; cz <= 1; cz++) level.setChunkForced(cx, cz, true);
        for (int x = -4; x <= 26; x++) for (int z = -4; z <= 12; z++) level.setBlockAndUpdate(o.offset(x, -1, z), Blocks.STONE.defaultBlockState());

        // ---- new creatures exist and get their fixed elements ----
        Mob lizard = spawn(level, Elementals.EMBERLIZARD.get(), o);
        Mob frost = spawn(level, Elementals.FROSTLING.get(), o.offset(2, 1, 0));
        Mob moss = spawn(level, Elementals.MOSSLING.get(), o.offset(4, 0, 0));
        Mob owl = spawn(level, Elementals.PUFFOWL.get(), o.offset(6, 0, 0));
        check(lizard != null && frost != null && moss != null && owl != null, "all four new creatures spawn");
        check(Elementals.element(lizard) == Element.FIRE, "Żarołaz is always fire");
        check(Elementals.element(frost) == Element.ICE, "Szroniec is always ice");
        check(Elementals.hasRolledElement(moss) && Elementals.hasRolledElement(owl), "other creatures get an element roll");
        check(moss.isFood(net.minecraft.world.item.Items.SWEET_BERRIES.getDefaultInstance()), "Mchowiec eats berries (can be bred)");
        check(frost.isNoGravity(), "Szroniec hovers");

        // ---- toughness bonus ----
        Mob cow = spawn(level, EntityTypes.COW, o.offset(0, 0, 4));
        double before = cow.getMaxHealth();
        ElementEvents.apply(cow, Element.FIRE);
        check(Math.abs(cow.getMaxHealth() - before * 1.2) < 0.01 || cow.getMaxHealth() > before, "fire cow is tougher (" + before + " -> " + cow.getMaxHealth() + ")");

        // ---- immunities ----
        float hp = cow.getHealth();
        cow.hurtServer(level, level.damageSources().lava(), 4F);
        check(cow.getHealth() == hp, "fire cow is immune to lava");
        Mob iceSheep = spawn(level, EntityTypes.SHEEP, o.offset(2, 0, 4));
        ElementEvents.apply(iceSheep, Element.ICE);
        hp = iceSheep.getHealth();
        iceSheep.hurtServer(level, level.damageSources().freeze(), 3F);
        check(iceSheep.getHealth() == hp, "ice sheep doesn't freeze");

        // ---- attacks ----
        Mob victim = spawn(level, EntityTypes.PIG, o.offset(4, 0, 4));
        victim.hurtServer(level, level.damageSources().mobAttack(cow), 1F);
        check(victim.getRemainingFireTicks() > 0, "fire creature sets its target on fire");
        Mob victim2 = spawn(level, EntityTypes.PIG, o.offset(6, 0, 4));
        victim2.hurtServer(level, level.damageSources().mobAttack(iceSheep), 1F);
        check(victim2.hasEffect(MobEffects.SLOWNESS), "ice creature slows its target");

        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setPos(o.getX() + 18.5, o.getY(), o.getZ() + 8.5);
        Mob z1 = spawn(level, EntityTypes.ZOMBIE, o.offset(20, 0, 8));
        Mob z2 = spawn(level, EntityTypes.ZOMBIE, o.offset(21, 0, 9));
        Elementals.setElement(player, Element.ELECTRIC);
        z1.hurtServer(level, level.damageSources().playerAttack(player), 4F);
        check(z2.getHealth() < z2.getMaxHealth(), "electric hit jumps to a nearby enemy");

        Mob d1 = spawn(level, EntityTypes.PIG, o.offset(8, 0, 4)), d2 = spawn(level, EntityTypes.PIG, o.offset(9, 0, 4));
        Mob dragonAttacker = spawn(level, EntityTypes.ZOMBIE, o.offset(8, 0, 6));
        ElementEvents.apply(dragonAttacker, Element.DRAGON);
        Mob plain = spawn(level, EntityTypes.ZOMBIE, o.offset(9, 0, 6));
        ElementEvents.apply(plain, Element.NONE);
        float h1 = d1.getHealth(), h2 = d2.getHealth();
        d1.hurtServer(level, level.damageSources().mobAttack(dragonAttacker), 3F);
        d2.hurtServer(level, level.damageSources().mobAttack(plain), 3F);
        check(h1 - d1.getHealth() > h2 - d2.getHealth(), "dragon creature hits harder");

        // ---- element rolls: Nether gives fire sometimes, never something else ----
        ServerLevel nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        if (nether != null) {
            int fire = 0, other = 0;
            for (int i = 0; i < 200; i++) {
                Element el = ElementEvents.roll(cow, nether, level.getRandom());
                if (el == Element.FIRE) fire++;
                else if (el != Element.NONE) other++;
            }
            check(fire > 10 && other == 0, "Nether creatures roll fire (" + fire + "/200) and nothing else");
        }

        if (ModList.get().isLoaded("animalbonds")) pl.kubalte.elementals.compat.AnimalBondsSelfTest.run(level, o.offset(0, 0, 20));

        for (Mob m : new Mob[] {lizard, frost, moss, owl, cow, iceSheep, victim, victim2, z1, z2, d1, d2, dragonAttacker, plain}) {
            if (m != null) m.discard();
        }
    }
}
