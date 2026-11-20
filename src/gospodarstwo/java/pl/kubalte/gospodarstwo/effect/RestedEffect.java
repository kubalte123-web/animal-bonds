package pl.kubalte.gospodarstwo.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** "Wypoczęty": after a night in a bed with a pillow and/or a featherbed. Extra hearts (set up where it is registered). */
public class RestedEffect extends MobEffect {
    public RestedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF2D9A0);
    }
}
