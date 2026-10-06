package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
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
        RailModels.rail(this, ServusStorageBlocks.RAIL.get(), "rail");
        ElevatorModels.elevator(this, ServusStorageBlocks.ELEVATOR.get(), "elevator", "rail");
    }

    /** Horizontal block with its own front texture, shared side and top, and a lit front while online. */
    private void oriented(Block block, String name) {
        ModelFile dark = models().orientable(name, modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front"), modLoc("block/" + name + "_top"));
        ModelFile lit = models().orientable(name + "_online", modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front_online"), modLoc("block/" + name + "_top"));
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(StorageNodeBlocks.ONLINE) ? lit : dark)
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(block, dark);
    }
}
