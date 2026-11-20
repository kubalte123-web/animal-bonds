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
import pl.kubalte.gospodarstwo.block.StationBlock;
import pl.kubalte.gospodarstwo.block.StationBlockEntity;
import pl.kubalte.gospodarstwo.kitchen.ChurnBlockEntity;
import pl.kubalte.gospodarstwo.kitchen.CrockBlockEntity;
import pl.kubalte.gospodarstwo.kitchen.PressBlockEntity;
import pl.kubalte.gospodarstwo.kitchen.QuernBlockEntity;
import pl.kubalte.gospodarstwo.kitchen.SmokehouseBlockEntity;

/** Moving parts of the kitchen stations. */
public final class KitchenRenderers {
    private KitchenRenderers() {}

    static final Identifier STONE = Identifier.withDefaultNamespace("textures/block/stone.png");
    static final Identifier COBBLE = Identifier.withDefaultNamespace("textures/block/cobblestone.png");
    static final Identifier LOG = Identifier.withDefaultNamespace("textures/block/stripped_oak_log.png");
    static final Identifier PLANKS = Identifier.withDefaultNamespace("textures/block/spruce_planks.png");
    static final Identifier IRON = Identifier.withDefaultNamespace("textures/block/anvil.png");
    static final Identifier WHITE = Identifier.withDefaultNamespace("textures/block/white_concrete_powder.png");

