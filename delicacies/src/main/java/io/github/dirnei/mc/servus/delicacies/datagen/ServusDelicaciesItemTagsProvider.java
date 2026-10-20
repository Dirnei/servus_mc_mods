package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.crop.CropTags;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenTags;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagEntry;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

/** Common crop, seed, knife, food and bakery tags, and the vanilla tags farmer villagers and chickens use. */
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
        tag(KitchenTags.KNIVES).add(ServusDelicaciesItems.KITCHEN_KNIFE.get());
        tag(Tags.Items.TOOLS).addTag(KitchenTags.KNIVES);
        ServusDelicaciesItems.FOODS.forEach(food -> tag(Tags.Items.FOODS).add(food.get()));
        ServusDelicaciesItems.BREADS.forEach(bread -> tag(Tags.Items.FOODS_BREAD).add(bread.get()));
        tag(KitchenTags.WHEAT_FLOUR).add(ServusDelicaciesItems.FLOUR.get());
        tag(KitchenTags.FLOURS).addTag(KitchenTags.WHEAT_FLOUR);
        tag(KitchenTags.WHEAT_DOUGH).add(ServusDelicaciesItems.DOUGH.get());
        tag(Tags.Items.FOODS_DOUGH).addTag(KitchenTags.WHEAT_DOUGH);
        tag(KitchenTags.SALT).add(ServusDelicaciesItems.SALT.get());
        tag(KitchenTags.ASH).add(ServusDelicaciesItems.WOOD_ASH.get())
                .add(TagEntry.optionalElement(Identifier.fromNamespaceAndPath("servus_alchemy", "ash")));
        tag(Tags.Items.DUSTS).addTag(KitchenTags.SALT).addTag(KitchenTags.ASH);
        meatDishes();
    }

    /** Meat foods and dishes, mustard flour, and the cheese tag the Cordon Bleu asks for, declared even while empty. */
    private void meatDishes() {
        ServusDelicaciesItems.MEAT_FOODS.forEach(food -> tag(Tags.Items.FOODS).add(food.get()));
        ServusDelicaciesItems.COOKED_MEATS.forEach(meat -> tag(Tags.Items.FOODS_COOKED_MEAT).add(meat.get()));
        tag(Tags.Items.FOODS_SOUP).add(ServusDelicaciesItems.GRAVY.get());
        tag(KitchenTags.MUSTARD_FLOUR).add(ServusDelicaciesItems.MUSTARD_FLOUR.get());
        tag(KitchenTags.FLOURS).addTag(KitchenTags.MUSTARD_FLOUR);
        tag(KitchenTags.CHEESE);
    }
}
