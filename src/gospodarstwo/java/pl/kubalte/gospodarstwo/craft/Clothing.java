package pl.kubalte.gospodarstwo.craft;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** Everyday clothes: weak armour with a small comfort effect (warm clothes in the cold, linen in the heat). */
public final class Clothing {
    private Clothing() {}

    public static final TagKey<Item> WARM = TagKey.create(Registries.ITEM, Gospodarstwo.id("warm_clothing"));
    public static final TagKey<Item> AIRY = TagKey.create(Registries.ITEM, Gospodarstwo.id("airy_clothing"));

    private static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Gospodarstwo.id(name));
    }

    private static Map<ArmorType, Integer> def(int boots, int legs, int chest, int helmet) {
        Map<ArmorType, Integer> m = new EnumMap<>(ArmorType.class);
        m.put(ArmorType.BOOTS, boots);
        m.put(ArmorType.LEGGINGS, legs);
        m.put(ArmorType.CHESTPLATE, chest);
        m.put(ArmorType.HELMET, helmet);
        m.put(ArmorType.BODY, chest);
        return m;
    }

    private static TagKey<Item> repair(String name) {
        return TagKey.create(Registries.ITEM, Gospodarstwo.id("repairs_" + name));
    }

    public static final ArmorMaterial WOOL = new ArmorMaterial(5, def(1, 1, 2, 1), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0F, 0F,
            repair("wool"), asset("wool"));
    public static final ArmorMaterial FELT = new ArmorMaterial(7, def(1, 1, 2, 1), 12, SoundEvents.ARMOR_EQUIP_LEATHER, 0F, 0F,
            repair("felt"), asset("felt"));
    public static final ArmorMaterial FUR = new ArmorMaterial(6, def(1, 1, 2, 1), 12, SoundEvents.ARMOR_EQUIP_LEATHER, 0F, 0F,
            repair("fur"), asset("fur"));
    public static final ArmorMaterial SHEEPSKIN = new ArmorMaterial(9, def(1, 2, 3, 1), 12, SoundEvents.ARMOR_EQUIP_LEATHER, 0F, 0F,
            repair("sheepskin"), asset("sheepskin"));
    public static final ArmorMaterial LINEN = new ArmorMaterial(4, def(1, 1, 1, 1), 18, SoundEvents.ARMOR_EQUIP_LEATHER, 0F, 0F,
            repair("linen"), asset("linen"));

    private static int count(Player p, TagKey<Item> tag) {
        int n = 0;
        for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
            if (p.getItemBySlot(s).is(tag)) n++;
        return n;
    }

    /** Cold = it would snow here; hot = deserts, savannas, badlands and the Nether. */
    public static boolean cold(Player p) {
        BlockPos pos = p.blockPosition();
        Holder<Biome> b = p.level().getBiome(pos);
        return b.value().coldEnoughToSnow(pos, p.level().getSeaLevel());
    }

    public static boolean hot(Player p) {
        Holder<Biome> b = p.level().getBiome(p.blockPosition());
        return b.value().getBaseTemperature() >= 1.5F;
    }

    public static void onTick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (p.level().isClientSide() || p.tickCount % 40 != 0 || !GConfig.CLOTHING_EFFECTS.get()) return;
        if (count(p, WARM) >= 3 && cold(p) && p.getHealth() < p.getMaxHealth())
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
        if (count(p, AIRY) >= 2 && hot(p))
            p.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false, true));
    }
}
