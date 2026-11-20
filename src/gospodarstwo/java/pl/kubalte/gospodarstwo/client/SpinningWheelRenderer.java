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
import net.minecraft.world.phys.Vec3;
import pl.kubalte.gospodarstwo.block.SpinningWheelBlockEntity;
import pl.kubalte.gospodarstwo.block.StationBlock;
import pl.kubalte.gospodarstwo.craft.Dyes;

/**
 * Moving parts of the spinning wheel (the bench and uprights are the block model): the big wheel with spokes turning,
 * the drive band, the fibre on the distaff and the yarn growing on the bobbin.
 */
public class SpinningWheelRenderer implements BlockEntityRenderer<SpinningWheelBlockEntity, SpinningWheelRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        int fibre = -1, yarn = -1, yarnCount;
        boolean working;
        float anim;
    }

    static final Identifier WOOD = Identifier.withDefaultNamespace("textures/block/oak_planks.png");
    static final Identifier DARK = Identifier.withDefaultNamespace("textures/block/dark_oak_planks.png");
    static final Identifier WOOL = Identifier.withDefaultNamespace("textures/block/white_wool.png");
    static final float WX0 = 11.25F, WX1 = 12.25F, WY = 11F, WZ = 8F, R = 4.5F;

    public SpinningWheelRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpinningWheelBlockEntity be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, crumbling);
        s.facing = be.getBlockState().hasProperty(StationBlock.FACING) ? be.getBlockState().getValue(StationBlock.FACING) : Direction.NORTH;
        SpinningWheelBlockEntity.Spin spin = SpinningWheelBlockEntity.find(be.input());
        int dyed = Dyes.colorOf(be.input());
        s.fibre = spin == null ? -1 : dyed >= 0 ? dyed : woolColor(be, spin.fibreColor());
        int outDyed = Dyes.colorOf(be.output());
        s.yarn = !be.output().isEmpty() ? (outDyed >= 0 ? outDyed : 0xE8E2D4) : s.fibre;
        s.yarnCount = be.output().getCount();
        s.working = be.working();
        s.anim = be.animTicks + (be.working() ? partialTick : 0F);
    }

    private static int woolColor(SpinningWheelBlockEntity be, int fallback) {
        var job = be.job(be.input());
        if (job != null) {
            int c = Dyes.colorOf(job.result());
            if (c >= 0) return c;
        }
        return fallback;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(-LoomRenderer.angle(s.facing)));
        ps.translate(-0.5F, 0F, -0.5F);

        // the wheel: rim of 12 segments and 6 spokes, turning about its axle
        ps.pushPose();
        ps.translate(0F, WY / 16F, WZ / 16F);
        ps.mulPose(Axis.XP.rotationDegrees(s.anim * 9F));
        ps.translate(0F, -WY / 16F, -WZ / 16F);
        collector.submitCustomGeometry(ps, RenderTypes.entityCutout(DARK), (pose, vc) -> {
            PoseStack m = new PoseStack();
            m.last().pose().set(pose.pose());
            m.last().normal().set(pose.normal());
            for (int i = 0; i < 12; i++) {
                m.pushPose();
                m.translate(0F, WY / 16F, WZ / 16F);
                m.mulPose(Axis.XP.rotationDegrees(i * 30F));
                Boxes.box(m.last(), vc, WX0, R - 0.8F, -1.25F, WX1, R + 0.4F, 1.25F, 0xFFFFFFFF, light);
                m.popPose();
            }
            for (int i = 0; i < 6; i++) {
                m.pushPose();
                m.translate(0F, WY / 16F, WZ / 16F);
                m.mulPose(Axis.XP.rotationDegrees(i * 60F + 15F));
                Boxes.box(m.last(), vc, WX0 + 0.25F, 0F, -0.25F, WX1 - 0.25F, R - 0.6F, 0.25F, 0xFFFFFFFF, light);
                m.popPose();
            }
            // hub
            Boxes.box(pose, vc, WX0 - 0.3F, WY - 0.8F, WZ - 0.8F, WX1 + 0.3F, WY + 0.8F, WZ + 0.8F, 0xFFFFFFFF, light);
        });
        ps.popPose();

        int band = 0xFFE8E0D0;
        collector.submitCustomGeometry(ps, RenderTypes.entityCutout(WOOL), (pose, vc) -> {
            // drive band from the wheel to the bobbin (two thin strands)
            Boxes.box(pose, vc, 4.6F, WY + R - 0.2F, 7.9F, WX0 + 0.5F, WY + R, 8.1F, band, light);
            Boxes.box(pose, vc, 4.6F, WY - R + 0.6F, 7.9F, WX0 + 0.5F, WY - R + 0.8F, 8.1F, band, light);
            if (s.fibre >= 0) {
                int f = 0xFF000000 | s.fibre;
                // fibre wound loosely on the distaff
                Boxes.box(pose, vc, 4.9F, 12.4F, 6.9F, 7.1F, 15.2F, 9.1F, f, light);
                Boxes.box(pose, vc, 5.3F, 15.2F, 7.3F, 6.7F, 15.9F, 8.7F, f, light);
                // thread from the distaff to the bobbin
                Boxes.box(pose, vc, 4.4F, 11.1F, 7.95F, 5.4F, 12.5F, 8.05F, f, light);
            }
            if (s.yarn >= 0 && (s.yarnCount > 0 || s.working)) {
                float r = 0.5F + Math.min(1.1F, s.yarnCount * 0.07F + (s.working ? 0.15F : 0F));
                int y = 0xFF000000 | s.yarn;
                Boxes.box(pose, vc, 3F, 11F - r, 8F - 1.6F, 4.4F, 11F + r, 8F + 1.6F, y, light);
            }
        });
        ps.popPose();
    }
}
