package pl.kubalte.elementals;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;
import pl.kubalte.elementals.entity.Emberlizard;
import pl.kubalte.elementals.entity.Frostling;
import pl.kubalte.elementals.entity.Mossling;
import pl.kubalte.elementals.entity.Puffowl;

@Mod(Elementals.MODID)
public class Elementals {
    public static final String MODID = "elementals";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** Element of a creature (ordinal of {@link Element}); absent = not yet rolled. */
    public static final Supplier<AttachmentType<Integer>> ELEMENT = ATTACHMENTS.register("element",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("value")).sync(ByteBufCodecs.VAR_INT).build());

    // ---- creatures ----
    public static final DeferredHolder<EntityType<?>, EntityType<Mossling>> MOSSLING = ENTITIES.registerEntityType("mossling",
            Mossling::new, MobCategory.CREATURE, b -> b.sized(0.8F, 0.6F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Puffowl>> PUFFOWL = ENTITIES.registerEntityType("puffowl",
            Puffowl::new, MobCategory.CREATURE, b -> b.sized(0.6F, 0.7F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Emberlizard>> EMBERLIZARD = ENTITIES.registerEntityType("emberlizard",
            Emberlizard::new, MobCategory.MONSTER, b -> b.sized(0.9F, 0.55F).fireImmune().clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Frostling>> FROSTLING = ENTITIES.registerEntityType("frostling",
            Frostling::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.5F).clientTrackingRange(10));

    public static final DeferredItem<SpawnEggItem> MOSSLING_EGG = ITEMS.registerItem("mossling_spawn_egg", p -> new SpawnEggItem(p.spawnEgg(MOSSLING.get())));
    public static final DeferredItem<SpawnEggItem> PUFFOWL_EGG = ITEMS.registerItem("puffowl_spawn_egg", p -> new SpawnEggItem(p.spawnEgg(PUFFOWL.get())));
    public static final DeferredItem<SpawnEggItem> EMBERLIZARD_EGG = ITEMS.registerItem("emberlizard_spawn_egg", p -> new SpawnEggItem(p.spawnEgg(EMBERLIZARD.get())));
    public static final DeferredItem<SpawnEggItem> FROSTLING_EGG = ITEMS.registerItem("frostling_spawn_egg", p -> new SpawnEggItem(p.spawnEgg(FROSTLING.get())));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.elementals"))
            .icon(() -> MOSSLING_EGG.get().getDefaultInstance())
            .displayItems((params, out) -> {
                out.accept(MOSSLING_EGG.get());
                out.accept(PUFFOWL_EGG.get());
                out.accept(EMBERLIZARD_EGG.get());
                out.accept(FROSTLING_EGG.get());
            }).build());

    public Elementals(IEventBus modBus, ModContainer container) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        ATTACHMENTS.register(modBus);
        TABS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, ElementalsConfig.SPEC);
        modBus.addListener(ElementalsSetup::attributes);
        modBus.addListener(ElementalsSetup::spawnPlacements);
        ElementEvents.register(NeoForge.EVENT_BUS);
        ElementalsSelfTest.register(NeoForge.EVENT_BUS);
        if (ModList.get().isLoaded("animalbonds")) pl.kubalte.elementals.compat.AnimalBondsCompat.init(modBus);
    }

    // ---- helpers ----
    public static Element element(net.minecraft.world.entity.Entity e) {
        Integer v = e.getExistingDataOrNull(ELEMENT.get());
        return v == null ? Element.NONE : Element.byOrdinal(v);
    }

    public static boolean hasRolledElement(net.minecraft.world.entity.Entity e) {
        return e.hasData(ELEMENT.get());
    }

    public static void setElement(net.minecraft.world.entity.Entity e, Element el) {
        e.setData(ELEMENT.get(), el.ordinal());
    }
}
