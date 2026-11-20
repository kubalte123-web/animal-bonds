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
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect, net.minecraft.world.effect.MobEffect> RESTED =
            MOB_EFFECTS.register("rested", () -> new pl.kubalte.gospodarstwo.effect.RestedEffect().addAttributeModifier(
                    net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, id("rested_health"), 4.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
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
    /** On the level: bed head position (as a long, in a string) -> 1 = pillow, 2 = featherbed. Beds have no block entity. */
    public static final Supplier<AttachmentType<java.util.Map<String, Integer>>> BED_COMFORT = ATTACHMENTS.register("bed_comfort",
            () -> AttachmentType.<java.util.Map<String, Integer>>builder(() -> new java.util.HashMap<>())
                    .serialize(Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("beds")).build());
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

    public static final DeferredBlock<pl.kubalte.gospodarstwo.block.SpinningWheelBlock> SPINNING_WHEEL = BLOCKS.registerBlock("spinning_wheel",
            pl.kubalte.gospodarstwo.block.SpinningWheelBlock::new, p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<pl.kubalte.gospodarstwo.block.DyeVatBlock> DYE_VAT = BLOCKS.registerBlock("dye_vat",
            pl.kubalte.gospodarstwo.block.DyeVatBlock::new, p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());

    public static final DeferredBlock<pl.kubalte.gospodarstwo.block.TanningRackBlock> TANNING_RACK = BLOCKS.registerBlock("tanning_rack",
            pl.kubalte.gospodarstwo.block.TanningRackBlock::new, p -> p.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<pl.kubalte.gospodarstwo.block.KettleBlock> KETTLE = BLOCKS.registerBlock("kettle",
            pl.kubalte.gospodarstwo.block.KettleBlock::new, p -> p.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<pl.kubalte.gospodarstwo.block.CurtainBlock> CURTAIN = BLOCKS.registerBlock("curtain",
            pl.kubalte.gospodarstwo.block.CurtainBlock::new, p -> p.mapColor(MapColor.WOOL).strength(0.4F).sound(SoundType.WOOL).noOcclusion()
                    .ignitedByLava());
    public static final DeferredBlock<pl.kubalte.gospodarstwo.block.RugBlock> WOOL_RUG = BLOCKS.registerBlock("wool_rug",
            pl.kubalte.gospodarstwo.block.RugBlock::new, p -> p.mapColor(MapColor.WOOL).strength(0.1F).sound(SoundType.WOOL).noOcclusion().ignitedByLava());
    public static final DeferredBlock<net.minecraft.world.level.block.CarpetBlock> RAG_RUG = BLOCKS.registerBlock("rag_rug",
            net.minecraft.world.level.block.CarpetBlock::new, p -> p.mapColor(MapColor.COLOR_BROWN).strength(0.1F).sound(SoundType.WOOL).ignitedByLava());
    public static final DeferredBlock<net.minecraft.world.level.block.LanternBlock> HORN_LANTERN = BLOCKS.registerBlock("horn_lantern",
            net.minecraft.world.level.block.LanternBlock::new, p -> p.mapColor(MapColor.TERRACOTTA_YELLOW).strength(2.5F).sound(SoundType.LANTERN)
                    .lightLevel(st -> 13).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    public static final Supplier<BlockEntityType<pl.kubalte.gospodarstwo.block.CurtainBlockEntity>> CURTAIN_BE = BLOCK_ENTITIES.register("curtain",
            () -> new BlockEntityType<>(pl.kubalte.gospodarstwo.block.CurtainBlockEntity::new, CURTAIN.get()));
    public static final Supplier<BlockEntityType<pl.kubalte.gospodarstwo.block.RugBlockEntity>> RUG_BE = BLOCK_ENTITIES.register("wool_rug",
            () -> new BlockEntityType<>(pl.kubalte.gospodarstwo.block.RugBlockEntity::new, WOOL_RUG.get()));
    public static final Supplier<BlockEntityType<pl.kubalte.gospodarstwo.block.TanningRackBlockEntity>> TANNING_RACK_BE = BLOCK_ENTITIES.register(
            "tanning_rack", () -> new BlockEntityType<>(pl.kubalte.gospodarstwo.block.TanningRackBlockEntity::new, TANNING_RACK.get()));
    public static final Supplier<BlockEntityType<pl.kubalte.gospodarstwo.block.KettleBlockEntity>> KETTLE_BE = BLOCK_ENTITIES.register("kettle",
            () -> new BlockEntityType<>(pl.kubalte.gospodarstwo.block.KettleBlockEntity::new, KETTLE.get()));
    public static final Supplier<BlockEntityType<pl.kubalte.gospodarstwo.block.SpinningWheelBlockEntity>> SPINNING_WHEEL_BE = BLOCK_ENTITIES.register(
            "spinning_wheel", () -> new BlockEntityType<>(pl.kubalte.gospodarstwo.block.SpinningWheelBlockEntity::new, SPINNING_WHEEL.get()));
    public static final Supplier<BlockEntityType<pl.kubalte.gospodarstwo.block.DyeVatBlockEntity>> DYE_VAT_BE = BLOCK_ENTITIES.register("dye_vat",
            () -> new BlockEntityType<>(pl.kubalte.gospodarstwo.block.DyeVatBlockEntity::new, DYE_VAT.get()));
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
    public static final DeferredItem<BlockItem> SPINNING_WHEEL_ITEM = tab(ITEMS.registerSimpleBlockItem("spinning_wheel", SPINNING_WHEEL));
    public static final DeferredItem<BlockItem> DYE_VAT_ITEM = tab(ITEMS.registerSimpleBlockItem("dye_vat", DYE_VAT));
    public static final DeferredItem<BlockItem> TANNING_RACK_ITEM = tab(ITEMS.registerSimpleBlockItem("tanning_rack", TANNING_RACK));
    public static final DeferredItem<BlockItem> KETTLE_ITEM = tab(ITEMS.registerSimpleBlockItem("kettle", KETTLE));
    public static final DeferredItem<BlockItem> CURTAIN_ITEM = tab(ITEMS.registerSimpleBlockItem("curtain", CURTAIN));
    public static final DeferredItem<BlockItem> WOOL_RUG_ITEM = tab(ITEMS.registerSimpleBlockItem("wool_rug", WOOL_RUG));
    public static final DeferredItem<BlockItem> RAG_RUG_ITEM = tab(ITEMS.registerSimpleBlockItem("rag_rug", RAG_RUG));
    public static final DeferredItem<BlockItem> HORN_LANTERN_ITEM = tab(ITEMS.registerSimpleBlockItem("horn_lantern", HORN_LANTERN));

    // ---------------- items: clothes (stage 3) ----------------
    private static DeferredItem<Item> wear(String name, net.minecraft.world.item.equipment.ArmorMaterial m, net.minecraft.world.item.equipment.ArmorType t) {
        return tab(ITEMS.registerSimpleItem(name, p -> p.humanoidArmor(m, t)));
    }

    public static final DeferredItem<Item> WOOL_CAP = wear("wool_cap", pl.kubalte.gospodarstwo.craft.Clothing.WOOL,
            net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final DeferredItem<Item> WOOL_SWEATER = wear("wool_sweater", pl.kubalte.gospodarstwo.craft.Clothing.WOOL,
            net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final DeferredItem<Item> WOOL_TROUSERS = wear("wool_trousers", pl.kubalte.gospodarstwo.craft.Clothing.WOOL,
            net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final DeferredItem<pl.kubalte.gospodarstwo.item.FeltBootsItem> FELT_BOOTS = tab(ITEMS.registerItem("felt_boots",
            pl.kubalte.gospodarstwo.item.FeltBootsItem::new, p -> p.humanoidArmor(pl.kubalte.gospodarstwo.craft.Clothing.FELT,
                    net.minecraft.world.item.equipment.ArmorType.BOOTS)));
    public static final DeferredItem<Item> FUR_HAT = wear("fur_hat", pl.kubalte.gospodarstwo.craft.Clothing.FUR,
            net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final DeferredItem<Item> SHEEPSKIN_COAT = wear("sheepskin_coat", pl.kubalte.gospodarstwo.craft.Clothing.SHEEPSKIN,
            net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final DeferredItem<Item> LINEN_SHIRT = wear("linen_shirt", pl.kubalte.gospodarstwo.craft.Clothing.LINEN,
            net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final DeferredItem<Item> LINEN_TROUSERS = wear("linen_trousers", pl.kubalte.gospodarstwo.craft.Clothing.LINEN,
            net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final DeferredItem<Item> PILLOW = tab(ITEMS.registerSimpleItem("pillow", p -> p.stacksTo(16)));
    public static final DeferredItem<Item> FEATHERBED = tab(ITEMS.registerSimpleItem("featherbed", p -> p.stacksTo(16)));
    public static final DeferredItem<pl.kubalte.gospodarstwo.item.CurryCombItem> CURRY_COMB = tab(ITEMS.registerItem("curry_comb",
            pl.kubalte.gospodarstwo.item.CurryCombItem::new, p -> p.durability(128)));

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
    public static final DeferredItem<Item> HORSE_HAIR = item("horse_hair");
    public static final DeferredItem<Item> CASHMERE = item("cashmere");
    public static final DeferredItem<Item> LLAMA_WOOL = item("llama_wool");
    public static final DeferredItem<Item> CAMEL_WOOL = item("camel_wool");
    public static final DeferredItem<Item> DOG_HAIR = item("dog_hair");

    // ---------------- items: from vanilla animals and monsters (stage 2) ----------------
    public static final DeferredItem<Item> RAW_HIDE = item("raw_hide");
    public static final DeferredItem<Item> TALLOW = item("tallow");
    public static final DeferredItem<Item> HORN = item("horn");
    public static final DeferredItem<Item> SINEW = item("sinew");
    public static final DeferredItem<Item> FATBACK = food("fatback", 2, 0.4F);
    public static final DeferredItem<Item> BRISTLES = item("bristles");
    public static final DeferredItem<Item> GUT = item("gut");
    public static final DeferredItem<Item> RABBIT_FUR = item("rabbit_fur");
    public static final DeferredItem<Item> FOX_FUR = item("fox_fur");
    public static final DeferredItem<Item> BEAR_FUR = item("bear_fur");
    public static final DeferredItem<Item> BEAR_CLAW = item("bear_claw");
    public static final DeferredItem<Item> POULTRY_BONE = item("poultry_bone");
    public static final DeferredItem<Item> SWIM_BLADDER = item("swim_bladder");
    public static final DeferredItem<Item> TENTACLE = food("tentacle", 2, 0.2F);
    public static final DeferredItem<Item> COOKED_TENTACLE = food("cooked_tentacle", 5, 0.6F);
    public static final DeferredItem<Item> SPINNERET = item("spinneret");
    public static final DeferredItem<Item> VENOM_GLAND = item("venom_gland");
    public static final DeferredItem<Item> RAGS = item("rags");
    public static final DeferredItem<Item> NET_SCRAP = item("net_scrap");
    public static final DeferredItem<Item> BONE_MARROW = item("bone_marrow");
    public static final DeferredItem<Item> CREEPER_SKIN = item("creeper_skin");
    public static final DeferredItem<Item> HERB_POUCH = item("herb_pouch");
    public static final DeferredItem<Item> STRIDER_SKIN = tab(ITEMS.registerSimpleItem("strider_skin", p -> p.fireResistant()));
    public static final DeferredItem<Item> BLAZE_ASH = tab(ITEMS.registerSimpleItem("blaze_ash", p -> p.fireResistant()));
    public static final DeferredItem<Item> GHAST_MEMBRANE = item("ghast_membrane");
    public static final DeferredItem<Item> THICK_HIDE = item("thick_hide");
    public static final DeferredItem<Item> HOGLIN_TUSK = item("hoglin_tusk");
    public static final DeferredItem<Item> RAVAGER_HORN = item("ravager_horn");
    public static final DeferredItem<Item> GUARDIAN_SPIKE = item("guardian_spike");
    public static final DeferredItem<Item> WIND_FIBRE = item("wind_fibre");
    public static final DeferredItem<Item> POISON_MYCELIUM = item("poison_mycelium");
    public static final DeferredItem<Item> SCULK_CORE = tab(ITEMS.registerSimpleItem("sculk_core", p -> p.rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<Item> DRAGON_SCALE = tab(ITEMS.registerSimpleItem("dragon_scale",
            p -> p.rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    // processed
    public static final DeferredItem<Item> THICK_LEATHER = item("thick_leather");
    public static final DeferredItem<Item> BEESWAX = item("beeswax");
    public static final DeferredItem<Item> GLUE = item("glue");
    public static final DeferredItem<Item> LARD = food("lard", 3, 0.5F);
    public static final DeferredItem<Item> SOAP = item("soap");

    // ---------------- items: yarn (dyeable) ----------------
    public static final DeferredItem<Item> YARN_WOOL = item("yarn_wool");
    public static final DeferredItem<Item> YARN_LINEN = item("yarn_linen");
    public static final DeferredItem<Item> YARN_MERINO = item("yarn_merino");
    public static final DeferredItem<Item> YARN_ANGORA = item("yarn_angora");
    public static final DeferredItem<Item> YARN_CASHMERE = item("yarn_cashmere");
    public static final DeferredItem<Item> YARN_LLAMA = item("yarn_llama");
    public static final DeferredItem<Item> YARN_COARSE = item("yarn_coarse");
    public static final DeferredItem<Item> YARN_SILK = item("yarn_silk");

    // ---------------- items: fabrics ----------------
    public static final DeferredItem<Item> CANVAS = item("canvas");
    public static final DeferredItem<Item> LINEN = item("linen");
    public static final DeferredItem<Item> WOOLEN_CLOTH = item("woolen_cloth");
    public static final DeferredItem<Item> MERINO_CLOTH = item("merino_cloth");
    public static final DeferredItem<Item> ANGORA_CLOTH = item("angora_cloth");
    public static final DeferredItem<Item> FELT = item("felt");
    public static final DeferredItem<Item> CASHMERE_CLOTH = item("cashmere_cloth");
    public static final DeferredItem<Item> LLAMA_CLOTH = item("llama_cloth");
    public static final DeferredItem<Item> SILK = item("silk");

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
        MOB_EFFECTS.register(modBus);
        COMPONENTS.register(modBus);
        TABS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, GConfig.SPEC);
        modBus.addListener(GSetup::attributes);
        modBus.addListener(GSetup::spawnPlacements);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(pl.kubalte.gospodarstwo.craft.Dyes::init));
        pl.kubalte.gospodarstwo.breed.BreedEvents.register(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener(pl.kubalte.gospodarstwo.craft.MobDrops::onDrops);
        NeoForge.EVENT_BUS.addListener(pl.kubalte.gospodarstwo.craft.Clothing::onTick);
        NeoForge.EVENT_BUS.addListener(pl.kubalte.gospodarstwo.craft.Bedding::onUse);
        NeoForge.EVENT_BUS.addListener(pl.kubalte.gospodarstwo.craft.Bedding::onBreak);
        NeoForge.EVENT_BUS.addListener(pl.kubalte.gospodarstwo.craft.Bedding::onSleepFinished);
        GSelfTest.register(NeoForge.EVENT_BUS);
        if (ModList.get().isLoaded("animalbonds")) pl.kubalte.gospodarstwo.compat.AnimalBondsCompat.init(modBus);
    }
}
