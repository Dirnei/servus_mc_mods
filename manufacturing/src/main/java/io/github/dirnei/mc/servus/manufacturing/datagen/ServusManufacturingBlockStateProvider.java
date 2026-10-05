package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.connectable.ConnectableBlock;
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
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
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

        connectable(ServusManufacturingBlocks.POWER_CABLE.get(), "power_cable", 3);
        connectable(ServusManufacturingBlocks.ITEM_DUCT.get(), "item_duct", 4);
        connectable(ServusManufacturingBlocks.SPLITTER_DUCT.get(), "splitter_duct", 4);
        connectable(ServusManufacturingBlocks.FILTER_DUCT.get(), "filter_duct", 4);
    }

    private void cubeAllWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
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

    /** Multipart cable or duct: a centre cube plus one arm per connected side. */
    private void connectable(Block block, String name, int radius) {
        int lo = 8 - radius;
        int hi = 8 + radius;
        ResourceLocation texture = modLoc("block/" + name);
        BlockModelBuilder center = models().getBuilder(name + "_center")
                .texture("particle", texture).texture("skin", texture)
                .element().from(lo, lo, lo).to(hi, hi, hi)
                .allFaces((direction, face) -> face.texture("#skin")).end();
        BlockModelBuilder arm = models().getBuilder(name + "_arm")
                .texture("particle", texture).texture("skin", texture)
                .element().from(lo, lo, 0).to(hi, hi, lo)
                .allFaces((direction, face) -> face.texture("#skin")).end();
        BlockModelBuilder inventory = models().getBuilder(name + "_inventory")
                .texture("particle", texture).texture("skin", texture)
                .element().from(lo, lo, 0).to(hi, hi, 16)
                .allFaces((direction, face) -> face.texture("#skin")).end();

        MultiPartBlockStateBuilder builder = getMultipartBuilder(block);
        builder.part().modelFile(center).addModel();
        builder.part().modelFile(arm).addModel().condition(ConnectableBlock.NORTH, true);
        builder.part().modelFile(arm).rotationY(90).addModel().condition(ConnectableBlock.EAST, true);
        builder.part().modelFile(arm).rotationY(180).addModel().condition(ConnectableBlock.SOUTH, true);
        builder.part().modelFile(arm).rotationY(270).addModel().condition(ConnectableBlock.WEST, true);
        builder.part().modelFile(arm).rotationX(270).addModel().condition(ConnectableBlock.UP, true);
        builder.part().modelFile(arm).rotationX(90).addModel().condition(ConnectableBlock.DOWN, true);

        simpleBlockItem(block, inventory);
    }
}
