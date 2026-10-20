package io.github.dirnei.mc.servus.delicacies.registry;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.food.DelicaciesFoodItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The items of Servus: Delicacies. */
public final class ServusDelicaciesItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusDelicaciesIds.MOD_ID);

    /** Cuts the Kitchen Knife makes before it breaks. */
    public static final int KNIFE_DURABILITY = 250;

    public static final DeferredItem<Item> PRETZEL = ITEMS.registerSimpleItem("pretzel");

    public static final DeferredItem<BlockItem> STOVE_TILE = ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.STOVE_TILE);
    public static final DeferredItem<BlockItem> POT = ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.POT);
    public static final DeferredItem<BlockItem> PAN = ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.PAN);
    public static final DeferredItem<BlockItem> CUTTING_BOARD = ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.CUTTING_BOARD);
    public static final DeferredItem<Item> KITCHEN_KNIFE = ITEMS.registerItem("kitchen_knife", Item::new,
            properties -> properties.durability(KNIFE_DURABILITY));

    public static final DeferredItem<Item> CHOPPED_ONION = ITEMS.registerSimpleItem("chopped_onion");
    public static final DeferredItem<Item> SHREDDED_CABBAGE = ITEMS.registerSimpleItem("shredded_cabbage");
    public static final DeferredItem<DelicaciesFoodItem> BOILED_POTATOES = food("boiled_potatoes");
    public static final DeferredItem<DelicaciesFoodItem> FRIED_EGG = food("fried_egg");
    public static final DeferredItem<DelicaciesFoodItem> FRIED_ONIONS = food("fried_onions");
    public static final DeferredItem<DelicaciesFoodItem> FRIED_POTATOES = food("fried_potatoes");
    /** Items whose model is their block's model rather than a flat sprite. */
    public static final List<DeferredItem<? extends Item>> BLOCK_MODEL_ITEMS = List.of(STOVE_TILE, POT, PAN, CUTTING_BOARD);
    /** The foods whose values the food data map gives. */
    public static final List<DeferredItem<DelicaciesFoodItem>> FOODS = List.of(BOILED_POTATOES, FRIED_EGG, FRIED_ONIONS, FRIED_POTATOES);

    /** The item that plants each crop, a block item of its crop block named like an item. */
    public static final Map<CropKind, DeferredItem<BlockItem>> PLANTING = plantingItems();

    /** What each ripe crop gives; for a self planting crop this is its planting item. */
    public static final Map<CropKind, DeferredItem<? extends Item>> HARVEST = harvestItems();

    /** Every Delicacies item in the order the creative tab lists it. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = creativeTabOrder();

    private ServusDelicaciesItems() {
    }

    /** The item that plants {@code crop}. */
    public static Item planting(CropKind crop) {
        return PLANTING.get(crop).get();
    }

    /** The item a ripe {@code crop} gives. */
    public static Item harvest(CropKind crop) {
        return HARVEST.get(crop).get();
    }

    private static DeferredItem<DelicaciesFoodItem> food(String name) {
        return ITEMS.registerItem(name, DelicaciesFoodItem::new);
    }

    private static Map<CropKind, DeferredItem<BlockItem>> plantingItems() {
        Map<CropKind, DeferredItem<BlockItem>> items = new EnumMap<>(CropKind.class);
        for (CropKind crop : CropKind.values()) {
            items.put(crop, ITEMS.registerItem(crop.plantingId(),
                    properties -> new BlockItem(ServusDelicaciesBlocks.crop(crop), properties),
                    Item.Properties::useItemDescriptionPrefix));
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<CropKind, DeferredItem<? extends Item>> harvestItems() {
        Map<CropKind, DeferredItem<? extends Item>> items = new EnumMap<>(CropKind.class);
        for (CropKind crop : CropKind.values()) {
            items.put(crop, crop.selfPlanting() ? PLANTING.get(crop) : ITEMS.registerSimpleItem(crop.harvestId()));
        }
        return Collections.unmodifiableMap(items);
    }

    private static List<DeferredItem<? extends Item>> creativeTabOrder() {
        List<DeferredItem<? extends Item>> order = new ArrayList<>();
        order.add(PRETZEL);
        order.add(STOVE_TILE);
        order.add(POT);
        order.add(PAN);
        order.add(CUTTING_BOARD);
        order.add(KITCHEN_KNIFE);
        order.add(CHOPPED_ONION);
        order.add(SHREDDED_CABBAGE);
        order.addAll(FOODS);
        order.addAll(HARVEST.values());
        for (CropKind crop : CropKind.values()) {
            if (!crop.selfPlanting()) {
                order.add(PLANTING.get(crop));
            }
        }
        return List.copyOf(order);
    }
}
