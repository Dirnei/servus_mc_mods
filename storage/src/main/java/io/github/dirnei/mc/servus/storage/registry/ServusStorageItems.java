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
    public static final DeferredItem<BlockItem> RACK = ITEMS.registerSimpleBlockItem(ServusStorageBlocks.RACK);

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(CRATE, RACK);

    private ServusStorageItems() {
    }
}
