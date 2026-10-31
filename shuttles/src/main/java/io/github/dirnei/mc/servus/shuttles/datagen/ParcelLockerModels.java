package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLamp;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLayout;
import io.github.dirnei.mc.servus.shuttles.locker.LockerPart;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Datagen for the Parcel Locker, every model built facing north (front at
 * z 0) and turned by the block state.
 *
 * <p>A box part is a steel body from z 1 to 16 with the yellow doors of its
 * boxes in front, z 0 to 1, each inset one pixel from the block's edges and
 * from its neighbours, so the body's front shows as a steel groove between
 * them. Doors have no back face, and an L door split over two blocks has no
 * face where its halves meet. The box whose top edge lies in a part carries
 * a handle there (z -1 to 0) and its lamp (z -0.3 to 0, its own model per
 * lamp state); neither has a back face, and they stand a pixel apart.
 *
 * <p>The dock is a body from z 2 with the hatch on its front, framed by
 * steel bars from z 0 to 2: full height side bars, the top and bottom bars
 * between them without end faces where they meet the side bars. The screen
 * is a body from z 1 without a front face, a yellow bezel of the same shape
 * from z 0 to 1, and the emissive LCD plate at z 0.5 inside it with only
 * its front face. No two faces share a plane where they overlap; outer side
 * faces of the dock and screen frames keep no cullface, so the grooves of
 * the box parts beside them never look into a culled face.
 */
final class ParcelLockerModels {

    private static final String NAME = "parcel_locker";
    private static final float HANDLE_DEPTH = 1;
    private static final float LAMP_DEPTH = 0.3F;
    private static final float DOCK_LAMP_DEPTH = 0.5F;
    private static final int LIT = 15;
    private static final String SCREEN = "#screen";

    private ParcelLockerModels() {
    }

    /**
     * One box in pixels with the texture key its faces use, the key of its
     * front face, the faces it has and those culled by a neighbour.
     */
    private record Box(float x0, float y0, float z0, float x1, float y1, float z1, String texture, String front, List<Direction> faces,
                       List<Direction> culled) {

        Box withoutFace(Direction face) {
            List<Direction> kept = new ArrayList<>(faces);
            kept.remove(face);
            return new Box(x0, y0, z0, x1, y1, z1, texture, front, kept, culled);
        }

        Box withFront(String key) {
            return new Box(x0, y0, z0, x1, y1, z1, texture, key, faces, culled);
        }

        String textureOf(Direction face) {
            return face == Direction.NORTH ? front : texture;
        }
    }

    private static Box box(float x0, float y0, float z0, float x1, float y1, float z1, String texture, List<Direction> faces,
                           List<Direction> culled) {
        return new Box(x0, y0, z0, x1, y1, z1, texture, texture, faces, culled);
    }

