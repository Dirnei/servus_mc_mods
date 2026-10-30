package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.AlchemyDataMaps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.DataMapProvider;

/** The bedding that warms the Venter Equinus: a Hay Bale keeps the bed warm for 6,000 work ticks. */
final class ServusAlchemyDataMapProvider extends DataMapProvider {

    ServusAlchemyDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        builder(AlchemyDataMaps.BEDDING).add(Items.HAY_BLOCK.builtInRegistryHolder(), new AlchemyDataMaps.Bedding(6000), false);
    }
}
