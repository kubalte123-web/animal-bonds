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

    public static final ContextKey<Boolean> FAINTED = new ContextKey<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "fainted"));
    public static final ContextKey<float[]> BAGS = new ContextKey<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "bags"));

    public static void register(IEventBus modBus) {
        modBus.addListener(BondRendering::registerModifiers);
        NeoForge.EVENT_BUS.addListener(BondRendering::pre);
        NeoForge.EVENT_BUS.addListener(BondRendering::post);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void registerModifiers(RegisterRenderStateModifiersEvent event) {
        // EntityRenderer (not only LivingEntityRenderer): also covers GeckoLib renderers
        event.registerEntityModifier((Class) net.minecraft.client.renderer.entity.EntityRenderer.class, (java.util.function.BiConsumer<net.minecraft.world.entity.Entity, net.minecraft.client.renderer.entity.state.EntityRenderState>) (entity, state) -> {
            if (!(entity instanceof Mob mob)) return;
            String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
            if (VariantTextures.applies(type)) {
                java.util.UUID u = mob.getUUID();
                state.setRenderData(VariantTextures.SEED, u.getMostSignificantBits() * 31 + u.getLeastSignificantBits());
                state.setRenderData(VariantTextures.NATURAL, mob instanceof net.minecraft.world.entity.animal.Animal);
                state.setRenderData(VariantTextures.TYPE, type);
            }
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

    public static final Identifier CHEST_TEXTURE = Identifier.withDefaultNamespace("textures/entity/chest/normal.png");
    /** Per creature type (and baby/adult): body box from the model's rest pose, in blocks: minX,minY,minZ,maxX,maxY,maxZ. */
    private static final java.util.Map<String, float[]> BODY_CACHE = new java.util.HashMap<>();
    private static final String[] BODY_PARTS = {"body", "body1", "torso", "upper_body", "cube", "main"};

    /** Finds the creature's body in its model and measures it, so the chests sit on its flanks. */
    @SuppressWarnings({"rawtypes"})
    private static float[] bodyBox(RenderLivingEvent.Post<?, ?, ?> e, String key) {
        float[] cached = BODY_CACHE.get(key);
        if (cached != null) return cached;
        float[] box = null;
        try {
            net.minecraft.client.model.EntityModel model = e.getRenderer().getModel();
            net.minecraft.client.model.geom.ModelPart root = model.root();
            String bodyName = null;
            for (String n : BODY_PARTS) {
                if (root.hasChild(n)) {
                    bodyName = n;
                    break;
                }
            }
            if (bodyName != null) {
                java.util.List<net.minecraft.client.model.geom.ModelPart> parts = model.allParts();
                java.util.List<net.minecraft.client.model.geom.PartPose> saved = new java.util.ArrayList<>();
                for (var part : parts) saved.add(part.storePose());
                model.resetPose();
                PoseStack ps = new PoseStack();
                ps.scale(-1.0F, -1.0F, 1.0F);
                ps.translate(0.0F, -1.501F, 0.0F);
                float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
                final String target = bodyName;
                root.visit(ps, (pose, path, index, cube) -> {
                    if (!(path.equals(target) || path.endsWith("/" + target))) return;
                    for (int c = 0; c < 8; c++) {
                        org.joml.Vector3f v = new org.joml.Vector3f(
                                ((c & 1) == 0 ? cube.minX : cube.maxX) / 16f,
                                ((c & 2) == 0 ? cube.minY : cube.maxY) / 16f,
                                ((c & 4) == 0 ? cube.minZ : cube.maxZ) / 16f);
                        pose.pose().transformPosition(v);
                        b[0] = Math.min(b[0], v.x); b[1] = Math.min(b[1], v.y); b[2] = Math.min(b[2], v.z);
                        b[3] = Math.max(b[3], v.x); b[4] = Math.max(b[4], v.y); b[5] = Math.max(b[5], v.z);
                    }
                });
                for (int i = 0; i < parts.size(); i++) parts.get(i).loadPose(saved.get(i));
                if (b[0] < b[3] && b[3] - b[0] < 8) box = b;
            }
        } catch (Exception ex) {
            AnimalBonds.LOGGER.debug("No model body for {}: {}", key, ex.toString());
        }
        BODY_CACHE.put(key, box == null ? new float[0] : box);
        return box == null ? new float[0] : box;
    }

    private static void renderBags(RenderLivingEvent.Post<?, ?, ?> e, float[] dims, int light) {
        LivingEntityRenderState state = e.getRenderState();
        float w = dims[0], h = dims[1], bodyRot = dims[2];
        float scale = state.scale <= 0 ? 1F : state.scale;
        String key = e.getRenderer().getClass().getName() + (state.isBaby ? "_baby" : "");
        float[] body = bodyBox(e, key);

        float minX, maxX, minY, maxY, minZ, maxZ;
        if (body.length == 6) {
            minX = body[0] * scale; maxX = body[3] * scale;
            minY = body[1] * scale; maxY = body[4] * scale;
            minZ = body[2] * scale; maxZ = body[5] * scale;
        } else { // no recognisable body part: use the hit box
            minX = -w / 2; maxX = w / 2; minY = h * 0.35F; maxY = h * 0.85F; minZ = -w / 2; maxZ = w / 2;
        }
        float bodyH = maxY - minY, bodyL = maxZ - minZ;
        // llama-like chest: 8 x 8 x 3 pixels relative to a body of llama size, fitted to this body
        float ch = Math.max(0.2F, Math.min(0.7F, Math.min(bodyH * 0.8F, bodyL * 0.6F)));
        float cl = ch;
        float ct = ch * 0.4F;
        float cy = (minY + maxY) / 2F - ch * 0.1F;
        float cz = (minZ + maxZ) / 2F;

        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        final float y0 = cy - ch / 2, y1 = cy + ch / 2, z0 = cz - cl / 2, z1 = cz + cl / 2;
        final float rx = maxX, lx = minX;
        e.getSubmitNodeCollector().submitCustomGeometry(ps, RenderTypes.entityCutout(CHEST_TEXTURE), (pose, vc) -> {
            chest(pose, vc, light, rx, rx + ct, y0, y1, z0, z1, true);
            chest(pose, vc, light, lx - ct, lx, y0, y1, z0, z1, false);
        });
        ps.popPose();
    }

    /** A small vanilla-looking chest lying against the flank: base + lid + latch, using the chest texture. */
    public static void chest(PoseStack.Pose pose, VertexConsumer vc, int light, float x0, float x1, float y0, float y1, float z0, float z1, boolean right) {
        float split = y0 + (y1 - y0) * (10F / 15F);
        // base: texOffs(0,19) 14x10x14 ; lid: texOffs(0,0) 14x5x14 (64x64 texture)
        texturedBox(pose, vc, light, x0, y0, z0, x1, split, z1, 0, 19, 14, 10, 14, right);
        texturedBox(pose, vc, light, x0, split, z0, x1, y1, z1, 0, 0, 14, 5, 14, right);
        // latch on the outer side, at the lid's lower edge: texOffs(0,0) 2x4x1
        float lw = (z1 - z0) * (2F / 14F), lh = (y1 - y0) * (4F / 15F), lt = (x1 - x0) * 0.12F;
        float zc = (z0 + z1) / 2;
        float lx0 = right ? x1 : x0 - lt, lx1 = right ? x1 + lt : x0;
        texturedBox(pose, vc, light, lx0, split - lh / 2, zc - lw / 2, lx1, split + lh / 2, zc + lw / 2, 0, 0, 2, 4, 1, right);
    }

    /**
     * Box with Minecraft's standard cube UV unwrap (texture 64x64). The "front" texture goes on the outward
     * X face, width runs along Z, depth along X.
     */
    private static void texturedBox(PoseStack.Pose pose, VertexConsumer vc, int light, float x0, float y0, float z0, float x1, float y1, float z1,
                                    int u, int v, int tw, int th, int td, boolean right) {
        float t = 64F;
        float[] top = {u + td, v, u + td + tw, v + td};
        float[] bottom = {u + td + tw, v, u + td + tw + tw, v + td};
        float[] front = {u + td, v + td, u + td + tw, v + td + th};
        float[] back = {u + td + tw + td, v + td, u + td + tw + td + tw, v + td + th};
        float[] sideA = {u, v + td, u + td, v + td + th};
        float[] sideB = {u + td + tw, v + td, u + td + tw + td, v + td + th};
        float ox = right ? x1 : x0, ix = right ? x0 : x1, onx = right ? 1 : -1;
        // outward face (front)
        face(pose, vc, light, onx, 0, 0, ox, y0, right ? z0 : z1, ox, y1, right ? z0 : z1, ox, y1, right ? z1 : z0, ox, y0, right ? z1 : z0, front, t);
        // inward face (back)
        face(pose, vc, light, -onx, 0, 0, ix, y0, right ? z1 : z0, ix, y1, right ? z1 : z0, ix, y1, right ? z0 : z1, ix, y0, right ? z0 : z1, back, t);
        // ends
        face(pose, vc, light, 0, 0, -1, x1, y0, z0, x1, y1, z0, x0, y1, z0, x0, y0, z0, sideA, t);
        face(pose, vc, light, 0, 0, 1, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, sideB, t);
        // top / bottom
        face(pose, vc, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, top, t);
        face(pose, vc, light, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, bottom, t);
    }

    /** Quad a-b-c-d where a=bottom-left, b=top-left, c=top-right, d=bottom-right of the uv rect. */
    private static void face(PoseStack.Pose pose, VertexConsumer vc, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float[] uv, float t) {
        vertex(pose, vc, light, nx, ny, nz, ax, ay, az, uv[0] / t, uv[3] / t);
        vertex(pose, vc, light, nx, ny, nz, bx, by, bz, uv[0] / t, uv[1] / t);
        vertex(pose, vc, light, nx, ny, nz, cx, cy, cz, uv[2] / t, uv[1] / t);
        vertex(pose, vc, light, nx, ny, nz, dx, dy, dz, uv[2] / t, uv[3] / t);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, int light, float nx, float ny, float nz, float x, float y, float z, float u, float v) {
        vc.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
