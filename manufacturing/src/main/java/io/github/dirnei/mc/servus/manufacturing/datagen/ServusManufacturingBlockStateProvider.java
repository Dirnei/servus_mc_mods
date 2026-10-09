package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlock;
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
        machine(ServusManufacturingBlocks.CALCINER.get(), "calciner");
        machine(ServusManufacturingBlocks.COAGULATOR.get(), "coagulator");
        simpleBlockWithItem(ServusManufacturingBlocks.SALT_CRYSTAL_BLOCK.get(), cubeAll(ServusManufacturingBlocks.SALT_CRYSTAL_BLOCK.get()));
        athanor();
        gnomeHollow();
        // Liquid blocks are drawn by the fluid renderer; the model only gives the particle.
        for (Principle principle : Principle.values()) {
            PrincipleFluids.Entry entry = PrincipleFluids.entry(principle);
            simpleBlock(entry.block().get(), models().getBuilder(principle.id())
                    .texture("particle", mcLoc("block/water_still")));
        }
    }

    /** The Athanor: brick sides, a front per tier, lit or not, turned by facing. */
    private void athanor() {
        ResourceLocation side = modLoc("block/athanor_side");
        ResourceLocation top = modLoc("block/athanor_top");
        ModelFile[][] models = new ModelFile[4][2];
        for (int tier = 0; tier < 4; tier++) {
            models[tier][0] = models().orientable("athanor_" + tier, side, modLoc("block/athanor_front_" + tier), top);
            models[tier][1] = models().orientable("athanor_" + tier + "_lit", side, modLoc("block/athanor_front_" + tier + "_lit"), top);
        }
        getVariantBuilder(ServusManufacturingBlocks.ATHANOR.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models[state.getValue(AthanorBlock.TIER)][state.getValue(MachineBlock.LIT) ? 1 : 0])
                .rotationY(((int) state.getValue(MachineBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(ServusManufacturingBlocks.ATHANOR.get(), models[0][0]);
    }

    /** Plain stone to the common eye: the vein and its gnome are drawn only under Elemental Sight. */
    private void gnomeHollow() {
        ModelFile model = models().cubeAll("gnome_hollow", mcLoc("block/stone"));
        getVariantBuilder(ServusManufacturingBlocks.GNOME_HOLLOW.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());
        simpleBlockItem(ServusManufacturingBlocks.GNOME_HOLLOW.get(), model);
    }

    /** Orientable machine with a lit and an unlit front, rotated by facing. */
    private void machine(Block block, String name) {
        ResourceLocation side = ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "block/machine_side");
        ResourceLocation top = ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "block/machine_top");
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
