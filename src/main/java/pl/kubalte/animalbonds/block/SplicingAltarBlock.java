package pl.kubalte.animalbonds.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.kubalte.animalbonds.bond.BondData;
import pl.kubalte.animalbonds.bond.BondLogic;
import pl.kubalte.animalbonds.bond.Hybrids;
import pl.kubalte.animalbonds.network.OpenAltarPayload;

/** "Ołtarz Splatania": joins two bonded creatures (within 10 blocks) into a hybrid. */
public class SplicingAltarBlock extends Block {
    public static final MapCodec<SplicingAltarBlock> CODEC = simpleCodec(SplicingAltarBlock::new);
    public static final int RANGE = 10;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(3, 4, 3, 13, 12, 13), Block.box(1, 12, 1, 15, 15, 15));

    public SplicingAltarBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    public static List<Mob> candidates(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(RANGE), m -> m.isAlive() && BondLogic.isBonded(m));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) open(sp, (ServerLevel) level, pos);
        return InteractionResult.SUCCESS;
    }

    public static void open(ServerPlayer sp, ServerLevel level, BlockPos pos) {
        List<OpenAltarPayload.Candidate> list = candidates(level, pos).stream().map(m -> {
            BondData d = BondLogic.get(m);
            String problem = Hybrids.problem(sp, m, null);
            // "missing" is about the second parent; only report problems of this creature itself
            if ("message.animalbonds.splice_missing".equals(problem)) problem = "";
            return new OpenAltarPayload.Candidate(m.getId(), Hybrids.typeId(m), m.getDisplayName().getString(), d.level(), problem == null ? "" : problem);
        }).toList();
        PacketDistributor.sendToPlayer(sp, new OpenAltarPayload(pos, list, Hybrids.costText().getString()));
    }
}
