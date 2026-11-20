package pl.kubalte.gospodarstwo.craft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** Extra materials from vanilla animals and monsters (raw hide, tallow, horns, glands, skins...). */
public final class MobDrops {
    private MobDrops() {}

    /** One possible extra drop: chance, min-max count, adults only. */
    public record Drop(Supplier<? extends Item> item, float chance, int min, int max, boolean adultOnly) {}

    private static final Map<EntityType<?>, List<Drop>> DROPS = new HashMap<>();

    private static Drop d(Supplier<? extends Item> item, float chance, int min, int max) {
        return new Drop(item, chance, min, max, true);
    }

    private static void put(EntityType<?> type, Drop... drops) {
        DROPS.put(type, List.of(drops));
    }

    public static void add(EntityType<?> type, Drop drop) {
        List<Drop> old = DROPS.getOrDefault(type, List.of());
        java.util.ArrayList<Drop> n = new java.util.ArrayList<>(old);
        n.add(drop);
        DROPS.put(type, n);
    }

    private static boolean ready;

    static void init() {
        put(EntityTypes.COW, d(Gospodarstwo.RAW_HIDE, 0.5F, 1, 2), d(Gospodarstwo.TALLOW, 0.4F, 1, 2), d(Gospodarstwo.HORN, 0.15F, 1, 1),
                d(Gospodarstwo.SINEW, 0.25F, 1, 1));
        put(EntityTypes.MOOSHROOM, d(Gospodarstwo.RAW_HIDE, 0.5F, 1, 2), d(Gospodarstwo.TALLOW, 0.4F, 1, 2), d(Gospodarstwo.HORN, 0.15F, 1, 1));
        put(EntityTypes.SHEEP, d(Gospodarstwo.TALLOW, 0.4F, 1, 1), d(Gospodarstwo.GUT, 0.3F, 1, 1), d(Gospodarstwo.RAW_HIDE, 0.25F, 1, 1));
        put(EntityTypes.PIG, d(Gospodarstwo.FATBACK, 0.5F, 1, 2), d(Gospodarstwo.BRISTLES, 0.3F, 1, 2));
        put(EntityTypes.GOAT, d(Gospodarstwo.HORN, 0.15F, 1, 1), d(Gospodarstwo.TALLOW, 0.3F, 1, 1));
        put(EntityTypes.HORSE, d(Gospodarstwo.SINEW, 0.25F, 1, 1), d(Gospodarstwo.RAW_HIDE, 0.3F, 1, 1));
        put(EntityTypes.RABBIT, d(Gospodarstwo.RABBIT_FUR, 0.5F, 1, 1));
        put(EntityTypes.FOX, d(Gospodarstwo.FOX_FUR, 0.6F, 1, 1));
        put(EntityTypes.POLAR_BEAR, d(Gospodarstwo.BEAR_FUR, 0.7F, 1, 1), d(Gospodarstwo.BEAR_CLAW, 0.3F, 1, 1));
        put(EntityTypes.CHICKEN, d(Gospodarstwo.POULTRY_BONE, 0.3F, 1, 1));
        put(EntityTypes.COD, new Drop(Gospodarstwo.SWIM_BLADDER, 0.2F, 1, 1, false));
        put(EntityTypes.SALMON, new Drop(Gospodarstwo.SWIM_BLADDER, 0.2F, 1, 1, false));
        put(EntityTypes.SQUID, d(Gospodarstwo.TENTACLE, 0.5F, 1, 2));
        put(EntityTypes.SPIDER, d(Gospodarstwo.SPINNERET, 0.2F, 1, 1));
        put(EntityTypes.CAVE_SPIDER, d(Gospodarstwo.VENOM_GLAND, 0.25F, 1, 1), d(Gospodarstwo.SPINNERET, 0.1F, 1, 1));
        put(EntityTypes.ZOMBIE, d(Gospodarstwo.RAGS, 0.3F, 1, 2));
        put(EntityTypes.HUSK, d(Gospodarstwo.RAGS, 0.3F, 1, 2));
        put(EntityTypes.ZOMBIE_VILLAGER, d(Gospodarstwo.RAGS, 0.4F, 1, 2));
        put(EntityTypes.DROWNED, d(Gospodarstwo.RAGS, 0.3F, 1, 1), d(Gospodarstwo.NET_SCRAP, 0.1F, 1, 1));
        put(EntityTypes.SKELETON, d(Gospodarstwo.BONE_MARROW, 0.15F, 1, 1));
        put(EntityTypes.STRAY, d(Gospodarstwo.BONE_MARROW, 0.15F, 1, 1));
        put(EntityTypes.BOGGED, d(Gospodarstwo.BONE_MARROW, 0.15F, 1, 1), d(Gospodarstwo.POISON_MYCELIUM, 0.25F, 1, 1));
        put(EntityTypes.CREEPER, d(Gospodarstwo.CREEPER_SKIN, 0.1F, 1, 1));
        put(EntityTypes.WITCH, d(Gospodarstwo.HERB_POUCH, 0.3F, 1, 1));
        put(EntityTypes.STRIDER, d(Gospodarstwo.STRIDER_SKIN, 0.4F, 1, 1));
        put(EntityTypes.BLAZE, d(Gospodarstwo.BLAZE_ASH, 0.4F, 1, 2));
        put(EntityTypes.GHAST, d(Gospodarstwo.GHAST_MEMBRANE, 0.3F, 1, 1));
        put(EntityTypes.HOGLIN, d(Gospodarstwo.THICK_HIDE, 0.5F, 1, 1), d(Gospodarstwo.HOGLIN_TUSK, 0.2F, 1, 1), d(Gospodarstwo.BRISTLES, 0.3F, 1, 2));
        put(EntityTypes.ZOGLIN, d(Gospodarstwo.HOGLIN_TUSK, 0.2F, 1, 1));
        put(EntityTypes.RAVAGER, d(Gospodarstwo.RAVAGER_HORN, 1F, 1, 1), d(Gospodarstwo.THICK_HIDE, 1F, 1, 2));
        put(EntityTypes.GUARDIAN, new Drop(Gospodarstwo.GUARDIAN_SPIKE, 0.3F, 1, 1, false));
        put(EntityTypes.ELDER_GUARDIAN, new Drop(Gospodarstwo.GUARDIAN_SPIKE, 1F, 2, 4, false));
        put(EntityTypes.BREEZE, d(Gospodarstwo.WIND_FIBRE, 0.3F, 1, 2));
        put(EntityTypes.WARDEN, d(Gospodarstwo.SCULK_CORE, 1F, 1, 1));
        put(EntityTypes.ENDER_DRAGON, d(Gospodarstwo.DRAGON_SCALE, 1F, 3, 5));
        // the new farm birds
        put(Gospodarstwo.DUCK.get(), d(Gospodarstwo.POULTRY_BONE, 0.3F, 1, 1));
        put(Gospodarstwo.GOOSE.get(), d(Gospodarstwo.POULTRY_BONE, 0.3F, 1, 1), d(Gospodarstwo.TALLOW, 0.2F, 1, 1));
        put(Gospodarstwo.TURKEY.get(), d(Gospodarstwo.POULTRY_BONE, 0.4F, 1, 2));
        ready = true;
    }

    public static List<Drop> dropsOf(EntityType<?> type) {
        if (!ready) init();
        return DROPS.getOrDefault(type, List.of());
    }

    public static void onDrops(LivingDropsEvent e) {
        LivingEntity ent = e.getEntity();
        if (ent.level().isClientSide() || !GConfig.EXTRA_DROPS.get()) return;
        RandomSource r = ent.getRandom();
        for (Drop drop : dropsOf(ent.getType())) {
            if (drop.adultOnly() && ent.isBaby()) continue;
            if (r.nextFloat() >= drop.chance()) continue;
            int n = drop.min() + (drop.max() > drop.min() ? r.nextInt(drop.max() - drop.min() + 1) : 0);
            ItemStack s = new ItemStack(drop.item().get(), n);
            e.getDrops().add(new ItemEntity(ent.level(), ent.getX(), ent.getY() + 0.3, ent.getZ(), s));
        }
    }
}
