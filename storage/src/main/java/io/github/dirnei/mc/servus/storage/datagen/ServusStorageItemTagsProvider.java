package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Item tags: the anti-corrosion items a Field Head takes (honeycomb) and the fertilizers a Spreader doses with (bone meal). */
final class ServusStorageItemTagsProvider extends ItemTagsProvider {

    ServusStorageItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                  CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, ServusStorageIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(FieldHeadBlockEntity.ANTI_CORROSION).add(Items.HONEYCOMB);
        tag(FieldHeadBlockEntity.FERTILIZER).add(Items.BONE_MEAL);
    }
}
