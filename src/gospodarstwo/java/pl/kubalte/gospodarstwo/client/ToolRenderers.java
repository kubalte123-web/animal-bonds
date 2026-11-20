package pl.kubalte.gospodarstwo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import pl.kubalte.gospodarstwo.tools.FishingNetBlock;
import pl.kubalte.gospodarstwo.tools.FishingNetBlockEntity;
import pl.kubalte.gospodarstwo.tools.IncubatorBlockEntity;

/** Eggs in the incubator (they wobble before hatching) and the catch caught in the fishing net. */
public final class ToolRenderers {
    private ToolRenderers() {}

    public static class EggState extends BlockEntityRenderState {
        int color = -1, count;
        float fraction, time;
    }

    public static class Incubator implements BlockEntityRenderer<IncubatorBlockEntity, EggState> {
        public Incubator(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public EggState createRenderState() {
            return new EggState();
        }

        @Override
        public void extractRenderState(IncubatorBlockEntity be, EggState s, float pt, Vec3 cam, ModelFeatureRenderer.CrumblingOverlay c) {
            BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, c);
            s.color = be.eggColor();
            s.count = be.eggs().getCount();
            s.fraction = be.fraction();
            s.time = be.getLevel() == null ? 0F : (be.getLevel().getGameTime() % 1000) + pt;
        }

        @Override
        public void submit(EggState s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
            if (s.color < 0) return;
            int light = s.lightCoords;
            int c = 0xFF000000 | s.color;
            float[][] spots = {{5.5F, 5.5F}, {10.5F, 6F}, {6F, 10.5F}, {10.5F, 10.5F}};
            for (int i = 0; i < Math.min(4, s.count); i++) {
                float x = spots[i][0], z = spots[i][1];
                // eggs close to hatching wobble
                float wob = s.fraction > 0.8F ? (float) Math.sin(s.time * 0.6F + i * 1.7F) * 12F * (s.fraction - 0.8F) * 5F : 0F;
                ps.pushPose();
                ps.translate(x / 16F, 4.5F / 16F, z / 16F);
                ps.mulPose(Axis.ZP.rotationDegrees(wob));
                ps.translate(-x / 16F, -4.5F / 16F, -z / 16F);
                col.submitCustomGeometry(ps, RenderTypes.entityCutout(KitchenRenderers.WHITE), (pose, vc) -> {
                    Boxes.box(pose, vc, x - 1.2F, 4.5F, z - 1.2F, x + 1.2F, 6.5F, z + 1.2F, c, light);
                    Boxes.box(pose, vc, x - 0.9F, 6.5F, z - 0.9F, x + 0.9F, 7.4F, z + 0.9F, c, light);
                    Boxes.box(pose, vc, x - 0.5F, 7.4F, z - 0.5F, x + 0.5F, 7.8F, z + 0.5F, c, light);
                });
                ps.popPose();
            }
        }
    }

    public static class NetState extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        final List<Integer> colors = new ArrayList<>();
        float time;
    }

    static int colorOf(ItemStack s) {
        if (s.is(Items.COD)) return 0xB89A70;
        if (s.is(Items.SALMON)) return 0xB8483C;
        if (s.is(Items.PUFFERFISH)) return 0xE8C040;
        if (s.is(Items.TROPICAL_FISH)) return 0xF08030;
        if (s.is(Items.KELP)) return 0x4E7A2C;
        if (s.is(Items.INK_SAC)) return 0x302838;
        return 0xE8D8D0;
    }

    public static class Net implements BlockEntityRenderer<FishingNetBlockEntity, NetState> {
        public Net(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public NetState createRenderState() {
            return new NetState();
        }

        @Override
        public void extractRenderState(FishingNetBlockEntity be, NetState s, float pt, Vec3 cam, ModelFeatureRenderer.CrumblingOverlay c) {
            BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, c);
            s.facing = be.getBlockState().hasProperty(FishingNetBlock.FACING) ? be.getBlockState().getValue(FishingNetBlock.FACING) : Direction.NORTH;
            s.colors.clear();
            for (ItemStack i : be.catches()) s.colors.add(colorOf(i));
            s.time = be.getLevel() == null ? 0F : (be.getLevel().getGameTime() % 2000) + pt;
        }

        @Override
        public void submit(NetState s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
            if (s.colors.isEmpty()) return;
            int light = s.lightCoords;
            ps.pushPose();
            KitchenRenderers.turn(ps, s.facing);
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(KitchenRenderers.WHITE), (pose, vc) -> {
                for (int i = 0; i < s.colors.size(); i++) {
                    int c = 0xFF000000 | s.colors.get(i);
                    float x = 3F + (i % 3) * 4.5F, y = 3.5F + (i / 3) * 5F + (float) Math.sin(s.time * 0.08F + i) * 0.3F;
                    // a fish caught in the mesh: body and tail
                    Boxes.box(pose, vc, x - 1.6F, y - 0.7F, 7.3F, x + 1.2F, y + 0.7F, 8.7F, c, light);
                    Boxes.box(pose, vc, x + 1.2F, y - 1F, 7.7F, x + 2F, y + 1F, 8.3F, c, light);
                }
            });
            ps.popPose();
        }
    }
}
