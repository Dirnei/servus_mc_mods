package io.github.dirnei.mc.servus.brewery.registry;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The items of Servus: Brewery. */
public final class ServusBreweryItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusBreweryIds.MOD_ID);

    /** The empty half litre glass that every beer is served in. */
    public static final DeferredItem<Item> BEER_GLASS = ITEMS.registerSimpleItem("beer_glass");

    /** Plants the barley crop; named after the item, not the crop block. */
    public static final DeferredItem<BlockItem> BARLEY_SEEDS = ITEMS.registerItem("barley_seeds",
            properties -> new BlockItem(ServusBreweryBlocks.BARLEY.get(), properties.useItemDescriptionPrefix()));

    /** The harvested barley. */
    public static final DeferredItem<Item> BARLEY = ITEMS.registerSimpleItem("barley");

    /** A root cutting that plants a hop vine at the foot of a Hop Pole. */
    public static final DeferredItem<Item> HOP_RHIZOME = ITEMS.registerSimpleItem("hop_rhizome");

    /** Picked hop cones. */
    public static final DeferredItem<Item> HOPS = ITEMS.registerSimpleItem("hops");

    public static final DeferredItem<BlockItem> HOP_POLE = ITEMS.registerSimpleBlockItem(ServusBreweryBlocks.HOP_POLE);

    /** Every Brewery item in the order the creative tab lists it. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(
            BEER_GLASS, BARLEY_SEEDS, BARLEY, HOP_RHIZOME, HOPS, HOP_POLE);

    private ServusBreweryItems() {
    }
}
