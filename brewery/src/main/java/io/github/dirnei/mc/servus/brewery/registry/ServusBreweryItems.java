package io.github.dirnei.mc.servus.brewery.registry;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import java.util.List;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The items of Servus: Brewery. */
public final class ServusBreweryItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusBreweryIds.MOD_ID);

    /** The empty half litre glass that every beer is served in. */
    public static final DeferredItem<Item> BEER_GLASS = ITEMS.registerSimpleItem("beer_glass");

    /** Every Brewery item in the order the creative tab lists it. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(BEER_GLASS);

    private ServusBreweryItems() {
    }
}
