package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
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
        oriented(ServusStorageBlocks.CRATE.get(), "crate", "crate");
        oriented(ServusStorageBlocks.DOUBLE_CRATE.get(), "double_crate", "crate");
        oriented(ServusStorageBlocks.QUAD_CRATE.get(), "quad_crate", "crate");
        oriented(ServusStorageBlocks.WAREHOUSE_CONTROLLER.get(), "warehouse_controller", "warehouse_controller");
        oriented(ServusStorageBlocks.COUNTER_TERMINAL.get(), "counter_terminal", "counter_terminal");
        RailModels.rail(this, ServusStorageBlocks.RAIL.get(), "rail");
        ElevatorModels.elevator(this, ServusStorageBlocks.ELEVATOR.get(), "elevator", "rail");
        HeavyRackModels.rack(this, ServusStorageBlocks.HEAVY_RACK.get(), "heavy_rack");
        station(ServusStorageBlocks.INBOUND_STATION.get(), "inbound_station");
        oriented(ServusStorageBlocks.SHUTTLE_GARAGE.get(), "shuttle_garage", "shuttle_garage");
        itemModels().basicItem(ServusStorageItems.ZONE_LABEL.get());
        itemModels().getBuilder("shuttle").parent(new ModelFile.UncheckedModelFile("block/block"))
                .texture("particle", modLoc("item/shuttle")).texture("skin", modLoc("item/shuttle"))
                .element().from(2, 0, 2).to(14, 3, 14)
                .allFaces((direction, face) -> face.texture("#skin")).end();
    }

    /** Like {@link #oriented} with a third, red front while the station is backed up. */
    private void station(Block block, String name) {
        ModelFile dark = models().orientable(name, modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front"), modLoc("block/" + name + "_top"));
        ModelFile lit = models().orientable(name + "_online", modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front_online"), modLoc("block/" + name + "_top"));
        ModelFile red = models().orientable(name + "_backed_up", modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front_backed_up"), modLoc("block/" + name + "_top"));
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(StorageNodeBlocks.BACKED_UP) ? red : state.getValue(StorageNodeBlocks.ONLINE) ? lit : dark)
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(block, dark);
    }

    /**
     * Horizontal block with its own front texture, side and top textures named
     * after {@code body}, and a lit front while online.
     */
    private void oriented(Block block, String name, String body) {
        ModelFile dark = models().orientable(name, modLoc("block/" + body + "_side"),
                modLoc("block/" + name + "_front"), modLoc("block/" + body + "_top"));
        ModelFile lit = models().orientable(name + "_online", modLoc("block/" + body + "_side"),
                modLoc("block/" + name + "_front_online"), modLoc("block/" + body + "_top"));
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(StorageNodeBlocks.ONLINE) ? lit : dark)
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(block, dark);
    }
}
