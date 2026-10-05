package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
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
}
