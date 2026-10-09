package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusManufacturingIds.MOD_ID);

    public static final DeferredItem<BlockItem> RESEARCH_LAB = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.RESEARCH_LAB);
    public static final DeferredItem<BlockItem> CALCINER = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.CALCINER);

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(CALCINER, RESEARCH_LAB);

    private ServusManufacturingItems() {
    }
}
