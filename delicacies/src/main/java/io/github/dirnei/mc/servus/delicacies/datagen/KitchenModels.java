package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.board.CuttingBoardBlock;
import io.github.dirnei.mc.servus.delicacies.cooking.BakingOvenBlock;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselBlock;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselParts;
import io.github.dirnei.mc.servus.delicacies.crock.CrockParts;
import io.github.dirnei.mc.servus.delicacies.crock.FermentingCrockBlock;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlock;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The kitchen's block models: the Stove Tile loose and formed (cooking plate on top, firebox door on the front, cold
 * and lit), and the Pot, Pan, Baking Oven and Cutting Board from {@link VesselParts}, built facing north. Box faces take their UVs
 * from their position, so a part shows the matching piece of its texture.
 */
final class KitchenModels {

    /** Walls and rims stand on the bottom: no down faces. */
    private static final Set<Direction> RESTING = Set.of(Direction.DOWN);

    private KitchenModels() {
    }

    static void register(BlockStateProvider provider) {
        stoveTile(provider);
        Identifier steel = Identifier.fromNamespaceAndPath("servus_foundation", "block/electric_pump_steel");
        Identifier heavyIron = Identifier.fromNamespaceAndPath("servus_foundation", "block/heavy_iron");
        Identifier oak = provider.mcLoc("block/oak_planks");

        BlockModelBuilder pot = model(provider, "pot", steel).texture("body", steel).texture("handle", heavyIron);
        box(pot, VesselParts.POT_BOTTOM, "#body", Set.of());
        for (float[] wall : VesselParts.POT_WALLS) {
            box(pot, wall, "#body", RESTING);
        }
        box(pot, VesselParts.POT_HANDLE_WEST, "#handle", Set.of(Direction.EAST));
        box(pot, VesselParts.POT_HANDLE_EAST, "#handle", Set.of(Direction.WEST));
        facing(provider, ServusDelicaciesBlocks.POT.get(), VesselBlock.FACING, pot);

        BlockModelBuilder pan = model(provider, "pan", heavyIron).texture("body", heavyIron).texture("handle", oak);
        box(pan, VesselParts.PAN_BOTTOM, "#body", Set.of());
        for (float[] rim : VesselParts.PAN_RIMS) {
            box(pan, rim, "#body", RESTING);
        }
        box(pan, VesselParts.PAN_HANDLE, "#handle", Set.of(Direction.WEST));
        facing(provider, ServusDelicaciesBlocks.PAN.get(), VesselBlock.FACING, pan);

        bakingOven(provider, steel);

        Identifier birch = provider.mcLoc("block/birch_planks");
        BlockModelBuilder board = model(provider, "cutting_board", birch).texture("wood", birch);
        box(board, VesselParts.BOARD, "#wood", Set.of());
        facing(provider, ServusDelicaciesBlocks.CUTTING_BOARD.get(), CuttingBoardBlock.FACING, board);

        fermentingCrock(provider);
    }

    /**
     * The crock open and sealed. Neck and lip stand on the belly; the sealed model adds the lid with its skirt in the
     * water seal and the knob, and leaves out the faces the lid covers.
     */
    private static void fermentingCrock(BlockStateProvider provider) {
        Identifier side = provider.modLoc("block/crock_side");
        ModelFile[] models = new ModelFile[2];
        for (int sealed = 0; sealed < 2; sealed++) {
            BlockModelBuilder crock = model(provider, "fermenting_crock" + (sealed == 1 ? "_sealed" : ""), side)
                    .texture("side", side).texture("top", provider.modLoc("block/crock_top"))
                    .texture("lid", provider.modLoc("block/crock_lid"));
            BlockModelBuilder.ElementBuilder belly = crock.element().from(CrockParts.BELLY[0], CrockParts.BELLY[1], CrockParts.BELLY[2])
                    .to(CrockParts.BELLY[3], CrockParts.BELLY[4], CrockParts.BELLY[5]);
            for (Direction direction : Direction.values()) {
                belly.face(direction).texture(direction == Direction.UP ? "#top" : "#side").end();
            }
            Set<Direction> neckSkip = sealed == 1 ? Set.of(Direction.DOWN, Direction.UP) : RESTING;
            for (float[] neck : CrockParts.NECK) {
                box(crock, neck, "#side", neckSkip);
            }
            for (float[] lip : CrockParts.LIP) {
                box(crock, lip, "#side", RESTING);
            }
            if (sealed == 1) {
                box(crock, CrockParts.LID, "#lid", Set.of());
                box(crock, CrockParts.SKIRT_NORTH, "#lid", Set.of(Direction.UP));
                box(crock, CrockParts.SKIRT_SOUTH, "#lid", Set.of(Direction.UP));
                box(crock, CrockParts.SKIRT_WEST, "#lid", Set.of(Direction.UP, Direction.NORTH, Direction.SOUTH));
                box(crock, CrockParts.SKIRT_EAST, "#lid", Set.of(Direction.UP, Direction.NORTH, Direction.SOUTH));
                box(crock, CrockParts.KNOB, "#lid", Set.of(Direction.DOWN));
            }
            models[sealed] = crock;
        }
        Block block = ServusDelicaciesBlocks.FERMENTING_CROCK.get();
        provider.getVariantBuilder(block).forAllStates(state ->
                new ConfiguredModel[] {new ConfiguredModel(models[state.getValue(FermentingCrockBlock.SEALED) ? 1 : 0])});
        provider.simpleBlockItem(block, models[0]);
    }