    static void locker(BlockStateProvider provider) {
        Map<String, ModelFile> lampModels = new HashMap<>();
        Map<LockerPart, ModelFile> bases = new EnumMap<>(LockerPart.class);
        for (LockerPart part : LockerPart.values()) {
            bases.put(part, base(provider, part));
        }
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(ServusShuttlesBlocks.PARCEL_LOCKER.get());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int angle = ModelRotations.rotationY(facing);
            for (LockerPart part : LockerPart.values()) {
                builder.part().modelFile(bases.get(part)).rotationY(angle).addModel()
                        .condition(ParcelLockerBlock.FACING, facing).condition(ParcelLockerBlock.PART, part);
                if (part == LockerPart.HEAD_DOCK) {
                    lamps(provider, lampModels, builder, facing, part, ParcelLockerBlock.LAMP_TOP, "dock",
                            box(6, 13.5F, -DOCK_LAMP_DEPTH, 10, 14.5F, 0, "#lamp", front(), List.of()));
                }
                List<LockerLayout.Box> lampBoxes = part.lampBoxes();
                for (int slot = 0; slot < lampBoxes.size(); slot++) {
                    LockerLayout.Box lampBox = lampBoxes.get(slot);
                    float[] door = door(part, lampBox);
                    lamps(provider, lampModels, builder, facing, part, slot == 0 ? ParcelLockerBlock.LAMP_TOP : ParcelLockerBlock.LAMP_BOTTOM,
                            lampBox.id().toLowerCase(Locale.ROOT).replace("-", ""),
                            box(4, door[1] - 2, -LAMP_DEPTH, 6, door[1] - 1, 0, "#lamp", front(), List.of()));
                }
            }
        }
        provider.itemModels().basicItem(ServusShuttlesItems.PARCEL_LOCKER.get());
    }

    /** The lamp of one box or the dock in each lamp state, on the part it sits on; each model is written once. */
    private static void lamps(BlockStateProvider provider, Map<String, ModelFile> lampModels, MultiPartBlockStateBuilder builder, Direction facing, LockerPart part,
                              EnumProperty<LockerLamp> property, String which, Box lamp) {
        for (LockerLamp state : LockerLamp.values()) {
            String name = NAME + "_lamp_" + which + "_" + state.getSerializedName();
            ModelFile model = lampModels.computeIfAbsent(name, key -> {
                BlockModelBuilder built = provider.models().withExistingParent(key, "minecraft:block/block")
                        .texture("particle", provider.modLoc("block/" + NAME + "_lamp_" + state.getSerializedName()))
                        .texture("lamp", provider.modLoc("block/" + NAME + "_lamp_" + state.getSerializedName()));
                element(built, lamp, state == LockerLamp.OFF ? 0 : LIT, true);
                return built;
            });
            builder.part().modelFile(model).rotationY(ModelRotations.rotationY(facing)).addModel()
                    .condition(ParcelLockerBlock.FACING, facing).condition(ParcelLockerBlock.PART, part).condition(property, state);
        }
    }

    /** The model of one part without its lamps. */
    private static ModelFile base(BlockStateProvider provider, LockerPart part) {
        String name = NAME + "_" + part.getSerializedName();
        BlockModelBuilder model = provider.models().withExistingParent(name, "minecraft:block/block")
                .texture("particle", provider.modLoc("block/heavy_rack_iron"))
                .texture("steel", provider.modLoc("block/heavy_rack_iron"))
                .texture("yellow", provider.modLoc("block/" + NAME + "_yellow"))
                .texture("handle", provider.modLoc("block/" + NAME + "_handle"));
        List<Box> boxes = switch (part) {
            case HEAD_DOCK -> {
                model.texture("hatch", provider.modLoc("block/" + NAME + "_hatch"));
                yield dock();
            }
            case HEAD_SCREEN -> {
                model.texture("screen", provider.modLoc("block/" + NAME + "_screen"));
                yield screen();
            }
            default -> {
                model.texture("front", provider.modLoc("block/" + NAME + "_front_" + part.getSerializedName()));
                yield boxPart(part);
            }
        };
        for (Box box : boxes) {
            element(model, box, box.texture().equals(SCREEN) ? LIT : 0, false);
        }
        return model;
    }

    /** The body, the doors of the part's boxes and the handle of the box whose top edge lies in it. */
    private static List<Box> boxPart(LockerPart part) {
        List<Box> boxes = new ArrayList<>();
        boxes.add(box(0, 0, 1, 16, 16, 16, "#steel",
                List.of(Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.UP, Direction.DOWN),
                List.of(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.UP, Direction.DOWN)));
        for (LockerLayout.Box lockerBox : LockerLayout.boxesIn(part.column(), part.row())) {
            float[] door = door(part, lockerBox);
            Box piece = box(1, door[0], 0, 15, door[1], 1, "#yellow", front(), List.of()).withFront("#front");
            if (door[1] >= LockerLayout.BLOCK) {
                piece = piece.withoutFace(Direction.UP);
            }
            if (door[0] <= 0) {
                piece = piece.withoutFace(Direction.DOWN);
            }
            boxes.add(piece);
            if (part.lampBoxes().contains(lockerBox)) {
                float middle = Math.clamp((lockerBox.bottom() + lockerBox.top()) / 2F - part.row() * LockerLayout.BLOCK,
                        door[0] + 2.5F, door[1] - 2.5F);
                boxes.add(box(2, middle - 1.5F, -HANDLE_DEPTH, 3, middle + 1.5F, 0, "#handle", front(), List.of()));
            }
        }
        return boxes;
    }

    /** The bottom and top of a box's door inside the part, in the part's pixels: one pixel in from the box's edges. */
    private static float[] door(LockerPart part, LockerLayout.Box box) {
        int low = part.row() * LockerLayout.BLOCK;
        return new float[] {Math.max(box.bottom() + 1, low) - low, Math.min(box.top() - 1, low + LockerLayout.BLOCK) - low};
    }

    /** The dock: body with the hatch, side bars, top and bottom bars. */
    private static List<Box> dock() {
        return List.of(
                box(0, 0, 2, 16, 16, 16, "#steel",
                        List.of(Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.UP, Direction.DOWN),
                        List.of(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.UP, Direction.DOWN)).withFront("#hatch"),
                box(0, 0, 0, 3, 16, 2, "#steel", List.of(Direction.NORTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN),
                        List.of(Direction.UP, Direction.DOWN)),
                box(13, 0, 0, 16, 16, 2, "#steel", List.of(Direction.NORTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN),
                        List.of(Direction.UP, Direction.DOWN)),
                box(3, 12, 0, 13, 16, 2, "#steel", List.of(Direction.NORTH, Direction.UP, Direction.DOWN), List.of(Direction.UP)),
                box(3, 0, 0, 13, 3, 2, "#steel", List.of(Direction.NORTH, Direction.UP, Direction.DOWN), List.of(Direction.DOWN)));
    }

    /** The screen: body without a front, the yellow bezel and the LCD plate inside it. */
    private static List<Box> screen() {
        return List.of(
                box(0, 0, 1, 16, 16, 16, "#steel", List.of(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.UP, Direction.DOWN),
                        List.of(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.UP, Direction.DOWN)),
                box(0, 0, 0, 2, 16, 1, "#yellow", List.of(Direction.NORTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN),
                        List.of(Direction.UP, Direction.DOWN)),
                box(14, 0, 0, 16, 16, 1, "#yellow", List.of(Direction.NORTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN),
                        List.of(Direction.UP, Direction.DOWN)),
                box(2, 14, 0, 14, 16, 1, "#yellow", List.of(Direction.NORTH, Direction.UP, Direction.DOWN), List.of(Direction.UP)),
                box(2, 0, 0, 14, 2, 1, "#yellow", List.of(Direction.NORTH, Direction.UP, Direction.DOWN), List.of(Direction.DOWN)),
                box(2, 2, 0.5F, 14, 14, 1, SCREEN, List.of(Direction.NORTH), List.of()));
    }

    /** The faces of a part that sits on another part's front: all but its back. */
    private static List<Direction> front() {
        return List.of(Direction.NORTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN);
    }

    /**
     * One element. Faces inside the block take their texture where the game
     * would place it, so the doors' fronts find their door in the part's
     * front texture; parts standing out of the block take a corner of their
     * plain texture of their own size.
     */
    private static void element(BlockModelBuilder model, Box box, int light, boolean small) {
        var element = model.element().from(box.x0(), box.y0(), box.z0()).to(box.x1(), box.y1(), box.z1());
        if (light > 0) {
            element.lightEmission(light);
        }
        for (Direction direction : box.faces()) {
            float[] uv = small || box.z0() < 0 ? corner(box, direction) : uv(box, direction);
            var face = element.face(direction).texture(box.textureOf(direction)).uvs(uv[0], uv[1], uv[2], uv[3]);
            if (box.culled().contains(direction)) {
                face.cullface(direction);
            }
        }
    }

    /** UVs of the face's own size from the texture's corner, for parts that stand out of the block. */
    private static float[] corner(Box box, Direction direction) {
        float w = switch (direction) {
            case NORTH, SOUTH, UP, DOWN -> box.x1() - box.x0();
            case EAST, WEST -> box.z1() - box.z0();
        };
        float h = switch (direction) {
            case UP, DOWN -> box.z1() - box.z0();
            default -> box.y1() - box.y0();
        };
        return new float[] {1, 1, 1 + w, 1 + h};
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
}
