package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.brewhouse.BrewKettleBlock;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseBlockEntity;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseBricksBlock;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Cell;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Kind;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseModel;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhousePart;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhousePartBlock;
import io.github.dirnei.mc.servus.brewery.brewhouse.ModelSlicer;
import io.github.dirnei.mc.servus.brewery.brewhouse.ModelSlicer.Piece;
import io.github.dirnei.mc.servus.brewery.brewhouse.ModelSlicer.Side;
import io.github.dirnei.mc.servus.brewery.brewhouse.TrayLook;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlocks;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Brewhouse models: every formed cell is built from the pieces {@link ModelSlicer} cuts out of
 * {@link BrewhouseModel}, with the door cold or lit and the tray by its look, and with a Heating Element the coil doors,
 * the thermometer and the power port; loose parts are full cubes.
 */
final class BrewhouseModels {

    private static final Identifier HEAVY_IRON = Identifier.fromNamespaceAndPath("servus_foundation", "block/heavy_iron");

    private final BlockStateProvider provider;
    private final Identifier bricks;
    private final Identifier copper;

    BrewhouseModels(BlockStateProvider provider) {
        this.provider = provider;
        this.bricks = provider.mcLoc("block/bricks");
        this.copper = provider.modLoc("block/brew_kettle_copper");
    }

    void register() {
        bricksBlock();
        kettleBlock();
    }

    private void bricksBlock() {
        ModelFile loose = provider.models().cube("brewhouse_bricks", HEAVY_IRON, HEAVY_IRON, bricks, bricks, bricks, bricks)
                .texture("particle", bricks);
        Map<String, ModelFile> formed = new HashMap<>();
        BrewhouseBricksBlock block = ServusBreweryBlocks.BREWHOUSE_BRICKS.get();
        provider.getVariantBuilder(block).forAllStates(state -> {
            if (!state.getValue(BrewhousePartBlock.FORMED)) {
                return new ConfiguredModel[] {new ConfiguredModel(loose)};
            }
            Cell cell = state.getValue(BrewhouseBricksBlock.PART).cell();
            boolean lit = cell.hasDoor() && state.getValue(BrewhouseBricksBlock.LIT);
            TrayLook look = cell.isKiln() ? state.getValue(BrewhouseBricksBlock.TRAY) : null;
            boolean heater = state.getValue(BrewhouseBricksBlock.HEATER) && (cell.hasDoor() || cell == BrewhouseBlockEntity.PORT_CELL);
            String name = "brewhouse_bricks_" + key(cell) + (lit ? "_lit" : "") + (look == null ? "" : "_" + look.getSerializedName())
                    + (heater ? "_heater" : "");
            ModelFile model = formed.computeIfAbsent(name, unused -> brickModel(name, cell, lit, look, heater));
            return turned(model, state);
        });
        provider.simpleBlockItem(block, loose);
    }

    private void kettleBlock() {
        ModelFile loose = provider.models().cubeAll("brew_kettle", copper);
        Map<Cell, ModelFile> formed = new EnumMap<>(Cell.class);
        for (Cell cell : Cell.values()) {
            if (cell.kind() == Kind.KETTLE) {
                formed.put(cell, cellModel("brew_kettle_" + key(cell), cell, copper, BrewhouseModel.PARTS));
            }
        }
        BrewKettleBlock block = ServusBreweryBlocks.BREW_KETTLE.get();
        provider.getVariantBuilder(block).forAllStates(state -> state.getValue(BrewhousePartBlock.FORMED)
                ? turned(formed.get(state.getValue(BrewKettleBlock.PART).cell()), state)
                : new ConfiguredModel[] {new ConfiguredModel(loose)});
        provider.simpleBlockItem(block, loose);
    }

    private ModelFile brickModel(String name, Cell cell, boolean lit, TrayLook look, boolean heater) {
        List<ModelSlicer.Part> parts = new ArrayList<>(BrewhouseModel.PARTS);
        if (heater) {
            parts.addAll(BrewhouseModel.HEATER_PARTS);
        }
        BlockModelBuilder model = cellModel(name, cell, bricks, parts);
        model.texture("door", provider.modLoc(heater ? "block/brewhouse_door_heater" + (lit ? "" : "_cold")
                : "block/brewhouse_door" + (lit ? "_lit" : "")));
        if (heater) {
            model.texture("gauge", Identifier.fromNamespaceAndPath("servus_delicacies", "block/gauge_dial"))
                    .texture("port", Identifier.fromNamespaceAndPath("servus_delicacies", "block/firebox_port"));
        }
        if (look != null) {
            model.texture("tray", trayTexture(look));
        }
        return model;
    }

    private Identifier trayTexture(TrayLook look) {
        return switch (look) {
            case EMPTY -> HEAVY_IRON;
            case GREEN -> provider.modLoc("block/kiln_green_malt");
            case PALE -> provider.modLoc("block/malt_floor");
            case DARK -> provider.modLoc("block/kiln_dark_malt");
        };
    }

    /** The pieces of every part in {@code cell}, each face with its texture variable and UVs. */
    private BlockModelBuilder cellModel(String name, Cell cell, Identifier particle, List<ModelSlicer.Part> parts) {
        BlockModelBuilder model = provider.models().withExistingParent(name, provider.mcLoc("block/block"))
                .texture("particle", particle)
                .texture("bricks", bricks)
                .texture("iron", HEAVY_IRON)
                .texture("copper", copper)
                .texture("grate", provider.modLoc("block/kiln_grate"));
        for (Piece piece : ModelSlicer.piecesIn(parts, cell)) {
            BlockModelBuilder.ElementBuilder element = model.element()
                    .from(piece.x0(), piece.y0(), piece.z0()).to(piece.x1(), piece.y1(), piece.z1());
            for (Side side : piece.sides()) {
                float[] fixed = BrewhouseModel.fixedUv(piece.part(), side);
                float[] uv = fixed != null ? fixed : piece.uv(side);
                element.face(direction(side)).texture(piece.texture(side)).uvs(uv[0], uv[1], uv[2], uv[3]).end();
            }
            element.end();
        }
        return model;
    }

    /** A side in the north built model: front north, left east. */
    private static Direction direction(Side side) {
        return switch (side) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case FRONT -> Direction.NORTH;
            case BACK -> Direction.SOUTH;
            case LEFT -> Direction.EAST;
            case RIGHT -> Direction.WEST;
        };
    }

    private static ConfiguredModel[] turned(ModelFile model, BlockState state) {
        return ConfiguredModel.builder().modelFile(model)
                .rotationY(ModelRotations.rotationY(state.getValue(BrewhousePartBlock.FACING))).build();
    }

    private static String key(Cell cell) {
        return BrewhousePart.of(cell).getSerializedName().toLowerCase(Locale.ROOT);
    }
}
