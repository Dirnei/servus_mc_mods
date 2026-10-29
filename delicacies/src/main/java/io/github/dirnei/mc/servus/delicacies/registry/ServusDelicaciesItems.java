package io.github.dirnei.mc.servus.delicacies.registry;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumLidItem;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.food.DelicaciesFoodItem;
import io.github.dirnei.mc.servus.delicacies.teller.MealKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
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
    public static final DeferredItem<BlockItem> FERMENTING_CROCK = ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.FERMENTING_CROCK);
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
    public static final List<DeferredItem<? extends Item>> BLOCK_MODEL_ITEMS = List.of(STOVE_TILE, POT, PAN, BAKING_OVEN, CUTTING_BOARD, FERMENTING_CROCK);
    /** The foods whose values the food data map gives. */
    public static final List<DeferredItem<DelicaciesFoodItem>> FOODS =
            List.of(PRETZEL, BREAD_ROLL, BOILED_POTATOES, FRIED_EGG, FRIED_ONIONS, FRIED_POTATOES);
    /** The baked goods, in {@code c:foods/bread}. */
    public static final List<DeferredItem<DelicaciesFoodItem>> BREADS = List.of(PRETZEL, BREAD_ROLL);

    public static final DeferredItem<Item> SHREDDED_RED_CABBAGE = ITEMS.registerSimpleItem("shredded_red_cabbage");
    public static final DeferredItem<Item> GRATED_POTATO = ITEMS.registerSimpleItem("grated_potato");
    public static final DeferredItem<Item> SAUERKRAUT = ITEMS.registerSimpleItem("sauerkraut");
    public static final DeferredItem<Item> BRAISED_RED_CABBAGE = ITEMS.registerSimpleItem("braised_red_cabbage");
    public static final DeferredItem<DelicaciesFoodItem> CHEESE = food("cheese");
    public static final DeferredItem<DelicaciesFoodItem> BREAD_DUMPLING = food("bread_dumpling");
    public static final DeferredItem<DelicaciesFoodItem> POTATO_DUMPLING = food("potato_dumpling");
    public static final DeferredItem<DelicaciesFoodItem> BOWL_OF_SAUERKRAUT = bowlFood("bowl_of_sauerkraut");
    public static final DeferredItem<DelicaciesFoodItem> BOWL_OF_RED_CABBAGE = bowlFood("bowl_of_red_cabbage");
    public static final DeferredItem<DelicaciesFoodItem> POTATO_SALAD = bowlFood("potato_salad");
    /** The ingredients of the sides; Sauerkraut and Braised Red Cabbage are served in a bowl or on a plate. */
    public static final List<DeferredItem<Item>> SIDE_INGREDIENTS = List.of(SHREDDED_RED_CABBAGE, GRATED_POTATO, SAUERKRAUT, BRAISED_RED_CABBAGE);
    /** The edible sides, whose values the food data map gives. */
    public static final List<DeferredItem<DelicaciesFoodItem>> SIDE_FOODS =
            List.of(CHEESE, BREAD_DUMPLING, POTATO_DUMPLING, BOWL_OF_SAUERKRAUT, BOWL_OF_RED_CABBAGE, POTATO_SALAD);
    /** The sides eaten from a bowl, which give the bowl back. */
    public static final List<DeferredItem<DelicaciesFoodItem>> BOWL_FOODS = List.of(BOWL_OF_SAUERKRAUT, BOWL_OF_RED_CABBAGE, POTATO_SALAD);
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
            properties -> properties.usingConvertsTo(Items.BOWL).craftRemainder(Items.BOWL).stacksTo(16));
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

    /** Mod id of Servus: Brewery, whose beer goes with the Weisswurst breakfast. */
    public static final String BREWERY = "servus_brewery";
    public static final DeferredItem<BlockItem> PLATE = ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.PLATE);
    /** The plated meals, stacking to 16. */
    public static final Map<MealKind, DeferredItem<BlockItem>> MEALS = mealItems();
    /** The Plate and the plated meals, whose item models are their block models. */
    public static final List<DeferredItem<? extends Item>> TELLER_ITEMS = tellerItems();

    /** The item that plants each crop, a block item of its crop block named like an item. */
    public static final Map<CropKind, DeferredItem<BlockItem>> PLANTING = plantingItems();

    /** What each ripe crop gives; for a self planting crop this is its planting item. */
    public static final Map<CropKind, DeferredItem<? extends Item>> HARVEST = harvestItems();

    public static final DeferredItem<BlockItem> AQUARIUM_FLOOR = ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.AQUARIUM_FLOOR);
    public static final DeferredItem<AquariumLidItem> AQUARIUM_LID = ITEMS.registerItem("aquarium_lid",
            properties -> new AquariumLidItem(ServusDelicaciesBlocks.AQUARIUM_LID.get(), properties.useBlockDescriptionPrefix()));
    /** The aquarium parts, whose item models are their block models. */
    public static final List<DeferredItem<? extends Item>> AQUARIUM_ITEMS = List.of(AQUARIUM_FLOOR, AQUARIUM_LID);

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

    /** A food served in a bowl: it stacks to 16 and leaves the Bowl when eaten or crafted. */
    private static DeferredItem<DelicaciesFoodItem> bowlFood(String name) {
        return ITEMS.registerItem(name, DelicaciesFoodItem::new,
                properties -> properties.stacksTo(16).usingConvertsTo(Items.BOWL).craftRemainder(Items.BOWL));
    }

    /** The item of the plated {@code meal}. */
    public static Item meal(MealKind meal) {
        return MEALS.get(meal).get();
    }

    private static Map<MealKind, DeferredItem<BlockItem>> mealItems() {
        Map<MealKind, DeferredItem<BlockItem>> items = new EnumMap<>(MealKind.class);
        for (MealKind meal : MealKind.values()) {
            items.put(meal, ITEMS.registerSimpleBlockItem(ServusDelicaciesBlocks.MEALS.get(meal), properties -> properties.stacksTo(16)));
        }
        return Collections.unmodifiableMap(items);
    }

    private static List<DeferredItem<? extends Item>> tellerItems() {
        List<DeferredItem<? extends Item>> items = new ArrayList<>();
        items.add(PLATE);
        items.addAll(MEALS.values());
        return List.copyOf(items);
    }

    /** The Teller items the creative tab shows: the Weissbier breakfast only when Servus: Brewery is installed. */
    private static List<DeferredItem<? extends Item>> tellerTabItems() {
        return TELLER_ITEMS.stream()
                .filter(item -> item != MEALS.get(MealKind.WEISSWURST_BREAKFAST_WITH_WEISSBIER) || ModList.get().isLoaded(BREWERY))
                .toList();
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
        order.add(FERMENTING_CROCK);
        order.add(AQUARIUM_FLOOR);
        order.add(AQUARIUM_LID);
        order.add(DelicaciesFluids.MURKY_WATER_BUCKET);
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
        order.addAll(SIDE_INGREDIENTS);
        order.addAll(SIDE_FOODS);
        order.addAll(MEAT_ITEMS);
        order.addAll(tellerTabItems());
        order.addAll(HARVEST.values());
        for (CropKind crop : CropKind.values()) {
            if (!crop.selfPlanting()) {
                order.add(PLANTING.get(crop));
            }
        }
        return List.copyOf(order);
    }
}
