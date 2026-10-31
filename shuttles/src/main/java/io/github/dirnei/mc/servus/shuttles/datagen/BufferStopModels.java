package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import io.github.dirnei.mc.servus.shuttles.rail.BufferStopBlock;
import io.github.dirnei.mc.servus.shuttles.rail.BufferStopLook;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;

/**
 * Datagen for the Buffer Stop: one model of the {@link BufferStopLook} boxes,
 * turned per facing. Sleepers use the wood board texture so
 * {@code MaterialModel} shows the stop's wood; the frame is steel on every
 * face, the beam carries the dock stripe and the stub the Warehouse Rail heads.
 */
final class BufferStopModels {

    private BufferStopModels() {
    }

    static void bufferStop(BlockStateProvider provider, Block block, String name) {
        Identifier steel = provider.modLoc("block/heavy_rack_iron");
        BlockModelBuilder model = provider.models().withExistingParent(name, "minecraft:block/block")
                .texture("particle", steel).texture("frame", steel)
                .texture("head", provider.modLoc("block/rail_head"))
                .texture("dock", provider.modLoc("block/rail_dock"))
                .texture("wood", MaterialKind.WOOD.placeholder());
        RailModels.addBoxes(model, BufferStopLook.boxes());

        provider.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(rotationY(state.getValue(BufferStopBlock.FACING)))
                .build());

        provider.itemModels().withExistingParent(name, provider.modLoc("block/" + name))
                .transforms()
                .transform(ItemDisplayContext.GUI).rotation(30, 225, 0).translation(0, 1, 0).scale(0.625F).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 0, 0).translation(0, 0, 0).scale(0.5F).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 0, 0).translation(0, 3, 0).scale(0.25F).end()
                .end();
    }

    /** The model faces north; a blockstate y rotation turns it clockwise. */
    private static int rotationY(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }
}
