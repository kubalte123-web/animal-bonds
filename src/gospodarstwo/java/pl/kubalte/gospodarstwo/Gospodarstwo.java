package pl.kubalte.gospodarstwo;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;
import pl.kubalte.gospodarstwo.block.BonsaiData;
import pl.kubalte.gospodarstwo.block.BonsaiPotBlock;
import pl.kubalte.gospodarstwo.block.BonsaiPotBlockEntity;
import pl.kubalte.gospodarstwo.block.FruitLeavesBlock;
import pl.kubalte.gospodarstwo.block.GooseberryBushBlock;
import pl.kubalte.gospodarstwo.block.LoomBlockEntity;
import pl.kubalte.gospodarstwo.block.ModCropBlock;
import pl.kubalte.gospodarstwo.block.WeavingLoomBlock;
import pl.kubalte.gospodarstwo.entity.Duck;
import pl.kubalte.gospodarstwo.entity.Goose;
import pl.kubalte.gospodarstwo.entity.Snail;
import pl.kubalte.gospodarstwo.entity.Turkey;

/** Swojskie Gospodarstwo: farm animals and breeds, crops, fruit trees, bonsai pots and a weaving loom. */
@Mod(Gospodarstwo.MODID)
public class Gospodarstwo {
    public static final String MODID = "gospodarstwo";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** Everything that goes into the creative tab, in order. */
    private static final List<Supplier<? extends Item>> TAB_ITEMS = new ArrayList<>();

