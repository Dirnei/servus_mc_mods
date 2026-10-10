package io.github.dirnei.mc.servus.manufacturing.datagen;

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
        // Alchemy is masonry: vanilla stone on the sides and top, our own fronts.
        new CalcinerModels(this).register();
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

    /**
     * The Athanor: unformed, a stone brick block with a front per tier, lit or not; formed, a brick
     * tower furnace with an iron fire door showing the tier's flame, an iron plate on top for the item
     * and copper sockets for the pipes the renderer draws to the tanks. Both turned by facing.
     */
    private void athanor() {
        ResourceLocation side = mcLoc("block/stone_bricks");
        ResourceLocation top = mcLoc("block/chiseled_stone_bricks");
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
                .rotationY(((int) state.getValue(MachineBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(ServusManufacturingBlocks.ATHANOR.get(), models[0][0]);
    }

    /** The tower, front to the north; the player's right is west. The door shows the arch of the front texture. */
    private ModelFile athanorTower(String name, ResourceLocation front) {
        BlockModelBuilder model = models().withExistingParent(name, mcLoc("block/block"))
                .texture("stone", mcLoc("block/stone_bricks"))
                .texture("iron", mcLoc("block/iron_block")).texture("copper", mcLoc("block/cut_copper"))
                .texture("front", front).texture("particle", mcLoc("block/stone_bricks"));
        // The tall faces have no neighbour to take light from; ambient occlusion would darken them.
        model.ao(false);
        CalcinerModels.uvBox(model, 0, 0, 0, 16, 2, 16, "#stone");
        CalcinerModels.uvBox(model, 1, 2, 1, 15, 16, 15, "#stone");
        CalcinerModels.uvBox(model, 1.5F, 16, 1.5F, 14.5F, 20, 14.5F, "#stone");
        CalcinerModels.uvBox(model, 2.5F, 20, 2.5F, 13.5F, 22, 13.5F, "#stone");
        CalcinerModels.uvBox(model, 2, 22, 2, 14, 23, 14, "#iron");
        // The fire door: an iron frame around the flame arch of the tier's front texture.
        model.element().from(4, 3, 0.5F).to(12, 13, 1)
                .allFaces((direction, face) -> face.uvs(0, 0, 1, 1).texture("#iron"))
                .face(Direction.NORTH).uvs(4, 5, 12, 15).texture("#front").end()
                .end();
        CalcinerModels.uvBox(model, 3.25F, 2.5F, 0.25F, 4, 13.75F, 1, "#iron");
        CalcinerModels.uvBox(model, 12, 2.5F, 0.25F, 12.75F, 13.75F, 1, "#iron");
        CalcinerModels.uvBox(model, 4, 13, 0.25F, 12, 13.75F, 1, "#iron");
        // Sockets for the pipes, at the height the renderer draws them (1.2 to 1.45 blocks).
        CalcinerModels.uvBox(model, 0, 19.2F, 6, 2.5F, 23.2F, 10, "#copper");
        CalcinerModels.uvBox(model, 13.5F, 19.2F, 6, 16, 23.2F, 10, "#copper");
        CalcinerModels.uvBox(model, 6, 19.2F, 13.5F, 10, 23.2F, 16, "#copper");
        return model;
    }

    /** Plain stone to the common eye: the vein and its gnome are drawn only under Elemental Sight. */
    private void gnomeHollow() {
        ModelFile model = models().cubeAll("gnome_hollow", mcLoc("block/stone"));
        getVariantBuilder(ServusManufacturingBlocks.GNOME_HOLLOW.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());
        simpleBlockItem(ServusManufacturingBlocks.GNOME_HOLLOW.get(), model);
    }

    /** Orientable machine with a lit and an unlit front, rotated by facing. */
    private void machine(Block block, String name, ResourceLocation side, ResourceLocation top) {
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
