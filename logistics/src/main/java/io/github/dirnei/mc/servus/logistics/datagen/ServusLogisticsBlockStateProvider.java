package io.github.dirnei.mc.servus.logistics.datagen;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusLogisticsBlockStateProvider extends BlockStateProvider {

    ServusLogisticsBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusLogisticsIds.MOD_ID, existingFileHelper);
    }

    /**
     * The duct is composed at render time from the part models; the block
     * state only names the thin centre so the dynamic model wrapper always
     * has a base model.
     */
    @Override
    protected void registerStatesAndModels() {
        ModelFile base = DuctPartModels.generate(this);
        simpleBlock(ServusLogisticsBlocks.UTILITY_DUCT.get(), base);
        FluidTankModels.generate(this);
    }
}
