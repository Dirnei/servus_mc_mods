package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import net.neoforged.neoforge.client.model.generators.ModelFile;
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
        // directionalBlock turns the model's up face toward the facing: the port on top, the funnel below.
        ModelFile adapter = models().cube("machine_adapter",
                        modLoc("block/machine_adapter_back"), modLoc("block/machine_adapter_front"),
                        modLoc("block/machine_adapter_side"), modLoc("block/machine_adapter_side"),
                        modLoc("block/machine_adapter_side"), modLoc("block/machine_adapter_side"))
                .texture("particle", modLoc("block/machine_adapter_side"));
        directionalBlock(AdapterContent.BLOCK.get(), adapter);
        simpleBlockItem(AdapterContent.BLOCK.get(), adapter);
    }
}
