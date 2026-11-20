package pl.kubalte.gospodarstwo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import pl.kubalte.gospodarstwo.Gospodarstwo;

public class CurtainBlockEntity extends ColoredBlockEntity {
    public CurtainBlockEntity(BlockPos pos, BlockState state) {
        super(Gospodarstwo.CURTAIN_BE.get(), pos, state);
    }

    @Override
    public int naturalColor() {
        return 0xE6DCC0;
    }
}
