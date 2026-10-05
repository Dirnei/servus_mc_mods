package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusStorageBlockStateProvider extends BlockStateProvider {

    ServusStorageBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusStorageIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        oriented(ServusStorageBlocks.CRATE.get(), "crate");
        oriented(ServusStorageBlocks.RACK.get(), "rack");
        oriented(ServusStorageBlocks.WAREHOUSE_CONTROLLER.get(), "warehouse_controller");
        oriented(ServusStorageBlocks.COUNTER_TERMINAL.get(), "counter_terminal");
    }

    /** Horizontal block with its own front texture, shared side and top. */
    private void oriented(Block block, String name) {
        ModelFile model = models().orientable(name, modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front"), modLoc("block/" + name + "_top"));
        horizontalBlock(block, model);
        simpleBlockItem(block, model);
    }
}
