package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.hint.OldGnome;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

/** Item tags: the traces the old gnome leaves behind. */
final class ServusAlchemyItemTagsProvider extends ItemTagsProvider {

    ServusAlchemyItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ServusAlchemyIds.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(OldGnome.TRACES).add(ServusAlchemyItems.TINY_PICKAXE.get(), ServusAlchemyItems.RED_FELT_SCRAP.get(),
                ServusAlchemyItems.SALT_OFFERING.get());
    }
}
