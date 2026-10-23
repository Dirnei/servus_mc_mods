package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlock;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RubedoObeliskBlock;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

final class ServusAlchemyBlockStateProvider extends BlockStateProvider {

    private static final String STEEL = "servus_foundation:block/electric_pump_steel";
    private static final String DARK_STEEL = "servus_foundation:block/coal_generator_side";

    ServusAlchemyBlockStateProvider(PackOutput output) {
        super(output, ServusAlchemyIds.MOD_ID);
    }

    @Override
    protected void registerStatesAndModels() {
        new AlchemyModels(this).register();
        simpleBlockWithItem(ServusAlchemyBlocks.SALT_CRYSTAL_BLOCK.get(), cubeAll(ServusAlchemyBlocks.SALT_CRYSTAL_BLOCK.get()));
        athanor();
        athanorCrown();
        rubedoObelisk();
        gnomeHollow();
        ancientHollow();
        for (Principle principle : Principle.values()) {
            PrincipleFluids.Entry entry = PrincipleFluids.entry(principle);
            simpleBlock(entry.block().get(), models().getBuilder(principle.id())
                    .texture("particle", mcLoc("block/water_still")));
        }
    }

    /**
     * The Athanor: unformed, a stone brick block with a front per tier, lit or not; formed, a brick
     * tower furnace with a black iron fire door showing the tier's flame, vents glowing in it, a steel
     * plate on top for the item and copper flanges for the ducts. Both turned by facing.
     */
    private void athanor() {
        Identifier side = mcLoc("block/stone_bricks");
        Identifier top = mcLoc("block/chiseled_stone_bricks");
        ModelFile[][] models = new ModelFile[4][2];
        ModelFile[][] formed = new ModelFile[4][2];
        for (int tier = 0; tier < 4; tier++) {
            models[tier][0] = models().orientable("athanor_" + tier, side, modLoc("block/athanor_front_" + tier), top);
            models[tier][1] = models().orientable("athanor_" + tier + "_lit", side, modLoc("block/athanor_front_" + tier + "_lit"), top);
            formed[tier][0] = athanorTower("athanor_formed_" + tier, modLoc("block/athanor_fire_" + tier), false);
            formed[tier][1] = athanorTower("athanor_formed_" + tier + "_lit", modLoc("block/athanor_fire_" + tier), true);
        }
        getVariantBuilder(ServusAlchemyBlocks.ATHANOR.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile((state.getValue(AthanorBlock.FORMED) ? formed : models)[state.getValue(AthanorBlock.TIER)][state.getValue(MachineBlock.LIT) ? 1 : 0])
                .rotationY(ModelRotations.rotationY(state.getValue(MachineBlock.FACING)))
                .build());
        simpleBlockItem(ServusAlchemyBlocks.ATHANOR.get(), models[0][0]);
    }

    /** The invisible crown above a formed Athanor: no faces, only the particle of its stone. */
    private void athanorCrown() {
        simpleBlock(ServusAlchemyBlocks.ATHANOR_CROWN.get(),
                models().getBuilder("athanor_crown").texture("particle", mcLoc("block/stone_bricks")));
    }

