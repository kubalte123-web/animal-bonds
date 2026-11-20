package pl.kubalte.gospodarstwo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import pl.kubalte.gospodarstwo.block.KettleBlockEntity;

/** The brew in the kettle, with bubbles rising and bursting while it cooks. */
public class KettleRenderer implements BlockEntityRenderer<KettleBlockEntity, KettleRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        int color = -1, amount;
        boolean working;
        float anim;
    }

    public KettleRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KettleBlockEntity be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, crumbling);
        s.color = be.liquidColor();
        s.amount = be.input().getCount() + be.output().getCount();
        s.working = be.working();
        s.anim = be.animTicks + (be.working() ? partialTick : 0F);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (s.color < 0) return;
        int light = s.lightCoords;
        int c = 0xFF000000 | s.color;
        float y = 5.5F + Math.min(3.5F, s.amount * 0.25F);
        collector.submitCustomGeometry(ps, RenderTypes.entityTranslucent(DyeVatRenderer.LIQUID), (pose, vc) -> {
            Boxes.box(pose, vc, 4F, y - 0.01F, 4F, 12F, y, 12F, c, light);
            if (!s.working) return;
            int bub = 0xFF000000 | brighter(s.color);
            for (int i = 0; i < 5; i++) {
                float t = (s.anim * 0.07F + i * 0.37F) % 1F;
                float bx = 5F + (i * 37 % 6), bz = 5F + (i * 53 % 6), r = 0.3F + 0.5F * t;
                Boxes.box(pose, vc, bx - r, y - 0.2F + t * 0.4F, bz - r, bx + r, y + r * 0.8F + t * 0.4F, bz + r, bub, light);
            }
        });
    }

    private static int brighter(int c) {
        int r = Math.min(255, ((c >> 16) & 255) + 40), g = Math.min(255, ((c >> 8) & 255) + 40), b = Math.min(255, (c & 255) + 40);
        return r << 16 | g << 8 | b;
    }
}
