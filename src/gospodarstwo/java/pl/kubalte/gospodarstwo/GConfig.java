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
    static {
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();
}
