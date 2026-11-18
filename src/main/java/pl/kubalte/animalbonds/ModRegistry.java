package pl.kubalte.animalbonds;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import pl.kubalte.animalbonds.block.NestBlock;
import pl.kubalte.animalbonds.block.PedestalBlock;
import pl.kubalte.animalbonds.block.PedestalBlockEntity;
import pl.kubalte.animalbonds.block.TroughBlock;
import pl.kubalte.animalbonds.bond.BagData;
import pl.kubalte.animalbonds.bond.BondData;

public final class ModRegistry {
    private ModRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AnimalBonds.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AnimalBonds.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AnimalBonds.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AnimalBonds.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AnimalBonds.MODID);

    // --- Blocks ---
    public static final DeferredBlock<PedestalBlock> PEDESTAL = BLOCKS.registerBlock("bond_pedestal", PedestalBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<TroughBlock> TROUGH = BLOCKS.registerBlock("feeding_trough", TroughBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<NestBlock> NEST = BLOCKS.registerBlock("animal_bed", NestBlock::new,
            p -> p.mapColor(MapColor.COLOR_YELLOW).strength(0.5F).sound(SoundType.GRASS).noOcclusion());

    // --- Items ---
    public static final DeferredItem<BlockItem> PEDESTAL_ITEM = ITEMS.registerSimpleBlockItem("bond_pedestal", PEDESTAL);
    public static final DeferredItem<BlockItem> TROUGH_ITEM = ITEMS.registerSimpleBlockItem("feeding_trough", TROUGH);
    public static final DeferredItem<BlockItem> NEST_ITEM = ITEMS.registerSimpleBlockItem("animal_bed", NEST);
    public static final DeferredItem<Item> SADDLEBAGS = ITEMS.registerSimpleItem("saddlebags", p -> p.stacksTo(1));

    // --- Block entities ---
    public static final Supplier<BlockEntityType<PedestalBlockEntity>> PEDESTAL_BE = BLOCK_ENTITIES.register("bond_pedestal",
            () -> new BlockEntityType<>(PedestalBlockEntity::new, PEDESTAL.get()));

    // --- Attachments ---
    public static final Supplier<AttachmentType<BondData>> BOND = ATTACHMENTS.register("bond",
            () -> AttachmentType.builder(BondData::new).serialize(BondData.MAP_CODEC).sync(BondData.STREAM_CODEC).build());
    public static final Supplier<AttachmentType<BagData>> BAGS = ATTACHMENTS.register("bags",
            () -> AttachmentType.builder(BagData::new).serialize(BagData.MAP_CODEC).build());

    // --- Creative tab ---
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.animalbonds"))
            .icon(() -> PEDESTAL_ITEM.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(PEDESTAL_ITEM.get());
                output.accept(TROUGH_ITEM.get());
                output.accept(NEST_ITEM.get());
                output.accept(SADDLEBAGS.get());
            }).build());

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ATTACHMENTS.register(modBus);
        TABS.register(modBus);
    }

    static void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Items live in our own tab; nothing to add to vanilla tabs.
    }
}
