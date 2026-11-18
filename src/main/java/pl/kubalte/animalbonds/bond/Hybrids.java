package pl.kubalte.animalbonds.bond;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import pl.kubalte.animalbonds.BondConfig;
import pl.kubalte.animalbonds.ModRegistry;

/** Splicing two bonded creatures into a hybrid, and the hybrid's merged traits. */
public final class Hybrids {
    private Hybrids() {}

    /** Choices made at the altar, relative to the two selected parents A and B. */
    public record Choices(boolean bodyB, boolean headB, boolean legsB, boolean extrasB, int colors, boolean movementB) {}

    public static HybridData get(Entity e) {
        return e.getExistingDataOrNull(ModRegistry.HYBRID.get());
    }

    public static boolean isHybrid(Entity e) {
        return e.hasData(ModRegistry.HYBRID.get());
    }

    // ---------------- merged profile ----------------

    private static final Map<String, SpeciesProfiles.Profile> MERGED = new HashMap<>();

    /** Needs, favourite food and gifts of both parents. */
    public static SpeciesProfiles.Profile mergedProfile(SpeciesProfiles.Profile body, String bodyType, String donorType) {
        return MERGED.computeIfAbsent(bodyType + "+" + donorType, k -> {
            SpeciesProfiles.Profile donor = SpeciesProfiles.byId(donorType);
            LinkedHashSet<Need> needs = new LinkedHashSet<>(body.needs());
            for (Need n : donor.needs()) if (needs.size() < 9) needs.add(n);
            LinkedHashSet<String> fav = new LinkedHashSet<>(body.favourites());
            fav.addAll(donor.favourites());
            List<SpeciesProfiles.Gift> gifts = new ArrayList<>(body.gifts());
            gifts.addAll(donor.gifts());
            return new SpeciesProfiles.Profile(List.copyOf(needs), List.copyOf(fav), List.copyOf(gifts), Math.max(body.space(), donor.space()));
        });
    }

    // ---------------- movement taken from the donor ----------------

    private static final Map<String, Mob> DUMMIES = new HashMap<>();

    /** A never-spawned instance of the given type, used to read how that creature moves. */
    public static Mob dummy(Level level, String typeId) {
        Mob cached = DUMMIES.get(typeId);
        if (cached != null) return cached;
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(typeId));
        Mob mob = null;
        if (type.isPresent()) {
            Entity e = type.get().create(level, EntitySpawnReason.LOAD);
            if (e instanceof Mob m) mob = m;
        }
        if (mob != null) DUMMIES.put(typeId, mob);
        return mob;
    }

    /** The creature whose way of moving this mob uses when ridden. */
    public static Mob movementSource(Mob mob) {
        HybridData h = get(mob);
        if (h == null || !h.movement()) return mob;
        Mob d = dummy(mob.level(), h.donorType());
        return d != null ? d : mob;
    }

    // ---------------- splicing ----------------

    public static String typeId(Entity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
    }

    /** Why these two can't be spliced (translation key), or null when they can. */
    public static String problem(ServerPlayer player, Mob a, Mob b) {
        for (Mob m : new Mob[] {a, b}) {
            if (m == null) continue;
            BondData d = BondLogic.get(m);
            if (d == null || d.level() < BondConfig.SPLICE_LEVEL.get()) return "message.animalbonds.splice_level";
            if (!BondAccess.canUse(player, m, d)) return "message.animalbonds.not_yours_short";
            if (d.isFainted()) return "message.animalbonds.splice_fainted";
            if (isHybrid(m)) return "message.animalbonds.splice_hybrid";
            if (m.level().getGameTime() - d.lastSplice < BondConfig.SPLICE_COOLDOWN.get()) return "message.animalbonds.splice_tired";
        }
        if (a == null || b == null || !a.isAlive() || !b.isAlive()) return "message.animalbonds.splice_missing";
        if (a == b) return "message.animalbonds.splice_same";
        return null;
    }

    private static ItemStack cost() {
        Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(BondConfig.SPLICE_COST_ITEM.get())).orElse(null);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, BondConfig.SPLICE_COST_COUNT.get());
    }

    private static boolean payCost(ServerPlayer player) {
        ItemStack cost = cost();
        if (cost.isEmpty() || cost.getCount() <= 0 || player.getAbilities().instabuild) return true;
        int have = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(cost.getItem())) have += inv.getItem(i).getCount();
        if (have < cost.getCount()) return false;
        int left = cost.getCount();
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(cost.getItem())) {
                int take = Math.min(left, st.getCount());
                st.shrink(take);
                left -= take;
            }
        }
        return true;
    }

    public static Component costText() {
        ItemStack c = cost();
        return c.isEmpty() ? Component.literal("-") : Component.literal(c.getCount() + "× ").append(c.getHoverName());
    }

    /** Creates the hybrid next to the altar. Returns it, or null (the player is told why). */
    public static Mob splice(ServerLevel level, ServerPlayer player, BlockPos altar, Mob a, Mob b, Choices c) {
        String problem = problem(player, a, b);
        if (problem != null) {
            player.sendOverlayMessage(Component.translatable(problem));
            return null;
        }
        if (!payCost(player)) {
            player.sendOverlayMessage(Component.translatable("message.animalbonds.splice_cost", costText()));
            return null;
        }
        Mob body = c.bodyB() ? b : a, donor = c.bodyB() ? a : b;
        // part flags are stored relative to the body: true = taken from the donor
        boolean head = c.headB() != c.bodyB();
        boolean legs = c.legsB() != c.bodyB();
        boolean extras = c.extrasB() != c.bodyB();
        boolean move = c.movementB() != c.bodyB();
        int colors = c.colors() == 2 ? 2 : ((c.colors() == 1) != c.bodyB() ? 1 : 0);
        String parents = a.getDisplayName().getString() + " × " + b.getDisplayName().getString();

        Entity created = body.getType().create(level, EntitySpawnReason.BREEDING);
        if (!(created instanceof Mob child)) return null;
        child.setPos(altar.getX() + 0.5, altar.getY() + 1.0, altar.getZ() + 0.5);
        child.setData(ModRegistry.HYBRID.get(), new HybridData(typeId(donor), head, legs, extras, colors, move, parents));
        level.addFreshEntity(child);
        BondData cd = BondLogic.bond(child);
        cd.owner = Optional.of(player.getUUID());
        BondData da = BondLogic.get(a), db = BondLogic.get(b);
        cd.home = da.home.isPresent() ? da.home : db.home;
        BondLogic.sync(child, cd);
        long now = level.getGameTime();
        da.lastSplice = now;
        db.lastSplice = now;
        BondLogic.sync(a, da);
        BondLogic.sync(b, db);

        level.sendParticles(ParticleTypes.END_ROD, child.getX(), child.getY() + 0.8, child.getZ(), 40, 0.5, 0.6, 0.5, 0.05);
        level.sendParticles(ParticleTypes.HEART, child.getX(), child.getY() + 1.2, child.getZ(), 8, 0.4, 0.3, 0.4, 0.05);
        level.playSound(null, altar, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.sendOverlayMessage(Component.translatable("message.animalbonds.spliced", parents));
        return child;
    }
}
