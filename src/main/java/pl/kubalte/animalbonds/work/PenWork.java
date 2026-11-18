package pl.kubalte.animalbonds.work;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import pl.kubalte.animalbonds.BondConfig;
import pl.kubalte.animalbonds.bond.BondData;
import pl.kubalte.animalbonds.bond.BondLogic;
import pl.kubalte.animalbonds.bond.PenScanner;

/**
 * Optional Palworld-like work in the pen (config "work.enabled", off by default). Bonded creatures from level
 * "workLevel" take a job that fits them: farmers harvest and replant crops, guards chase wild monsters away,
 * collectors tidy dropped items into the pen storage, smelters cook what lies in it. Every job done is a bit of bond.
 */
public final class PenWork {
    private PenWork() {}

    private static final Map<String, EnumSet<Work>> BY_TYPE = new HashMap<>();
    private static final List<Function<Mob, Set<Work>>> PROVIDERS = new CopyOnWriteArrayList<>();
    /** Creature -> block it is walking to. */
    private static final Map<Mob, BlockPos> TARGETS = new WeakHashMap<>();
    private static final Map<Mob, Integer> TRIES = new WeakHashMap<>();

    static {
        for (String t : List.of("cow", "mooshroom", "sheep", "pig", "goat", "rabbit", "chicken", "horse", "donkey", "mule", "llama",
                "trader_llama", "camel", "sniffer", "armadillo", "turtle", "villager", "happy_ghast")) put(t, Work.FARMER);
        for (String t : List.of("wolf", "iron_golem", "snow_golem", "polar_bear", "zombie", "husk", "drowned", "skeleton", "stray",
                "spider", "cave_spider", "creaking", "piglin", "zombified_piglin", "hoglin", "ravager", "vindicator", "enderman", "bogged",
                "panda", "goat")) put(t, Work.GUARD);
        for (String t : List.of("fox", "cat", "ocelot", "parrot", "allay", "bee", "frog", "axolotl", "dolphin", "bat", "phantom", "vex"))
            put(t, Work.COLLECTOR);
        for (String t : List.of("blaze", "strider", "magma_cube", "ghast", "wither_skeleton")) put(t, Work.SMELTER);
    }

    private static void put(String vanillaId, Work w) {
        BY_TYPE.computeIfAbsent("minecraft:" + vanillaId, k -> EnumSet.noneOf(Work.class)).add(w);
    }

    public static void register(String typeId, Work work) {
        BY_TYPE.computeIfAbsent(typeId, k -> EnumSet.noneOf(Work.class)).add(work);
    }

    public static void registerProvider(Function<Mob, Set<Work>> provider) {
        PROVIDERS.add(provider);
    }

    public static boolean enabled() {
        return BondConfig.WORK_ENABLED.get();
    }

