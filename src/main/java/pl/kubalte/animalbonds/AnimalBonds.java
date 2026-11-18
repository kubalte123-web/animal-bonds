package pl.kubalte.animalbonds;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import pl.kubalte.animalbonds.bond.BondEvents;
import pl.kubalte.animalbonds.network.ModNetwork;

@Mod(AnimalBonds.MODID)
public class AnimalBonds {
    public static final String MODID = "animalbonds";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AnimalBonds(IEventBus modBus, ModContainer container) {
        ModRegistry.register(modBus);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(ModRegistry::addCreative);
        container.registerConfig(ModConfig.Type.COMMON, BondConfig.SPEC);

        BondEvents.register(NeoForge.EVENT_BUS);
        SelfTest.register(NeoForge.EVENT_BUS);
    }
}
