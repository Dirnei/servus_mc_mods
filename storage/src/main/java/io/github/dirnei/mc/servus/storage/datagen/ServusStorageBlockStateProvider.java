package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
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
        oriented(ServusStorageBlocks.ORDER_BOARD.get(), "order_board", "order_board");
        RailModels.rail(this, ServusStorageBlocks.RAIL.get(), "rail");
        ElevatorModels.elevator(this, ServusStorageBlocks.ELEVATOR.get(), "elevator");
        HeavyRackModels.rack(this, ServusStorageBlocks.HEAVY_RACK.get(), "heavy_rack");
        station(ServusStorageBlocks.INBOUND_STATION.get(), "inbound_station");
        garage(ServusStorageBlocks.SHUTTLE_GARAGE.get(), "shuttle_garage");
        station(ServusStorageBlocks.OUTBOUND_STATION.get(), "outbound_station");
        machine(ServusStorageBlocks.CRAFTING_STATION.get(), "crafting_station");
        itemModels().basicItem(ServusStorageItems.ZONE_LABEL.get());
        itemModels().getBuilder("shuttle").parent(new ModelFile.UncheckedModelFile("block/block"))
                .texture("particle", modLoc("item/shuttle")).texture("skin", modLoc("item/shuttle"))
                .element().from(2, 0, 2).to(14, 3, 14)
                .allFaces((direction, face) -> face.texture("#skin")).end();
    }

    /** Horizontal block with a lit front while it works. */
    private void machine(Block block, String name) {
        ModelFile dark = models().orientable(name, modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front"), modLoc("block/" + name + "_top"));
        ModelFile lit = models().orientable(name + "_lit", modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front_lit"), modLoc("block/" + name + "_top"));
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? lit : dark)
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(block, dark);
    }

    /** Like {@link #oriented} with an activity lamp: idle, working and blocked fronts while online. */
    private void station(Block block, String name) {
        ModelFile dark = models().orientable(name, modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front"), modLoc("block/" + name + "_top"));
        ModelFile idle = models().orientable(name + "_idle", modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front_idle"), modLoc("block/" + name + "_top"));
        ModelFile working = models().orientable(name + "_working", modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front_working"), modLoc("block/" + name + "_top"));
        ModelFile blocked = models().orientable(name + "_blocked", modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front_blocked"), modLoc("block/" + name + "_top"));
        getVariantBuilder(block).forAllStates(state -> {
            ModelFile model = dark;
            if (state.getValue(StorageNodeBlocks.ONLINE)) {
                model = switch (state.getValue(StorageNodeBlocks.ACTIVITY)) {
                    case IDLE -> idle;
                    case WORKING -> working;
                    case BLOCKED -> blocked;
                };
            }
            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360)
                    .build();
        });
        simpleBlockItem(block, dark);
    }

    /**
     * The garage: the lamp on the front, the doorway with the rail on the back,
     * the side texture left and right, the top texture above and below, and a
     * lit lamp while online. Rotated by the facing like {@link #oriented}.
     */
    private void garage(Block block, String name) {
        ModelFile dark = garageModel(name, name + "_front");
        ModelFile lit = garageModel(name + "_online", name + "_front_online");
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(StorageNodeBlocks.ONLINE) ? lit : dark)
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(block, dark);
    }

    private ModelFile garageModel(String model, String front) {
        ResourceLocation side = modLoc("block/shuttle_garage_side");
        ResourceLocation top = modLoc("block/shuttle_garage_top");
        return models().cube(model, top, top, modLoc("block/" + front), modLoc("block/shuttle_garage_back"), side, side)
                .texture("particle", top);
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
