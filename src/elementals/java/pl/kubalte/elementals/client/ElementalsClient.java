package pl.kubalte.elementals.client;

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
import pl.kubalte.elementals.Elementals;

@Mod(value = Elementals.MODID, dist = Dist.CLIENT)
public class ElementalsClient {
    static final String[] CREATURES = {"mossling", "puffowl", "emberlizard", "frostling"};

    /** GeckoLib renderers when GeckoLib is there (can be turned off with -Delementals.forceFallback=true for tests). */
    public static boolean useGecko() {
        return ModList.get().isLoaded("geckolib") && !Boolean.getBoolean("elementals.forceFallback");
    }

    public ElementalsClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(ElementalsClient::layers);
        modBus.addListener(ElementalsClient::renderers);
        ElementVisuals.register(modBus);
        ElementalsClientTest.register(modBus);
        Elementals.LOGGER.info("Żywiołaki: creatures drawn with {}", useGecko() ? "GeckoLib" : "the built-in renderer");
    }

    static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        for (String c : CREATURES) e.registerLayerDefinition(CreatureRenderer.layer(c), () -> GeoJsonModel.load(c).layer());
    }

    static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        if (useGecko()) {
            pl.kubalte.elementals.client.compat.GeckoCreatures.register(e);
            return;
        }
        fallback(e, Elementals.MOSSLING.get(), "mossling", 0.5F);
        fallback(e, Elementals.PUFFOWL.get(), "puffowl", 0.35F);
        fallback(e, Elementals.EMBERLIZARD.get(), "emberlizard", 0.5F);
        fallback(e, Elementals.FROSTLING.get(), "frostling", 0.3F);
    }

    private static <E extends Mob> void fallback(EntityRenderersEvent.RegisterRenderers e, EntityType<E> type, String name, float shadow) {
        e.registerEntityRenderer(type, ctx -> new CreatureRenderer<E>(ctx, name, shadow));
    }
}
