package pl.kubalte.elementals;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ElementalsConfig {
    private ElementalsConfig() {}

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    static {
        B.push("elements");
    }
    public static final ModConfigSpec.BooleanValue ELEMENTS_ON_ALL = B.comment("Czy zwykłe moby (także z innych modów) mogą dostać żywioł")
            .define("elementsOnAllMobs", true);
    public static final ModConfigSpec.DoubleValue CHANCE = B.comment("Mnożnik szans na żywioł przy pojawieniu się stwora")
            .defineInRange("chanceMultiplier", 1.0, 0.0, 20.0);
    public static final ModConfigSpec.BooleanValue PARTICLES = B.comment("Cząsteczki wokół stworzeń z żywiołem")
            .define("particles", true);
    static {
        B.pop().push("creatures");
    }
    public static final ModConfigSpec.BooleanValue SPAWN_NEW_CREATURES = B.comment("Czy nowe stworzenia pojawiają się naturalnie")
            .define("naturalSpawns", true);
    static {
        B.pop();
    }
    public static final ModConfigSpec SPEC = B.build();
}
