package pl.kubalte.gospodarstwo.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** A crop (age 0-7) planted from its own seed item. */
public class ModCropBlock extends CropBlock {
    private final Supplier<? extends Item> seed;
    private final MapCodec<ModCropBlock> codec;

    public ModCropBlock(Properties props, Supplier<? extends Item> seed) {
        super(props);
        this.seed = seed;
        this.codec = simpleCodec(p -> new ModCropBlock(p, seed));
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return codec;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }
}
