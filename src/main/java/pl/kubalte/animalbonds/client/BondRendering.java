package pl.kubalte.animalbonds.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import pl.kubalte.animalbonds.AnimalBonds;
import pl.kubalte.animalbonds.bond.BondData;
import pl.kubalte.animalbonds.bond.BondLogic;

/** Client visuals: fainted creatures lie on their side, creatures with saddlebags carry two chests. */
public final class BondRendering {
    private BondRendering() {}

    private static final ContextKey<Boolean> FAINTED = new ContextKey<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "fainted"));
    private static final ContextKey<float[]> BAGS = new ContextKey<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "bags"));
    private static final Identifier BAG_TEXTURE = Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "textures/entity/saddlebag.png");

    public static void register(IEventBus modBus) {
        modBus.addListener(BondRendering::registerModifiers);
        NeoForge.EVENT_BUS.addListener(BondRendering::pre);
        NeoForge.EVENT_BUS.addListener(BondRendering::post);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void registerModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier((Class) LivingEntityRenderer.class, (java.util.function.BiConsumer<LivingEntity, LivingEntityRenderState>) (entity, state) -> {
            if (!(entity instanceof Mob mob)) return;
            BondData d = BondLogic.get(mob);
            if (d == null) return;
            if (d.isFainted()) state.setRenderData(FAINTED, Boolean.TRUE);
            if (d.bags) state.setRenderData(BAGS, new float[] {mob.getBbWidth(), mob.getBbHeight(), mob.yBodyRot});
        });
    }

    static void pre(RenderLivingEvent.Pre<?, ?, ?> e) {
        if (Boolean.TRUE.equals(e.getRenderState().getRenderData(FAINTED))) {
            PoseStack ps = e.getPoseStack();
            ps.pushPose();
            ps.translate(0.0F, 0.15F, 0.0F);
            ps.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }

    static void post(RenderLivingEvent.Post<?, ?, ?> e) {
        LivingEntityRenderState state = e.getRenderState();
        boolean fainted = Boolean.TRUE.equals(state.getRenderData(FAINTED));
        float[] bags = state.getRenderData(BAGS);
        if (bags != null) renderBags(e, bags, state.lightCoords);
        if (fainted) e.getPoseStack().popPose();
    }

    private static void renderBags(RenderLivingEvent.Post<?, ?, ?> e, float[] dims, int light) {
        float w = dims[0], h = dims[1], bodyRot = dims[2];
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        float y = Math.max(0.3F, h * 0.68F);
        float side = w / 2.0F + 0.12F;
        float s = Math.max(0.22F, Math.min(0.45F, w * 0.4F));
        e.getSubmitNodeCollector().submitCustomGeometry(ps, RenderTypes.entityCutout(BAG_TEXTURE), (pose, vc) -> {
            box(pose, vc, side, y - s / 2, -s / 2, side + s * 0.6F, y + s / 2, s / 2, light);
            box(pose, vc, -side - s * 0.6F, y - s / 2, -s / 2, -side, y + s / 2, s / 2, light);
        });
        ps.popPose();
    }

    private static void box(PoseStack.Pose pose, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
        // six faces, full texture on each
        quad(pose, vc, light, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0); // north
        quad(pose, vc, light, 0, 0, 1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);  // south
        quad(pose, vc, light, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0); // west
        quad(pose, vc, light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);  // east
        quad(pose, vc, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);  // up
        quad(pose, vc, light, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1); // down
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vc, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vertex(pose, vc, light, nx, ny, nz, ax, ay, az, 0, 1);
        vertex(pose, vc, light, nx, ny, nz, bx, by, bz, 0, 0);
        vertex(pose, vc, light, nx, ny, nz, cx, cy, cz, 1, 0);
        vertex(pose, vc, light, nx, ny, nz, dx, dy, dz, 1, 1);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, int light, float nx, float ny, float nz, float x, float y, float z, float u, float v) {
        vc.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
