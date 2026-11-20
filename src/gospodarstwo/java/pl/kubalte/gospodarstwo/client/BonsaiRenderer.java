package pl.kubalte.gospodarstwo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import pl.kubalte.gospodarstwo.block.BonsaiData;
import pl.kubalte.gospodarstwo.block.BonsaiPotBlockEntity;

/**
 * Draws the tree in a bonsai pot: a sapling, then a little trunk with bends and branches ending in leaf pads. The shape
 * comes from the tree's own seed; trimmed trees have neat flat pads, untrimmed ones shaggy clumps.
 */
public class BonsaiRenderer implements BlockEntityRenderer<BonsaiPotBlockEntity, BonsaiRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        BonsaiData data = BonsaiData.EMPTY;
    }

    record Look(Identifier log, Identifier leaves, int tint, Identifier sapling) {}

    private static Identifier mc(String tex) {
        return Identifier.withDefaultNamespace("textures/block/" + tex + ".png");
    }

    private static Look vanilla(String wood, int tint) {
        return new Look(mc(wood + "_log"), mc(wood + "_leaves"), tint, mc(wood + "_sapling"));
    }

    private static final Map<String, Look> LOOKS = Map.ofEntries(
            Map.entry("minecraft:oak_sapling", vanilla("oak", 0x59AE30)),
            Map.entry("minecraft:spruce_sapling", vanilla("spruce", 0x619961)),
            Map.entry("minecraft:birch_sapling", vanilla("birch", 0x80A755)),
            Map.entry("minecraft:jungle_sapling", vanilla("jungle", 0x30BB0B)),
            Map.entry("minecraft:acacia_sapling", vanilla("acacia", 0xAEA42A)),
            Map.entry("minecraft:dark_oak_sapling", vanilla("dark_oak", 0x59AE30)),
            Map.entry("minecraft:cherry_sapling", vanilla("cherry", 0xFFFFFF)),
            Map.entry("minecraft:pale_oak_sapling", vanilla("pale_oak", 0xFFFFFF)),
            Map.entry("minecraft:mangrove_propagule", new Look(mc("mangrove_log"), mc("mangrove_leaves"), 0x8DB127, mc("mangrove_propagule"))),
            Map.entry("minecraft:azalea", new Look(mc("oak_log"), mc("azalea_leaves"), 0xFFFFFF, mc("azalea_leaves"))),
            Map.entry("minecraft:flowering_azalea", new Look(mc("oak_log"), mc("flowering_azalea_leaves"), 0xFFFFFF, mc("flowering_azalea_leaves"))),
            Map.entry("gospodarstwo:apple_sapling", new Look(mc("oak_log"), Gospodarstwo.id("textures/block/apple_leaves_fruit.png"), 0xFFFFFF,
                    Gospodarstwo.id("textures/block/apple_sapling.png"))),
            Map.entry("gospodarstwo:plum_sapling", new Look(mc("dark_oak_log"), Gospodarstwo.id("textures/block/plum_leaves_fruit.png"), 0xFFFFFF,
                    Gospodarstwo.id("textures/block/plum_sapling.png"))));

    static Look look(String sapling) {
        return LOOKS.getOrDefault(sapling, LOOKS.get("minecraft:oak_sapling"));
    }

    public BonsaiRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BonsaiPotBlockEntity be, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, crumbling);
        state.data = be.data();
    }

    /** One piece of the tree: a box in pixels, either wood or leaves. */
    record Piece(boolean leaves, float x0, float y0, float z0, float x1, float y1, float z1) {}

    /** The tree's shape for its seed, growth and trimming. */
    static List<Piece> shape(BonsaiData d) {
        List<Piece> out = new ArrayList<>();
        float g = Math.min(1F, d.growth() / (float) BonsaiPotBlockEntity.FULL);
        boolean neat = d.trims() >= BonsaiPotBlockEntity.TRIMS_NEEDED;
        Random r = new Random(d.seed());
        float soil = 3.5F;
        float thick = 1.0F + 1.8F * g;
        int segments = 3;
        float segH = (1.8F + 2.4F * g);
        float x = 8F + (r.nextFloat() - 0.5F) * 2F, z = 8F + (r.nextFloat() - 0.5F) * 1.5F, y = soil;
        List<float[]> tips = new ArrayList<>();
        for (int s = 0; s < segments; s++) {
            float nx = x + (r.nextFloat() - 0.5F) * 3.2F * g, nz = z + (r.nextFloat() - 0.5F) * 2.4F * g;
            float t = thick * (1F - s * 0.22F);
            float cx = (x + nx) / 2, cz = (z + nz) / 2;
            out.add(new Piece(false, cx - t / 2, y, cz - t / 2, cx + t / 2, y + segH + 0.3F, cz + t / 2));
            x = nx;
            z = nz;
            y += segH;
            if (s >= 1 && g > 0.25F) {
                // a branch to one side
                float dir = r.nextBoolean() ? 1 : -1;
                float len = 1.6F + 2.8F * g;
                float bx = x + dir * len, bz = z + (r.nextFloat() - 0.5F) * 2F;
                float bt = Math.max(0.8F, t * 0.55F);
                float by = y - segH * 0.4F;
                out.add(new Piece(false, Math.min(x, bx), by, Math.min(z, bz) - bt / 2, Math.max(x, bx), by + bt, Math.max(z, bz) + bt / 2));
                tips.add(new float[] {bx, by + bt, bz});
            }
        }
        tips.add(new float[] {x, y, z});
        for (float[] tip : tips) {
            float w = 2.5F + 4F * g;
            if (neat) {
                float h = 1.2F + 1.3F * g;
                out.add(new Piece(true, tip[0] - w / 2, tip[1] - 0.3F, tip[2] - w / 2.4F, tip[0] + w / 2, tip[1] + h, tip[2] + w / 2.4F));
            } else {
                int blobs = 2 + r.nextInt(3);
                for (int k = 0; k < blobs; k++) {
                    float bw = w * (0.5F + r.nextFloat() * 0.6F), bh = bw * (0.6F + r.nextFloat() * 0.4F);
                    float ox = (r.nextFloat() - 0.5F) * w * 0.7F, oy = (r.nextFloat() - 0.3F) * w * 0.4F, oz = (r.nextFloat() - 0.5F) * w * 0.6F;
                    out.add(new Piece(true, tip[0] + ox - bw / 2, tip[1] + oy - bh / 3, tip[2] + oz - bw / 2.4F, tip[0] + ox + bw / 2,
                            tip[1] + oy + bh * 2 / 3, tip[2] + oz + bw / 2.4F));
                }
            }
        }
        return out;
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        BonsaiData d = state.data;
        int stage = BonsaiPotBlockEntity.stage(d);
        if (stage < 0) return;
        Look look = look(d.sapling());
        int light = state.lightCoords;
        if (stage == 0) {
            float size = 5F + 3F * d.growth() / 120F;
            collector.submitCustomGeometry(ps, RenderTypes.entityCutout(look.sapling()),
                    (pose, vc) -> Boxes.cross(pose, vc, 8F, 3.5F, 8F, size, 0xFFFFFFFF, light));
            return;
        }
        List<Piece> pieces = shape(d);
        collector.submitCustomGeometry(ps, RenderTypes.entityCutout(look.log()), (pose, vc) -> {
            for (Piece p : pieces) if (!p.leaves()) Boxes.box(pose, vc, p.x0(), p.y0(), p.z0(), p.x1(), p.y1(), p.z1(), 0xFFFFFFFF, light);
        });
        int leafColor = 0xFF000000 | look.tint();
        collector.submitCustomGeometry(ps, RenderTypes.entityCutout(look.leaves()), (pose, vc) -> {
            for (Piece p : pieces) if (p.leaves()) Boxes.box(pose, vc, p.x0(), p.y0(), p.z0(), p.x1(), p.y1(), p.z1(), leafColor, light);
        });
    }
}
