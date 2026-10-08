package io.github.dirnei.mc.servus.lib.datagen;

import io.github.dirnei.mc.servus.lib.ServusLibIds;
import io.github.dirnei.mc.servus.lib.registry.ServusLibBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusLibBlockStateProvider extends BlockStateProvider {

    ServusLibBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusLibIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        ConnectableModels.connectable(this, ServusLibBlocks.DATA_CABLE.get(), "data_cable", 2, true);
    }
}