    /** Loose: tiles all round. Formed: plate on top, the door tile with its door on the north face, turned to the front. */
    private static void stoveTile(BlockStateProvider provider) {
        Identifier tile = provider.modLoc("block/stove_tile");
        ModelFile loose = provider.models().cubeAll("stove_tile", tile);
        ModelFile[][] formed = new ModelFile[2][2];
        for (int door = 0; door < 2; door++) {
            for (int lit = 0; lit < 2; lit++) {
                String suffix = (door == 1 ? "_door" : "_formed") + (lit == 1 ? "_lit" : "");
                Identifier plate = provider.modLoc("block/stove_plate" + (lit == 1 ? "_lit" : ""));
                Identifier front = door == 1 ? provider.modLoc("block/stove_tile_door" + (lit == 1 ? "_lit" : "")) : tile;
                formed[door][lit] = provider.models().cube("stove_tile" + suffix, tile, plate, front, tile, tile, tile)
                        .texture("particle", tile);
            }
        }
        Block block = ServusDelicaciesBlocks.STOVE_TILE.get();
        provider.getVariantBuilder(block).forAllStates(state -> {
            if (!state.getValue(StoveTileBlock.FORMED)) {
                return new ConfiguredModel[] {new ConfiguredModel(loose)};
            }
            ModelFile model = formed[state.getValue(StoveTileBlock.DOOR) ? 1 : 0][state.getValue(StoveTileBlock.LIT) ? 1 : 0];
            return ConfiguredModel.builder().modelFile(model)
                    .rotationY(ModelRotations.rotationY(state.getValue(StoveTileBlock.FACING))).build();
        });
        provider.simpleBlockItem(block, loose);
    }

    /** The oven cold and baking (the window glows), turned to its facing; door and handle stand on the front. */
    private static void bakingOven(BlockStateProvider provider, Identifier steel) {
        Identifier brass = Identifier.fromNamespaceAndPath("servus_foundation", "block/brass");
        ModelFile[] models = new ModelFile[2];
        for (int lit = 0; lit < 2; lit++) {
            String suffix = lit == 1 ? "_lit" : "";
            BlockModelBuilder oven = model(provider, "baking_oven" + suffix, steel).texture("body", steel)
                    .texture("door", provider.modLoc("block/baking_oven_door" + suffix)).texture("handle", brass);
            box(oven, VesselParts.OVEN_BODY, "#body", Set.of());
            box(oven, VesselParts.OVEN_DOOR, "#door", Set.of(Direction.SOUTH));
            box(oven, VesselParts.OVEN_HANDLE, "#handle", Set.of(Direction.SOUTH));
            models[lit] = oven;
        }
        Block block = ServusDelicaciesBlocks.BAKING_OVEN.get();
        provider.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models[state.getValue(BakingOvenBlock.COOKING) ? 1 : 0])
                .rotationY(ModelRotations.rotationY(state.getValue(BakingOvenBlock.FACING))).build());
        provider.simpleBlockItem(block, models[0]);
    }

    private static BlockModelBuilder model(BlockStateProvider provider, String name, Identifier particle) {
        return provider.models().withExistingParent(name, provider.mcLoc("block/block")).texture("particle", particle);
    }

    /** One variant per facing, turned from the north built model; the item shows the model unturned. */
    private static void facing(BlockStateProvider provider, Block block, EnumProperty<Direction> facing, ModelFile model) {
        provider.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder().modelFile(model)
                .rotationY(ModelRotations.rotationY(state.getValue(facing))).build());
        provider.simpleBlockItem(block, model);
    }

    /** A box of {@code b} (pixels) with every face but {@code skip} showing {@code texture}. */
    private static void box(BlockModelBuilder model, float[] b, String texture, Set<Direction> skip) {
        BlockModelBuilder.ElementBuilder element = model.element().from(b[0], b[1], b[2]).to(b[3], b[4], b[5]);
        for (Direction direction : Direction.values()) {
            if (!skip.contains(direction)) {
                element.face(direction).texture(texture).end();
            }
        }
    }
}
