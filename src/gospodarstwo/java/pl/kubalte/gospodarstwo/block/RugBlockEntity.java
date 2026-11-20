package pl.kubalte.gospodarstwo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import pl.kubalte.gospodarstwo.Gospodarstwo;

public class RugBlockEntity extends ColoredBlockEntity {
    public RugBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.RUG_BE.get(), pos, state);
    }

    @Override
    public int naturalColor() {
        return 0xC8C2B8;
    }
}
