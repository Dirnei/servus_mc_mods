package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusManufacturingIds.MOD_ID);

    public static final DeferredItem<BlockItem> IRON_DEPOSIT = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.IRON_DEPOSIT);
    public static final DeferredItem<BlockItem> COPPER_DEPOSIT = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.COPPER_DEPOSIT);
    public static final DeferredItem<BlockItem> COAL_DEPOSIT = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.COAL_DEPOSIT);
    public static final DeferredItem<BlockItem> STONE_DEPOSIT = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.STONE_DEPOSIT);

    private ServusManufacturingItems() {
    }
}
