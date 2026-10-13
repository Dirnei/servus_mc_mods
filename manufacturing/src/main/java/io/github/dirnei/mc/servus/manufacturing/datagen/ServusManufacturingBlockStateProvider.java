package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlock;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

final class ServusManufacturingBlockStateProvider extends BlockStateProvider {

    ServusManufacturingBlockStateProvider(PackOutput output) {
        super(output, ServusManufacturingIds.MOD_ID);
    }

    @Override
    protected void registerStatesAndModels() {
        new AlchemyModels(this).register();
        simpleBlockWithItem(ServusManufacturingBlocks.SALT_CRYSTAL_BLOCK.get(), cubeAll(ServusManufacturingBlocks.SALT_CRYSTAL_BLOCK.get()));
        athanor();
        athanorCrown();
        gnomeHollow();
        for (Principle principle : Principle.values()) {
            PrincipleFluids.Entry entry = PrincipleFluids.entry(principle);
            simpleBlock(entry.block().get(), models().getBuilder(principle.id())
                    .texture("particle", mcLoc("block/water_still")));
        }
    }

    /**
     * The Athanor: unformed, a stone brick block with a front per tier, lit or not; formed, a brick
     * tower furnace with an iron fire door showing the tier's flame, an iron plate on top for the item
     * and copper collars for the ducts. Both turned by facing.
     */
    private void athanor() {
        Identifier side = mcLoc("block/stone_bricks");
        Identifier top = mcLoc("block/chiseled_stone_bricks");
        ModelFile[][] models = new ModelFile[4][2];
        ModelFile[][] formed = new ModelFile[4][2];
        for (int tier = 0; tier < 4; tier++) {
            models[tier][0] = models().orientable("athanor_" + tier, side, modLoc("block/athanor_front_" + tier), top);
            models[tier][1] = models().orientable("athanor_" + tier + "_lit", side, modLoc("block/athanor_front_" + tier + "_lit"), top);
            formed[tier][0] = athanorTower("athanor_formed_" + tier, modLoc("block/athanor_front_" + tier));
            formed[tier][1] = athanorTower("athanor_formed_" + tier + "_lit", modLoc("block/athanor_front_" + tier + "_lit"));
        }
        getVariantBuilder(ServusManufacturingBlocks.ATHANOR.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile((state.getValue(AthanorBlock.FORMED) ? formed : models)[state.getValue(AthanorBlock.TIER)][state.getValue(MachineBlock.LIT) ? 1 : 0])
                .rotationY(ModelRotations.rotationY(state.getValue(MachineBlock.FACING)))
                .build());
        simpleBlockItem(ServusManufacturingBlocks.ATHANOR.get(), models[0][0]);
    }

    /** The invisible crown above a formed Athanor: no faces, only the particle of its stone. */
    private void athanorCrown() {
        simpleBlock(ServusManufacturingBlocks.ATHANOR_CROWN.get(),
                models().getBuilder("athanor_crown").texture("particle", mcLoc("block/stone_bricks")));
    }

    /** The tower, front to the north; the player's right is west. The door shows the arch of the front texture. */
    private ModelFile athanorTower(String name, Identifier front) {
        BlockModelBuilder model = models().withExistingParent(name, mcLoc("block/block"))
                .texture("stone", mcLoc("block/stone_bricks"))
                .texture("iron", mcLoc("block/iron_block")).texture("copper", mcLoc("block/cut_copper"))
                .texture("front", front).texture("particle", mcLoc("block/stone_bricks"));
        // The tall faces have no neighbour to take light from; ambient occlusion would darken them.
        model.ao(false);
        AlchemyModels.uvBox(model, 0, 0, 0, 16, 2, 16, "#stone");
        AlchemyModels.uvBox(model, 1, 2, 1, 15, 16, 15, "#stone");
        AlchemyModels.uvBox(model, 1.5F, 16, 1.5F, 14.5F, 20, 14.5F, "#stone");
        AlchemyModels.uvBox(model, 2.5F, 20, 2.5F, 13.5F, 22, 13.5F, "#stone");
        AlchemyModels.uvBox(model, 2, 22, 2, 14, 23, 14, "#iron");
        model.element().from(4, 3, 0.5F).to(12, 13, 1)
                .allFaces((direction, face) -> face.uvs(0, 0, 1, 1).texture("#iron"))
                .face(Direction.NORTH).uvs(4, 5, 12, 15).texture("#front").end()
                .end();
        AlchemyModels.uvBox(model, 3.25F, 2.5F, 0.25F, 4, 13.75F, 1, "#iron");
        AlchemyModels.uvBox(model, 12, 2.5F, 0.25F, 12.75F, 13.75F, 1, "#iron");
        AlchemyModels.uvBox(model, 4, 13, 0.25F, 12, 13.75F, 1, "#iron");
        // Duct collars, a bit larger than the pipe so no face shares the shoulder's plane and flickers.
        AlchemyModels.uvBox(model, 0, 15.5F, 5.5F, 2.5F, 20.5F, 10.5F, "#copper");
        AlchemyModels.uvBox(model, 13.5F, 15.5F, 5.5F, 16, 20.5F, 10.5F, "#copper");
        AlchemyModels.uvBox(model, 5.5F, 15.5F, 13.5F, 10.5F, 20.5F, 16, "#copper");
        return model;
    }

    /** Plain stone to the common eye: the vein and its gnome are drawn only under Elemental Sight. */
    private void gnomeHollow() {
        ModelFile model = models().cubeAll("gnome_hollow", mcLoc("block/stone"));
        getVariantBuilder(ServusManufacturingBlocks.GNOME_HOLLOW.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());
        simpleBlockItem(ServusManufacturingBlocks.GNOME_HOLLOW.get(), model);
    }
}
