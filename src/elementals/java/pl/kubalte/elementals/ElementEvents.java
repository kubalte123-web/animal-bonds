package pl.kubalte.elementals;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Gameplay of the elements (works on its own, for every creature):
 * <ul>
 *   <li>creatures get an element when they appear, depending on where (Nether = fire, snow = ice, underground/night = dark,
 *       thunderstorm = electric, End/high peaks = dragon), and are a bit tougher</li>
 *   <li>fire: immune to fire and sets its targets on fire; ice: immune to freezing and slows its targets;
 *       dark: blinds in the dark; electric: hits jump to nearby enemies, stronger in rain; dragon: hits harder, takes less from elements</li>
 *   <li>riding an elemental creature gives the rider a partner bonus; ice mounts freeze water under them</li>
 *   <li>babies inherit an element from a parent</li>
 * </ul>
 */
public final class ElementEvents {
    private ElementEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(ElementEvents::onJoin);
        bus.addListener(ElementEvents::onDamage);
        bus.addListener(ElementEvents::onPlayerTick);
        bus.addListener(ElementEvents::onMobTick);
        bus.addListener(ElementEvents::onBaby);
    }

    private static final Identifier BONUS_ID = Identifier.fromNamespaceAndPath(Elementals.MODID, "element");
    private static final Set<String> NEVER = Set.of("minecraft:ender_dragon", "minecraft:wither", "minecraft:villager",
            "minecraft:wandering_trader", "minecraft:armor_stand", "minecraft:mannequin", "minecraft:elder_guardian", "minecraft:warden");

    // ---------------- assigning elements ----------------

    static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob mob) || Elementals.hasRolledElement(mob)) return;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        Element el;
        if (mob.getType() == Elementals.EMBERLIZARD.get()) el = Element.FIRE;
        else if (mob.getType() == Elementals.FROSTLING.get()) el = Element.ICE;
        else if (NEVER.contains(id) || Boolean.getBoolean("elementals.noRolls")) el = Element.NONE;
        else if (!id.startsWith(Elementals.MODID + ":") && !ElementalsConfig.ELEMENTS_ON_ALL.get()) el = Element.NONE;
        else el = roll(mob, (ServerLevel) e.getLevel(), mob.getRandom());
        apply(mob, el);
    }

    /** Sets the element and its toughness bonus. */
    public static void apply(Mob mob, Element el) {
        Elementals.setElement(mob, el);
        var hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.removeModifier(BONUS_ID);
            if (el != Element.NONE) {
                double bonus = el == Element.DRAGON ? 0.5 : 0.2;
                hp.addPermanentModifier(new AttributeModifier(BONUS_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                mob.setHealth(mob.getMaxHealth());
            }
        }
    }

    /** Chance of each element where the creature appears. */
    public static Element roll(Mob mob, ServerLevel level, RandomSource rnd) {
        double m = ElementalsConfig.CHANCE.get();
        BlockPos pos = mob.blockPosition();
        if (level.dimension() == Level.NETHER) return rnd.nextDouble() < 0.25 * m ? Element.FIRE : Element.NONE;
        if (level.dimension() == Level.END) return rnd.nextDouble() < 0.15 * m ? Element.DRAGON : Element.NONE;
        Holder<Biome> biome = level.getBiome(pos);
        float temp = biome.value().getBaseTemperature();
        if (level.isThundering() && rnd.nextDouble() < 0.2 * m) return Element.ELECTRIC;
        if (pos.getY() > 150 && rnd.nextDouble() < 0.01 * m) return Element.DRAGON;
        if (temp >= 1.5F && rnd.nextDouble() < 0.10 * m) return Element.FIRE;
        if (temp <= 0.15F && rnd.nextDouble() < 0.12 * m) return Element.ICE;
        boolean dark = !level.canSeeSky(pos) && pos.getY() < 50;
        if ((dark || (mob instanceof Enemy && level.isDarkOutside())) && rnd.nextDouble() < 0.08 * m) return Element.DARK;
        if (rnd.nextDouble() < 0.03 * m) {
            Element[] any = {Element.FIRE, Element.ICE, Element.DARK, Element.ELECTRIC};
            return any[rnd.nextInt(any.length)];
        }
        return Element.NONE;
    }

    static void onBaby(BabyEntitySpawnEvent e) {
        if (!(e.getChild() instanceof Mob child)) return;
        Element a = Elementals.element(e.getParentA()), b = Elementals.element(e.getParentB());
        RandomSource r = child.getRandom();
        Element el = r.nextFloat() < 0.1F ? Element.NONE : r.nextBoolean() ? a : b;
        if (el == Element.NONE && (a != Element.NONE || b != Element.NONE) && r.nextFloat() < 0.5F) el = a != Element.NONE ? a : b;
        apply(child, el);
    }

    // ---------------- combat ----------------

    private static final ThreadLocal<Boolean> CHAIN = ThreadLocal.withInitial(() -> false);

    static void onDamage(LivingIncomingDamageEvent e) {
        LivingEntity target = e.getEntity();
        Element te = Elementals.element(target);
        var src = e.getSource();
        // immunities
        if (te == Element.FIRE && src.is(DamageTypeTags.IS_FIRE)) {
            e.setCanceled(true);
            target.clearFire();
            return;
        }
        if (te == Element.ICE && src.is(DamageTypeTags.IS_FREEZING)) {
            e.setCanceled(true);
            return;
        }
        Entity attacker = src.getEntity();
        Element ae = attacker == null ? Element.NONE : Elementals.element(attacker);
        if (te == Element.DRAGON && ae != Element.NONE) e.setAmount(e.getAmount() * 0.75F);
        if (ae == Element.NONE || src.getDirectEntity() != attacker || target.level().isClientSide()) return;
        ServerLevel level = (ServerLevel) target.level();
        switch (ae) {
            case FIRE -> target.igniteForSeconds(3.0F);
            case ICE -> {
                effect(target, MobEffects.SLOWNESS, 60, 1);
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40, target.getTicksFrozen() + 60));
            }
            case DARK -> {
                if (level.getMaxLocalRawBrightness(target.blockPosition()) <= 7) effect(target, MobEffects.BLINDNESS, 50, 0);
            }
            case ELECTRIC -> {
                if (level.isRainingAt(target.blockPosition())) e.setAmount(e.getAmount() * 1.5F);
                if (!CHAIN.get()) chain(level, attacker, target, e.getAmount() * 0.5F);
            }
            case DRAGON -> e.setAmount(e.getAmount() * 1.3F);
            default -> {}
        }
    }

    /** Electric hits jump to up to two nearby enemies of the attacker. */
    private static void chain(ServerLevel level, Entity attacker, LivingEntity first, float dmg) {
        CHAIN.set(true);
        try {
            int hits = 0;
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, first.getBoundingBox().inflate(4),
                    o -> o != first && o != attacker && o.isAlive() && !(o instanceof Player p && p.isCreative()) && !o.isAlliedTo(attacker))) {
                if (attacker instanceof Mob am && !(other instanceof Player) && !(other instanceof Enemy) && am.getTarget() != other) continue;
                other.hurtServer(level, level.damageSources().lightningBolt(), Math.max(1F, dmg));
                spark(level, first, other);
                if (++hits >= 2) break;
            }
        } finally {
            CHAIN.set(false);
        }
    }

    private static void spark(ServerLevel level, Entity a, Entity b) {
        double ax = a.getX(), ay = a.getY() + a.getBbHeight() / 2, az = a.getZ();
        double bx = b.getX(), by = b.getY() + b.getBbHeight() / 2, bz = b.getZ();
        for (int i = 0; i <= 8; i++) {
            double t = i / 8.0;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, ax + (bx - ax) * t, ay + (by - ay) * t, az + (bz - az) * t, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    private static void effect(LivingEntity target, Holder<MobEffect> effect, int ticks, int amp) {
        target.addEffect(new MobEffectInstance(effect, ticks, amp));
    }

    // ---------------- riding: partner bonuses ----------------

    static void onPlayerTick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (p.level().isClientSide() || p.tickCount % 20 != 0 || !(p.getVehicle() instanceof Mob mount)) return;
        switch (Elementals.element(mount)) {
            case FIRE -> effect(p, MobEffects.FIRE_RESISTANCE, 60, 0);
            case ICE -> effect(p, MobEffects.RESISTANCE, 60, 0);
            case DARK -> effect(p, MobEffects.NIGHT_VISION, 260, 0);
            case ELECTRIC -> effect(p, MobEffects.HASTE, 60, 1);
            case DRAGON -> {
                effect(p, MobEffects.SLOW_FALLING, 60, 0);
                effect(p, MobEffects.STRENGTH, 60, 0);
            }
            default -> {}
        }
    }

    /** Ice creatures freeze the water they walk on while ridden; fire creatures in water hiss. */
    static void onMobTick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof Mob mob) || mob.level().isClientSide() || !mob.isVehicle()) return;
        if (Elementals.element(mob) != Element.ICE) return;
        Level level = mob.level();
        BlockPos below = mob.blockPosition().below();
        int r = 2;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + 1) continue;
                m.set(below.getX() + dx, below.getY(), below.getZ() + dz);
                BlockState st = level.getBlockState(m);
                if (st.getFluidState().is(Fluids.WATER) && st.getFluidState().isSource() && level.getBlockState(m.above()).isAir()) {
                    level.setBlockAndUpdate(m, Blocks.FROSTED_ICE.defaultBlockState());
                    level.scheduleTick(m.immutable(), Blocks.FROSTED_ICE, 60 + mob.getRandom().nextInt(60));
                }
            }
        }
    }
}
