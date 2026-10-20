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
