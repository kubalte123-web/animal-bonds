package pl.kubalte.animalbonds;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Everything gameplay-relevant is configurable (config/animalbonds-common.toml). */
public final class BondConfig {
    private BondConfig() {}

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    static {
        B.push("progress");
    }
    public static final ModConfigSpec.DoubleValue XP_MULTIPLIER = B.comment("Mnożnik całego zdobywanego doświadczenia więzi / XP multiplier")
            .defineInRange("xpMultiplier", 1.0, 0.01, 100.0);
    public static final ModConfigSpec.IntValue PET_XP = B.comment("XP za pogłaskanie")
            .defineInRange("petXp", 12, 0, 10000);
    public static final ModConfigSpec.IntValue PET_COOLDOWN = B.comment("Odstęp między głaskaniami dającymi XP (ticki)")
            .defineInRange("petCooldownTicks", 3000, 0, 1_000_000);
    public static final ModConfigSpec.IntValue TREAT_XP = B.comment("XP za karmienie z ręki ulubionym jedzeniem")
            .defineInRange("treatXp", 25, 0, 10000);
    public static final ModConfigSpec.IntValue TREAT_COOLDOWN = B.comment("Odstęp między karmieniami z ręki dającymi XP (ticki)")
            .defineInRange("treatCooldownTicks", 6000, 0, 1_000_000);
    public static final ModConfigSpec.IntValue PASSIVE_XP = B.comment("Maks. XP za jedno skanowanie zagrody przy 100% zadowolenia")
            .defineInRange("passiveXpPerScan", 3, 0, 1000);
    public static final ModConfigSpec.IntValue TAME_CHANCE = B.comment("Szansa (%) na nawiązanie więzi przy pierwszym karmieniu z ręki nieoswojonego stwora")
            .defineInRange("bondChancePercent", 40, 1, 100);
    static {
        B.pop().push("levels");
    }
    public static final ModConfigSpec.IntValue NAME_LEVEL = B.comment("Poziom, od którego można nadać imię w Kronice")
            .defineInRange("nameLevel", 1, 1, 10);
    public static final ModConfigSpec.IntValue GIFT_LEVEL = B.comment("Poziom, od którego stwór przynosi podarunki (materiały)")
            .defineInRange("giftLevel", 2, 1, 10);
    public static final ModConfigSpec.IntValue PEACEFUL_LEVEL = B.comment("Poziom, od którego potwór przestaje atakować graczy")
            .defineInRange("peacefulLevel", 1, 1, 10);
    public static final ModConfigSpec.IntValue RIDE_LEVEL = B.comment("Poziom, od którego można ujeżdżać stwora")
            .defineInRange("rideLevel", 5, 1, 10);
    public static final ModConfigSpec.IntValue BAGS_LEVEL = B.comment("Poziom, od którego można założyć juki")
            .defineInRange("bagsLevel", 4, 1, 10);
    public static final ModConfigSpec.IntValue FAINT_LEVEL = B.comment("Poziom, od którego stwór mdleje zamiast ginąć")
            .defineInRange("faintLevel", 7, 1, 11);
    static {
        B.pop().push("pen");
    }
    public static final ModConfigSpec.IntValue SCAN_INTERVAL = B.comment("Co ile ticków piedestał skanuje zagrodę")
            .defineInRange("scanIntervalTicks", 600, 20, 72000);
    public static final ModConfigSpec.IntValue MAX_PEN_AREA = B.comment("Maks. powierzchnia zagrody w blokach")
            .defineInRange("maxPenArea", 1600, 16, 16384);
    public static final ModConfigSpec.IntValue MAX_PEN_STEP = B.comment("Maks. różnica wysokości między sąsiednimi kratkami zagrody (wzniesienia/doły)")
            .defineInRange("maxPenStep", 5, 1, 16);
    static {
        B.pop().push("gifts");
    }
    public static final ModConfigSpec.IntValue GIFT_INTERVAL = B.comment("Bazowy odstęp między podarunkami na poziomie 1 (ticki)")
            .defineInRange("giftIntervalTicks", 12000, 200, 1_000_000);
    static {
        B.pop().push("faint");
    }
    public static final ModConfigSpec.IntValue FAINT_TICKS = B.comment("Czas omdlenia/regeneracji na poziomie omdleń (ticki); maleje z poziomem")
            .defineInRange("faintTicks", 6000, 20, 1_000_000);
    public static final ModConfigSpec.BooleanValue FAINT_TELEPORT = B.comment("Czy omdlały stwór wraca do swojej zagrody")
            .define("teleportHome", true);
    static {
        B.pop().push("riding");
    }
    public static final ModConfigSpec.DoubleValue RIDE_SPEED = B.comment("Mnożnik prędkości ujeżdżanego stwora")
            .defineInRange("rideSpeed", 1.0, 0.1, 5.0);
    static {
        B.pop().push("splicing");
    }
    public static final ModConfigSpec.IntValue SPLICE_LEVEL = B.comment("Poziom więzi, jaki muszą mieć oboje rodzice, by ich splatać")
            .defineInRange("spliceLevel", 5, 1, 10);
    public static final ModConfigSpec.ConfigValue<String> SPLICE_COST_ITEM = B.comment("Przedmiot zużywany przy splataniu")
            .define("costItem", "minecraft:golden_apple");
    public static final ModConfigSpec.IntValue SPLICE_COST_COUNT = B.comment("Ile sztuk przedmiotu kosztuje splatanie")
            .defineInRange("costCount", 1, 0, 64);
    public static final ModConfigSpec.IntValue SPLICE_COOLDOWN = B.comment("Odpoczynek rodzica po splataniu (ticki)")
            .defineInRange("parentCooldownTicks", 24000, 0, 10_000_000);
    static {
        B.pop().push("variety");
    }
    public static final ModConfigSpec.BooleanValue TEXTURE_VARIETY = B.comment("Każdy stwór dostaje własne, losowe ubarwienie (owce bez zmian)")
            .define("textureVariety", true);
    public static final ModConfigSpec.BooleanValue SIZE_VARIETY = B.comment("Losowy rozmiar stworów: mniejsze są szybsze, większe wolniejsze, ale mają więcej życia")
            .define("sizeVariety", true);
    public static final ModConfigSpec.DoubleValue MIN_SCALE = B.comment("Najmniejszy rozmiar (1.0 = normalny)")
            .defineInRange("minScale", 0.85, 0.3, 1.0);
    public static final ModConfigSpec.DoubleValue MAX_SCALE = B.comment("Największy rozmiar (1.0 = normalny)")
            .defineInRange("maxScale", 1.2, 1.0, 3.0);
    static {
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();
}
