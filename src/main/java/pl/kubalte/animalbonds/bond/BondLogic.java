package pl.kubalte.animalbonds.bond;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import pl.kubalte.animalbonds.BondConfig;
import pl.kubalte.animalbonds.ModRegistry;
import pl.kubalte.animalbonds.block.TroughBlock;

/** Server-side bond rules shared by interactions, the pedestal and the self test. */
public final class BondLogic {
    private BondLogic() {}

    public static final long DAY = 24000L;

    public static BondData get(Mob mob) {
        return mob.getExistingDataOrNull(ModRegistry.BOND.get());
    }

    public static boolean isBonded(Mob mob) {
        return mob.hasData(ModRegistry.BOND.get());
    }

    public static int level(Mob mob) {
        BondData d = get(mob);
        return d == null ? 0 : d.level();
    }

    /** Vanilla-tamed creatures (wolves, cats, horses...) are bonded automatically. */
    public static boolean isVanillaTamed(Mob mob) {
        if (mob instanceof AbstractHorse horse) return horse.isTamed();
        return mob instanceof TamableAnimal t && t.isTame();
    }

    /** Returns existing bond data, auto-creating it for vanilla-tamed creatures. */
    public static BondData getOrAutoBond(Mob mob) {
        BondData d = get(mob);
        if (d == null && isVanillaTamed(mob)) d = bond(mob);
        return d;
    }

    public static BondData bond(Mob mob) {
        BondData d = get(mob);
        if (d != null) return d;
        d = new BondData();
        d.lastGift = mob.level().getGameTime();
        mob.setData(ModRegistry.BOND.get(), d);
        mob.setPersistenceRequired();
        return d;
    }

    public static void sync(Mob mob, BondData d) {
        mob.setData(ModRegistry.BOND.get(), d);
    }

    /** Adds xp (scaled by config); returns true when a new level was reached. */
    public static boolean addXp(Mob mob, BondData d, int amount) {
        if (amount <= 0) return false;
        int before = d.level();
        int scaled = (int) Math.max(1, Math.round(amount * BondConfig.XP_MULTIPLIER.get()));
        d.xp = Math.min(BondLevels.maxXp(), d.xp + scaled);
        sync(mob, d);
        boolean up = d.level() > before;
        if (up && mob.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
        }
        return up;
    }