    /**
     * The Rubedo Obelisk: the lower half is a plinth of the Obsidian it grew out of, the crystal is drawn by its renderer;
     * the upper half has no faces. Both break with the shell's particles.
     */
    private void rubedoObelisk() {
        Identifier shell = modLoc("block/rubedo_obelisk");
        BlockModelBuilder lower = models().withExistingParent("rubedo_obelisk_lower", mcLoc("block/block"))
                .texture("obsidian", mcLoc("block/obsidian")).texture("particle", shell);
        AlchemyModels.uvBox(lower, 0, 0, 0, 16, RubedoObeliskBlock.PLINTH, 16, "#obsidian");
        ModelFile upper = models().getBuilder("rubedo_obelisk_upper").texture("particle", shell);
        getVariantBuilder(ServusAlchemyBlocks.RUBEDO_OBELISK.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(RubedoObeliskBlock.HALF) == DoubleBlockHalf.LOWER ? lower : upper)
                .build());
    }

    /**
     * The tower, front to the north; the player's right is west. A brick plinth and body with steel corner posts and
     * two copper bands, a black iron fire door in a steel frame with a window onto the stage flame, copper flanges where
     * the ducts meet it, a stepped brick and copper dome with a vent on each side and the steel plate for the item.
     * The flame and the vents glow at full light while it burns.
     */
    private ModelFile athanorTower(String name, Identifier fire, boolean lit) {
        BlockModelBuilder model = models().withExistingParent(name, mcLoc("block/block"))
                .texture("bricks", mcLoc("block/bricks")).texture("steel", STEEL).texture("door", DARK_STEEL)
                .texture("copper", mcLoc("block/cut_copper")).texture("fire", fire)
                .texture("particle", mcLoc("block/bricks"));
        // The tall faces have no neighbour to take light from; ambient occlusion would darken them.
        model.ao(false);
        AlchemyModels.uvBox(model, 0, 0, 0, 16, 3, 16, "#bricks");
        onPlinth(model, 1.5F, 1.5F, 14.5F, 17, 14.5F, "#bricks");
        for (float x : new float[] {1, 13.5F}) {
            for (float z : new float[] {1, 13.5F}) {
                onPlinth(model, x, z, x + 1.5F, 17.25F, z + 1.5F, "#steel");
            }
        }
        onPlinth(model, 0.9F, 0.9F, 15.1F, 4, 15.1F, "#copper");
        AlchemyModels.uvBox(model, 0.9F, 15, 0.9F, 15.1F, 16, 15.1F, "#copper");
        door(model);
        AlchemyModels.uvBox(model, 0, 5.5F, 5.5F, 1.5F, 10.5F, 10.5F, "#copper");
        AlchemyModels.uvBox(model, 14.5F, 5.5F, 5.5F, 16, 10.5F, 10.5F, "#copper");
        AlchemyModels.uvBox(model, 5.5F, 5.5F, 14.5F, 10.5F, 10.5F, 16, "#copper");
        AlchemyModels.uvBox(model, 2.5F, 17, 2.5F, 13.5F, 19, 13.5F, "#bricks");
        vent(model, lit, 6, 2.3F, 10, 2.5F, Direction.SOUTH);
        vent(model, lit, 6, 13.5F, 10, 13.7F, Direction.NORTH);
        vent(model, lit, 2.3F, 6, 2.5F, 10, Direction.EAST);
        vent(model, lit, 13.5F, 6, 13.7F, 10, Direction.WEST);
        AlchemyModels.uvBox(model, 4, 19, 4, 12, 21, 12, "#copper");
        AlchemyModels.uvBox(model, 5.5F, 21, 5.5F, 10.5F, 22.5F, 10.5F, "#copper");
        AlchemyModels.uvBox(model, 3.5F, 22.5F, 3.5F, 12.5F, 23.5F, 12.5F, "#steel");
        model.element().from(6, 7, 0.75F).to(10, 10.5F, 0.78F).lightEmission(lit ? 15 : 0)
                .face(Direction.NORTH).uvs(0, 0, 16, 12).texture("#fire").end()
                .end();
        return model;
    }

    /** A box standing on the plinth; it has no bottom face, which would lie on the plinth's top with the others standing there. */
    private static void onPlinth(BlockModelBuilder model, float x1, float z1, float x2, float y2, float z2, String texture) {
        float dx = x2 - x1;
        float dy = Math.min(16, y2 - 3);
        float dz = z2 - z1;
        model.element().from(x1, 3, z1).to(x2, y2, z2).allFacesExcept((direction, face) -> {
            switch (direction.getAxis()) {
                case X -> face.uvs(0, 0, dz, dy);
                case Y -> face.uvs(0, 0, dx, dz);
                case Z -> face.uvs(0, 0, dx, dy);
            }
            face.texture(texture);
        }, Set.of(Direction.DOWN)).end();
    }

    /** The fire door: a steel frame, the black iron plate around the window, the window's steel lips and a handle. */
    private static void door(BlockModelBuilder model) {
        AlchemyModels.uvBox(model, 4, 4, 0.8F, 12, 12.5F, 1.5F, "#steel");
        AlchemyModels.uvBox(model, 4.5F, 4.5F, 0.6F, 6, 12, 0.8F, "#door");
        AlchemyModels.uvBox(model, 10, 4.5F, 0.6F, 11.5F, 12, 0.8F, "#door");
        AlchemyModels.uvBox(model, 6, 10.5F, 0.6F, 10, 12, 0.8F, "#door");
        AlchemyModels.uvBox(model, 6, 4.5F, 0.6F, 10, 7, 0.8F, "#door");
        AlchemyModels.uvBox(model, 5.6F, 6.6F, 0.45F, 10.4F, 7, 0.6F, "#steel");
        AlchemyModels.uvBox(model, 5.6F, 10.5F, 0.45F, 10.4F, 10.9F, 0.6F, "#steel");
        AlchemyModels.uvBox(model, 5.6F, 7, 0.45F, 6, 10.5F, 0.6F, "#steel");
        AlchemyModels.uvBox(model, 10, 7, 0.45F, 10.4F, 10.5F, 0.6F, "#steel");
        AlchemyModels.uvBox(model, 10.6F, 7.5F, 0.3F, 11.2F, 10, 0.6F, "#steel");
    }

    /** A dome vent from {@code (x1, z1)} to {@code (x2, z2)}, showing the vent rows of the fire texture; {@code inward} lies on the dome. */
    private static void vent(BlockModelBuilder model, boolean lit, float x1, float z1, float x2, float z2, Direction inward) {
        model.element().from(x1, 17.4F, z1).to(x2, 18.6F, z2).lightEmission(lit ? 15 : 0)
                .allFacesExcept((direction, face) -> face.uvs(0, 12, 16, 16).texture("#fire"), Set.of(inward))
                .end();
    }

    /** Plain stone to the common eye: the vein and its gnome are drawn only under Elemental Sight. */
    private void gnomeHollow() {
        ModelFile model = models().cubeAll("gnome_hollow", mcLoc("block/stone"));
        getVariantBuilder(ServusAlchemyBlocks.GNOME_HOLLOW.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());
        simpleBlockItem(ServusAlchemyBlocks.GNOME_HOLLOW.get(), model);
    }

    /** Deepslate to the common eye: the old gnome is drawn only under Elemental Sight. */
    private void ancientHollow() {
        ModelFile model = models().withExistingParent("ancient_hollow", mcLoc("block/cube_column"))
                .texture("side", mcLoc("block/deepslate")).texture("end", mcLoc("block/deepslate_top"));
        getVariantBuilder(ServusAlchemyBlocks.ANCIENT_HOLLOW.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());
    }
}