    public static EnumSet<Work> jobs(Mob mob) {
        EnumSet<Work> out = EnumSet.noneOf(Work.class);
        EnumSet<Work> base = BY_TYPE.get(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
        if (base != null) out.addAll(base);
        for (var p : PROVIDERS) {
            Set<Work> w = p.apply(mob);
            if (w != null) out.addAll(w);
        }
        return out;
    }

    /** Workers busy outside the fence are not "runaways". */
    public static boolean isBusy(Mob mob) {
        return TARGETS.containsKey(mob);
    }

    public static boolean canWork(Mob mob, BondData d) {
        return d != null && !d.isFainted() && d.level() >= BondConfig.WORK_LEVEL.get() && !mob.isPassenger() && !mob.isVehicle()
                && !mob.isLeashed() && !mob.isNoAi();
    }

    /** Called on every pen scan. */
    public static void tick(ServerLevel level, PenScanner.Pen pen, List<Mob> mobs) {
        if (!enabled()) return;
        AABB area = pen.box().inflate(BondConfig.WORK_RADIUS.get(), 2, BondConfig.WORK_RADIUS.get());
        Container storage = PenStorage.get(level, pen.storage);
        for (Mob mob : mobs) {
            BondData d = BondLogic.get(mob);
            if (!canWork(mob, d)) {
                TARGETS.remove(mob);
                continue;
            }
            int steps = (int) pen.workSpeed + (level.getRandom().nextFloat() < pen.workSpeed % 1F ? 1 : 0);
            for (int i = 0; i < Math.max(1, steps); i++) {
                boolean worked = false;
                for (Work w : jobs(mob)) {
                    worked = switch (w) {
                        case FARMER -> farm(level, mob, area, storage);
                        case GUARD -> guard(level, mob, area);
                        case COLLECTOR -> collect(level, mob, pen, storage);
                        case SMELTER -> smelt(level, mob, storage);
                    };
                    if (worked) break;
                }
                if (worked) {
                    BondLogic.addXp(mob, d, 1);
                    BondLogic.sync(mob, d);
                }
            }
        }
    }

    // ---------------- farmer ----------------

    static boolean ripe(BlockState st) {
        if (st.getBlock() instanceof CropBlock crop) return crop.isMaxAge(st);
        if (st.getBlock() instanceof SweetBerryBushBlock) return st.getValue(SweetBerryBushBlock.AGE) >= 3;
        return false;
    }

    private static boolean farm(ServerLevel level, Mob mob, AABB area, Container storage) {
        BlockPos target = TARGETS.get(mob);
        if (target != null && !ripe(level.getBlockState(target))) target = null;
        if (target == null) {
            target = findRipe(level, mob, area);
            if (target == null) {
                TARGETS.remove(mob);
                return false;
            }
            TARGETS.put(mob, target);
            TRIES.put(mob, 0);
        }
        if (mob.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5) > 2.5 * 2.5) {
            int tries = TRIES.merge(mob, 1, Integer::sum);
            if (tries < 6) {
                mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.1);
                return false;
            }
        }
        harvest(level, target, storage);
        TARGETS.remove(mob);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX() + 0.5, target.getY() + 0.6, target.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0);
        return true;
    }

    private static BlockPos findRipe(ServerLevel level, Mob mob, AABB area) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = (int) Math.floor(area.minX); x <= (int) Math.floor(area.maxX); x++) {
            for (int z = (int) Math.floor(area.minZ); z <= (int) Math.floor(area.maxZ); z++) {
                for (int y = (int) Math.floor(area.minY); y <= (int) Math.floor(area.maxY); y++) {
                    m.set(x, y, z);
                    if (!ripe(level.getBlockState(m)) || claimed(m)) continue;
                    double dd = mob.distanceToSqr(x + 0.5, y, z + 0.5);
                    if (dd < bestD) {
                        bestD = dd;
                        best = m.immutable();
                    }
                }
            }
        }
        return best;
    }

    private static boolean claimed(BlockPos p) {
        return TARGETS.containsValue(p);
    }

    /** Drops of a ripe crop go to the storage (or on the ground); the crop is replanted. */
    public static void harvest(ServerLevel level, BlockPos pos, Container storage) {
        BlockState st = level.getBlockState(pos);
        List<ItemStack> drops = Block.getDrops(st, level, pos, null);
        boolean replantedFromSeed = false;
        for (ItemStack s : drops) {
            // the plant keeps one of its seeds for replanting
            if (!replantedFromSeed && st.getBlock() instanceof CropBlock crop && s.is(st.getBlock().asItem())) {
                s.shrink(1);
                replantedFromSeed = true;
            }
            if (s.isEmpty()) continue;
            ItemStack rest = storage == null ? s : PenStorage.insert(storage, s);
            if (!rest.isEmpty()) Block.popResource(level, pos, rest);
        }
        if (st.getBlock() instanceof CropBlock crop) level.setBlockAndUpdate(pos, crop.getStateForAge(0));
        else if (st.getBlock() instanceof SweetBerryBushBlock) level.setBlockAndUpdate(pos, st.setValue(SweetBerryBushBlock.AGE, 1));
    }

    // ---------------- guard ----------------

    private static boolean guard(ServerLevel level, Mob mob, AABB area) {
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) == null) return false;
        if (mob.getTarget() != null && mob.getTarget().isAlive()) return true;
        Mob intruder = level.getEntitiesOfClass(Mob.class, area.inflate(8), m -> m instanceof Enemy && m.isAlive() && !BondLogic.isBonded(m))
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob))).orElse(null);
        if (intruder == null) return false;
        mob.setTarget(intruder);
        return true;
    }

    // ---------------- collector ----------------

    private static boolean collect(ServerLevel level, Mob mob, PenScanner.Pen pen, Container storage) {
        if (storage == null) return false;
        ItemEntity item = level.getEntitiesOfClass(ItemEntity.class, pen.box().inflate(2), e -> e.isAlive() && !pl.kubalte.animalbonds.play.BallLogic.isBall(e))
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob))).orElse(null);
        if (item == null) return false;
        if (mob.distanceToSqr(item) > 2.0 * 2.0) {
            int tries = TRIES.merge(mob, 1, Integer::sum);
            if (tries < 6) {
                mob.getNavigation().moveTo(item, 1.15);
                return false;
            }
        }
        TRIES.put(mob, 0);
        ItemStack rest = PenStorage.insert(storage, item.getItem());
        if (rest.isEmpty()) item.discard();
        else item.setItem(rest);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 4, 0.2, 0.2, 0.2, 0);
        return true;
    }

    // ---------------- smelter ----------------

    private static final Map<Item, Item> SMELT = Map.ofEntries(
            Map.entry(Items.BEEF, Items.COOKED_BEEF), Map.entry(Items.PORKCHOP, Items.COOKED_PORKCHOP),
            Map.entry(Items.CHICKEN, Items.COOKED_CHICKEN), Map.entry(Items.MUTTON, Items.COOKED_MUTTON),
            Map.entry(Items.RABBIT, Items.COOKED_RABBIT), Map.entry(Items.COD, Items.COOKED_COD), Map.entry(Items.SALMON, Items.COOKED_SALMON),
            Map.entry(Items.POTATO, Items.BAKED_POTATO), Map.entry(Items.KELP, Items.DRIED_KELP), Map.entry(Items.RAW_IRON, Items.IRON_INGOT),
            Map.entry(Items.RAW_GOLD, Items.GOLD_INGOT), Map.entry(Items.RAW_COPPER, Items.COPPER_INGOT), Map.entry(Items.SAND, Items.GLASS),
            Map.entry(Items.COBBLESTONE, Items.STONE), Map.entry(Items.CLAY_BALL, Items.BRICK), Map.entry(Items.NETHERRACK, Items.NETHER_BRICK),
            Map.entry(Items.WET_SPONGE, Items.SPONGE), Map.entry(Items.IRON_ORE, Items.IRON_INGOT), Map.entry(Items.GOLD_ORE, Items.GOLD_INGOT),
            Map.entry(Items.COPPER_ORE, Items.COPPER_INGOT), Map.entry(Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP));

    public static Item smelted(Item raw) {
        return SMELT.get(raw);
    }

    private static boolean smelt(ServerLevel level, Mob mob, Container storage) {
        if (storage == null) return false;
        ItemStack raw = PenStorage.takeOne(storage, s -> SMELT.containsKey(s.getItem()));
        if (raw.isEmpty()) return false;
        ItemStack rest = PenStorage.insert(storage, new ItemStack(SMELT.get(raw.getItem())));
        if (!rest.isEmpty()) {
            PenStorage.insert(storage, raw); // full: put it back
            return false;
        }
        level.sendParticles(ParticleTypes.FLAME, mob.getX(), mob.getY() + mob.getBbHeight() * 0.7, mob.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
        return true;
    }

    /** Pen upgrade: empty troughs are refilled with feed from the storage. */
    public static void refillTroughs(ServerLevel level, PenScanner.Pen pen) {
        Container storage = PenStorage.get(level, pen.storage);
        if (storage == null) return;
        for (BlockPos t : pen.allTroughs) {
            BlockState st = level.getBlockState(t);
            if (!(st.getBlock() instanceof pl.kubalte.animalbonds.block.TroughBlock)) continue;
            int fill = st.getValue(pl.kubalte.animalbonds.block.TroughBlock.FILL);
            if (fill >= 2) continue;
            ItemStack feed = PenStorage.takeOne(storage, s -> pl.kubalte.animalbonds.block.TroughBlock.feedValue(s) > 0);
            if (feed.isEmpty()) return;
            int add = pl.kubalte.animalbonds.block.TroughBlock.feedValue(feed);
            level.setBlockAndUpdate(t, st.setValue(pl.kubalte.animalbonds.block.TroughBlock.FILL, Math.min(4, fill + add)));
        }
    }

    static UUID id(Mob m) {
        return m.getUUID();
    }
}
