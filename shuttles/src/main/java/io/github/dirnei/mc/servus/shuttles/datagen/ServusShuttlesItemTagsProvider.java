package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.TreeTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import net.minecraft.world.item.Items;

/**
 * Item tags: the anti-corrosion items a Field Head takes (honeycomb), the
 * fertilizers a Spreader doses with (bone meal) and the saplings that grow
 * big trees from a square of four.
 */
final class ServusShuttlesItemTagsProvider extends ItemTagsProvider {

    ServusShuttlesItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ServusShuttlesIds.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(FieldHeadBlockEntity.ANTI_CORROSION).add(Items.HONEYCOMB);
        tag(FieldHeadBlockEntity.FERTILIZER).add(Items.BONE_MEAL);
        tag(TreeTags.SAPLINGS_2X2).add(Items.SPRUCE_SAPLING, Items.JUNGLE_SAPLING, Items.DARK_OAK_SAPLING, Items.PALE_OAK_SAPLING);
        tag(TreeTags.SAPLINGS_2X2_ONLY).add(Items.DARK_OAK_SAPLING, Items.PALE_OAK_SAPLING);
    }
}
