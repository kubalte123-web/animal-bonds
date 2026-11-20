package pl.kubalte.gospodarstwo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import pl.kubalte.gospodarstwo.block.LoomBlockEntity;
import pl.kubalte.gospodarstwo.block.LoomRecipes;
import pl.kubalte.gospodarstwo.block.WeavingLoomBlock;

/**
 * The moving parts of the weaving loom (the wooden frame is the block model): warp threads from the back beam to the
 * breast beam, two heddle frames that lift the threads in turn, the shuttle flying through the gap, and the cloth
 * growing towards the weaver and rolled on the front beam.
 */
public class LoomRenderer implements BlockEntityRenderer<LoomBlockEntity, LoomRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        int yarn = -1, cloth = -1, clothCount;
        boolean weaving;
        float anim, progress;
    }

    static final Identifier WOOL = Identifier.withDefaultNamespace("textures/block/white_wool.png");
    static final Identifier WOOD = Identifier.withDefaultNamespace("textures/block/spruce_planks.png");
    static final Identifier STRING = Identifier.withDefaultNamespace("textures/block/white_concrete.png");

    public LoomRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LoomBlockEntity be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, crumbling);
        s.facing = be.getBlockState().hasProperty(WeavingLoomBlock.FACING) ? be.getBlockState().getValue(WeavingLoomBlock.FACING) : Direction.NORTH;
        LoomRecipes.Recipe in = LoomRecipes.find(be.input());
        s.yarn = in == null ? -1 : in.yarnColor();
        s.cloth = clothColor(be.output(), in);
        s.clothCount = be.output().getCount();
        s.weaving = be.weaving();
        s.anim = be.animTicks + (be.weaving() ? partialTick : 0F);
        s.progress = be.weaving() ? (be.animTicks % 100F) / 100F : 0F;
    }

    static int clothColor(ItemStack out, LoomRecipes.Recipe current) {
        if (!out.isEmpty()) {
            for (LoomRecipes.Recipe r : LoomRecipes.all()) if (out.is(r.output().get())) return r.clothColor();
        }
        return current == null ? -1 : current.clothColor();
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(-angle(s.facing)));
        ps.translate(-0.5F, 0F, -0.5F);

        float t = s.anim;
        // which heddle is up: switches each pass of the shuttle (40 ticks per pass)
        float pass = t / 40F;
        float lift = s.weaving ? Mth.sin((float) (pass * Math.PI)) : 0F;
        float shed = 1.4F * lift;

        // heddle frames hanging from the top beam
        float h1 = shed, h2 = -shed;
        collector.submitCustomGeometry(ps, RenderTypes.entityCutout(WOOD), (pose, vc) -> {
            Boxes.box(pose, vc, 2.5F, 8.5F + h1, 7.2F, 13.5F, 9.0F + h1, 7.8F, 0xFFFFFFFF, light);
            Boxes.box(pose, vc, 2.5F, 12.5F + h1, 7.2F, 13.5F, 13.0F + h1, 7.8F, 0xFFFFFFFF, light);
            Boxes.box(pose, vc, 2.5F, 8.5F + h2, 8.6F, 13.5F, 9.0F + h2, 9.2F, 0xFFFFFFFF, light);
            Boxes.box(pose, vc, 2.5F, 12.5F + h2, 8.6F, 13.5F, 13.0F + h2, 9.2F, 0xFFFFFFFF, light);
            if (s.yarn >= 0 || s.weaving) {
                // the shuttle runs to and fro inside the open shed
                float phase = pass - (float) Math.floor(pass);
                boolean leftToRight = ((int) Math.floor(pass)) % 2 == 0;
                float sx = s.weaving ? (leftToRight ? phase : 1F - phase) * 12F + 1.5F : 1.0F;
                Boxes.box(pose, vc, sx - 1.5F, 10.2F, 5.2F, sx + 1.5F, 11.0F, 6.4F, 0xFFC08850, light);
            }
        });

        if (s.yarn >= 0) {
            int yarn = 0xFF000000 | s.yarn;
            collector.submitCustomGeometry(ps, RenderTypes.entityCutout(STRING), (pose, vc) -> {
                for (int i = 0; i < 12; i++) {
                    float x = 3F + i * (10F / 11F);
                    float off = (i % 2 == 0) ? h1 : h2;
                    // back beam -> heddle, heddle -> breast beam (the shed opens in the middle)
                    Boxes.box(pose, vc, x - 0.15F, 10.6F + off * 0.5F, 8F, x + 0.15F, 10.9F + off * 0.5F, 13.5F, yarn, light);
                    Boxes.box(pose, vc, x - 0.15F, 10.6F + off * 0.3F, 3.5F, x + 0.15F, 10.9F + off * 0.3F, 8F, yarn, light);
                }
            });
        }

        if (s.cloth >= 0) {
            int cloth = 0xFF000000 | s.cloth;
            float len = Math.min(4.5F, 0.8F + s.progress * 1.5F + Math.min(s.clothCount, 8) * 0.4F);
            float roll = Math.min(1.6F, s.clothCount * 0.1F);
            collector.submitCustomGeometry(ps, RenderTypes.entityCutout(WOOL), (pose, vc) -> {
                // woven part between the breast beam and the heddles
                Boxes.box(pose, vc, 2.8F, 10.5F, 3.0F, 13.2F, 11.0F, 3.0F + len, cloth, light);
                // rolled cloth on the breast beam
                if (roll > 0) Boxes.box(pose, vc, 2.6F, 9.0F - roll, 1.0F - roll, 13.4F, 11.2F + roll * 0.3F, 3.0F + roll * 0.3F, cloth, light);
            });
        }
        ps.popPose();
    }

    static float angle(Direction d) {
        return switch (d) {
            case EAST -> 90F;
            case SOUTH -> 180F;
            case WEST -> 270F;
            default -> 0F;
        };
    }
}
