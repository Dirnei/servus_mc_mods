package io.github.dirnei.mc.servus.foundation.datagen;

import static io.github.dirnei.mc.servus.foundation.datagen.MachineModels.box;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

/**
 * The Coal Generator's models: the dark steel cube per lit state with its own
 * front, side and top, the square chimney with its cap for the upper half, and
 * an item model with both halves.
 */
final class CoalGeneratorModels {

    private static final String NAME = "coal_generator";

    private CoalGeneratorModels() {
    }

    static void coalGenerator(BlockStateProvider provider) {
        Block block = MachineContent.COAL_GENERATOR.get();
        ModelFile unlit = cube(provider, NAME, "_front");
        ModelFile lit = cube(provider, NAME + "_lit", "_front_lit");
        BlockModelBuilder chimney = textured(provider, NAME + "_chimney");
        addChimney(chimney, 0);
        provider.getVariantBuilder(block).forAllStates(state -> {
            ModelFile model = CoalGeneratorBlock.isUpper(state) ? chimney : state.getValue(MachineBlock.LIT) ? lit : unlit;
            return ConfiguredModel.builder().modelFile(model)
                    .rotationY(MachineModels.rotationY(state.getValue(MachineBlock.FACING))).build();
        });
        provider.simpleBlockItem(block, item(provider));
    }

    private static ModelFile cube(BlockStateProvider provider, String name, String frontSuffix) {
        return provider.models().orientable(name, provider.modLoc("block/" + NAME + "_side"),
                provider.modLoc("block/" + NAME + frontSuffix), provider.modLoc("block/" + NAME + "_top"));
    }

    private static BlockModelBuilder textured(BlockStateProvider provider, String name) {
        return MachineModels.blockModel(provider, name)
                .texture("particle", provider.modLoc("block/" + NAME + "_side"))
                .texture("stack", provider.modLoc("block/" + NAME + "_stack"))
                .texture("top", provider.modLoc("block/" + NAME + "_top"))
                .texture("side", provider.modLoc("block/" + NAME + "_side"))
                .texture("front", provider.modLoc("block/" + NAME + "_front"));
    }

    /** The stack with its copper band, and the cap whose sides take the stack's dark rim and whose top shows the flue. */
    private static void addChimney(BlockModelBuilder model, float dy) {
        box(model, CoalGeneratorBlock.STACK, dy, "#stack", "#stack");
        box(model, CoalGeneratorBlock.CAP, dy, "#stack", "#stack")
                .face(Direction.UP).texture("#top").uvs(4, 4, 12, 12).end();
    }

    /** The cube and the chimney stacked. */
    private static ModelFile item(BlockStateProvider provider) {
        BlockModelBuilder model = textured(provider, NAME + "_item");
        model.element().from(0, 0, 0).to(16, 16, 16)
                .allFaces((direction, face) -> face.texture(direction == Direction.NORTH ? "#front"
                        : direction.getAxis() == Direction.Axis.Y ? "#top" : "#side").uvs(0, 0, 16, 16));
        addChimney(model, 16);
        MachineModels.twoBlockDisplay(model);
        return model;
    }
}
