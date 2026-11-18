package pl.kubalte.elementals.compat;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import pl.kubalte.animalbonds.api.AnimalBondsApi;
import pl.kubalte.animalbonds.api.BondGiftEvent;
import pl.kubalte.animalbonds.api.PenTickEvent;
import pl.kubalte.animalbonds.bond.BondLogic;
import pl.kubalte.animalbonds.bond.Need;
import pl.kubalte.animalbonds.bond.PenScanner;
import pl.kubalte.elementals.Element;
import pl.kubalte.elementals.Elementals;

/**
 * Only loaded when Animal Bonds is installed: the new creatures get needs/food/gifts, and elements and species
 * give real bonuses to the whole pen they live in.
 * <ul>
 *   <li>Fire: warmth for everyone; gifts come out smelted (raw meat cooked, raw ore → ingots...)</li>
 *   <li>Ice: snow for everyone; trough food lasts twice as long</li>
 *   <li>Dark: darkness for everyone; at night wild monsters near the pen glow and are weakened</li>
 *   <li>Electric: gifts come 40% more often, bond grows twice as fast during thunderstorms</li>
 *   <li>Dragon: +50% bond growth and one extra item in every gift</li>
 *   <li>Mchowiec: plants and crops in and around the pen grow faster; grass for everyone</li>
 *   <li>Puszek: sociable, each one makes bonds in the pen grow 10% faster (up to +30%)</li>
 * </ul>
 */
public final class AnimalBondsCompat {
    private AnimalBondsCompat() {}

    /** Creatures living in a pen with a fire creature, until the given game time (refreshed every scan). */
    private static final Map<Mob, Long> FIRE_PENS = new WeakHashMap<>();

