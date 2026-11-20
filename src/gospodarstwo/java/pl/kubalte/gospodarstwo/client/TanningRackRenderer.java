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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.block.StationBlock;
import pl.kubalte.gospodarstwo.block.TanningRackBlockEntity;

/** The hide laced into the rack; its colour slowly turns from raw hide to leather. */
public class TanningRackRenderer implements BlockEntityRenderer<TanningRackBlockEntity, TanningRackRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        boolean hide, thick;
        float tan;
    }

    static final Identifier HIDE = Gospodarstwo.id("textures/block/stretched_hide.png");
    static final Identifier THONG = Identifier.withDefaultNamespace("textures/block/stripped_dark_oak_log.png");

    public TanningRackRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TanningRackBlockEntity be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, crumbling);
        s.facing = be.getBlockState().hasProperty(StationBlock.FACING) ? be.getBlockState().getValue(StationBlock.FACING) : Direction.NORTH;
        ItemStack in = be.input(), out = be.output();
        s.hide = !in.isEmpty() || !out.isEmpty();
        s.thick = in.is(Gospodarstwo.THICK_HIDE.get()) || out.is(Gospodarstwo.THICK_LEATHER.get());
        s.tan = in.isEmpty() ? 1F : be.tanFraction();
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!s.hide) return;
        int light = s.lightCoords;
        int color = s.thick ? lerp(0x7A6048, 0x55361F, s.tan) : lerp(0xD8B092, 0x9A6236, s.tan);
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(-LoomRenderer.angle(s.facing)));
        ps.translate(-0.5F, 0F, -0.5F);
        collector.submitCustomGeometry(ps, RenderTypes.entityCutout(HIDE), (pose, vc) -> Boxes.panel(pose, vc, 2.6F, 3.4F, 13.4F, 13.6F, 8F, color, light));
        int thong = 0xFF6B4A2E;
        collector.submitCustomGeometry(ps, RenderTypes.entityCutout(THONG), (pose, vc) -> {
            for (float y : new float[] {5F, 7.5F, 10F, 12.5F}) {
                Boxes.box(pose, vc, 1.9F, y, 7.8F, 3.6F, y + 0.4F, 8.2F, thong, light);
                Boxes.box(pose, vc, 12.4F, y, 7.8F, 14.1F, y + 0.4F, 8.2F, thong, light);
            }
            for (float x : new float[] {4.5F, 7F, 9F, 11.5F}) {
                Boxes.box(pose, vc, x, 12.6F, 7.8F, x + 0.4F, 14.1F, 8.2F, thong, light);
                Boxes.box(pose, vc, x, 2.9F, 7.8F, x + 0.4F, 4.4F, 8.2F, thong, light);
            }
        });
        ps.popPose();
    }
}
