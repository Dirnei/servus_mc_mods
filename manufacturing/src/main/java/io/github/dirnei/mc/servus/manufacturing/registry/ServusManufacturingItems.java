package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import java.util.List;
import net.minecraft.world.item.BlockItem;
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
    public static final DeferredItem<BlockItem> POWER_CABLE = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.POWER_CABLE);

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends BlockItem>> CREATIVE_TAB_ORDER = List.of(
            BURNER_EXTRACTOR, BURNER_SMELTER, COAL_GENERATOR, POWER_CABLE,
            IRON_DEPOSIT, COPPER_DEPOSIT, COAL_DEPOSIT, STONE_DEPOSIT);

    private ServusManufacturingItems() {
    }
}
