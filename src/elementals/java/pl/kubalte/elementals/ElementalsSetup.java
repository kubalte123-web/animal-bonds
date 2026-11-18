package pl.kubalte.elementals;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import pl.kubalte.elementals.entity.Emberlizard;
import pl.kubalte.elementals.entity.Frostling;
import pl.kubalte.elementals.entity.Mossling;
import pl.kubalte.elementals.entity.Puffowl;

final class ElementalsSetup {
    private ElementalsSetup() {}

    static void attributes(EntityAttributeCreationEvent e) {
        e.put(Elementals.MOSSLING.get(), Mossling.createAttributes().build());
        e.put(Elementals.PUFFOWL.get(), Puffowl.createAttributes().build());
        e.put(Elementals.EMBERLIZARD.get(), Emberlizard.createAttributes().build());
        e.put(Elementals.FROSTLING.get(), Frostling.createAttributes().build());
    }

    static void spawnPlacements(RegisterSpawnPlacementsEvent e) {
        var R = RegisterSpawnPlacementsEvent.Operation.REPLACE;
        e.register(Elementals.MOSSLING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> ElementalsConfig.SPAWN_NEW_CREATURES.get() && Animal.checkAnimalSpawnRules(t, l, r, p, rnd), R);
        e.register(Elementals.PUFFOWL.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> ElementalsConfig.SPAWN_NEW_CREATURES.get() && Animal.checkAnimalSpawnRules(t, l, r, p, rnd), R);
        e.register(Elementals.EMBERLIZARD.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> ElementalsConfig.SPAWN_NEW_CREATURES.get() && Monster.checkAnyLightMonsterSpawnRules(t, l, r, p, rnd), R);
        e.register(Elementals.FROSTLING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> ElementalsConfig.SPAWN_NEW_CREATURES.get() && Monster.checkMonsterSpawnRules(t, l, r, p, rnd), R);
    }
}
