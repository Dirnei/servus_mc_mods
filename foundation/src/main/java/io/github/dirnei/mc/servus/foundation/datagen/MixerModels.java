package io.github.dirnei.mc.servus.foundation.datagen;

import static io.github.dirnei.mc.servus.foundation.datagen.MachineModels.blockModel;
import static io.github.dirnei.mc.servus.foundation.datagen.MachineModels.box;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.mixer.MixerParts;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

/**
 * The Mixer's models, built facing north from {@link MixerParts}: the block (base, tub, rim, bridge, chute and port),
 * the paddle (hub, shaft and two blades) for the renderer, and an item model with the paddle at rest.
 */
final class MixerModels {

    private static final String NAME = "mixer";
    /** Parts that rest on another part: no down faces. */
    private static final Set<Direction> RESTING = Set.of(Direction.DOWN);

    private MixerModels() {
    }

    static void mixer(BlockStateProvider provider) {
        Block block = MachineContent.MIXER.get();
        BlockModelBuilder model = part(provider, NAME);
        addStatic(model);
        provider.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(ModelRotations.rotationY(state.getValue(MachineBlock.FACING)))
                .build());
        addPaddle(part(provider, NAME + "_paddle"));
        provider.simpleBlockItem(block, item(provider));
    }

    private static BlockModelBuilder part(BlockStateProvider provider, String name) {
        return blockModel(provider, name)
                .texture("particle", provider.modLoc("block/electric_pump_steel"))
                .texture("heavy", provider.modLoc("block/heavy_iron"))
                .texture("steel", provider.modLoc("block/electric_pump_steel"))
                .texture("brass", provider.modLoc("block/brass"))
                .texture("chute", provider.modLoc("block/mill_chute"))
                .texture("port", provider.modLoc("block/mixer_port"));
    }

    private static void addStatic(BlockModelBuilder model) {
        box(model, MixerParts.BASE, 0, "#heavy", "#heavy");
        box(model, MixerParts.FLOOR, 0, "#steel", "#steel", RESTING);
        for (float[] wall : new float[][] {MixerParts.WALL_FRONT, MixerParts.WALL_BACK, MixerParts.WALL_LEFT, MixerParts.WALL_RIGHT}) {
            box(model, wall, 0, "#steel", "#steel", RESTING);
        }
        for (float[] rim : new float[][] {MixerParts.RIM_FRONT, MixerParts.RIM_BACK, MixerParts.RIM_LEFT, MixerParts.RIM_RIGHT}) {
            box(model, rim, 0, "#brass", "#brass");
        }
        box(model, MixerParts.BRIDGE, 0, "#steel", "#steel", RESTING);
        // The mouths are painted at fixed pixels of their textures; these faces map exactly those pixels.
        box(model, MixerParts.CHUTE, 0, "#steel", "#steel", Set.of(Direction.SOUTH))
                .face(Direction.NORTH).texture("#chute").uvs(6, 11.5F, 10, 14.5F).end();
        box(model, MixerParts.PORT, 0, "#brass", "#brass", Set.of(Direction.NORTH))
                .face(Direction.SOUTH).texture("#port").uvs(6.75F, 6.5F, 9.25F, 9.5F).end();
    }

    private static void addPaddle(BlockModelBuilder model) {
        box(model, MixerParts.HUB, 0, "#brass", "#brass", RESTING);
        box(model, MixerParts.SHAFT, 0, "#steel", "#steel");
        box(model, MixerParts.BLADE_A, 0, "#heavy", "#heavy");
        box(model, MixerParts.BLADE_B, 0, "#heavy", "#heavy");
    }

    /** The whole Mixer with the paddle at rest, as the renderer draws it. */
    private static ModelFile item(BlockStateProvider provider) {
        BlockModelBuilder model = part(provider, NAME + "_item");
        addStatic(model);
        addPaddle(model);
        return model;
    }
}
