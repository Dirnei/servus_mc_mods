package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
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
    }

    /** The kitchen's blocks, items, messages and JEI categories. */
    private void kitchen() {
        add(ServusDelicaciesBlocks.STOVE_TILE.get(), "Stove Tile");
        add(ServusDelicaciesBlocks.POT.get(), "Pot");
        add(ServusDelicaciesBlocks.PAN.get(), "Pan");
        add(ServusDelicaciesBlocks.CUTTING_BOARD.get(), "Cutting Board");
        add(ServusDelicaciesItems.KITCHEN_KNIFE.get(), "Kitchen Knife");
        add(ServusDelicaciesItems.CHOPPED_ONION.get(), "Chopped Onion");
        add(ServusDelicaciesItems.SHREDDED_CABBAGE.get(), "Shredded Cabbage");
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
        add(jei + "cutting", "Cutting Board");
        add(jei + "hot_stove", "On a burning Tiled Stove");
    }

    /** Names a crop's harvest, its seed item when it has its own, and its block. */
    private void crop(CropKind crop, String harvest, String seeds, String block) {
        add(ServusDelicaciesItems.harvest(crop), harvest);
        if (seeds != null) {
            add(ServusDelicaciesItems.planting(crop), seeds);
        }
        add(ServusDelicaciesBlocks.crop(crop), block);
    }
}
