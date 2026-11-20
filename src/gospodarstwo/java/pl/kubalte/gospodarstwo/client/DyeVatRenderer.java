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
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.block.DyeVatBlockEntity;

/** The liquid in the dye vat: clear water or the colour of the dye bath, a bit lower as the dye is used. */
public class DyeVatRenderer implements BlockEntityRenderer<DyeVatBlockEntity, DyeVatRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        boolean water;
        int color = -1, charges;
    }

    static final net.minecraft.resources.Identifier LIQUID = Gospodarstwo.id("textures/block/dye_liquid.png");

    public DyeVatRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DyeVatBlockEntity be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, crumbling);
        s.water = be.hasWater();
        s.color = be.color();
        s.charges = be.charges();
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!s.water) return;
        int c = 0xFF000000 | (s.color >= 0 ? s.color : 0x3F76E4);
        float y = s.color >= 0 ? 8.5F + 2F * s.charges / DyeVatBlockEntity.MAX_CHARGES : 9.5F;
        int light = s.lightCoords;
        collector.submitCustomGeometry(ps, RenderTypes.entityTranslucent(LIQUID), (pose, vc) -> Boxes.box(pose, vc, 2F, y - 0.01F, 2F, 14F, y, 14F, c, light));
    }
}
