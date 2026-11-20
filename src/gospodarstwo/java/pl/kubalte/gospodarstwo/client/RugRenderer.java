package pl.kubalte.gospodarstwo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.block.RugBlockEntity;

/** The woven rug: a patterned (grey) texture tinted with the rug's colour; the border pattern joins with neighbouring rugs. */
public class RugRenderer implements BlockEntityRenderer<RugBlockEntity, RugRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        int color;
        boolean n, s, e, w;
    }

    static final Identifier[] TEX = new Identifier[16];
    static {
        for (int i = 0; i < 16; i++) TEX[i] = Gospodarstwo.id("textures/block/rug/rug_" + i + ".png");
    }

    public RugRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RugBlockEntity be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, crumbling);
        s.color = 0xFF000000 | be.shownColor();
        if (be.getLevel() != null) {
            var p = be.getBlockPos();
            s.n = be.getLevel().getBlockState(p.north()).is(Gospodarstwo.WOOL_RUG.get());
            s.s = be.getLevel().getBlockState(p.south()).is(Gospodarstwo.WOOL_RUG.get());
            s.e = be.getLevel().getBlockState(p.east()).is(Gospodarstwo.WOOL_RUG.get());
            s.w = be.getLevel().getBlockState(p.west()).is(Gospodarstwo.WOOL_RUG.get());
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        // bit set = that side continues into another rug (no border there)
        int mask = (s.n ? 1 : 0) | (s.s ? 2 : 0) | (s.w ? 4 : 0) | (s.e ? 8 : 0);
        int light = s.lightCoords;
        collector.submitCustomGeometry(ps, RenderTypes.entityCutout(TEX[mask]), (pose, vc) -> Boxes.box(pose, vc, 0F, 0F, 0F, 16F, 1F, 16F, s.color, light));
    }
}
