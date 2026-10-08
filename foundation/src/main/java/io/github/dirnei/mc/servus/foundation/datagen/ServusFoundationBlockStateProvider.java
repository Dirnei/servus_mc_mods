package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.datagen.LogisticsDataParts;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusFoundationBlockStateProvider extends BlockStateProvider {

    ServusFoundationBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusFoundationIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        LogisticsDataParts.blockStates(this);
    }
}