    private static <T extends Item> DeferredItem<T> tab(DeferredItem<T> item) {
        TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<Item> item(String name) {
        return tab(ITEMS.registerSimpleItem(name));
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return tab(ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build())));
    }

    // ---------------- breeds ----------------
    /** Breed of a vanilla animal ({@link pl.kubalte.gospodarstwo.breed.Breed} ordinal + 1, 0 = ordinary). Absent = not rolled yet. */
    public static final Supplier<AttachmentType<Integer>> BREED = ATTACHMENTS.register("breed",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("value")).sync(ByteBufCodecs.VAR_INT).build());
    /** Game time when the animal was last sheared / gave its product. */
    public static final Supplier<AttachmentType<Long>> LAST_HARVEST = ATTACHMENTS.register("last_harvest",
            () -> AttachmentType.builder(() -> -1_000_000L).serialize(Codec.LONG.fieldOf("value")).build());

    // ---------------- crops ----------------
    public static final DeferredBlock<ModCropBlock> FLAX = BLOCKS.registerBlock("flax", p -> new ModCropBlock(p, () -> Gospodarstwo.FLAX_SEEDS.get()),
            Gospodarstwo::cropProps);
    public static final DeferredBlock<ModCropBlock> CABBAGES = BLOCKS.registerBlock("cabbages", p -> new ModCropBlock(p, () -> Gospodarstwo.CABBAGE_SEEDS.get()),
            Gospodarstwo::cropProps);
    public static final DeferredBlock<ModCropBlock> ONIONS = BLOCKS.registerBlock("onions", p -> new ModCropBlock(p, () -> Gospodarstwo.ONION.get()),
            Gospodarstwo::cropProps);
    public static final DeferredBlock<ModCropBlock> STRAWBERRIES = BLOCKS.registerBlock("strawberries",
            p -> new ModCropBlock(p, () -> Gospodarstwo.STRAWBERRY_RUNNER.get()), Gospodarstwo::cropProps);
    public static final DeferredBlock<GooseberryBushBlock> GOOSEBERRY_BUSH = BLOCKS.registerBlock("gooseberry_bush", GooseberryBushBlock::new,
            p -> p.mapColor(MapColor.PLANT).randomTicks().noCollision().sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY));

    private static Block.Properties cropProps(Block.Properties p) {
        return p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY);
    }

    // ---------------- fruit trees ----------------
    static ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> feature(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, id(name));
    }

    public static final TreeGrower APPLE_GROWER = new TreeGrower("gospodarstwo_apple", Optional.empty(), Optional.of(feature("apple_tree")), Optional.empty());
    public static final TreeGrower PLUM_GROWER = new TreeGrower("gospodarstwo_plum", Optional.empty(), Optional.of(feature("plum_tree")), Optional.empty());

    public static final DeferredBlock<FruitLeavesBlock> APPLE_LEAVES = BLOCKS.registerBlock("apple_leaves",
            p -> new FruitLeavesBlock(p, () -> net.minecraft.world.item.Items.APPLE), Gospodarstwo::leavesProps);
    public static final DeferredBlock<FruitLeavesBlock> PLUM_LEAVES = BLOCKS.registerBlock("plum_leaves",
            p -> new FruitLeavesBlock(p, () -> Gospodarstwo.PLUM.get()), Gospodarstwo::leavesProps);
    public static final DeferredBlock<SaplingBlock> APPLE_SAPLING = BLOCKS.registerBlock("apple_sapling", p -> new SaplingBlock(APPLE_GROWER, p),
            Gospodarstwo::saplingProps);
    public static final DeferredBlock<SaplingBlock> PLUM_SAPLING = BLOCKS.registerBlock("plum_sapling", p -> new SaplingBlock(PLUM_GROWER, p),
            Gospodarstwo::saplingProps);

    private static Block.Properties leavesProps(Block.Properties p) {
        return p.mapColor(MapColor.PLANT).strength(0.2F).randomTicks().sound(SoundType.GRASS).noOcclusion()
                .isValidSpawn((s, l, pos, t) -> false).isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false)
                .pushReaction(PushReaction.DESTROY).isRedstoneConductor((s, l, pos) -> false);
    }

    private static Block.Properties saplingProps(Block.Properties p) {
        return p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY);
    }

    // ---------------- bonsai & loom ----------------
    public static final DeferredBlock<BonsaiPotBlock> BONSAI_POT = BLOCKS.registerBlock("bonsai_pot", BonsaiPotBlock::new,
            p -> p.mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.6F).sound(SoundType.DECORATED_POT).noOcclusion());
    public static final DeferredBlock<WeavingLoomBlock> WEAVING_LOOM = BLOCKS.registerBlock("weaving_loom", WeavingLoomBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());

    public static final Supplier<BlockEntityType<BonsaiPotBlockEntity>> BONSAI_BE = BLOCK_ENTITIES.register("bonsai_pot",
            () -> new BlockEntityType<>(BonsaiPotBlockEntity::new, BONSAI_POT.get()));
    public static final Supplier<BlockEntityType<LoomBlockEntity>> LOOM_BE = BLOCK_ENTITIES.register("weaving_loom",
            () -> new BlockEntityType<>(LoomBlockEntity::new, WEAVING_LOOM.get()));

    public static final Supplier<net.minecraft.core.component.DataComponentType<BonsaiData>> BONSAI_DATA = COMPONENTS.registerComponentType("bonsai",
            b -> b.persistent(BonsaiData.CODEC).networkSynchronized(BonsaiData.STREAM_CODEC));

    // ---------------- animals ----------------
    public static final DeferredHolder<EntityType<?>, EntityType<Duck>> DUCK = ENTITIES.registerEntityType("duck", Duck::new, MobCategory.CREATURE,
            b -> b.sized(0.45F, 0.65F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Goose>> GOOSE = ENTITIES.registerEntityType("goose", Goose::new, MobCategory.CREATURE,
            b -> b.sized(0.55F, 0.95F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Turkey>> TURKEY = ENTITIES.registerEntityType("turkey", Turkey::new,
            MobCategory.CREATURE, b -> b.sized(0.6F, 0.95F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Snail>> SNAIL = ENTITIES.registerEntityType("snail", Snail::new, MobCategory.CREATURE,
            b -> b.sized(0.45F, 0.45F).clientTrackingRange(8));

    // ---------------- items: plants ----------------
    public static final DeferredItem<BlockItem> FLAX_SEEDS = tab(ITEMS.registerItem("flax_seeds", p -> new BlockItem(FLAX.get(), p.useItemDescriptionPrefix())));
    public static final DeferredItem<Item> FLAX_FIBER = item("flax_fiber");
    public static final DeferredItem<BlockItem> CABBAGE_SEEDS = tab(ITEMS.registerItem("cabbage_seeds",
            p -> new BlockItem(CABBAGES.get(), p.useItemDescriptionPrefix())));
    public static final DeferredItem<Item> CABBAGE = food("cabbage", 3, 0.6F);
    public static final DeferredItem<BlockItem> ONION = tab(ITEMS.registerItem("onion", p -> new BlockItem(ONIONS.get(),
            p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4F).build()).useItemDescriptionPrefix())));
    public static final DeferredItem<BlockItem> STRAWBERRY_RUNNER = tab(ITEMS.registerItem("strawberry_runner",
            p -> new BlockItem(STRAWBERRIES.get(), p.useItemDescriptionPrefix())));
    public static final DeferredItem<Item> STRAWBERRY = food("strawberry", 2, 0.3F);
    public static final DeferredItem<BlockItem> GOOSEBERRIES = tab(ITEMS.registerItem("gooseberries", p -> new BlockItem(GOOSEBERRY_BUSH.get(),
            p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build()).useItemDescriptionPrefix())));
    public static final DeferredItem<Item> PLUM = food("plum", 4, 0.3F);
    public static final DeferredItem<BlockItem> APPLE_SAPLING_ITEM = tab(ITEMS.registerSimpleBlockItem("apple_sapling", APPLE_SAPLING));
    public static final DeferredItem<BlockItem> PLUM_SAPLING_ITEM = tab(ITEMS.registerSimpleBlockItem("plum_sapling", PLUM_SAPLING));
    public static final DeferredItem<BlockItem> APPLE_LEAVES_ITEM = tab(ITEMS.registerSimpleBlockItem("apple_leaves", APPLE_LEAVES));
    public static final DeferredItem<BlockItem> PLUM_LEAVES_ITEM = tab(ITEMS.registerSimpleBlockItem("plum_leaves", PLUM_LEAVES));
    public static final DeferredItem<BlockItem> BONSAI_POT_ITEM = tab(ITEMS.registerSimpleBlockItem("bonsai_pot", BONSAI_POT));
    public static final DeferredItem<BlockItem> WEAVING_LOOM_ITEM = tab(ITEMS.registerSimpleBlockItem("weaving_loom", WEAVING_LOOM));

    // ---------------- items: from animals ----------------
    public static final DeferredItem<Item> DUCK_EGG = item("duck_egg");
    public static final DeferredItem<Item> GOOSE_EGG = item("goose_egg");
    public static final DeferredItem<Item> GREEN_EGG = item("green_egg");
    public static final DeferredItem<Item> RAW_DUCK = food("raw_duck", 2, 0.3F);
    public static final DeferredItem<Item> COOKED_DUCK = food("cooked_duck", 6, 0.6F);
    public static final DeferredItem<Item> RAW_GOOSE = food("raw_goose", 3, 0.3F);
    public static final DeferredItem<Item> COOKED_GOOSE = food("cooked_goose", 7, 0.7F);
    public static final DeferredItem<Item> RAW_TURKEY = food("raw_turkey", 3, 0.3F);
    public static final DeferredItem<Item> COOKED_TURKEY = food("cooked_turkey", 8, 0.7F);
    public static final DeferredItem<Item> DOWN = item("down");
    public static final DeferredItem<Item> TURKEY_FEATHER = item("turkey_feather");
    public static final DeferredItem<Item> SNAIL_SHELL = item("snail_shell");
    public static final DeferredItem<Item> SNAIL_SLIME = item("snail_slime");
    public static final DeferredItem<Item> HIGHLAND_HAIR = item("highland_hair");
    public static final DeferredItem<Item> MERINO_WOOL = item("merino_wool");
    public static final DeferredItem<Item> MANGALICA_WOOL = item("mangalica_wool");
    public static final DeferredItem<Item> ANGORA_WOOL = item("angora_wool");

    // ---------------- items: fabrics ----------------
    public static final DeferredItem<Item> CANVAS = item("canvas");
    public static final DeferredItem<Item> LINEN = item("linen");
    public static final DeferredItem<Item> WOOLEN_CLOTH = item("woolen_cloth");
    public static final DeferredItem<Item> MERINO_CLOTH = item("merino_cloth");
    public static final DeferredItem<Item> ANGORA_CLOTH = item("angora_cloth");
    public static final DeferredItem<Item> FELT = item("felt");

    // ---------------- spawn eggs ----------------
    public static final DeferredItem<SpawnEggItem> DUCK_SPAWN_EGG = tab(ITEMS.registerItem("duck_spawn_egg", p -> new SpawnEggItem(p.spawnEgg(DUCK.get()))));
    public static final DeferredItem<SpawnEggItem> GOOSE_SPAWN_EGG = tab(ITEMS.registerItem("goose_spawn_egg", p -> new SpawnEggItem(p.spawnEgg(GOOSE.get()))));
    public static final DeferredItem<SpawnEggItem> TURKEY_SPAWN_EGG = tab(ITEMS.registerItem("turkey_spawn_egg", p -> new SpawnEggItem(p.spawnEgg(TURKEY.get()))));
    public static final DeferredItem<SpawnEggItem> SNAIL_SPAWN_EGG = tab(ITEMS.registerItem("snail_spawn_egg", p -> new SpawnEggItem(p.spawnEgg(SNAIL.get()))));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.gospodarstwo"))
            .icon(() -> WEAVING_LOOM_ITEM.get().getDefaultInstance())
            .displayItems((params, out) -> {
                for (Supplier<? extends Item> s : TAB_ITEMS) out.accept(s.get());
            }).build());

    public Gospodarstwo(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ATTACHMENTS.register(modBus);
        COMPONENTS.register(modBus);
        TABS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, GConfig.SPEC);
        modBus.addListener(GSetup::attributes);
        modBus.addListener(GSetup::spawnPlacements);
        pl.kubalte.gospodarstwo.breed.BreedEvents.register(NeoForge.EVENT_BUS);
        GSelfTest.register(NeoForge.EVENT_BUS);
        if (ModList.get().isLoaded("animalbonds")) pl.kubalte.gospodarstwo.compat.AnimalBondsCompat.init(modBus);
    }
}
