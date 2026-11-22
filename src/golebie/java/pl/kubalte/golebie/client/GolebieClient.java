package pl.kubalte.golebie.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import pl.kubalte.golebie.Golebie;

@Mod(value = Golebie.MODID, dist = Dist.CLIENT)
public class GolebieClient {
    public GolebieClient(IEventBus modBus, ModContainer container) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(Golebie.PIGEON.get(), ctx -> new BirdRenderer(ctx, "pigeon", 0.34F));
            e.registerEntityRenderer(Golebie.FANTAIL.get(), ctx -> new BirdRenderer(ctx, "fantail", 0.33F));
        });
        PClientTest.register(modBus);
    }
}
