package pl.kubalte.gospodarstwo.compat;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import pl.kubalte.animalbonds.api.AnimalBondsApi;
import pl.kubalte.animalbonds.bond.Need;
import pl.kubalte.animalbonds.work.Work;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.breed.Breed;
import pl.kubalte.gospodarstwo.breed.BreedEvents;

/** Only loaded with Animal Bonds: needs, favourite food and gifts of the new animals, breed shown in the Chronicle. */
public final class AnimalBondsCompat {
    private AnimalBondsCompat() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(AnimalBondsCompat::profiles));
        AnimalBondsApi.registerBookLine(mob -> {
            Breed b = BreedEvents.breed(mob);
            return b == null ? null : Component.translatable("breed.gospodarstwo.label", b.displayName());
        });
        AnimalBondsApi.registerWork("gospodarstwo:goose", Work.GUARD); // geese are the best farm guards
        AnimalBondsApi.registerWork("gospodarstwo:duck", Work.COLLECTOR);
        AnimalBondsApi.registerWork("gospodarstwo:turkey", Work.FARMER);
        Gospodarstwo.LOGGER.info("Swojskie Gospodarstwo: Animal Bonds integration active");
    }

    static void profiles() {
        String g = Gospodarstwo.MODID + ":";
        AnimalBondsApi.registerProfile(g + "duck", List.of(Need.WATER, Need.GRASS, Need.BED, Need.COMPANY, Need.TREAT, Need.AFFECTION),
                List.of(g + "flax_seeds", g + "cabbage_seeds", "minecraft:wheat_seeds"),
                List.of(g + "duck_egg,1,2,2", "minecraft:feather,1,3,2", g + "down,1,1,4", "minecraft:kelp,1,3,3", "minecraft:lily_pad,1,1,5"), 3);
        AnimalBondsApi.registerProfile(g + "goose", List.of(Need.WATER, Need.GRASS, Need.BED, Need.SPACE, Need.TREAT, Need.AFFECTION),
                List.of(g + "cabbage", g + "flax_seeds", "minecraft:wheat"),
                List.of(g + "goose_egg,1,1,2", g + "down,1,2,3", "minecraft:feather,1,3,2", "minecraft:short_grass,1,3,2"), 4);
        AnimalBondsApi.registerProfile(g + "turkey", List.of(Need.GRASS, Need.ROOF, Need.SPACE, Need.TREAT, Need.AFFECTION),
                List.of(g + "strawberry", "minecraft:sweet_berries", "minecraft:wheat_seeds"),
                List.of(g + "turkey_feather,1,2,2", "minecraft:feather,1,3,2", "minecraft:pumpkin_seeds,1,4,3"), 5);
        AnimalBondsApi.registerProfile(g + "snail", List.of(Need.DARKNESS, Need.FLOWERS, Need.ROOF, Need.TREAT, Need.AFFECTION),
                List.of(g + "cabbage", "minecraft:moss_block", "minecraft:brown_mushroom"),
                List.of(g + "snail_slime,1,1,3", "minecraft:moss_carpet,1,2,2", "minecraft:brown_mushroom,1,2,2", g + "snail_shell,1,1,6"), 2);
    }
}
