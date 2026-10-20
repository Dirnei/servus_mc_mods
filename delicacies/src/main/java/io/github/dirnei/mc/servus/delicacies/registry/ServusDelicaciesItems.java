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
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The items of Servus: Delicacies. */
public final class ServusDelicaciesItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusDelicaciesIds.MOD_ID);

    /** Cuts the Kitchen Knife makes before it breaks. */
    public static final int KNIFE_DURABILITY = 250;

    public static final DeferredItem<DelicaciesFoodItem> PRETZEL = food("pretzel");

    public static final DeferredItem<BlockItem> STOVE_TILE = ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.STOVE_TILE);
    public static final DeferredItem<BlockItem> POT = ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.POT);
    public static final DeferredItem<BlockItem> PAN = ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.PAN);
    public static final DeferredItem<BlockItem> BAKING_OVEN = ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.BAKING_OVEN);
    public static final DeferredItem<BlockItem> CUTTING_BOARD = ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.CUTTING_BOARD);
    public static final DeferredItem<Item> KITCHEN_KNIFE = ITEMS.registerItem("kitchen_knife", Item::new,
            properties -> properties.durability(KNIFE_DURABILITY));

    public static final DeferredItem<Item> CHOPPED_ONION = ITEMS.registerSimpleItem("chopped_onion");
    public static final DeferredItem<Item> SHREDDED_CABBAGE = ITEMS.registerSimpleItem("shredded_cabbage");
    public static final DeferredItem<Item> FLOUR = ITEMS.registerSimpleItem("flour");
    public static final DeferredItem<Item> DOUGH = ITEMS.registerSimpleItem("dough");
    public static final DeferredItem<Item> SALT = ITEMS.registerSimpleItem("salt");
    public static final DeferredItem<Item> WOOD_ASH = ITEMS.registerSimpleItem("wood_ash");
    public static final DeferredItem<Item> LYE = ITEMS.registerSimpleItem("lye");
    public static final DeferredItem<Item> BREAD_CRUMBS = ITEMS.registerSimpleItem("bread_crumbs");
    public static final DeferredItem<DelicaciesFoodItem> BREAD_ROLL = food("bread_roll");
    public static final DeferredItem<DelicaciesFoodItem> BOILED_POTATOES = food("boiled_potatoes");
    public static final DeferredItem<DelicaciesFoodItem> FRIED_EGG = food("fried_egg");
    public static final DeferredItem<DelicaciesFoodItem> FRIED_ONIONS = food("fried_onions");
    public static final DeferredItem<DelicaciesFoodItem> FRIED_POTATOES = food("fried_potatoes");
    /** Items whose model is their block's model rather than a flat sprite. */
    public static final List<DeferredItem<? extends Item>> BLOCK_MODEL_ITEMS = List.of(STOVE_TILE, POT, PAN, BAKING_OVEN, CUTTING_BOARD);
    /** The foods whose values the food data map gives. */
    public static final List<DeferredItem<DelicaciesFoodItem>> FOODS =
            List.of(PRETZEL, BREAD_ROLL, BOILED_POTATOES, FRIED_EGG, FRIED_ONIONS, FRIED_POTATOES);
    /** The baked goods, in {@code c:foods/bread}. */
    public static final List<DeferredItem<DelicaciesFoodItem>> BREADS = List.of(PRETZEL, BREAD_ROLL);

    public static final DeferredItem<Item> PORK_CUTLET = ITEMS.registerSimpleItem("pork_cutlet");
    public static final DeferredItem<Item> MINCED_PORK = ITEMS.registerSimpleItem("minced_pork");
    public static final DeferredItem<Item> MINCED_BEEF = ITEMS.registerSimpleItem("minced_beef");
    public static final DeferredItem<Item> BREADED_CUTLET = ITEMS.registerSimpleItem("breaded_cutlet");
    public static final DeferredItem<Item> RAW_CORDON_BLEU = ITEMS.registerSimpleItem("raw_cordon_bleu");
    public static final DeferredItem<Item> SAUSAGE_MEAT = ITEMS.registerSimpleItem("sausage_meat");
    public static final DeferredItem<Item> MUSTARD_FLOUR = ITEMS.registerSimpleItem("mustard_flour");
    public static final DeferredItem<Item> SWEET_MUSTARD = ITEMS.registerSimpleItem("sweet_mustard");
    public static final DeferredItem<BlockItem> LEBERKAESE = ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.LEBERKAESE,
            properties -> properties.stacksTo(16));
    public static final DeferredItem<DelicaciesFoodItem> SCHNITZEL = food("schnitzel");
    public static final DeferredItem<DelicaciesFoodItem> CORDON_BLEU = food("cordon_bleu");
    public static final DeferredItem<DelicaciesFoodItem> HAM = food("ham");
    public static final DeferredItem<DelicaciesFoodItem> WEISSWURST = food("weisswurst");
    public static final DeferredItem<DelicaciesFoodItem> LEBERKAESE_SLICE = food("leberkaese_slice");
    public static final DeferredItem<DelicaciesFoodItem> LEBERKAESE_ROLL = food("leberkaese_roll");
    public static final DeferredItem<DelicaciesFoodItem> ROAST_PORK = food("roast_pork");
    /** Eaten like a stew: the bowl comes back. */
    public static final DeferredItem<DelicaciesFoodItem> GRAVY = ITEMS.registerItem("gravy", DelicaciesFoodItem::new,
            properties -> properties.usingConvertsTo(Items.BOWL).stacksTo(16));
    /** The meat dishes, in {@code c:foods/cooked_meat}. */
    public static final List<DeferredItem<DelicaciesFoodItem>> COOKED_MEATS =
            List.of(SCHNITZEL, CORDON_BLEU, HAM, WEISSWURST, LEBERKAESE_SLICE, LEBERKAESE_ROLL, ROAST_PORK);
    /** The meat foods whose values the food data map gives. */
    public static final List<DeferredItem<DelicaciesFoodItem>> MEAT_FOODS =
            List.of(SCHNITZEL, CORDON_BLEU, HAM, WEISSWURST, LEBERKAESE_SLICE, LEBERKAESE_ROLL, ROAST_PORK, GRAVY);
    /** The meat ingredients and dishes in the order the creative tab lists them. */
    public static final List<DeferredItem<? extends Item>> MEAT_ITEMS = List.of(PORK_CUTLET, MINCED_PORK, MINCED_BEEF, BREADED_CUTLET,
            RAW_CORDON_BLEU, SAUSAGE_MEAT, MUSTARD_FLOUR, SWEET_MUSTARD, LEBERKAESE, SCHNITZEL, CORDON_BLEU, HAM, WEISSWURST,
            LEBERKAESE_SLICE, LEBERKAESE_ROLL, ROAST_PORK, GRAVY);

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
        order.add(BAKING_OVEN);
        order.add(CUTTING_BOARD);
        order.add(KITCHEN_KNIFE);
        order.add(CHOPPED_ONION);
        order.add(SHREDDED_CABBAGE);
        order.add(FLOUR);
        order.add(DOUGH);
        order.add(SALT);
        order.add(WOOD_ASH);
        order.add(LYE);
        order.add(BREAD_CRUMBS);
        FOODS.stream().filter(food -> food != PRETZEL).forEach(order::add);
        order.addAll(MEAT_ITEMS);
        order.addAll(HARVEST.values());
        for (CropKind crop : CropKind.values()) {
            if (!crop.selfPlanting()) {
                order.add(PLANTING.get(crop));
            }
        }
        return List.copyOf(order);
    }
}
