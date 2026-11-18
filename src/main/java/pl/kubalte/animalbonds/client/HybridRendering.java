package pl.kubalte.animalbonds.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import pl.kubalte.animalbonds.AnimalBonds;
import pl.kubalte.animalbonds.bond.HybridData;

/**
 * Draws hybrids: the body parent's model with some groups of parts (head, legs, tail/wings) replaced by the
 * donor parent's parts, re-fitted to the body (moved and scaled into the place of the original parts).
 * The vanilla model of a hybrid is hidden (render state set invisible) and this composite is drawn instead.
 * Each part keeps its own animation: the donor's parts are animated with the hybrid's walk/head movement.
 */
public final class HybridRendering {
    private HybridRendering() {}

    public static final ContextKey<HybridData> HYBRID = new ContextKey<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "hybrid"));

    static final int BODY = 0, HEAD = 1, LEGS = 2, EXTRAS = 3;

    /** Which group a model part belongs to, from its path of part names. */
    static int group(String path) {
        String[] segs = path.toLowerCase(java.util.Locale.ROOT).split("/");
        for (String s : segs) {
            if (s.contains("head") || s.equals("beak") || s.equals("red_thing") || s.contains("jaw") || s.contains("nose")
                    || s.contains("snout") || s.contains("mouth") || s.equals("bill") || s.startsWith("ear") || s.contains("horn")
                    || s.equals("hat") || s.contains("mane")) return HEAD;
        }
        for (String s : segs) if (s.contains("leg") || s.contains("foot")) return LEGS;
        for (String s : segs) if (s.contains("tail") || s.contains("wing") || s.contains("fin")) return EXTRAS;
        return BODY;
    }

    /** Rest-pose bounding boxes of the 4 groups in model space (null when the model has no such parts). */
    record Layout(float[][] boxes) {
        boolean has(int g) {
            return boxes[g] != null;
        }
    }

    private static final Map<Object, Layout> LAYOUTS = new HashMap<>();

    @SuppressWarnings({"rawtypes"})
    static Layout layout(Object key, EntityModel model) {
        Layout cached = LAYOUTS.get(key);
        if (cached != null) return cached;
        List<ModelPart> parts = model.allParts();
        List<PartPose> saved = new java.util.ArrayList<>();
        for (ModelPart p : parts) saved.add(p.storePose());
        model.resetPose();
        float[][] boxes = new float[4][];
        model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
            int g = group(path);
            float[] b = boxes[g];
            if (b == null) b = boxes[g] = new float[] {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
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
        Layout l = new Layout(boxes);
        LAYOUTS.put(key, l);
        return l;
    }

    // ---------------- donor (other parent) model ----------------

    private record Donor(Entity dummy, LivingEntityRenderer renderer, LivingEntityRenderState state) {}

    private static final Map<String, Optional<Donor>> DONORS = new HashMap<>();

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Donor donor(String typeId) {
        return DONORS.computeIfAbsent(typeId, id -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id));
                if (type.isEmpty() || mc.level == null) return Optional.empty();
                Entity e = type.get().create(mc.level, EntitySpawnReason.LOAD);
                if (!(e instanceof LivingEntity)) return Optional.empty();
                e.setId(fakeId());
                EntityRenderer r = mc.getEntityRenderDispatcher().getRenderer(e);
                if (!(r instanceof LivingEntityRenderer lr)) return Optional.empty();
                LivingEntityRenderState st = (LivingEntityRenderState) lr.createRenderState(e, 0F);
                return Optional.of(new Donor(e, lr, st));
            } catch (Exception ex) {
                AnimalBonds.LOGGER.warn("Hybrid donor model for {} unavailable: {}", id, ex.toString());
                return Optional.empty();
            }
        }).orElse(null);
    }

    private static int nextFakeId = -100_000;

    /** Client-only helper creatures (donor models, previews) need an id; negative ids never clash with real ones. */
    public static int fakeId() {
        return nextFakeId--;
    }

    /** Clears cached models (on resource reload / world change). */
    public static void clear() {
        DONORS.clear();
        LAYOUTS.clear();
    }

    // ---------------- drawing ----------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void render(RenderLivingEvent.Post<?, ?, ?> e) {
        LivingEntityRenderState state = e.getRenderState();
        HybridData h = state.getRenderData(HYBRID);
        if (h == null) return;
        LivingEntityRenderer baseRenderer = e.getRenderer();
        EntityModel baseModel = baseRenderer.getModel();
        Layout base = layout(baseRenderer.getClass(), baseModel);

        Donor donor = donor(h.donorType());
        Layout don = donor == null ? null : layout(donor.renderer().getClass(), donor.renderer().getModel());

        boolean swapHead = h.head() && don != null && don.has(HEAD);
        boolean swapLegs = h.legs() && don != null && don.has(LEGS);
        boolean swapExtras = h.extras() && don != null && don.has(EXTRAS);

        // textures: each creature's own unique colouring; the colour option decides whose palette is used
        Long seed = state.getRenderData(VariantTextures.SEED);
        String bodyType = state.getRenderData(VariantTextures.TYPE);
        Boolean bodyNatural = state.getRenderData(VariantTextures.NATURAL);
        long s = seed == null ? 0L : seed;
        boolean donorNatural = donor != null && donor.dummy() instanceof net.minecraft.world.entity.animal.Animal;
        String bodyStyle = h.colors() == 1 ? h.donorType() : bodyType;
        boolean bodyStyleNatural = h.colors() == 1 ? donorNatural : bodyNatural != null && bodyNatural;
        String donorStyle = h.colors() == 0 ? bodyType : h.donorType();
        boolean donorStyleNatural = h.colors() == 0 ? bodyNatural != null && bodyNatural : donorNatural;
        Identifier baseTex = baseRenderer.getTextureLocation(state);
        if (bodyStyle != null && seed != null) baseTex = VariantTextures.resolve(baseTex, s, bodyStyle, bodyStyleNatural);

        int light = state.lightCoords;
        int overlay = OverlayTexture.NO_OVERLAY;
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        // same transform the living-entity renderer applies to its model
        ps.scale(state.scale, state.scale, state.scale);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.translate(0.0F, -1.501F, 0.0F);

        final boolean hHead = swapHead, hLegs = swapLegs, hExtras = swapExtras;
        e.getSubmitNodeCollector().submitCustomGeometry(ps, RenderTypes.entityCutout(baseTex), (pose, vc) -> {
            baseModel.setupAnim(state);
            PoseStack m = stackOf(pose);
            baseModel.root().visit(m, (p, path, idx, cube) -> {
                int g = group(path);
                if ((g == HEAD && hHead) || (g == LEGS && hLegs) || (g == EXTRAS && hExtras)) return;
                cube.compile(p, vc, light, overlay, -1);
            });
        });

        if (donor != null && (swapHead || swapLegs || swapExtras)) {
            LivingEntityRenderState ds = donor.state();
            ds.walkAnimationPos = state.walkAnimationPos;
            ds.walkAnimationSpeed = state.walkAnimationSpeed;
            ds.ageInTicks = state.ageInTicks;
            ds.yRot = state.yRot;
            ds.xRot = state.xRot;
            Identifier donorTex = donor.renderer().getTextureLocation(ds);
            if (donorStyle != null && seed != null) donorTex = VariantTextures.resolve(donorTex, s ^ 0x5DEECE66DL, donorStyle, donorStyleNatural);
            EntityModel dm = donor.renderer().getModel();
            float[][] B = base.boxes(), D = don.boxes();
            e.getSubmitNodeCollector().submitCustomGeometry(ps, RenderTypes.entityCutout(donorTex), (pose, vc) -> {
                dm.setupAnim(ds);
                if (hHead) drawGroup(dm, pose, vc, light, overlay, HEAD, D[HEAD], headTarget(B), true);
                if (hLegs) {
                    float[] target = legsTarget(B);
                    if (target != null) drawGroup(dm, pose, vc, light, overlay, LEGS, D[LEGS], target, false);
                }
                if (hExtras && D[BODY] != null && B[BODY] != null) {
                    // tails and wings keep their place relative to the body
                    drawGroupMapped(dm, pose, vc, light, overlay, EXTRAS, D[BODY], B[BODY], false);
                }
            });
        }
        ps.popPose();
    }

    private static PoseStack stackOf(PoseStack.Pose pose) {
        PoseStack m = new PoseStack();
        m.last().pose().set(pose.pose());
        m.last().normal().set(pose.normal());
        return m;
    }

    /** Where the head goes: the body parent's head, or (no head) the front-top of its body. */
    private static float[] headTarget(float[][] b) {
        if (b[HEAD] != null) return b[HEAD];
        float[] body = b[BODY];
        float s = Math.max(0.25F, (body[4] - body[1]) * 0.6F);
        float cx = (body[0] + body[3]) / 2;
        return new float[] {cx - s / 2, body[1] - s * 0.6F, body[2] - s * 0.8F, cx + s / 2, body[1] + s * 0.4F, body[2] + s * 0.2F};
    }

    /** Where the legs go: the body parent's legs, or (no legs) the space between the body and the ground. */
    private static float[] legsTarget(float[][] b) {
        if (b[LEGS] != null) return b[LEGS];
        float[] body = b[BODY];
        if (body == null || body[4] > 1.45F) return null;
        return new float[] {body[0] + 0.05F, body[4], body[2] + 0.05F, body[3] - 0.05F, 1.5F, body[5] - 0.05F};
    }

    private static void drawGroup(EntityModel<?> dm, PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vc, int light, int overlay,
                                  int group, float[] from, float[] to, boolean uniform) {
        drawGroupMapped(dm, pose, vc, light, overlay, group, from, to, uniform);
    }

    /** Draws the donor's parts of one group, mapping box {@code from} (donor model space) onto box {@code to} (body model space). */
    private static void drawGroupMapped(EntityModel<?> dm, PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vc, int light, int overlay,
                                        int group, float[] from, float[] to, boolean uniform) {
        if (from == null || to == null) return;
        float fx = Math.max(0.01F, from[3] - from[0]), fy = Math.max(0.01F, from[4] - from[1]), fz = Math.max(0.01F, from[5] - from[2]);
        float tx = Math.max(0.01F, to[3] - to[0]), ty = Math.max(0.01F, to[4] - to[1]), tz = Math.max(0.01F, to[5] - to[2]);
        float kx = tx / fx, ky = ty / fy, kz = tz / fz;
        if (uniform) {
            float k = Math.max(0.5F, Math.min(1.8F, Math.max(tx, Math.max(ty, tz)) / Math.max(fx, Math.max(fy, fz))));
            kx = ky = kz = k;
        } else {
            kx = Math.max(0.3F, Math.min(3F, kx));
            ky = Math.max(0.2F, Math.min(4F, ky));
            kz = Math.max(0.3F, Math.min(3F, kz));
        }
        PoseStack m = stackOf(pose);
        // bottom-centre of "from" lands on bottom-centre of "to" (model space: y grows downwards)
        m.translate((to[0] + to[3]) / 2, uniform ? (to[1] + to[4]) / 2 : to[4], (to[2] + to[5]) / 2);
        m.scale(kx, ky, kz);
        m.translate(-(from[0] + from[3]) / 2, uniform ? -(from[1] + from[4]) / 2 : -from[4], -(from[2] + from[5]) / 2);
        dm.root().visit(m, (p, path, idx, cube) -> {
            if (group(path) == group) cube.compile(p, vc, light, overlay, -1);
        });
    }
}
