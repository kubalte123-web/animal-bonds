package pl.kubalte.golebie;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import pl.kubalte.golebie.bird.Bird;
import pl.kubalte.golebie.bird.Fantail;
import pl.kubalte.golebie.bird.Pigeon;
import pl.kubalte.golebie.item.DiaperItem;

/**
 * Gołębie: the grey pigeon and the chestnut fantail, as two real pets behave. They run up with their heads low, circle
 * in front of you, lean into your hand when stroked, fly onto your shoulder or head and nibble at your face, bow back
 * when you nod, and wear diapers.
 */
@Mod(Golebie.MODID)
public class Golebie {
    public static final String MODID = "golebie";
    public static final Logger LOGGER = LogUtils.getLogger();
    /** One chance in this many, each tick, that a tame bird standing near its still owner flies up onto them. */
    public static final int PERCH_CHANCE = 1800;
    /** Which nibble animation turns the head towards the face from the left shoulder (flip if it ever looks away). */
    public static final int NIBBLE_LEFT_SHOULDER = Bird.A_NIBBLE_L;
    public static final String[] DIAPER_PATTERNS = {"gingham_black", "stripes_grey", "gingham_red", "gingham_blue", "dots_pink", "denim"};

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Pigeon>> PIGEON = ENTITIES.registerEntityType("pigeon", Pigeon::new,
            MobCategory.CREATURE, b -> b.sized(0.4F, 0.45F).eyeHeight(0.38F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Fantail>> FANTAIL = ENTITIES.registerEntityType("fantail", Fantail::new,
            MobCategory.CREATURE, b -> b.sized(0.42F, 0.55F).eyeHeight(0.45F).clientTrackingRange(10));

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id(name)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> COO = sound("coo");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOW_COO = sound("bow_coo");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURR = sound("purr");
    public static final DeferredHolder<SoundEvent, SoundEvent> PECK = sound("peck");
    public static final DeferredHolder<SoundEvent, SoundEvent> WINGS = sound("wings");

    private static final List<Supplier<? extends Item>> TAB_ITEMS = new ArrayList<>();

    private static <T extends Item> DeferredItem<T> tab(DeferredItem<T> item) {
        TAB_ITEMS.add(item);
        return item;
    }

    public static final DeferredItem<SpawnEggItem> PIGEON_SPAWN_EGG = tab(ITEMS.registerItem("pigeon_spawn_egg",
            p -> new SpawnEggItem(p.spawnEgg(PIGEON.get()))));
    public static final DeferredItem<SpawnEggItem> FANTAIL_SPAWN_EGG = tab(ITEMS.registerItem("fantail_spawn_egg",
            p -> new SpawnEggItem(p.spawnEgg(FANTAIL.get()))));
    public static final List<DeferredItem<DiaperItem>> DIAPERS = new ArrayList<>();

    static {
        for (int i = 0; i < DIAPER_PATTERNS.length; i++) {
            final int n = i;
            DIAPERS.add(tab(ITEMS.registerItem("diaper_" + DIAPER_PATTERNS[i], p -> new DiaperItem(p.stacksTo(16), n))));
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.golebie"))
            .icon(() -> DIAPERS.get(0).get().getDefaultInstance())
            .displayItems((params, out) -> {
                for (Supplier<? extends Item> s : TAB_ITEMS) out.accept(s.get());
            }).build());

    public Golebie(IEventBus modBus, ModContainer container) {
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(Golebie::attributes);
        modBus.addListener(Golebie::spawnPlacements);
        NeoForge.EVENT_BUS.addListener(Nods::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Nods::onLogout);
        PSelfTest.register(NeoForge.EVENT_BUS);
    }

    static void attributes(EntityAttributeCreationEvent e) {
        e.put(PIGEON.get(), Bird.createAttributes().build());
        e.put(FANTAIL.get(), Bird.createAttributes().build());
    }

    static void spawnPlacements(RegisterSpawnPlacementsEvent e) {
        var R = RegisterSpawnPlacementsEvent.Operation.REPLACE;
        e.register(PIGEON.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules, R);
        e.register(FANTAIL.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules, R);
    }
}
