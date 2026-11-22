package pl.kubalte.golebie.bird;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;

/** Pawik: the chestnut fantail with the grey split fan that trembles when it is happy. */
public class Fantail extends Bird {
    public Fantail(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public String species() {
        return "fantail";
    }

    @Override
    protected boolean hasTailShiver() {
        return true;
    }

    @Override
    int circleTicks() {
        return 52;
    }
}
