package pl.kubalte.gospodarstwo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.breed.Breed;
import pl.kubalte.gospodarstwo.breed.BreedEvents;

/**
 * How breeds look: the ordinary animal's model gets a colour wash and (for woolly breeds) a shaggy coat - every part of
 * the model drawn again a bit bigger with a see-through fur texture. Works on top of other mods' textures.
 */
public final class BreedVisuals {
    private BreedVisuals() {}

    public static final ContextKey<Breed> BREED = new ContextKey<>(Gospodarstwo.id("breed"));
    static final Identifier WHITE = Gospodarstwo.id("textures/misc/white.png");
    static final Identifier FUR = Gospodarstwo.id("textures/misc/fur.png");

    public static void register(IEventBus modBus) {
        modBus.addListener(BreedVisuals::registerModifiers);
        NeoForge.EVENT_BUS.addListener(BreedVisuals::post);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void registerModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier((Class) net.minecraft.client.renderer.entity.EntityRenderer.class,
                (java.util.function.BiConsumer<Entity, EntityRenderState>) (entity, state) -> {
                    Breed b = BreedEvents.breed(entity);
                    if (b != null) state.setRenderData(BREED, b);
                });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void post(RenderLivingEvent.Post<?, ?, ?> e) {
        LivingEntityRenderState state = e.getRenderState();
        Breed b = state.getRenderData(BREED);
        if (b == null || state.isInvisible) return;
        EntityModel model = e.getRenderer().getModel();
        int light = state.lightCoords;
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.scale(state.scale, state.scale, state.scale);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.translate(0.0F, -1.501F, 0.0F);
        int wash = (b.washAlpha << 24) | (b.wash & 0xFFFFFF);
        e.getSubmitNodeCollector().submitCustomGeometry(ps, RenderTypes.entityTranslucent(WHITE), (pose, vc) -> {
            model.setupAnim(state);
            PoseStack m = new PoseStack();
            m.last().pose().set(pose.pose());
            m.last().normal().set(pose.normal());
            model.root().visit(m, (p, path, idx, cube) -> cube.compile(p, vc, light, OverlayTexture.NO_OVERLAY, wash));
        });
        if (b.furScale > 0F) {
            int fur = 0xFF000000 | (b.furColor & 0xFFFFFF);
            float k = b.furScale;
            e.getSubmitNodeCollector().submitCustomGeometry(ps, RenderTypes.entityCutout(FUR), (pose, vc) -> {
                model.setupAnim(state);
                PoseStack m = new PoseStack();
                m.last().pose().set(pose.pose());
                m.last().normal().set(pose.normal());
                model.root().visit(m, (p, path, idx, cube) -> {
                    float sx = cube.maxX - cube.minX, sy = cube.maxY - cube.minY, sz = cube.maxZ - cube.minZ;
                    if (Math.min(sx, Math.min(sy, sz)) < 1.5F) return; // eyes, beaks, horns stay bare
                    PoseStack c = new PoseStack();
                    c.last().pose().set(p.pose());
                    c.last().normal().set(p.normal());
                    float cx = (cube.minX + cube.maxX) / 32F, cy = (cube.minY + cube.maxY) / 32F, cz = (cube.minZ + cube.maxZ) / 32F;
                    // grow the coat by a fixed thickness, so small parts get relatively more fluff than the body
                    float t = (k - 1F) * 4F;
                    c.translate(cx, cy, cz);
                    c.scale((sx + t) / sx, (sy + t * 0.7F) / sy, (sz + t) / sz);
                    c.translate(-cx, -cy, -cz);
                    cube.compile(c.last(), vc, light, OverlayTexture.NO_OVERLAY, fur);
                });
            });
        }
        ps.popPose();
    }
}
