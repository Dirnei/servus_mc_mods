package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

/** The common {@code c:} crop and seed tags, and the vanilla tags that make farmers and chickens take Barley Seeds. */
final class ServusBreweryItemTagsProvider extends ItemTagsProvider {

    static final TagKey<Item> CROPS_BARLEY = common("crops/barley");
    static final TagKey<Item> CROPS_HOPS = common("crops/hops");
    static final TagKey<Item> SEEDS_BARLEY = common("seeds/barley");

    ServusBreweryItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ServusBreweryIds.MOD_ID);
    }

    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(CROPS_BARLEY).add(ServusBreweryItems.BARLEY.get());
        tag(CROPS_HOPS).add(ServusBreweryItems.HOPS.get());
        tag(Tags.Items.CROPS).addTag(CROPS_BARLEY).addTag(CROPS_HOPS);
        tag(SEEDS_BARLEY).add(ServusBreweryItems.BARLEY_SEEDS.get());
        tag(Tags.Items.SEEDS).addTag(SEEDS_BARLEY);
        tag(ItemTags.VILLAGER_PLANTABLE_SEEDS).add(ServusBreweryItems.BARLEY_SEEDS.get());
        tag(ItemTags.CHICKEN_FOOD).add(ServusBreweryItems.BARLEY_SEEDS.get());
    }
}