    public static void hearts(Mob mob, int count) {
        if (mob.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), count, 0.4, 0.3, 0.4, 0.05);
        }
    }

    /** Evaluates all needs of the mob against the pen and stores the result. */
    public static void evaluateNeeds(Mob mob, BondData d, PenScanner.Pen pen, List<Mob> penMobs) {
        SpeciesProfiles.Profile profile = SpeciesProfiles.get(mob);
        long now = mob.level().getGameTime();
        int mask = 0;
        int satisfied = 0;
        List<Need> needs = profile.needs();
        int sameSpecies = 0;
        int needBeds = 0;
        for (Mob other : penMobs) {
            if (other.getType() == mob.getType()) sameSpecies++;
            if (SpeciesProfiles.get(other).needs().contains(Need.BED)) needBeds++;
        }
        BlockPos pos = mob.blockPosition();
        for (int i = 0; i < needs.size(); i++) {
            boolean ok = switch (needs.get(i)) {
                case TROUGH -> !pen.troughs.isEmpty();
                case BED -> pen.beds > 0 && pen.beds * 2 >= needBeds;
                case ROOF -> !mob.level().canSeeSky(pos.above());
                case WATER -> pen.water;
                case GRASS -> pen.grassCells * 4 >= pen.area();
                case SPACE -> pen.area() >= profile.space() * Math.max(1, penMobs.size());
                case COMPANY -> sameSpecies >= 2;
                case TREAT -> now - d.lastTreat < DAY;
                case AFFECTION -> now - d.lastPet < DAY;
                case DARKNESS -> mob.level().getMaxLocalRawBrightness(pos) <= 7;
                case LIGHT -> mob.level().getMaxLocalRawBrightness(pos) >= 10;
                case HAY -> pen.hay;
                case SAND -> pen.sandCells * 4 >= pen.area();
                case FLOWERS -> pen.flowers;
                case SNOW -> pen.snow;
                case WARMTH -> pen.warmth;
            };
            if (ok) {
                mask |= 1 << i;
                satisfied++;
            }
        }
        d.needMask = mask;
        d.happiness = needs.isEmpty() ? 100 : satisfied * 100 / needs.size();
    }

    /** One pen scan tick for a bonded mob: needs, passive xp, trough food and gifts. */
    public static void penTick(ServerLevel level, BlockPos pedestal, Mob mob, BondData d, PenScanner.Pen pen, List<Mob> penMobs) {
        d.home = java.util.Optional.of(GlobalPos.of(level.dimension(), pedestal));
        evaluateNeeds(mob, d, pen, penMobs);
        RandomSource rnd = level.getRandom();

        // Eating from the trough.
        if (SpeciesProfiles.get(mob).needs().contains(Need.TROUGH) && !pen.troughs.isEmpty() && rnd.nextInt(8) == 0) {
            BlockPos t = pen.troughs.get(rnd.nextInt(pen.troughs.size()));
            BlockState st = level.getBlockState(t);
            if (st.getBlock() instanceof TroughBlock && st.getValue(TroughBlock.FILL) > 0) {
                level.setBlockAndUpdate(t, st.setValue(TroughBlock.FILL, st.getValue(TroughBlock.FILL) - 1));
            }
        }

        // Passive bond growth when the creature is content.
        if (d.happiness >= 50) {
            addXp(mob, d, Math.round(BondConfig.PASSIVE_XP.get() * d.happiness / 100f));
        }

        tryGift(level, mob, d);
        sync(mob, d);
    }

    public static long giftInterval(BondData d) {
        double base = BondConfig.GIFT_INTERVAL.get();
        double lvlFactor = 1.0 + 0.2 * (d.level() - 1);
        double moodFactor = d.happiness >= 75 ? 1.0 : d.happiness >= 50 ? 1.5 : 3.0;
        return (long) (base / lvlFactor * moodFactor);
    }

    /** Drops a material gift if enough time passed. Returns the stack that was given (or EMPTY). */
    public static ItemStack tryGift(ServerLevel level, Mob mob, BondData d) {
        if (d.isFainted() || d.level() < BondConfig.GIFT_LEVEL.get()) return ItemStack.EMPTY;
        long now = level.getGameTime();
        if (now - d.lastGift < giftInterval(d)) return ItemStack.EMPTY;
        ItemStack gift = rollGift(mob, d.level(), level.getRandom());
        d.lastGift = now;
        if (!gift.isEmpty()) {
            mob.spawnAtLocation(level, gift);
            hearts(mob, 3);
        }
        return gift;
    }

    public static ItemStack rollGift(Mob mob, int level, RandomSource rnd) {
        List<SpeciesProfiles.Gift> pool = SpeciesProfiles.get(mob).gifts().stream().filter(g -> g.minLevel() <= level).toList();
        if (pool.isEmpty()) return ItemStack.EMPTY;
        SpeciesProfiles.Gift g = pool.get(rnd.nextInt(pool.size()));
        Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(g.item())).orElse(null);
        if (item == null) return ItemStack.EMPTY;
        int bonus = level >= 8 ? 1 : 0;
        int count = g.min() + (g.max() > g.min() ? rnd.nextInt(g.max() - g.min() + 1) : 0) + bonus;
        return new ItemStack(item, Math.min(count, item.getDefaultMaxStackSize()));
    }

    public static long faintDuration(BondData d) {
        int over = Math.max(0, d.level() - BondConfig.FAINT_LEVEL.get());
        return (long) (BondConfig.FAINT_TICKS.get() / (1.0 + 0.35 * over));
    }
}
