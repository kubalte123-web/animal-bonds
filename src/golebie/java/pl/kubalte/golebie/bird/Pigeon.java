package pl.kubalte.golebie.bird;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;

/** Gołąb: the grey one, with the dark shimmering neck and the orange eye. */
public class Pigeon extends Bird {
    public Pigeon(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public String species() {
        return "pigeon";
    }
}
