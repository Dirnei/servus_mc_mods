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
    public static final DeferredItem<BlockItem> POWER_CABLE = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.POWER_CABLE);
    public static final DeferredItem<BlockItem> ITEM_DUCT = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.ITEM_DUCT);

    public static final DeferredItem<Item> IRON_GEAR = ITEMS.registerSimpleItem("iron_gear");
    public static final DeferredItem<Item> COPPER_WIRE = ITEMS.registerSimpleItem("copper_wire");
    public static final DeferredItem<Item> RED_PACK = ITEMS.registerSimpleItem("red_pack");

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(
            BURNER_EXTRACTOR, BURNER_SMELTER, COAL_GENERATOR, ASSEMBLER, POWER_CABLE, ITEM_DUCT,
            IRON_GEAR, COPPER_WIRE, RED_PACK,
            IRON_DEPOSIT, COPPER_DEPOSIT, COAL_DEPOSIT, STONE_DEPOSIT);

    private ServusManufacturingItems() {
    }
}
