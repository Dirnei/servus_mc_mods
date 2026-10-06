package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusStorageItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusStorageIds.MOD_ID);

    public static final DeferredItem<BlockItem> CRATE = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.CRATE);
    public static final DeferredItem<BlockItem> DOUBLE_CRATE = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.DOUBLE_CRATE);
    public static final DeferredItem<BlockItem> QUAD_CRATE = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.QUAD_CRATE);
    public static final DeferredItem<BlockItem> WAREHOUSE_CONTROLLER = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.WAREHOUSE_CONTROLLER);
    public static final DeferredItem<BlockItem> COUNTER_TERMINAL = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.COUNTER_TERMINAL);
    public static final DeferredItem<BlockItem> RAIL = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.RAIL);
    public static final DeferredItem<BlockItem> ELEVATOR = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.ELEVATOR);

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(CRATE, DOUBLE_CRATE, QUAD_CRATE, WAREHOUSE_CONTROLLER, COUNTER_TERMINAL, RAIL, ELEVATOR);

    private ServusStorageItems() {
    }
}
