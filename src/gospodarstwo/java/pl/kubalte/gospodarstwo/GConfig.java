package pl.kubalte.gospodarstwo;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GConfig {
    private GConfig() {}

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    static {
        B.push("animals");
    }
    public static final ModConfigSpec.BooleanValue SPAWN_ANIMALS = B.comment("Kaczki, gęsi, indyki i ślimaki pojawiają się w świecie")
            .define("naturalSpawns", true);
    public static final ModConfigSpec.DoubleValue BREED_CHANCE = B.comment("Szansa, że zwykłe zwierzę pojawi się jako rasa (0-1)")
            .defineInRange("breedChance", 0.15, 0.0, 1.0);
    public static final ModConfigSpec.IntValue HARVEST_COOLDOWN = B.comment("Co ile ticków można strzyc rasę / zebrać produkt (24000 = doba)")
            .defineInRange("harvestCooldown", 12000, 20, 1_000_000);
    static {
        B.pop().push("bonsai");
    }
    public static final ModConfigSpec.IntValue BONSAI_TICKS = B.comment("Ile ticków podlanego wzrostu potrzeba do pełnego bonsai")
            .defineInRange("growTicks", 36000, 200, 10_000_000);
    public static final ModConfigSpec.IntValue BONSAI_WATER = B.comment("Na ile ticków starcza jedno podlanie")
            .defineInRange("waterTicks", 24000, 200, 10_000_000);
    static {
        B.pop().push("loom");
    }
    public static final ModConfigSpec.IntValue LOOM_TICKS = B.comment("Ile ticków trwa utkanie jednej sztuki materiału")
            .defineInRange("weaveTicks", 100, 1, 100_000);
    public static final ModConfigSpec.IntValue TAN_TICKS = B.comment("Ile ticków trwa garbowanie skór na ramie (24000 = doba)")
            .defineInRange("tanTicks", 12000, 1, 10_000_000);
    public static final ModConfigSpec.IntValue KETTLE_TICKS = B.comment("Ile ticków trwa jedna porcja w kotle")
            .defineInRange("kettleTicks", 160, 1, 100_000);
    public static final ModConfigSpec.IntValue QUERN_TICKS = B.comment("Ile ticków trwa zmielenie jednej porcji na żarnach").defineInRange("quernTicks", 60, 1, 100_000);
    public static final ModConfigSpec.IntValue PRESS_TICKS = B.comment("Ile ticków trwa wyciśnięcie jednej butelki soku").defineInRange("pressTicks", 100, 1, 100_000);
    public static final ModConfigSpec.IntValue FERMENT_TICKS = B.comment("Ile ticków trwa kiszenie w garnku (24000 = doba)")
            .defineInRange("fermentTicks", 24000, 1, 10_000_000);
    public static final ModConfigSpec.IntValue SMOKE_TICKS = B.comment("Ile ticków trwa wędzenie (6000 = ćwierć doby)")
            .defineInRange("smokeTicks", 6000, 1, 10_000_000);
    public static final ModConfigSpec.IntValue CHURN_STROKES = B.comment("Ile ruchów tłoka trzeba, żeby ubić masło").defineInRange("churnStrokes", 12, 1, 1000);
    public static final ModConfigSpec.BooleanValue CLOTHING_EFFECTS = B.comment("Ciepłe ubrania leczą na mrozie, lniane przyspieszają w upale")
            .define("clothingEffects", true);
    public static final ModConfigSpec.IntValue RESTED_PERCENT = B.comment("Długość efektu Wypoczęty w % (100 = pół doby z poduszką, doba z poduszką i pierzyną)")
            .defineInRange("restedPercent", 100, 0, 1000);
    public static final ModConfigSpec.BooleanValue EXTRA_DROPS = B.comment("Dodatkowe łupy ze zwierząt i potworów (skóra, łój, rogi, gruczoły...)")
            .define("extraDrops", true);
    static {
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();
}
