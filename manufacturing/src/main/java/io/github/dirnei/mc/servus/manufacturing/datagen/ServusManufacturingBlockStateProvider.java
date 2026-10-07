package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusManufacturingBlockStateProvider extends BlockStateProvider {

    ServusManufacturingBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusManufacturingIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        cubeAllWithItem(ServusManufacturingBlocks.IRON_DEPOSIT.get());
        cubeAllWithItem(ServusManufacturingBlocks.COPPER_DEPOSIT.get());
        cubeAllWithItem(ServusManufacturingBlocks.COAL_DEPOSIT.get());
        cubeAllWithItem(ServusManufacturingBlocks.STONE_DEPOSIT.get());

        machine(ServusManufacturingBlocks.BURNER_EXTRACTOR.get(), "burner_extractor");
        machine(ServusManufacturingBlocks.BURNER_SMELTER.get(), "burner_smelter");
        machine(ServusManufacturingBlocks.COAL_GENERATOR.get(), "coal_generator");
        machine(ServusManufacturingBlocks.ASSEMBLER.get(), "assembler");
        machine(ServusManufacturingBlocks.RESEARCH_LAB.get(), "research_lab");
        machine(ServusManufacturingBlocks.ELECTRIC_EXTRACTOR.get(), "electric_extractor");
        machine(ServusManufacturingBlocks.CRUSHER.get(), "crusher");
        machine(ServusManufacturingBlocks.CHUNK_LOADER.get(), "chunk_loader");
        pump(ServusManufacturingBlocks.BURNER_PUMP.get(), "burner_pump");
        pump(ServusManufacturingBlocks.ELECTRIC_PUMP.get(), "electric_pump");
    }

    private void cubeAllWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }

    /** The compact pump: base plate, body with the front, outlet; lit and unlit, rotated by facing. */
    private void pump(Block block, String name) {
        ModelFile unlit = pumpModel(name, modLoc("block/" + name + "_front"));
        ModelFile lit = pumpModel(name + "_lit", modLoc("block/" + name + "_front_lit"));
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(MachineBlock.FACING);
            boolean isLit = state.getValue(MachineBlock.LIT);
            return ConfiguredModel.builder()
                    .modelFile(isLit ? lit : unlit)
                    .rotationY(((int) facing.toYRot() + 180) % 360)
                    .build();
        });
        simpleBlockItem(block, unlit);
    }

    private ModelFile pumpModel(String name, ResourceLocation front) {
        BlockModelBuilder model = models().withExistingParent(name, mcLoc("block/block"))
                .texture("particle", modLoc("block/machine_side"))
                .texture("side", modLoc("block/machine_side"))
                .texture("top", modLoc("block/machine_top"))
                .texture("front", front);
        model.element().from(1, 0, 1).to(15, 2, 15)
                .allFaces((direction, face) -> face.texture(direction.getAxis() == Direction.Axis.Y ? "#top" : "#side")
                        .uvs(1, 14, 15, 16))
                .face(Direction.UP).uvs(1, 1, 15, 15).end()
                .face(Direction.DOWN).uvs(1, 1, 15, 15).cullface(Direction.DOWN).end();
        model.element().from(3, 2, 3).to(13, 12, 13)
                .allFaces((direction, face) -> face.texture(direction.getAxis() == Direction.Axis.Y ? "#top" : "#side")
                        .uvs(3, 4, 13, 14))
                .face(Direction.NORTH).texture("#front").uvs(0, 0, 16, 16).end()
                .face(Direction.UP).uvs(3, 3, 13, 13).end();
        model.element().from(6, 12, 6).to(10, 15, 10)
                .allFaces((direction, face) -> face.texture(direction.getAxis() == Direction.Axis.Y ? "#top" : "#side")
                        .uvs(6, 1, 10, 4))
                .face(Direction.UP).uvs(6, 6, 10, 10).end()
                .face(Direction.DOWN).uvs(6, 6, 10, 10).end();
        return model;
    }

    /** Orientable machine with a lit and an unlit front, rotated by facing. */
    private void machine(Block block, String name) {
        ResourceLocation side = modLoc("block/machine_side");
        ResourceLocation top = modLoc("block/machine_top");
        ModelFile unlit = models().orientable(name, side, modLoc("block/" + name + "_front"), top);
        ModelFile lit = models().orientable(name + "_lit", side, modLoc("block/" + name + "_front_lit"), top);
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(MachineBlock.FACING);
            boolean isLit = state.getValue(MachineBlock.LIT);
            return ConfiguredModel.builder()
                    .modelFile(isLit ? lit : unlit)
                    .rotationY(((int) facing.toYRot() + 180) % 360)
                    .build();
        });
        simpleBlockItem(block, unlit);
    }
}
