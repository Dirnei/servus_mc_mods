package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import net.minecraft.world.item.Items;

/** Item tags: the anti-corrosion items a Field Head takes (honeycomb) and the fertilizers a Spreader doses with (bone meal). */
final class ServusShuttlesItemTagsProvider extends ItemTagsProvider {

    ServusShuttlesItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ServusShuttlesIds.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(FieldHeadBlockEntity.ANTI_CORROSION).add(Items.HONEYCOMB);
        tag(FieldHeadBlockEntity.FERTILIZER).add(Items.BONE_MEAL);
    }
}
