package pl.kubalte.gospodarstwo;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import pl.kubalte.gospodarstwo.entity.Duck;
import pl.kubalte.gospodarstwo.entity.Goose;
import pl.kubalte.gospodarstwo.entity.Snail;
import pl.kubalte.gospodarstwo.entity.Turkey;

final class GSetup {
    private GSetup() {}

    static void attributes(EntityAttributeCreationEvent e) {
        e.put(Gospodarstwo.DUCK.get(), Duck.createAttributes().build());
        e.put(Gospodarstwo.GOOSE.get(), Goose.createAttributes().build());
        e.put(Gospodarstwo.TURKEY.get(), Turkey.createAttributes().build());
        e.put(Gospodarstwo.SNAIL.get(), Snail.createAttributes().build());
    }

    static void spawnPlacements(RegisterSpawnPlacementsEvent e) {
        var R = RegisterSpawnPlacementsEvent.Operation.REPLACE;
        for (var type : java.util.List.of(Gospodarstwo.DUCK.get(), Gospodarstwo.GOOSE.get(), Gospodarstwo.TURKEY.get(), Gospodarstwo.SNAIL.get())) {
            e.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, l, r, p, rnd) -> GConfig.SPAWN_ANIMALS.get() && Animal.checkAnimalSpawnRules(t, l, r, p, rnd), R);
        }
    }
}
