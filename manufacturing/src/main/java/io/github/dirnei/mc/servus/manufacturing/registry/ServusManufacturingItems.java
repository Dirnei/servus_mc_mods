package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusManufacturingIds.MOD_ID);

    public static final DeferredItem<BlockItem> IRON_DEPOSIT = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.IRON_DEPOSIT);
    public static final DeferredItem<BlockItem> COPPER_DEPOSIT = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.COPPER_DEPOSIT);
    public static final DeferredItem<BlockItem> COAL_DEPOSIT = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.COAL_DEPOSIT);
    public static final DeferredItem<BlockItem> STONE_DEPOSIT = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.STONE_DEPOSIT);

    public static final DeferredItem<BlockItem> BURNER_EXTRACTOR = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.BURNER_EXTRACTOR);
    public static final DeferredItem<BlockItem> BURNER_SMELTER = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.BURNER_SMELTER);
    public static final DeferredItem<BlockItem> COAL_GENERATOR = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.COAL_GENERATOR);
    public static final DeferredItem<BlockItem> ASSEMBLER = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.ASSEMBLER);
    public static final DeferredItem<BlockItem> ELECTRIC_EXTRACTOR = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.ELECTRIC_EXTRACTOR);
    public static final DeferredItem<BlockItem> CRUSHER = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.CRUSHER);
    public static final DeferredItem<BlockItem> CHUNK_LOADER = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.CHUNK_LOADER);
    public static final DeferredItem<BlockItem> RESEARCH_LAB = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.RESEARCH_LAB);
    public static final DeferredItem<BlockItem> BURNER_PUMP = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.BURNER_PUMP);
    public static final DeferredItem<BlockItem> ELECTRIC_PUMP = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.ELECTRIC_PUMP);
    public static final DeferredItem<BlockItem> SINK = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.SINK);
    public static final DeferredItem<BlockItem> CHIMNEY = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.CHIMNEY);

    public static final DeferredItem<Item> IRON_GEAR = ITEMS.registerSimpleItem("iron_gear");
    public static final DeferredItem<Item> COPPER_WIRE = ITEMS.registerSimpleItem("copper_wire");
    public static final DeferredItem<Item> RED_PACK = ITEMS.registerSimpleItem("red_pack");
    public static final DeferredItem<Item> IRON_GRIT = ITEMS.registerSimpleItem("iron_grit");
    public static final DeferredItem<Item> COPPER_GRIT = ITEMS.registerSimpleItem("copper_grit");

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(
            BURNER_EXTRACTOR, BURNER_SMELTER, ELECTRIC_EXTRACTOR, CRUSHER, BURNER_PUMP, ELECTRIC_PUMP, COAL_GENERATOR, ASSEMBLER,
            RESEARCH_LAB, CHUNK_LOADER, SINK, CHIMNEY,
            IRON_GEAR, COPPER_WIRE, RED_PACK, IRON_GRIT, COPPER_GRIT,
            IRON_DEPOSIT, COPPER_DEPOSIT, COAL_DEPOSIT, STONE_DEPOSIT);

    private ServusManufacturingItems() {
    }
}