    public static void init(IEventBus modBus) {
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(AnimalBondsCompat::registerProfiles));
        NeoForge.EVENT_BUS.addListener(AnimalBondsCompat::onPenTick);
        NeoForge.EVENT_BUS.addListener(AnimalBondsCompat::onGift);
        for (String c : new String[] {"mossling", "puffowl", "emberlizard", "frostling"})
            AnimalBondsApi.excludeFromTextureVariety(Elementals.MODID + ":" + c); // their colours show the element
        AnimalBondsApi.registerBookLine(mob -> {
            Element el = Elementals.element(mob);
            return el == Element.NONE ? null : Component.translatable("element.elementals.label", el.displayName());
        });
        Elementals.LOGGER.info("Żywiołaki: Animal Bonds integration active");
    }

    static void registerProfiles() {
        AnimalBondsApi.registerProfile("elementals:mossling", List.of(Need.GRASS, Need.WATER, Need.TROUGH, Need.SPACE, Need.TREAT, Need.AFFECTION),
                List.of("minecraft:sweet_berries", "minecraft:glow_berries", "minecraft:moss_block"),
                List.of("minecraft:moss_block,1,2,2", "minecraft:sweet_berries,1,3,2", "minecraft:glow_berries,1,2,4", "minecraft:azalea,1,1,5",
                        "minecraft:flowering_azalea,1,1,7"), 6);
        AnimalBondsApi.registerProfile("elementals:puffowl", List.of(Need.BED, Need.DARKNESS, Need.COMPANY, Need.FLOWERS, Need.TREAT, Need.AFFECTION),
                List.of("minecraft:wheat_seeds", "minecraft:melon_seeds", "minecraft:sweet_berries"),
                List.of("minecraft:feather,1,3,2", "minecraft:wheat_seeds,1,4,2", "minecraft:glow_berries,1,2,5", "minecraft:spider_eye,1,1,4"), 3);
        AnimalBondsApi.registerProfile("elementals:emberlizard", List.of(Need.WARMTH, Need.ROOF, Need.SAND, Need.TREAT, Need.AFFECTION),
                List.of("minecraft:magma_cream", "minecraft:blaze_powder", "minecraft:cooked_porkchop"),
                List.of("minecraft:netherrack,2,4,2", "minecraft:magma_cream,1,1,3", "minecraft:blaze_powder,1,2,4", "minecraft:fire_charge,1,1,6",
                        "minecraft:quartz,1,2,3"), 6);
        AnimalBondsApi.registerProfile("elementals:frostling", List.of(Need.SNOW, Need.DARKNESS, Need.SPACE, Need.TREAT, Need.AFFECTION),
                List.of("minecraft:snowball", "minecraft:ice", "minecraft:sweet_berries"),
                List.of("minecraft:snowball,2,6,2", "minecraft:ice,1,2,2", "minecraft:packed_ice,1,2,4", "minecraft:blue_ice,1,1,8",
                        "minecraft:powder_snow_bucket,1,1,6"), 4);
    }

    static void onPenTick(PenTickEvent e) {
        PenScanner.Pen pen = e.getPen();
        ServerLevel level = e.getLevel();
        EnumSet<Element> elements = EnumSet.noneOf(Element.class);
        int mosslings = 0, puffowls = 0;
        for (Mob m : e.getCreatures()) {
            elements.add(Elementals.element(m));
            if (m.getType() == Elementals.MOSSLING.get()) mosslings++;
            if (m.getType() == Elementals.PUFFOWL.get()) puffowls++;
        }
        if (elements.contains(Element.FIRE)) {
            pen.forcedNeeds.add(Need.WARMTH);
            long until = level.getGameTime() + 2400;
            for (Mob m : e.getCreatures()) FIRE_PENS.put(m, until);
        }
        if (elements.contains(Element.ICE)) {
            pen.forcedNeeds.add(Need.SNOW);
            pen.troughUse *= 0.5F;
        }
        if (elements.contains(Element.DARK)) {
            pen.forcedNeeds.add(Need.DARKNESS);
            if (level.isDarkOutside()) guard(level, e.getPedestal(), pen);
        }
        if (elements.contains(Element.ELECTRIC)) {
            pen.giftSpeed *= 1.4F;
            if (level.isThundering()) pen.xpMultiplier *= 2F;
        }
        if (elements.contains(Element.DRAGON)) {
            pen.xpMultiplier *= 1.5F;
            pen.giftBonus += 1;
        }
        if (mosslings > 0) {
            pen.forcedNeeds.add(Need.GRASS);
            growPlants(level, pen, 6 + 4 * Math.min(mosslings, 3));
        }
        if (puffowls > 0) pen.xpMultiplier *= 1F + 0.1F * Math.min(puffowls, 3);
    }

    /** Dark creatures watch over the pen at night: wild monsters around it glow and are weakened. */
    private static void guard(ServerLevel level, BlockPos pedestal, PenScanner.Pen pen) {
        AABB area = pen.box().inflate(16);
        for (Mob m : level.getEntitiesOfClass(Mob.class, area, m -> m instanceof Enemy && !BondLogic.isBonded(m))) {
            m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 700, 0));
            m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 700, 1));
        }
    }

    /** Mchowiec: bone-meal-like growth on random plants in and around the pen. */
    private static void growPlants(ServerLevel level, PenScanner.Pen pen, int tries) {
        var rnd = level.getRandom();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = 0; i < tries * 4; i++) {
            int x = pen.minX - 4 + rnd.nextInt(pen.maxX - pen.minX + 9);
            int z = pen.minZ - 4 + rnd.nextInt(pen.maxZ - pen.minZ + 9);
            int y = pen.minY - 1 + rnd.nextInt(Math.max(1, pen.maxY - pen.minY + 3));
            m.set(x, y, z);
            BlockState st = level.getBlockState(m);
            if (st.getBlock() instanceof BonemealableBlock b && !st.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                    && b.isValidBonemealTarget(level, m, st) && b.isBonemealSuccess(level, rnd, m, st)) {
                b.performBonemeal(level, rnd, m.immutable(), st);
                if (--tries <= 0) return;
            }
        }
    }

    private static final Map<Item, Item> SMELT = Map.ofEntries(
            Map.entry(Items.BEEF, Items.COOKED_BEEF), Map.entry(Items.PORKCHOP, Items.COOKED_PORKCHOP),
            Map.entry(Items.CHICKEN, Items.COOKED_CHICKEN), Map.entry(Items.MUTTON, Items.COOKED_MUTTON),
            Map.entry(Items.RABBIT, Items.COOKED_RABBIT), Map.entry(Items.COD, Items.COOKED_COD), Map.entry(Items.SALMON, Items.COOKED_SALMON),
            Map.entry(Items.POTATO, Items.BAKED_POTATO), Map.entry(Items.KELP, Items.DRIED_KELP), Map.entry(Items.RAW_IRON, Items.IRON_INGOT),
            Map.entry(Items.RAW_GOLD, Items.GOLD_INGOT), Map.entry(Items.RAW_COPPER, Items.COPPER_INGOT), Map.entry(Items.SAND, Items.GLASS),
            Map.entry(Items.COBBLESTONE, Items.STONE), Map.entry(Items.CLAY_BALL, Items.BRICK), Map.entry(Items.NETHERRACK, Items.NETHER_BRICK),
            Map.entry(Items.WET_SPONGE, Items.SPONGE), Map.entry(Items.IRON_NUGGET, Items.IRON_NUGGET));

    /** Fire in the pen: gifts come out smelted. */
    static void onGift(BondGiftEvent e) {
        Mob mob = e.getCreature();
        Long until = FIRE_PENS.get(mob);
        boolean fire = Elementals.element(mob) == Element.FIRE || (until != null && until > mob.level().getGameTime());
        if (!fire) return;
        Item cooked = SMELT.get(e.getStack().getItem());
        if (cooked != null && cooked != e.getStack().getItem()) e.setStack(new ItemStack(cooked, e.getStack().getCount()));
    }

    /** Used by the self test. */
    public static Item smelted(Item raw) {
        return SMELT.getOrDefault(raw, raw);
    }

    static String id(Mob m) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).toString();
    }
}
