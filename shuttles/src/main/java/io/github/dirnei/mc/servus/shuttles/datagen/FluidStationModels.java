package io.github.dirnei.mc.servus.shuttles.datagen;

import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.FRAME;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.GLASS_HALF;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.GLASS_INSET;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.GLASS_PLANE;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ItemModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;
import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlock;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Datagen for the fluid stations. The lower half is the tank, shared by
 * both stations: iron posts and bars on its twelve edges, a glass pane on
 * each side and an iron floor, open at the top to the bottom of the upper
 * half. The upper half is the item station's block model as it is, the
 * Inbound Station's on the Fluid Inbound and the Outbound Station's on the
 * Fluid Outbound, in its dark, idle, working and blocked front.
 *
 * <p>Only faces that can be seen are written: posts show their outer and
 * inner sides and their bottom, bars their outer side, their bottom on the
 * station's bottom and the face toward the glass; nothing faces up into the
 * upper half's bottom. Parts that touch meet at their edges, so no two
 * faces share a plane where they overlap. Panes stand {@link
 * io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook#GLASS_INSET}
 * off the frame and the floor and have only their two broad faces.
 */
final class FluidStationModels {

    private static final String TANK = "fluid_station_tank";
    private static final Identifier GLASS = Identifier.fromNamespaceAndPath("servus_foundation", "block/fluid_tank_glass");
    private static final float IN = FRAME;
    private static final float OUT = 16 - FRAME;

    private FluidStationModels() {
    }

    /** One box in pixels with the texture key its faces use and the faces it has. */
    private record Box(float x0, float y0, float z0, float x1, float y1, float z1, String texture, List<Direction> faces) {
    }

    private static Box box(float x0, float y0, float z0, float x1, float y1, float z1, String texture, Direction... faces) {
        return new Box(x0, y0, z0, x1, y1, z1, texture, List.of(faces));
    }

    /** The tank model both stations share. */
    static void tank(BlockStateProvider provider) {
        BlockModelBuilder builder = provider.models().withExistingParent(TANK, "minecraft:block/block")
                .texture("particle", iron(provider)).texture("iron", iron(provider)).texture("glass", GLASS);
        tankBoxes().forEach(box -> element(builder, box, 0));
    }

    /**
     * The block states and the item of one station: the tank below, the item
     * station {@code itemStation} above, its front lamps by the station's lamp state.
     */
    static void station(BlockStateProvider provider, FluidStationBlock block, String name, String itemStation) {
        ModelFile tank = new ModelFile.UncheckedModelFile(provider.modLoc("block/" + TANK));
        ModelFile dark = stationModel(provider, itemStation, "");
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        builder.part().modelFile(tank).addModel().condition(FluidStationBlock.HALF, DoubleBlockHalf.LOWER);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int angle = ModelRotations.rotationY(facing);
            builder.part().modelFile(dark).rotationY(angle).addModel()
                    .condition(FluidStationBlock.FACING, facing).condition(FluidStationBlock.HALF, DoubleBlockHalf.UPPER)
                    .condition(StorageNodeBlocks.ONLINE, false);
            for (StorageNodeBlocks.Activity activity : StorageNodeBlocks.Activity.values()) {
                ModelFile lit = stationModel(provider, itemStation, "_" + activity.getSerializedName());
                builder.part().modelFile(lit).rotationY(angle).addModel()
                        .condition(FluidStationBlock.FACING, facing).condition(FluidStationBlock.HALF, DoubleBlockHalf.UPPER)
                        .condition(StorageNodeBlocks.ONLINE, true).condition(StorageNodeBlocks.ACTIVITY, activity);
            }
        }
        item(provider, name, itemStation);
    }

    /** An item station block model as the item station's own datagen writes it. */
    private static ModelFile stationModel(BlockStateProvider provider, String itemStation, String suffix) {
        return new ModelFile.UncheckedModelFile(provider.modLoc("block/" + itemStation + suffix));
    }

    private static Identifier iron(BlockStateProvider provider) {
        return provider.modLoc("block/heavy_rack_iron");
    }

    /** Posts, bars, the floor and the glass of the tank. */
    private static List<Box> tankBoxes() {
        List<Box> boxes = new ArrayList<>();
        float[] edges = {0, OUT};
        for (float x : edges) {
            for (float z : edges) {
                Direction xOut = x == 0 ? Direction.WEST : Direction.EAST;
                Direction zOut = z == 0 ? Direction.NORTH : Direction.SOUTH;
                boxes.add(box(x, 0, z, x + FRAME, 16, z + FRAME, "#iron", xOut, zOut, xOut.getOpposite(), zOut.getOpposite(), Direction.DOWN));
            }
        }
        for (float y : edges) {
            boolean bottom = y == 0;
            Direction inward = bottom ? Direction.UP : Direction.DOWN;
            for (float z : edges) {
                Direction zOut = z == 0 ? Direction.NORTH : Direction.SOUTH;
                boxes.add(bottom ? box(IN, y, z, OUT, y + FRAME, z + FRAME, "#iron", zOut, Direction.DOWN, inward)
                        : box(IN, y, z, OUT, y + FRAME, z + FRAME, "#iron", zOut, inward));
            }
            for (float x : edges) {
                Direction xOut = x == 0 ? Direction.WEST : Direction.EAST;
                boxes.add(bottom ? box(x, y, IN, x + FRAME, y + FRAME, OUT, "#iron", xOut, Direction.DOWN, inward)
                        : box(x, y, IN, x + FRAME, y + FRAME, OUT, "#iron", xOut, inward));
            }
        }
        boxes.add(box(IN, 0, IN, OUT, FRAME, OUT, "#iron", Direction.UP, Direction.DOWN));
        float g0 = IN + GLASS_INSET;
        float g1 = OUT - GLASS_INSET;
        for (float plane : new float[] {GLASS_PLANE, 16 - GLASS_PLANE}) {
            boxes.add(box(g0, g0, plane - GLASS_HALF, g1, g1, plane + GLASS_HALF, "#glass", Direction.NORTH, Direction.SOUTH));
            boxes.add(box(plane - GLASS_HALF, g0, g0, plane + GLASS_HALF, g1, g1, "#glass", Direction.WEST, Direction.EAST));
        }
        return boxes;
    }

    /** One element moved up by {@code dy}; explicit UVs keep a moved element's texture where it is in its own block. */
    private static void element(ModelBuilder<?> builder, Box box, float dy) {
        var element = builder.element().from(box.x0(), box.y0() + dy, box.z0()).to(box.x1(), box.y1() + dy, box.z1());
        for (Direction direction : box.faces()) {
            float[] uv = uv(box, direction);
            element.face(direction).texture(box.texture()).uvs(uv[0], uv[1], uv[2], uv[3]);
        }
    }

    /** The UVs the game derives for a face of a box inside one block. */
    private static float[] uv(Box box, Direction direction) {
        return switch (direction) {
            case DOWN -> new float[] {box.x0(), 16 - box.z1(), box.x1(), 16 - box.z0()};
            case UP -> new float[] {box.x0(), box.z0(), box.x1(), box.z1()};
            case NORTH -> new float[] {16 - box.x1(), 16 - box.y1(), 16 - box.x0(), 16 - box.y0()};
            case SOUTH -> new float[] {box.x0(), 16 - box.y1(), box.x1(), 16 - box.y0()};
            case WEST -> new float[] {box.z0(), 16 - box.y1(), box.z1(), 16 - box.y0()};
            case EAST -> new float[] {16 - box.z1(), 16 - box.y1(), 16 - box.z0(), 16 - box.y0()};
        };
    }

    /**
     * The item: the tank with the item station on top, its boards under its
     * overlay textures like {@code LayeredModels}, scaled down in the hand and
     * the GUI. As a material block item it shows the stack's wood.
     */
    private static void item(BlockStateProvider provider, String name, String itemStation) {
        ItemModelBuilder builder = provider.itemModels().withExistingParent(name, "minecraft:block/block")
                .texture("particle", iron(provider)).texture("iron", iron(provider)).texture("glass", GLASS)
                .texture("board", MaterialKind.WOOD.placeholder())
                .texture("front", provider.modLoc("block/" + itemStation + "_front"))
                .texture("side", provider.modLoc("block/" + itemStation + "_side"))
                .texture("top", provider.modLoc("block/" + itemStation + "_top"));
        tankBoxes().forEach(box -> element(builder, box, 0));
        for (String layer : new String[] {"#board", null}) {
            var element = builder.element().from(0, 16, 0).to(16, 32, 16);
            for (Direction direction : Direction.values()) {
                String texture = layer != null ? layer : switch (direction) {
                    case NORTH -> "#front";
                    case UP, DOWN -> "#top";
                    default -> "#side";
                };
                element.face(direction).texture(texture).uvs(0, 0, 16, 16);
            }
        }
        builder.transforms()
                .transform(ItemDisplayContext.GUI).rotation(30, 225, 0).translation(0, -3, 0).scale(0.4F).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 0, 0).translation(0, 2, 0).scale(0.2F).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 0, 0).translation(0, -3, 0).scale(0.4F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 2.5F, 0).scale(0.3F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).translation(0, 0, 0).scale(0.3F).end()
                .end();
    }
}
