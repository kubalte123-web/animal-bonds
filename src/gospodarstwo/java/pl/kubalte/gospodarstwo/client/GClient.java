package pl.kubalte.gospodarstwo.client;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import pl.kubalte.gospodarstwo.Gospodarstwo;

@Mod(value = Gospodarstwo.MODID, dist = Dist.CLIENT)
public class GClient {
    static final String[] ANIMALS = {"duck", "goose", "turkey", "snail"};

    public static boolean useGecko() {
        return ModList.get().isLoaded("geckolib") && !Boolean.getBoolean("gospodarstwo.forceFallback");
    }

    public GClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(GClient::layers);
        modBus.addListener(GClient::renderers);
        BreedVisuals.register(modBus);
        GClientTest.register(modBus);
    }

    static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        for (String c : ANIMALS) e.registerLayerDefinition(CreatureRenderer.layer(c), () -> GeoJsonModel.load(c).layer());
    }

    static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(Gospodarstwo.BONSAI_BE.get(), BonsaiRenderer::new);
        e.registerBlockEntityRenderer(Gospodarstwo.LOOM_BE.get(), LoomRenderer::new);
        e.registerBlockEntityRenderer(Gospodarstwo.SPINNING_WHEEL_BE.get(), SpinningWheelRenderer::new);
        e.registerBlockEntityRenderer(Gospodarstwo.DYE_VAT_BE.get(), DyeVatRenderer::new);
        if (useGecko()) {
            pl.kubalte.gospodarstwo.client.compat.GeckoCreatures.register(e);
            return;
        }
        fallback(e, Gospodarstwo.DUCK.get(), "duck", 0.3F);
        fallback(e, Gospodarstwo.GOOSE.get(), "goose", 0.4F);
        fallback(e, Gospodarstwo.TURKEY.get(), "turkey", 0.45F);
        fallback(e, Gospodarstwo.SNAIL.get(), "snail", 0.3F);
    }

    private static <E extends Mob> void fallback(EntityRenderersEvent.RegisterRenderers e, EntityType<E> type, String name, float shadow) {
        e.registerEntityRenderer(type, ctx -> new CreatureRenderer<E>(ctx, name, shadow));
    }
}
