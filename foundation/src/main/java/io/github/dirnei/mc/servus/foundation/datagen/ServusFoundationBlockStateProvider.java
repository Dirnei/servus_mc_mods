package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusFoundationBlockStateProvider extends BlockStateProvider {

    ServusFoundationBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusFoundationIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        ConnectableModels.connectable(this, ServusFoundationBlocks.DATA_CABLE.get(), "data_cable", 2, true);
    }
}
