package pl.kubalte.gospodarstwo.client.compat;

import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.client.CreatureRenderer;

/** With GeckoLib the animals are drawn by GeckoLib from the same geo.json, with keyframed idle/walk animations. */
public final class GeckoCreatures {
    private GeckoCreatures() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        reg(e, Gospodarstwo.DUCK.get(), "duck", 0.3F);
        reg(e, Gospodarstwo.GOOSE.get(), "goose", 0.4F);
        reg(e, Gospodarstwo.TURKEY.get(), "turkey", 0.45F);
        reg(e, Gospodarstwo.SNAIL.get(), "snail", 0.3F);
    }

    private static <E extends Mob> void reg(EntityRenderersEvent.RegisterRenderers e, EntityType<E> type, String name, float shadow) {
        Animatable anim = new Animatable();
        e.registerEntityRenderer(type, ctx -> new Renderer<E>(ctx, name, anim, shadow));
    }

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
        private final Identifier geo, texture;

        Model(String name) {
            this.geo = Gospodarstwo.id("entity/" + name);
            this.texture = CreatureRenderer.texture(name);
        }

        @Override
        public Identifier getModelResource(GeoRenderState renderState) {
            return geo;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState renderState) {
            return texture;
        }

        @Override
        public Identifier getAnimationResource(Animatable animatable) {
            return geo;
        }
    }

    static final class Renderer<E extends Mob> extends GeoReplacedEntityRenderer<Animatable, E, LivingEntityRenderState> {
        Renderer(EntityRendererProvider.Context ctx, String name, Animatable anim, float shadow) {
            super(ctx, new Model(name), anim);
            this.shadowRadius = shadow;
        }

        @Override
        public void addRenderData(Animatable animatable, E entity, LivingEntityRenderState renderState, float partialTick) {
            if (renderState.isBaby) renderState.scale *= 0.55F;
        }
    }
}
