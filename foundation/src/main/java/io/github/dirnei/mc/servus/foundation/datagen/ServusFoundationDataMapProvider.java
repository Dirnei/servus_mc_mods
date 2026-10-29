package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.filter.FilterDataMaps;
import io.github.dirnei.mc.servus.foundation.filter.FilterSpec;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

/** The Carbon Filter's points and spent item, and the compost chance of the Spent Filter. */
final class ServusFoundationDataMapProvider extends DataMapProvider {

    private static final int CARBON_FILTER_POINTS = 512;
    private static final float SPENT_FILTER_COMPOST = 0.65F;

    ServusFoundationDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        builder(FilterDataMaps.FILTER).add(ServusFoundationItems.CARBON_FILTER,
                new FilterSpec(CARBON_FILTER_POINTS, ServusFoundationItems.SPENT_FILTER.get()), false);
        builder(NeoForgeDataMaps.COMPOSTABLES).add(ServusFoundationItems.SPENT_FILTER, new Compostable(SPENT_FILTER_COMPOST), false);
    }
}
