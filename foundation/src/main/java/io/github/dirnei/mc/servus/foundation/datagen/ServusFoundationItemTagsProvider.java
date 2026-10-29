package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.filter.FilterTags;
import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.upgrade.Upgrades;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

/** Item tags: the upgrades other mods can join and the stones of the stone material kind. */
final class ServusFoundationItemTagsProvider extends ItemTagsProvider {

    ServusFoundationItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ServusFoundationIds.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(Upgrades.TAG).add(ServusFoundationItems.SPEED_UPGRADE.get());
        tag(FilterTags.FILTER_BINDERS).addTag(Tags.Items.CROPS_WHEAT).addOptionalTag(FilterTags.FLOURS);
        tag(MaterialKind.STONES).add(
                Items.BRICKS,
                Items.STONE, Items.SMOOTH_STONE, Items.STONE_BRICKS, Items.MOSSY_STONE_BRICKS, Items.CRACKED_STONE_BRICKS,
                Items.CHISELED_STONE_BRICKS, Items.COBBLESTONE, Items.MOSSY_COBBLESTONE,
                Items.ANDESITE, Items.POLISHED_ANDESITE, Items.DIORITE, Items.POLISHED_DIORITE, Items.GRANITE, Items.POLISHED_GRANITE,
                Items.DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.POLISHED_DEEPSLATE, Items.DEEPSLATE_BRICKS,
                Items.CRACKED_DEEPSLATE_BRICKS, Items.DEEPSLATE_TILES, Items.CRACKED_DEEPSLATE_TILES, Items.CHISELED_DEEPSLATE,
                Items.BLACKSTONE, Items.POLISHED_BLACKSTONE, Items.POLISHED_BLACKSTONE_BRICKS,
                Items.CRACKED_POLISHED_BLACKSTONE_BRICKS, Items.CHISELED_POLISHED_BLACKSTONE,
                Items.TUFF, Items.POLISHED_TUFF, Items.TUFF_BRICKS, Items.CHISELED_TUFF, Items.CHISELED_TUFF_BRICKS,
                Items.MUD_BRICKS,
                Items.NETHER_BRICKS, Items.RED_NETHER_BRICKS, Items.CRACKED_NETHER_BRICKS, Items.CHISELED_NETHER_BRICKS,
                Items.SANDSTONE, Items.CUT_SANDSTONE, Items.CHISELED_SANDSTONE, Items.SMOOTH_SANDSTONE,
                Items.RED_SANDSTONE, Items.CUT_RED_SANDSTONE, Items.CHISELED_RED_SANDSTONE, Items.SMOOTH_RED_SANDSTONE,
                Items.QUARTZ_BLOCK, Items.QUARTZ_BRICKS, Items.QUARTZ_PILLAR, Items.CHISELED_QUARTZ_BLOCK, Items.SMOOTH_QUARTZ,
                Items.END_STONE, Items.END_STONE_BRICKS,
                Items.PRISMARINE, Items.PRISMARINE_BRICKS, Items.DARK_PRISMARINE);
    }
}
