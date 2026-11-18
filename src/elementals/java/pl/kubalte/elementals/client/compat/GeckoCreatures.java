package pl.kubalte.elementals.client.compat;

import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import pl.kubalte.elementals.Element;
import pl.kubalte.elementals.Elementals;
import pl.kubalte.elementals.client.CreatureRenderer;

/**
 * With GeckoLib installed the creatures are drawn by GeckoLib from the same geo.json, with keyframed idle/walk
 * animations and the element glow mask. The entities themselves stay ordinary vanilla mobs.
 */
public final class GeckoCreatures {
    private GeckoCreatures() {}

    static final DataTicket<Integer> ELEMENT = DataTicket.create("elementals_element", Integer.class);

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        reg(e, Elementals.MOSSLING.get(), "mossling", 0.5F);
        reg(e, Elementals.PUFFOWL.get(), "puffowl", 0.35F);
        reg(e, Elementals.EMBERLIZARD.get(), "emberlizard", 0.5F);
        reg(e, Elementals.FROSTLING.get(), "frostling", 0.3F);
    }

    private static <E extends Mob> void reg(EntityRenderersEvent.RegisterRenderers e, EntityType<E> type, String name, float shadow) {
        Animatable anim = new Animatable();
        e.registerEntityRenderer(type, ctx -> new Renderer<E>(ctx, name, anim, shadow));
    }

    /** One shared animatable per creature type (GeckoLib keeps the per-entity animation state itself). */
    public static final class Animatable implements GeoReplacedEntity {
        static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
        static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<Animatable>("main", 4, test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }

    static final class Model extends GeoModel<Animatable> {
        private final String name;
        private final Identifier geo;

        Model(String name) {
            this.name = name;
            this.geo = Identifier.fromNamespaceAndPath(Elementals.MODID, "entity/" + name);
        }

        @Override
        public Identifier getModelResource(GeoRenderState renderState) {
            return geo;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState renderState) {
            Integer el = renderState.getOrDefaultGeckolibData(ELEMENT, 0);
            return CreatureRenderer.texture(name, Element.byOrdinal(el == null ? 0 : el));
        }

        @Override
        public Identifier getAnimationResource(Animatable animatable) {
            return geo;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static final class Renderer<E extends Mob> extends GeoReplacedEntityRenderer<Animatable, E, LivingEntityRenderState> {
        Renderer(EntityRendererProvider.Context ctx, String name, Animatable anim, float shadow) {
            super(ctx, new Model(name), anim);
            this.shadowRadius = shadow;
            withRenderLayer((com.geckolib.renderer.layer.GeoRenderLayer) new AutoGlowingGeoLayer(this));
        }

        @Override
        public void addRenderData(Animatable animatable, E entity, LivingEntityRenderState renderState, float partialTick) {
            ((GeoRenderState) (Object) renderState).addGeckolibData(ELEMENT, entity == null ? 0 : Elementals.element(entity).ordinal());
            if (renderState.isBaby) renderState.scale *= 0.55F; // babies are small versions of the same model
        }
    }
}
