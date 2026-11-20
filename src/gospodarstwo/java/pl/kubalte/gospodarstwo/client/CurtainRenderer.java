package pl.kubalte.gospodarstwo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.block.CurtainBlock;
import pl.kubalte.gospodarstwo.block.CurtainBlockEntity;

/**
 * A pleated curtain: zig-zag folds across the whole width when closed, gathered into two bunches with tie-backs when
 * open. The rod is drawn only on the top piece, the tie-backs only on the bottom one.
 */
public class CurtainRenderer implements BlockEntityRenderer<CurtainBlockEntity, CurtainRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        boolean open, top, bottom, left, right;
        int color;
    }

    static final Identifier CLOTH = Gospodarstwo.id("textures/block/curtain_cloth.png");
    static final Identifier ROD = Identifier.withDefaultNamespace("textures/block/dark_oak_log.png");

    public CurtainRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    private static boolean same(CurtainBlockEntity be, BlockState s, net.minecraft.core.BlockPos p) {
        BlockState o = be.getLevel().getBlockState(p);
        return o.getBlock() instanceof CurtainBlock && o.getValue(CurtainBlock.FACING) == s.getValue(CurtainBlock.FACING);
    }

    @Override
    public void extractRenderState(CurtainBlockEntity be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, crumbling);
        BlockState st = be.getBlockState();
        if (!(st.getBlock() instanceof CurtainBlock)) return;
        s.facing = st.getValue(CurtainBlock.FACING);
        s.open = st.getValue(CurtainBlock.OPEN);
        s.color = 0xFF000000 | be.shownColor();
        if (be.getLevel() != null) {
            s.top = !same(be, st, be.getBlockPos().above());
            s.bottom = !same(be, st, be.getBlockPos().below());
            // facing north we look at it from the north: the player's left is west (-x)
            Direction leftDir = s.facing.getClockWise();
            s.left = !same(be, st, be.getBlockPos().relative(leftDir));
            s.right = !same(be, st, be.getBlockPos().relative(leftDir.getOpposite()));
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(-LoomRenderer.angle(s.facing)));
        ps.translate(-0.5F, 0F, -0.5F);
        float yTop = s.top ? 14.6F : 16F;
        collector.submitCustomGeometry(ps, RenderTypes.entityCutout(CLOTH), (pose, vc) -> {
            if (!s.open) {
                pleats(pose, vc, 0F, 16F, 8, 0F, yTop, 14.0F, 0.9F, s.color, light);
            } else {
                // two bunches; a bunch only where this piece is the outer edge of the curtain
                if (s.left) bunch(pose, vc, 0F, yTop, s.bottom, s.color, light);
                if (s.right) bunch(pose, vc, 11.5F, yTop, s.bottom, s.color, light);
            }
        });
        if (s.top) collector.submitCustomGeometry(ps, RenderTypes.entityCutout(ROD), (pose, vc) -> {
            Boxes.box(pose, vc, s.left ? 0.5F : 0F, 14.8F, 14.6F, s.right ? 15.5F : 16F, 15.8F, 15.6F, 0xFFFFFFFF, light);
            if (s.left) Boxes.box(pose, vc, 0F, 14.5F, 14.3F, 1F, 16F, 15.9F, 0xFFFFFFFF, light);
            if (s.right) Boxes.box(pose, vc, 15F, 14.5F, 14.3F, 16F, 16F, 15.9F, 0xFFFFFFFF, light);
            // rings
            for (float x = 1.5F; x < 15F; x += 2F) Boxes.box(pose, vc, x, 14.4F, 14.3F, x + 0.6F, 16F, 15.9F, 0xFF8A6A40, light);
        });
        ps.popPose();
    }

    /** A gathered bunch 4.5 px wide with deep folds and a tie-back. */
    private static void bunch(PoseStack.Pose pose, VertexConsumer vc, float x0, float yTop, boolean bottom, int color, int light) {
        pleats(pose, vc, x0, x0 + 4.5F, 6, 0F, yTop, 13.2F, 1.8F, color, light);
        if (bottom) {
            int tie = darker(color);
            Boxes.box(pose, vc, x0 - 0.1F, 6.5F, 12.7F, x0 + 4.6F, 7.5F, 15.4F, tie, light);
        }
    }

    /** Vertical zig-zag strips between x0 and x1: depth alternates between z and z + depth. */
    private static void pleats(PoseStack.Pose pose, VertexConsumer vc, float x0, float x1, int n, float y0, float y1, float z, float depth, int color,
                               int light) {
        float w = (x1 - x0) / n;
        for (int i = 0; i < n; i++) {
            float xa = (x0 + i * w) / 16F, xb = (x0 + (i + 1) * w) / 16F;
            float za = (z + (i % 2 == 0 ? 0 : depth)) / 16F, zb = (z + (i % 2 == 0 ? depth : 0)) / 16F;
            float ua = (x0 + i * w) / 16F, ub = (x0 + (i + 1) * w) / 16F, v0 = 1F - y1 / 16F, v1 = 1F - y0 / 16F;
            float dx = xb - xa, dz = zb - za, len = (float) Math.sqrt(dx * dx + dz * dz);
            float nx = -dz / len, nz = dx / len;
            // front (towards the room, -z) and back
            quad(pose, vc, xa, y1 / 16F, za, xa, y0 / 16F, za, xb, y0 / 16F, zb, xb, y1 / 16F, zb, ua, v0, ub, v1, -nx, -nz, color, light);
            quad(pose, vc, xb, y1 / 16F, zb, xb, y0 / 16F, zb, xa, y0 / 16F, za, xa, y1 / 16F, za, ub, v0, ua, v1, nx, nz, color, light);
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vc, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3,
                             float z3, float x4, float y4, float z4, float u0, float v0, float u1, float v1, float nx, float nz, int color, int light) {
        v(pose, vc, x1, y1, z1, u0, v0, nx, nz, color, light);
        v(pose, vc, x2, y2, z2, u0, v1, nx, nz, color, light);
        v(pose, vc, x3, y3, z3, u1, v1, nx, nz, color, light);
        v(pose, vc, x4, y4, z4, u1, v0, nx, nz, color, light);
    }

    private static void v(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z, float u, float vv, float nx, float nz, int color, int light) {
        vc.addVertex(pose, x, y, z).setColor(color).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, 0F, nz);
    }

    static int darker(int c) {
        int r = ((c >> 16) & 255) * 7 / 10, g = ((c >> 8) & 255) * 7 / 10, b = (c & 255) * 7 / 10;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
