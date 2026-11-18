package pl.kubalte.animalbonds.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import pl.kubalte.animalbonds.AnimalBonds;
import pl.kubalte.animalbonds.bond.BondData;
import pl.kubalte.animalbonds.bond.BondLogic;
import pl.kubalte.animalbonds.network.OpenBagsPayload;
import pl.kubalte.animalbonds.network.OpenBookPayload;

@Mod(value = AnimalBonds.MODID, dist = Dist.CLIENT)
public class AnimalBondsClient {
    public AnimalBondsClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(AnimalBondsClient::registerPayloads);
        BondRendering.register(modBus);
        NeoForge.EVENT_BUS.addListener(AnimalBondsClient::onScreenOpening);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> HybridRendering.clear());
        ClientSelfTest.register(modBus);
        if (net.neoforged.fml.ModList.get().isLoaded("geckolib")) pl.kubalte.animalbonds.client.compat.GeckoLibCompat.init();
    }

    static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenBookPayload.TYPE, AnimalBondsClient::handleOpenBook);
        event.register(pl.kubalte.animalbonds.network.OpenAltarPayload.TYPE, (p, ctx) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gui.screen() instanceof AltarScreen a && a.altar().equals(p.altar())) a.setData(p);
            else mc.gui.setScreen(new AltarScreen(p));
        });
    }

    static void handleOpenBook(OpenBookPayload p, IPayloadContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof BondBookScreen book && book.pedestal().equals(p.pedestal())) {
            book.update(p);
        } else {
            mc.gui.setScreen(new BondBookScreen(p));
        }
    }

    /** Pressing the inventory key while riding a creature with saddlebags opens the bags instead. */
    static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof InventoryScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof Mob mob)) return;
        BondData d = BondLogic.get(mob);
        if (d == null || !d.bags) return;
        event.setCanceled(true);
        ClientPacketDistributor.sendToServer(new OpenBagsPayload(BlockPos.ZERO, -1));
    }
}
