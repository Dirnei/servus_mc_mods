package io.github.dirnei.mc.servus.delicacies.registry;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import java.util.List;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The items of Servus: Delicacies. */
public final class ServusDelicaciesItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusDelicaciesIds.MOD_ID);

    public static final DeferredItem<Item> PRETZEL = ITEMS.registerSimpleItem("pretzel");

    /** Every Delicacies item in the order the creative tab lists it. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(PRETZEL);

    private ServusDelicaciesItems() {
    }
}
