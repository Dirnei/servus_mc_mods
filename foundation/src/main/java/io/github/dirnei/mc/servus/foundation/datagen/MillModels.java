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
import io.github.dirnei.mc.servus.foundation.machine.mill.MillParts;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

/**
 * The Mill's models, built facing north from {@link MillParts}: the block
 * (base, housing, hopper with its brass rim, chute), the wheel's rim segment,
 * spoke and brass hub for the renderer, and an item model with the wheel at rest.
 */
final class MillModels {

    private static final String NAME = "mill";
    /** Wheel parts rest on the housing: no down faces. */
    private static final Set<Direction> RESTING = Set.of(Direction.DOWN);

    private MillModels() {
    }

    static void mill(BlockStateProvider provider) {
        Block block = MachineContent.MILL.get();
        BlockModelBuilder model = part(provider, NAME);
        addStatic(model);
        provider.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(ModelRotations.rotationY(state.getValue(MachineBlock.FACING)))
                .build());
        box(part(provider, NAME + "_rim"), MillParts.RIM_SEGMENT, 0, "#heavy", "#heavy", RESTING);
        box(part(provider, NAME + "_spoke"), MillParts.SPOKE, 0, "#heavy", "#heavy", RESTING);
        addHub(part(provider, NAME + "_hub"));
        provider.simpleBlockItem(block, item(provider));
    }

    private static BlockModelBuilder part(BlockStateProvider provider, String name) {
        return blockModel(provider, name)
                .texture("particle", provider.modLoc("block/electric_pump_steel"))
                .texture("heavy", provider.modLoc("block/heavy_iron"))
                .texture("steel", provider.modLoc("block/electric_pump_steel"))
                .texture("brass", provider.modLoc("block/brass"))
                .texture("hopper", provider.modLoc("block/mill_hopper_top"))
                .texture("chute", provider.modLoc("block/mill_chute"));
    }

    private static void addStatic(BlockModelBuilder model) {
        box(model, MillParts.BASE, 0, "#heavy", "#heavy");
        box(model, MillParts.HOUSING, 0, "#steel", "#steel");
        box(model, MillParts.HOPPER, 0, "#steel", "#steel");
        box(model, MillParts.HOPPER_RIM, 0, "#brass", "#brass").face(Direction.UP).texture("#hopper").end();
        box(model, MillParts.CHUTE, 0, "#steel", "#steel").face(Direction.NORTH).texture("#chute").end();
    }

    /** The hub twice, the second turned 45 degrees about the wheel's axis so it reads round. */
    private static void addHub(BlockModelBuilder model) {
        box(model, MillParts.HUB, 0, "#brass", "#brass", RESTING);
        turned(model, MillParts.HUB_TURNED, "#brass", 45);
    }

    /** A wheel part turned about the wheel's axis by an element rotation, for the item model. */
    private static void turned(BlockModelBuilder model, float[] b, String texture, float degrees) {
        box(model, b, 0, texture, texture, RESTING).rotation()
                .origin(MillParts.AXIS_X, MillParts.WHEEL_Y, MillParts.AXIS_Z)
                .axis(Direction.Axis.Y).angle(degrees).end();
    }

    /** The whole mill with the wheel at rest, as the renderer draws it. */
    private static ModelFile item(BlockStateProvider provider) {
        BlockModelBuilder model = part(provider, NAME + "_item");
        addStatic(model);
        addHub(model);
        for (int i = 0; i < MillParts.SPOKES; i++) {
            turned(model, MillParts.SPOKE, "#heavy", 360.0F * i / MillParts.SPOKES);
        }
        for (int i = 0; i < MillParts.RIM_SEGMENTS; i++) {
            turned(model, MillParts.rimSegment(i), "#heavy", 360.0F * i / MillParts.RIM_SEGMENTS);
        }
        return model;
    }
}
