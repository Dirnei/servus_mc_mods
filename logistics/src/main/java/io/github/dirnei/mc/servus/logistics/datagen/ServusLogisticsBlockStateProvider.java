package io.github.dirnei.mc.servus.logistics.datagen;

import io.github.dirnei.mc.servus.lib.datagen.ConnectableModels;
import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusLogisticsBlockStateProvider extends BlockStateProvider {

    ServusLogisticsBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusLogisticsIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        ConnectableModels.connectable(this, ServusLogisticsBlocks.UTILITY_DUCT.get(), "utility_duct", 4, false);
    }
}
