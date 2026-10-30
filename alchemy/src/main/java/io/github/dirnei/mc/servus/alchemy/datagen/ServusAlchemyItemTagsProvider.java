package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.hint.OldGnome;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.venter.PutrefactionRecipe;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

/** Item tags: the traces the old gnome leaves behind and the rot the Venter Equinus takes. */
final class ServusAlchemyItemTagsProvider extends ItemTagsProvider {

    ServusAlchemyItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ServusAlchemyIds.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(OldGnome.TRACES).add(ServusAlchemyItems.TINY_PICKAXE.get(), ServusAlchemyItems.RED_FELT_SCRAP.get(),
                ServusAlchemyItems.SALT_OFFERING.get());
        tag(PutrefactionRecipe.PUTREFIABLE).add(Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO, Items.BROWN_MUSHROOM,
                Items.RED_MUSHROOM).addTags(Tags.Items.CROPS_BEETROOT, Tags.Items.CROPS_CACTUS, Tags.Items.CROPS_CARROT, Tags.Items.CROPS_COCOA_BEAN,
                        Tags.Items.CROPS_MELON, Tags.Items.CROPS_NETHER_WART, Tags.Items.CROPS_POTATO, Tags.Items.CROPS_PUMPKIN,
                        Tags.Items.CROPS_SUGAR_CANE, ItemTags.LEAVES);
    }
}
