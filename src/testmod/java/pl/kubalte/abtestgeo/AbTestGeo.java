package pl.kubalte.abtestgeo;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** CI-only test mod: one simple GeckoLib creature, used to check Animal Bonds' GeckoLib integration. Not shipped. */
@Mod(AbTestGeo.MODID)
public class AbTestGeo {
    public static final String MODID = "abtestgeo";
    static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Cubeling>> CUBELING =
            ENTITIES.registerEntityType("cubeling", Cubeling::new, MobCategory.CREATURE, b -> b.sized(0.8F, 0.9F));

    public AbTestGeo(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> e.put(CUBELING.get(),
                PathfinderMob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.MAX_HEALTH, 10).build()));
        if (FMLEnvironment.getDist() == Dist.CLIENT) Client.init(modBus);
    }

    static final class Client {
        static void init(IEventBus modBus) {
            modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
                    e.registerEntityRenderer(CUBELING.get(), ctx -> new GeoEntityRenderer<>(ctx, CUBELING.get())));
        }
    }

    public static class Cubeling extends PathfinderMob implements GeoEntity {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        public Cubeling(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0));
            goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }
}
