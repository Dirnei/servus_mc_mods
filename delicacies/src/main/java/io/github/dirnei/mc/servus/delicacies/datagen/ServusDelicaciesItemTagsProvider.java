package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.crop.CropTags;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

/** Common crop and seed tags, and the vanilla tags farmer villagers and chickens use. */
final class ServusDelicaciesItemTagsProvider extends ItemTagsProvider {

    ServusDelicaciesItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ServusDelicaciesIds.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (CropKind crop : CropKind.values()) {
            tag(CropTags.crop(crop)).add(ServusDelicaciesItems.harvest(crop));
            tag(Tags.Items.CROPS).addTag(CropTags.crop(crop));
            tag(ItemTags.VILLAGER_PLANTABLE_SEEDS).add(ServusDelicaciesItems.planting(crop));
            if (crop.seedId().isPresent()) {
                tag(CropTags.seed(crop)).add(ServusDelicaciesItems.planting(crop));
                tag(Tags.Items.SEEDS).addTag(CropTags.seed(crop));
                tag(ItemTags.CHICKEN_FOOD).add(ServusDelicaciesItems.planting(crop));
            }
        }
    }
}
