package pl.kubalte.elementals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import pl.kubalte.elementals.Element;
import pl.kubalte.elementals.Elementals;
import pl.kubalte.elementals.ElementalsConfig;

/**
 * How elements look on every creature: a colour wash over the model, glowing speckles in the element's colour and
 * particles around it (flames, snowflakes, smoke, sparks, dragon motes). The new creatures have their own element
 * textures and glow masks instead of the wash.
 */
public final class ElementVisuals {
    private ElementVisuals() {}

    public static final ContextKey<Element> ELEMENT = new ContextKey<>(Identifier.fromNamespaceAndPath(Elementals.MODID, "element"));
    /** Set for the mod's own creatures (their name), which use element textures instead of the wash. */
    public static final ContextKey<String> OWN = new ContextKey<>(Identifier.fromNamespaceAndPath(Elementals.MODID, "own"));

    static final Identifier WHITE = Identifier.fromNamespaceAndPath(Elementals.MODID, "textures/misc/white.png");
    static final Identifier SPECKLES = Identifier.fromNamespaceAndPath(Elementals.MODID, "textures/misc/speckles.png");

    public static void register(IEventBus modBus) {
        modBus.addListener(ElementVisuals::registerModifiers);
        NeoForge.EVENT_BUS.addListener(ElementVisuals::post);
        NeoForge.EVENT_BUS.addListener(ElementVisuals::tick);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void registerModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier((Class) net.minecraft.client.renderer.entity.EntityRenderer.class,
                (java.util.function.BiConsumer<Entity, EntityRenderState>) (entity, state) -> {
                    if (!(entity instanceof LivingEntity)) return;
                    Element el = Elementals.element(entity);
                    if (el != Element.NONE) state.setRenderData(ELEMENT, el);
                });
    }

    static int argb(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void post(RenderLivingEvent.Post<?, ?, ?> e) {
        LivingEntityRenderState state = e.getRenderState();
        Element el = state.getRenderData(ELEMENT);
        if (el == null || el == Element.NONE || state.isInvisible) return;
        String own = state.getRenderData(OWN);
        EntityModel model = e.getRenderer().getModel();
        int light = state.lightCoords;
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        // same transform the living-entity renderer applies to its model
        ps.scale(state.scale, state.scale, state.scale);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.translate(0.0F, -1.501F, 0.0F);
        if (own != null) {
            submit(e, ps, model, state, RenderTypes.eyes(CreatureRenderer.glowmask(own, el)), light, 0xFFFFFFFF);
        } else {
            submit(e, ps, model, state, RenderTypes.entityTranslucent(WHITE), light, argb(el == Element.DARK ? 0x70 : 0x55, el.color));
            submit(e, ps, model, state, RenderTypes.eyes(SPECKLES), light, argb(0xFF, el.color));
        }
        ps.popPose();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void submit(RenderLivingEvent.Post<?, ?, ?> e, PoseStack ps, EntityModel model, LivingEntityRenderState state, RenderType type,
                               int light, int color) {
        e.getSubmitNodeCollector().submitCustomGeometry(ps, type, (pose, vc) -> {
            model.setupAnim(state);
            PoseStack m = new PoseStack();
            m.last().pose().set(pose.pose());
            m.last().normal().set(pose.normal());
            model.root().visit(m, (p, path, idx, cube) -> cube.compile(p, vc, light, OverlayTexture.NO_OVERLAY, color));
        });
    }

    // ---------------- particles ----------------

    static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || !ElementalsConfig.PARTICLES.get()) return;
        RandomSource r = mc.level.getRandom();
        for (Entity ent : mc.level.entitiesForRendering()) {
            if (!(ent instanceof Mob mob) || mob.isInvisible() || mob.distanceToSqr(mc.player) > 48 * 48) continue;
            Element el = Elementals.element(mob);
            if (el == Element.NONE || r.nextFloat() > 0.2F) continue;
            double x = mob.getRandomX(0.6), y = mob.getRandomY(), z = mob.getRandomZ(0.6);
            ParticleOptions p = switch (el) {
                case FIRE -> r.nextInt(3) == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME;
                case ICE -> ParticleTypes.SNOWFLAKE;
                case DARK -> ParticleTypes.REVERSE_PORTAL;
                case ELECTRIC -> ParticleTypes.ELECTRIC_SPARK;
                case DRAGON -> ParticleTypes.END_ROD;
                default -> null;
            };
            if (p == null) continue;
            double vy = el == Element.ICE ? -0.02 : 0.01;
            mc.level.addParticle(p, x, y, z, 0, vy, 0);
        }
    }
}