    static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    public static class StationState extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        boolean working;
        float anim, fraction;
        int color = -1, color2 = -1, count, count2;
    }

    static void facing(StationBlockEntity be, StationState s, float partialTick) {
        s.facing = be.getBlockState().hasProperty(StationBlock.FACING) ? be.getBlockState().getValue(StationBlock.FACING) : Direction.NORTH;
        s.working = be.working();
        s.anim = be.animTicks + (be.working() ? partialTick : 0F);
        s.count = be.input().getCount();
        s.count2 = be.output().getCount();
    }

    static void turn(PoseStack ps, Direction d) {
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(-LoomRenderer.angle(d)));
        ps.translate(-0.5F, 0F, -0.5F);
    }

    // ------------------------------------------------------------------ quern
    public static class Quern implements BlockEntityRenderer<QuernBlockEntity, StationState> {
        public Quern(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public StationState createRenderState() {
            return new StationState();
        }

        @Override
        public void extractRenderState(QuernBlockEntity be, StationState s, float pt, Vec3 cam, ModelFeatureRenderer.CrumblingOverlay c) {
            BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, c);
            facing(be, s, pt);
        }

        @Override
        public void submit(StationState s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
            int light = s.lightCoords;
            ps.pushPose();
            turn(ps, s.facing);
            // flour heap under the spout
            if (s.count2 > 0) {
                float h = Math.min(1.6F, 0.4F + s.count2 * 0.05F);
                col.submitCustomGeometry(ps, RenderTypes.entityCutout(WHITE), (pose, vc) -> {
                    Boxes.box(pose, vc, 5.5F, 3F, 14F, 10.5F, 3F + h * 0.6F, 15.8F, 0xFFF4EEE0, light);
                    Boxes.box(pose, vc, 6.5F, 3F, 13.6F, 9.5F, 3F + h, 15.4F, 0xFFF8F4EA, light);
                });
            }
            // the turning upper stone with its handle
            ps.pushPose();
            ps.translate(0.5F, 0F, 0.5F);
            ps.mulPose(Axis.YP.rotationDegrees(-s.anim * 6F));
            ps.translate(-0.5F, 0F, -0.5F);
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(STONE), (pose, vc) -> {
                Boxes.box(pose, vc, 2.5F, 5F, 2.5F, 13.5F, 8.5F, 13.5F, 0xFFE0E0E0, light);
                Boxes.box(pose, vc, 3.5F, 5F, 1.8F, 12.5F, 8.5F, 2.5F, 0xFFD8D8D8, light);
                Boxes.box(pose, vc, 3.5F, 5F, 13.5F, 12.5F, 8.5F, 14.2F, 0xFFD8D8D8, light);
                Boxes.box(pose, vc, 1.8F, 5F, 3.5F, 2.5F, 8.5F, 12.5F, 0xFFD8D8D8, light);
                Boxes.box(pose, vc, 13.5F, 5F, 3.5F, 14.2F, 8.5F, 12.5F, 0xFFD8D8D8, light);
                // feed hole, dark (or showing the grain)
                Boxes.box(pose, vc, 7F, 8.5F, 7F, 9F, 8.55F, 9F, s.count > 0 ? 0xFFD8B860 : 0xFF303030, light);
            });
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(LOG), (pose, vc) -> {
                Boxes.box(pose, vc, 10.5F, 8.5F, 7.5F, 11.5F, 13.5F, 8.5F, 0xFFFFFFFF, light);
                Boxes.box(pose, vc, 10.3F, 13.5F, 7.3F, 11.7F, 14.2F, 8.7F, 0xFFFFFFFF, light);
            });
            ps.popPose();
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ fruit press
    public static class Press implements BlockEntityRenderer<PressBlockEntity, StationState> {
        public Press(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public StationState createRenderState() {
            return new StationState();
        }

        @Override
        public void extractRenderState(PressBlockEntity be, StationState s, float pt, Vec3 cam, ModelFeatureRenderer.CrumblingOverlay c) {
            BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, c);
            facing(be, s, pt);
            s.color = be.mashColor();
            s.color2 = be.juiceColor();
        }

        @Override
        public void submit(StationState s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
            int light = s.lightCoords;
            ps.pushPose();
            turn(ps, s.facing);
            float mash = s.color >= 0 ? 3.5F + Math.min(4F, s.count * 0.12F) : 3.2F;
            float squeeze = s.working ? (float) Math.abs(Math.sin(s.anim * 0.06F)) * 1.2F : 0F;
            float plate = Math.max(mash, mash + 1.2F - squeeze);
            if (s.color >= 0) col.submitCustomGeometry(ps, RenderTypes.entityTranslucent(DyeVatRenderer.LIQUID),
                    (pose, vc) -> Boxes.box(pose, vc, 4F, 3F, 4F, 12F, mash, 12F, lerp(0xFF000000 | s.color, 0xFF5A3A20, 0.25F), light));
            if (s.color2 >= 0) {
                float lvl = 2.2F + Math.min(0.8F, s.count2 * 0.1F);
                col.submitCustomGeometry(ps, RenderTypes.entityTranslucent(DyeVatRenderer.LIQUID),
                        (pose, vc) -> Boxes.box(pose, vc, 2F, lvl - 0.01F, 2F, 14F, lvl, 14F, 0xFF000000 | s.color2, light));
            }
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(PLANKS), (pose, vc) -> Boxes.box(pose, vc, 3.6F, plate, 3.6F, 12.4F, plate + 1F, 12.4F,
                    0xFFFFFFFF, light));
            // the screw from the plate up through the beam, and the turning bar on top
            ps.pushPose();
            ps.translate(0.5F, 0F, 0.5F);
            ps.mulPose(Axis.YP.rotationDegrees(-squeeze * 120F));
            ps.translate(-0.5F, 0F, -0.5F);
            float top = 17F + (plate - mash);
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(IRON), (pose, vc) -> Boxes.box(pose, vc, 7.4F, plate + 1F, 7.4F, 8.6F, top, 8.6F,
                    0xFFFFFFFF, light));
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(LOG), (pose, vc) -> Boxes.box(pose, vc, 3F, top, 7.5F, 13F, top + 1F, 8.5F,
                    0xFFFFFFFF, light));
            ps.popPose();
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ crock
    public static class Crock implements BlockEntityRenderer<CrockBlockEntity, StationState> {
        public Crock(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public StationState createRenderState() {
            return new StationState();
        }

        @Override
        public void extractRenderState(CrockBlockEntity be, StationState s, float pt, Vec3 cam, ModelFeatureRenderer.CrumblingOverlay c) {
            BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, c);
            facing(be, s, pt);
            s.color = be.contentColor();
            s.fraction = be.fraction();
        }

        @Override
        public void submit(StationState s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
            int light = s.lightCoords;
            if (s.color >= 0) {
                // brine around the lid, cloudier as it ferments
                int c = lerp(0xFF000000 | s.color, 0xFFC8B880, s.count > 0 ? s.fraction * 0.4F : 0.4F);
                col.submitCustomGeometry(ps, RenderTypes.entityTranslucent(DyeVatRenderer.LIQUID), (pose, vc) -> {
                    Boxes.box(pose, vc, 3.2F, 11.1F, 3.2F, 12.8F, 11.2F, 12.8F, c, light);
                    if (s.working) for (int i = 0; i < 3; i++) {
                        float t = (s.anim * 0.02F + i * 0.33F) % 1F;
                        float bx = i == 0 ? 3.6F : i == 1 ? 11.6F : 7.6F, bz = i == 2 ? 3.5F : 7F + i;
                        float r = 0.2F + 0.3F * t;
                        Boxes.box(pose, vc, bx - r, 11.2F, bz - r, bx + r, 11.2F + r, bz + r, 0xFFF0F0E0, light);
                    }
                });
            }
            // a stone weighs the lid down while it ferments
            if (s.count > 0) col.submitCustomGeometry(ps, RenderTypes.entityCutout(COBBLE), (pose, vc) -> {
                Boxes.box(pose, vc, 5.5F, 12.6F, 6F, 10.5F, 14.2F, 10F, 0xFFFFFFFF, light);
                Boxes.box(pose, vc, 6.5F, 14.2F, 6.8F, 9.5F, 14.8F, 9.2F, 0xFFFFFFFF, light);
            });
        }
    }

    // ------------------------------------------------------------------ smokehouse
    public static class Smokehouse implements BlockEntityRenderer<SmokehouseBlockEntity, StationState> {
        public Smokehouse(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public StationState createRenderState() {
            return new StationState();
        }

        @Override
        public void extractRenderState(SmokehouseBlockEntity be, StationState s, float pt, Vec3 cam, ModelFeatureRenderer.CrumblingOverlay c) {
            BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, c);
            facing(be, s, pt);
            s.color = be.hangingColor();
            s.count = be.hangingCount();
            s.fraction = be.fraction();
        }

        @Override
        public void submit(StationState s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
            if (s.color < 0 || s.count <= 0) return;
            int light = s.lightCoords;
            int c = lerp(0xFF000000 | s.color, 0xFF7A3E1E, s.fraction);
            ps.pushPose();
            turn(ps, s.facing);
            int n = s.count;
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(WHITE), (pose, vc) -> {
                for (int i = 0; i < n; i++) {
                    float x = n == 1 ? 8F : 3F + i * (10F / (n - 1));
                    float sway = s.working ? (float) Math.sin(s.anim * 0.05F + i) * 0.15F : 0F;
                    Boxes.box(pose, vc, x - 0.1F + sway, 9.5F, 7.9F, x + 0.1F + sway, 12.2F, 8.1F, 0xFF8A7A60, light);
                    float w = 1.2F + (i % 2) * 0.4F;
                    Boxes.box(pose, vc, x - w / 2 + sway, 4.5F + (i % 3) * 0.5F, 7.2F, x + w / 2 + sway, 9.5F, 8.8F, c, light);
                }
            });
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ butter churn
    public static class ChurnState extends BlockEntityRenderState {
        float plunger, progress;
        int butter;
        boolean milk;
    }

    public static class Churn implements BlockEntityRenderer<ChurnBlockEntity, ChurnState> {
        public Churn(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public ChurnState createRenderState() {
            return new ChurnState();
        }

        @Override
        public void extractRenderState(ChurnBlockEntity be, ChurnState s, float pt, Vec3 cam, ModelFeatureRenderer.CrumblingOverlay c) {
            BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, c);
            float t = be.getLevel() == null ? 99F : be.getLevel().getGameTime() - be.lastStroke + pt;
            // a stroke: down for 4 ticks, back up for 6
            s.plunger = t < 4F ? t / 4F : t < 10F ? 1F - (t - 4F) / 6F : 0F;
            s.butter = be.butter();
            s.milk = be.milk();
            s.progress = be.progress();
        }

        @Override
        public void submit(ChurnState s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
            int light = s.lightCoords;
            float y = 14F - s.plunger * 3F;
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(LOG), (pose, vc) -> {
                Boxes.box(pose, vc, 7.5F, y, 7.5F, 8.5F, y + 7F, 8.5F, 0xFFFFFFFF, light);
                Boxes.box(pose, vc, 6.5F, y + 7F, 7.5F, 9.5F, y + 7.8F, 8.5F, 0xFFFFFFFF, light);
            });
            if (s.butter > 0) col.submitCustomGeometry(ps, RenderTypes.entityCutout(WHITE), (pose, vc) -> {
                Boxes.box(pose, vc, 5F, 14F, 9F, 8F, 15.6F, 11.5F, 0xFFF2D46A, light);
                Boxes.box(pose, vc, 8.6F, 14F, 4.6F, 11F, 15.2F, 7F, 0xFFEAC85A, light);
            });
            // a few drops of cream around the dasher while churning
            if (s.milk && s.plunger > 0F) col.submitCustomGeometry(ps, RenderTypes.entityCutout(WHITE), (pose, vc) -> {
                int cream = lerp(0xFFF8F6EE, 0xFFF4E2A0, s.progress);
                Boxes.box(pose, vc, 6.9F, 14F, 6.9F, 9.1F, 14.25F, 9.1F, cream, light);
            });
        }
    }
}
