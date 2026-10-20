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

    public static final DeferredItem<BlockItem> MALTING_FLOOR = ITEMS.registerSimpleBlockItem(ServusBreweryBlocks.MALTING_FLOOR);

    /** Barley soaked in water, ready to spread on a Malting Floor. */
    public static final DeferredItem<Item> SOAKED_BARLEY = ITEMS.registerSimpleItem("soaked_barley");

    /** Wheat soaked in water, ready to spread on a Malting Floor. */
    public static final DeferredItem<Item> SOAKED_WHEAT = ITEMS.registerSimpleItem("soaked_wheat");

    /** Sprouted barley from the Malting Floor, before the kiln. */
    public static final DeferredItem<Item> GREEN_MALT = ITEMS.registerSimpleItem("green_malt");

    /** Sprouted wheat from the Malting Floor, before the kiln. */
    public static final DeferredItem<Item> GREEN_WHEAT_MALT = ITEMS.registerSimpleItem("green_wheat_malt");

    /** Lightly kilned barley malt, for Helles. */
    public static final DeferredItem<Item> PALE_MALT = ITEMS.registerSimpleItem("pale_malt");

    /** Darkly kilned barley malt, for Dunkles. */
    public static final DeferredItem<Item> DARK_MALT = ITEMS.registerSimpleItem("dark_malt");

    /** Kilned wheat malt, for Weissbier. */
    public static final DeferredItem<Item> WHEAT_MALT = ITEMS.registerSimpleItem("wheat_malt");

    /** Pale Malt from the Mill. */
    public static final DeferredItem<Item> PALE_MALT_GRIST = ITEMS.registerSimpleItem("pale_malt_grist");

    /** Dark Malt from the Mill. */
    public static final DeferredItem<Item> DARK_MALT_GRIST = ITEMS.registerSimpleItem("dark_malt_grist");

    /** Wheat Malt from the Mill. */
    public static final DeferredItem<Item> WHEAT_MALT_GRIST = ITEMS.registerSimpleItem("wheat_malt_grist");

    /** Every Brewery item in the order the creative tab lists it. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(
            BEER_GLASS, BARLEY_SEEDS, BARLEY, HOP_RHIZOME, HOPS, HOP_POLE,
            MALTING_FLOOR, SOAKED_BARLEY, SOAKED_WHEAT, GREEN_MALT, GREEN_WHEAT_MALT, PALE_MALT, DARK_MALT, WHEAT_MALT,
            PALE_MALT_GRIST, DARK_MALT_GRIST, WHEAT_MALT_GRIST);

    private ServusBreweryItems() {
    }
}
