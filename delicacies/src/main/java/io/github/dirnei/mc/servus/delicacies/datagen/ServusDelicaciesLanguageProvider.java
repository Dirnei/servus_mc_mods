package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.registry.DelicaciesFluids;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesEffects;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.delicacies.teller.MealKind;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusDelicaciesLanguageProvider extends LanguageProvider {

    ServusDelicaciesLanguageProvider(PackOutput output) {
        super(output, ServusDelicaciesIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + ServusDelicaciesIds.MOD_ID, "Servus: Delicacies");
        add(ServusDelicaciesItems.PRETZEL.get(), "Pretzel");
        crop(CropKind.CABBAGE, "Cabbage", "Cabbage Seeds", "Cabbages");
        crop(CropKind.RED_CABBAGE, "Red Cabbage", "Red Cabbage Seeds", "Red Cabbages");
        crop(CropKind.ONION, "Onion", null, "Onions");
        crop(CropKind.PARSLEY, "Parsley", "Parsley Seeds", "Parsley");
        crop(CropKind.HORSERADISH, "Horseradish", null, "Horseradish");
        crop(CropKind.MUSTARD, "Mustard Seeds", null, "Mustard");
        kitchen();
        meatDishes();
        sides();
        teller();
        heating();
        aquarium();
    }

    /** The aquarium parts, Murky Water, the carrot and bucket messages, the GUI and the JEI pages. */
    private void aquarium() {
        add(ServusDelicaciesBlocks.AQUARIUM_FLOOR.get(), "Aquarium Floor");
        add(ServusDelicaciesBlocks.AQUARIUM_LID.get(), "Aquarium Lid");
        add("fluid_type." + ServusDelicaciesIds.MOD_ID + "." + DelicaciesFluids.MURKY_WATER_ID, "Murky Water");
        add(DelicaciesFluids.MURKY_WATER_BLOCK.get(), "Murky Water");
        add(DelicaciesFluids.MURKY_WATER_BUCKET.get(), "Murky Water Bucket");
        add("aquarium." + ServusDelicaciesIds.MOD_ID + ".species.cod", "Cod");
        add("aquarium." + ServusDelicaciesIds.MOD_ID + ".species.salmon", "Salmon");
        String message = "message." + ServusDelicaciesIds.MOD_ID + ".aquarium.";
        add(message + "formed", "Aquarium formed: %s by %s by %s");
        add(message + "no_tank", "No Fluid Tank rests on this part");
        add(message + "floor_missing", "Aquarium Floor blocks must cover the whole bottom of the tank");
        add(message + "lid_missing", "The lid does not cover the tank: lay an Aquarium Lid on every column");
        add(message + "not_water", "The tank must hold water or nothing");
        add(message + "status", "Aquarium %s by %s by %s: %s of %s fish, water %s%%");
        add(message + "loose", "Not part of an aquarium");
        add(message + "form_first", "Form the aquarium with the Configuration Carrot first");
        add(message + "other_species", "This aquarium keeps %s");
        add(message + "full", "The aquarium is full");
        add(message + "empty", "There is no fish to catch");
        String gui = "gui." + ServusDelicaciesIds.MOD_ID + ".aquarium";
        add(gui, "Aquarium");
        add(gui + ".no_fish", "No fish");
        add(gui + ".fullness", "%s, %s%% full");
        add(gui + ".stock", "%s of %s fish");
        add(gui + ".water_powered", "Water %s%%, powered");
        add(gui + ".water_unpowered", "Water %s%%, no power");
        add(gui + ".feed", "Feed");
        add(gui + ".filter", "Filter");
        add(gui + ".catch", "Catch");
        add(gui + ".murky", "Murky");
        add(gui + ".keep", "Keep");
        add(gui + ".auto_catch_on", "Auto catch: On");
        add(gui + ".auto_catch_off", "Auto catch: Off");
        String jei = "gui." + ServusDelicaciesIds.MOD_ID + ".jei.";
        add(jei + "aquarium_floor_info", "Lay Aquarium Floor blocks under every column of a Fluid Tank of water and Aquarium Lids on top, then use the Configuration Carrot on a part. Pour cod or salmon in with a fish bucket, on a part or the tank; seeds in the feed slot, a Carbon Filter and FE through the floor keep them breeding. Fish above the keep level land in the catch slots. The filter turns dirty water into Murky Water; drain it through a part, or the filter stops when its tank is full.");
        add(jei + "aquarium_lid_info", "Covers the top of an aquarium's tank. A stack used on the tank's top lays a lid on every column at once. Glows while the aquarium has power.");
        add(jei + "murky_water_info", "Made by an aquarium's Carbon Filter from the water holding the dirt it cleaned. Drain it through an aquarium part; top the tank up with fresh water.");
    }

    /** The Plate, the plated meals and Properly Full. */
    private void teller() {
        add(ServusDelicaciesBlocks.PLATE.get(), "Plate");
        add(ServusDelicaciesBlocks.meal(MealKind.ROAST_PORK_PLATE), "Roast Pork Plate");
        add(ServusDelicaciesBlocks.meal(MealKind.SCHNITZEL_PLATE), "Schnitzel Plate");
        add(ServusDelicaciesBlocks.meal(MealKind.WEISSWURST_BREAKFAST), "Weisswurst Breakfast");
        add(ServusDelicaciesBlocks.meal(MealKind.WEISSWURST_BREAKFAST_WITH_WEISSBIER), "Weisswurst Breakfast with Weissbier");
        add(ServusDelicaciesEffects.PROPERLY_FULL.get().getDescriptionId(), "Properly Full");
    }

    /** The Fermenting Crock, the sides, their messages and the JEI category. */
    private void sides() {
        add(ServusDelicaciesBlocks.FERMENTING_CROCK.get(), "Fermenting Crock");
        add(ServusDelicaciesItems.SHREDDED_RED_CABBAGE.get(), "Shredded Red Cabbage");
        add(ServusDelicaciesItems.GRATED_POTATO.get(), "Grated Potato");
        add(ServusDelicaciesItems.SAUERKRAUT.get(), "Sauerkraut");
        add(ServusDelicaciesItems.BRAISED_RED_CABBAGE.get(), "Braised Red Cabbage");
        add(ServusDelicaciesItems.CHEESE.get(), "Cheese");
        add(ServusDelicaciesItems.BREAD_DUMPLING.get(), "Bread Dumpling");
        add(ServusDelicaciesItems.POTATO_DUMPLING.get(), "Potato Dumpling");
        add(ServusDelicaciesItems.BOWL_OF_SAUERKRAUT.get(), "Bowl of Sauerkraut");
        add(ServusDelicaciesItems.BOWL_OF_RED_CABBAGE.get(), "Bowl of Red Cabbage");
        add(ServusDelicaciesItems.POTATO_SALAD.get(), "Potato Salad");
        String crock = "message." + ServusDelicaciesIds.MOD_ID + ".crock.";
        add(crock + "fermenting", "Fermenting into %s, about %s in-game hours left");
        add(crock + "empty", "Put in what should ferment; the lid goes on once it matches a recipe");
        add(crock + "unknown", "something");
        String jei = "gui." + ServusDelicaciesIds.MOD_ID + ".jei.";
        add(jei + "fermenting", "Fermenting Crock");
        add(jei + "days", "%s days");
        add(jei + "sealed", "Sealed in the crock");
    }

    /** The kitchen's blocks, items, messages and JEI categories. */
    private void kitchen() {
        add(ServusDelicaciesBlocks.STOVE_TILE.get(), "Stove Tile");
        add(ServusDelicaciesBlocks.POT.get(), "Pot");
        add(ServusDelicaciesBlocks.PAN.get(), "Pan");
        add(ServusDelicaciesBlocks.BAKING_OVEN.get(), "Baking Oven");
        add(ServusDelicaciesBlocks.CUTTING_BOARD.get(), "Cutting Board");
        add(ServusDelicaciesItems.KITCHEN_KNIFE.get(), "Kitchen Knife");
        add(ServusDelicaciesItems.CHOPPED_ONION.get(), "Chopped Onion");
        add(ServusDelicaciesItems.SHREDDED_CABBAGE.get(), "Shredded Cabbage");
        add(ServusDelicaciesItems.FLOUR.get(), "Flour");
        add(ServusDelicaciesItems.DOUGH.get(), "Dough");
        add(ServusDelicaciesItems.SALT.get(), "Salt");
        add(ServusDelicaciesItems.WOOD_ASH.get(), "Wood Ash");
        add(ServusDelicaciesItems.LYE.get(), "Lye");
        add(ServusDelicaciesItems.BREAD_CRUMBS.get(), "Bread Crumbs");
        add(ServusDelicaciesItems.BREAD_ROLL.get(), "Bread Roll");
        add(ServusDelicaciesItems.BOILED_POTATOES.get(), "Boiled Potatoes");
        add(ServusDelicaciesItems.FRIED_EGG.get(), "Fried Egg");
        add(ServusDelicaciesItems.FRIED_ONIONS.get(), "Fried Onions");
        add(ServusDelicaciesItems.FRIED_POTATOES.get(), "Fried Potatoes");
        String stove = "message." + ServusDelicaciesIds.MOD_ID + ".stove.";
        add(stove + "formed", "Tiled Stove formed, %s tiles long");
        add(stove + "too_short", "A Tiled Stove needs a straight row of at least %s Stove Tiles");
        add(stove + "too_long", "A Tiled Stove is at most %s Stove Tiles long");
        add(stove + "loose", "Use the Configuration Carrot on a row of Stove Tiles to build a Tiled Stove");
        add(stove + "lit", "The fire burns; fuel: %s %s");
        add(stove + "cold", "The fire is out; fuel: %s %s");
        add(stove + "lit.no_fuel", "The fire burns; no fuel left");
        add(stove + "cold.no_fuel", "The fire is out; no fuel");
        String jei = "gui." + ServusDelicaciesIds.MOD_ID + ".jei.";
        add(jei + "pot", "Pot");
        add(jei + "pan", "Pan");
        add(jei + "baking", "Baking Oven");
        add(jei + "cutting", "Cutting Board");
        add(jei + "hot_stove", "On a burning Tiled Stove");
    }

    /** The meat ingredients and dishes, and the loaf's JEI info page. */
    private void meatDishes() {
        add(ServusDelicaciesItems.PORK_CUTLET.get(), "Pork Cutlet");
        add(ServusDelicaciesItems.MINCED_PORK.get(), "Minced Pork");
        add(ServusDelicaciesItems.MINCED_BEEF.get(), "Minced Beef");
        add(ServusDelicaciesItems.BREADED_CUTLET.get(), "Breaded Cutlet");
        add(ServusDelicaciesItems.RAW_CORDON_BLEU.get(), "Raw Cordon Bleu");
        add(ServusDelicaciesItems.SAUSAGE_MEAT.get(), "Sausage Meat");
        add(ServusDelicaciesItems.MUSTARD_FLOUR.get(), "Mustard Flour");
        add(ServusDelicaciesItems.SWEET_MUSTARD.get(), "Sweet Mustard");
        add(ServusDelicaciesBlocks.LEBERKAESE.get(), "Leberkaese");
        add(ServusDelicaciesItems.SCHNITZEL.get(), "Schnitzel");
        add(ServusDelicaciesItems.CORDON_BLEU.get(), "Cordon Bleu");
        add(ServusDelicaciesItems.HAM.get(), "Ham");
        add(ServusDelicaciesItems.WEISSWURST.get(), "Weisswurst");
        add(ServusDelicaciesItems.LEBERKAESE_SLICE.get(), "Leberkaese Slice");
        add(ServusDelicaciesItems.LEBERKAESE_ROLL.get(), "Leberkaese Roll");
        add(ServusDelicaciesItems.ROAST_PORK.get(), "Roast Pork");
        add(ServusDelicaciesItems.GRAVY.get(), "Gravy");
        add("gui." + ServusDelicaciesIds.MOD_ID + ".jei.leberkaese_info",
                "Place the Leberkaese on a solid block and right click it with a Kitchen Knife: each cut gives one slice, four in all. "
                        + "On the Cutting Board a knife cuts the whole loaf into slices.");
    }

    /** Names a crop's harvest, its seed item when it has its own, and its block. */
    private void crop(CropKind crop, String harvest, String seeds, String block) {
        add(ServusDelicaciesItems.harvest(crop), harvest);
        if (seeds != null) {
            add(ServusDelicaciesItems.planting(crop), seeds);
        }
        add(ServusDelicaciesBlocks.crop(crop), block);
    }

    /** The Heating Element's messages on every firebox, and the stove's status with it. */
    private void heating() {
        String firebox = "message." + ServusDelicaciesIds.MOD_ID + ".firebox.";
        add(firebox + "element_in", "Heating Element installed: this firebox now runs on FE");
        add(firebox + "element_already", "This firebox already has a Heating Element");
        add(firebox + "element_out", "Heating Element taken out: this firebox burns fuel again");
        add(firebox + "no_element", "This firebox has no Heating Element");
        add(firebox + "fuel_refused", "The Heating Element heats this firebox; take it out to burn fuel");
        String stove = "message." + ServusDelicaciesIds.MOD_ID + ".stove.electric.";
        add(stove + "lit", "Heating Element hot: %s%% (cooks from %s%%), %s FE stored");
        add(stove + "cold", "Heating Element not hot yet: %s%% (cooks from %s%%), %s FE stored");
    }
}
