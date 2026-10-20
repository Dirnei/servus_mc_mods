package io.github.dirnei.mc.servus.brewery.registry;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.beer.BeerItem;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The items of Servus: Brewery. */
public final class ServusBreweryItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusBreweryIds.MOD_ID);

    /** Empty glasses stack this far; full ones do not stack. */
    private static final int EMPTY_GLASS_STACK = 16;

    /** The empty half litre glass for Helles and Dunkles. */
    public static final DeferredItem<Item> BEER_GLASS = ITEMS.registerItem("beer_glass", Item::new,
            () -> new Item.Properties().stacksTo(EMPTY_GLASS_STACK));

    /** The tall empty glass that only Weissbier goes into. */
    public static final DeferredItem<Item> WEISSBIER_GLASS = ITEMS.registerItem("weissbier_glass", Item::new,
            () -> new Item.Properties().stacksTo(EMPTY_GLASS_STACK));

    /** A half litre of Helles, tapped at a Cask. */
    public static final DeferredItem<BeerItem> HELLES = beer("helles", BEER_GLASS);

    /** A half litre of Dunkles, tapped at a Cask. */
    public static final DeferredItem<BeerItem> DUNKLES = beer("dunkles", BEER_GLASS);

    /** A half litre of Weissbier in its own glass, tapped at a Cask. */
    public static final DeferredItem<BeerItem> WEISSBIER = beer("weissbier", WEISSBIER_GLASS);

    /** Crafted once; the Fermenting Vat never uses it up. */
    public static final DeferredItem<Item> YEAST = ITEMS.registerSimpleItem("yeast");

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

    public static final DeferredItem<BlockItem> BREWHOUSE_BRICKS = ITEMS.registerSimpleBlockItem(ServusBreweryBlocks.BREWHOUSE_BRICKS);

    public static final DeferredItem<BlockItem> BREW_KETTLE = ITEMS.registerSimpleBlockItem(ServusBreweryBlocks.BREW_KETTLE);

    public static final DeferredItem<BlockItem> FERMENTING_VAT = ITEMS.registerSimpleBlockItem(ServusBreweryBlocks.FERMENTING_VAT);

    public static final DeferredItem<BlockItem> CASK = ITEMS.registerSimpleBlockItem(ServusBreweryBlocks.CASK);

    /** Every Brewery item in the order the creative tab lists it. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(
            BEER_GLASS, WEISSBIER_GLASS, HELLES, DUNKLES, WEISSBIER, YEAST, BARLEY_SEEDS, BARLEY, HOP_RHIZOME, HOPS, HOP_POLE,
            MALTING_FLOOR, SOAKED_BARLEY, SOAKED_WHEAT, GREEN_MALT, GREEN_WHEAT_MALT, PALE_MALT, DARK_MALT, WHEAT_MALT,
            PALE_MALT_GRIST, DARK_MALT_GRIST, WHEAT_MALT_GRIST, BREWHOUSE_BRICKS, BREW_KETTLE, FERMENTING_VAT, CASK,
            BreweryFluids.PALE_WORT.bucket(), BreweryFluids.DARK_WORT.bucket(), BreweryFluids.WHEAT_WORT.bucket(),
            BreweryFluids.HELLES.bucket(), BreweryFluids.DUNKLES.bucket(), BreweryFluids.WEISSBIER.bucket());

    private ServusBreweryItems() {
    }

    /** A full glass that does not stack and leaves {@code glass} when drunk. */
    private static DeferredItem<BeerItem> beer(String name, DeferredItem<Item> glass) {
        return ITEMS.registerItem(name, BeerItem::new, () -> new Item.Properties().stacksTo(1).usingConvertsTo(glass.get()));
    }
}
