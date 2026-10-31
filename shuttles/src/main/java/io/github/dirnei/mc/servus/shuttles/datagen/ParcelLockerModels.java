package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLayout;
import io.github.dirnei.mc.servus.shuttles.locker.LockerPart;
import io.github.dirnei.mc.servus.shuttles.locker.LockerScreen;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;

/**
 * Datagen for the Parcel Locker's static parts, every model built facing
 * north (front at z 0) and turned by the block state. Positions are given
 * as the viewer in front sees them, x from the left; the viewer's left is
 * model east. Doors, their lamps and the items of open boxes are drawn by
 * the dock's renderer.
 *
 * <p>A box part is a carcass: a back wall from z 8 to 16 with the dark inside
 * on its front, posts at x 0 to 1 and 14 to 16 and bars between them from
 * z 1 to 8, framing each box's dark inside at x 1 to 14 and from two pixels
 * above its bottom to one below its top; the frames' fronts are ochre. Two
 * boxes stacked in a part share one bar. The doors stand in front, z 0 to
 * 0.95. The head's screen is a steel body from z 1 with a bezel of four bars
 * from z 0 to 1 and the emissive LCD at z 0.5 inside it, one model per
 * screen state; the dock is a body from z 1 with the hatch on its front,
 * framed by steel bars from z 0 to 1, and its lamp from z -0.5 to 0. A roof
 * strip 3 pixels tall lies on the five upper parts, standing out a pixel at
 * the front, back and both ends.
 *
 * <p>Faces between parts are left out instead of culled: every part fills
 * its block's boundary from z 1 to 16 where it meets another part, and the
 * frames of two parts meeting at a block boundary are solid on both sides
 * of it. No upper part has a top face under the roof. The head's bars keep
 * their outer sides, seen through the gap beside a door. No two faces share
 * a plane where they overlap.
 */
final class ParcelLockerModels {

    private static final String NAME = "parcel_locker";
    private static final float RECESS = 8;
    private static final int LIT = 15;
    private static final Direction LEFT = Direction.EAST;
    private static final Direction RIGHT = Direction.WEST;

    private ParcelLockerModels() {
    }

    /** How a face takes its texture: where the game would place it in the block, or from the texture's corner. */
    private enum Uv { BLOCK, CORNER }

    /**
     * One box in model pixels with the texture key its faces use, the key of
     * its front face and of faces set apart, the faces it has, those culled
     * by a neighbour and whether it glows.
     */
    private record Box(float x0, float y0, float z0, float x1, float y1, float z1, String texture, String front, Map<Direction, String> keys,
                       List<Direction> faces, List<Direction> culled, Uv uv, boolean lit) {

        Box with(Direction face, String key) {
            Map<Direction, String> changed = new EnumMap<>(Direction.class);
            changed.putAll(keys);
            changed.put(face, key);
            return new Box(x0, y0, z0, x1, y1, z1, texture, front, changed, faces, culled, uv, lit);
        }

        Box glowing() {
            return new Box(x0, y0, z0, x1, y1, z1, texture, front, keys, faces, culled, uv, true);
        }

        String keyOf(Direction face) {
            return face == Direction.NORTH ? front : keys.getOrDefault(face, texture);
        }
    }

    /** A box from the viewer's x {@code left} to {@code right}. */
    private static Box box(float left, float y0, float z0, float right, float y1, float z1, String texture, String front,
                           List<Direction> faces, List<Direction> culled, Uv uv) {
        return new Box(16 - right, y0, z0, 16 - left, y1, z1, texture, front, Map.of(), faces, culled, uv, false);
    }

    static void locker(BlockStateProvider provider) {
        Map<LockerPart, ModelFile> bases = new EnumMap<>(LockerPart.class);
        for (LockerPart part : LockerPart.values()) {
            bases.put(part, base(provider, part));
        }
        Map<LockerScreen, ModelFile> screens = new EnumMap<>(LockerScreen.class);
        for (LockerScreen screen : LockerScreen.values()) {
            screens.put(screen, model(provider, NAME + "_screen_" + screen.getSerializedName(), screenFront(),
                    "screen", "block/" + NAME + "_screen_" + screen.getSerializedName()));
        }
        ModelFile lampOff = model(provider, NAME + "_dock_lamp_off", List.of(dockLamp()), "lamp", "block/" + NAME + "_dock_lamp_off");
        ModelFile lampOn = model(provider, NAME + "_dock_lamp_loading", List.of(dockLamp().glowing()), "lamp",
                "block/" + NAME + "_dock_lamp_loading");
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(ServusShuttlesBlocks.PARCEL_LOCKER.get());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int angle = ModelRotations.rotationY(facing);
            for (LockerPart part : LockerPart.values()) {
                builder.part().modelFile(bases.get(part)).rotationY(angle).addModel()
                        .condition(ParcelLockerBlock.FACING, facing).condition(ParcelLockerBlock.PART, part);
            }
            for (LockerScreen screen : LockerScreen.values()) {
                builder.part().modelFile(screens.get(screen)).rotationY(angle).addModel()
                        .condition(ParcelLockerBlock.FACING, facing).condition(ParcelLockerBlock.PART, LockerPart.HEAD_SCREEN)
                        .condition(ParcelLockerBlock.SCREEN, screen);
            }
            for (boolean loading : new boolean[] {false, true}) {
                builder.part().modelFile(loading ? lampOn : lampOff).rotationY(angle).addModel()
                        .condition(ParcelLockerBlock.FACING, facing).condition(ParcelLockerBlock.PART, LockerPart.HEAD_DOCK)
                        .condition(ParcelLockerBlock.LOADING, loading);
            }
        }
        // The item is one block of the head: the screen on its front, the roof on top.
        ModelFile item = provider.models().orientable(NAME + "_item", provider.modLoc("block/" + NAME + "_side"),
                provider.modLoc("block/" + NAME + "_screen_idle"), provider.modLoc("block/" + NAME + "_roof_top"));
        provider.simpleBlockItem(ServusShuttlesBlocks.PARCEL_LOCKER.get(), item);
    }

    /** A model of the boxes with one texture key. */
    private static ModelFile model(BlockStateProvider provider, String name, List<Box> boxes, String key, String texture) {
        BlockModelBuilder model = provider.models().withExistingParent(name, "minecraft:block/block")
                .texture("particle", provider.modLoc(texture))
                .texture(key, provider.modLoc(texture));
        for (Box box : boxes) {
            element(model, box);
        }
        return model;
    }

    /** The model of one part without its screen and dock lamp. */
    private static ModelFile base(BlockStateProvider provider, LockerPart part) {
        BlockModelBuilder model = provider.models().withExistingParent(NAME + "_" + part.getSerializedName(), "minecraft:block/block")
                .texture("particle", provider.modLoc("block/" + NAME + "_side"))
                .texture("back", provider.modLoc("block/heavy_rack_iron"))
                .texture("side", provider.modLoc("block/" + NAME + "_side"))
                .texture("carcass", provider.modLoc("block/" + NAME + "_carcass"))
                .texture("ochre", provider.modLoc("block/" + NAME + "_ochre"))
                .texture("roof", provider.modLoc("block/" + NAME + "_roof"))
                .texture("roof_top", provider.modLoc("block/" + NAME + "_roof_top"))
                .texture("dock", provider.modLoc("block/" + NAME + "_dock"));
        List<Box> boxes = new ArrayList<>(switch (part) {
            case HEAD_DOCK -> dock();
            case HEAD_SCREEN -> List.of(box(0, 0, 1, 16, 16, 16, "#back", "#back", List.of(Direction.SOUTH), List.of(Direction.SOUTH), Uv.BLOCK));
            default -> carcass(part);
        });
        if (part.row() == 1) {
            boxes.add(roof(part.column()));
        }
        for (Box box : boxes) {
            element(model, box);
        }
        return model;
    }

    /** The back wall, the two posts and the bars between the boxes' insides. */
    private static List<Box> carcass(LockerPart part) {
        boolean low = part.row() == 0;
        List<Direction> ground = low ? List.of(Direction.DOWN) : List.of();
        List<Box> boxes = new ArrayList<>();
        boxes.add(box(0, 0, RECESS, 16, 16, 16, "#back", "#carcass",
                faces(List.of(Direction.NORTH, Direction.SOUTH), ground, outerEnds(part.column())),
                culled(Direction.SOUTH, ground), Uv.BLOCK).with(LEFT, "#side").with(RIGHT, "#side"));
        boxes.add(box(0, 0, 1, 1, 16, RECESS, "#carcass", "#ochre",
                faces(List.of(Direction.NORTH, RIGHT), ground, part.column() == 0 ? List.of(LEFT) : List.of()), ground, Uv.BLOCK)
                .with(LEFT, "#side"));
        boxes.add(box(14, 0, 1, 16, 16, RECESS, "#carcass", "#ochre",
                faces(List.of(Direction.NORTH, LEFT), ground, part.column() == LockerLayout.COLUMNS - 1 ? List.of(RIGHT) : List.of()), ground,
                Uv.BLOCK).with(RIGHT, "#side"));
        for (float[] bar : bars(part)) {
            List<Direction> faces = new ArrayList<>(List.of(Direction.NORTH));
            if (bar[1] < LockerLayout.BLOCK) {
                faces.add(Direction.UP);
            }
            if (bar[0] > 0 || low) {
                faces.add(Direction.DOWN);
            }
            boxes.add(box(1, bar[0], 1, 14, bar[1], RECESS, "#carcass", "#ochre", faces, bar[0] <= 0 ? ground : List.of(), Uv.BLOCK));
        }
        return boxes;
    }

    /** The heights inside a part not taken by a box's inside, as {bottom, top} pairs from the bottom. */
    private static List<float[]> bars(LockerPart part) {
        int low = part.row() * LockerLayout.BLOCK;
        List<float[]> insides = new ArrayList<>();
        for (LockerLayout.Box box : LockerLayout.boxesIn(part.column(), part.row())) {
            float bottom = box.bottom() < low ? 0 : box.bottom() + 2 - low;
            float top = box.top() > low + LockerLayout.BLOCK ? LockerLayout.BLOCK : box.top() - 1 - low;
            insides.add(new float[] {bottom, top});
        }
        insides.sort((a, b) -> Float.compare(a[0], b[0]));
        List<float[]> bars = new ArrayList<>();
        float at = 0;
        for (float[] inside : insides) {
            if (inside[0] > at) {
                bars.add(new float[] {at, inside[0]});
            }
            at = inside[1];
        }
        if (at < LockerLayout.BLOCK) {
            bars.add(new float[] {at, LockerLayout.BLOCK});
        }
        return bars;
    }

    /** The dock: body with the hatch on its front, framed by steel bars. */
    private static List<Box> dock() {
        List<Direction> ground = List.of(Direction.DOWN);
        return List.of(
                box(0, 0, 1, 16, 16, 16, "#back", "#dock", List.of(Direction.NORTH, Direction.SOUTH, Direction.DOWN),
                        List.of(Direction.SOUTH, Direction.DOWN), Uv.BLOCK),
                box(0, 0, 0, 3, 16, 1, "#dock", "#dock", List.of(Direction.NORTH, LEFT, RIGHT, Direction.DOWN), ground, Uv.BLOCK),
                box(13, 0, 0, 16, 16, 1, "#dock", "#dock", List.of(Direction.NORTH, LEFT, RIGHT, Direction.DOWN), ground, Uv.BLOCK),
                box(3, 11, 0, 13, 16, 1, "#dock", "#dock", List.of(Direction.NORTH, Direction.DOWN), List.of(), Uv.BLOCK),
                box(3, 0, 0, 13, 2, 1, "#dock", "#dock", List.of(Direction.NORTH, Direction.UP, Direction.DOWN), ground, Uv.BLOCK));
    }

    /** The dock lamp on the top bar, without a back face. */
    private static Box dockLamp() {
        return box(6, 12, -0.5F, 10, 14, 0, "#lamp", "#lamp",
                List.of(Direction.NORTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN), List.of(), Uv.CORNER);
    }

    /** The screen's bezel of four bars and its emissive LCD, all from the state's texture. */
    private static List<Box> screenFront() {
        return List.of(
                box(0, 0, 0, 1.5F, 16, 1, "#screen", "#screen", List.of(Direction.NORTH, LEFT, RIGHT), List.of(), Uv.BLOCK),
                box(14.5F, 0, 0, 16, 16, 1, "#screen", "#screen", List.of(Direction.NORTH, LEFT, RIGHT), List.of(), Uv.BLOCK),
                box(1.5F, 13, 0, 14.5F, 16, 1, "#screen", "#screen", List.of(Direction.NORTH, Direction.DOWN), List.of(), Uv.BLOCK),
                box(1.5F, 0, 0, 14.5F, 1.5F, 1, "#screen", "#screen", List.of(Direction.NORTH, Direction.UP), List.of(), Uv.BLOCK),
                box(1.5F, 1.5F, 0.5F, 14.5F, 13, 1, "#screen", "#screen", List.of(Direction.NORTH), List.of(), Uv.BLOCK).glowing());
    }

    /** The roof strip on an upper part, standing out a pixel at the front, back and the locker's ends. */
    private static Box roof(int column) {
        float left = column == 0 ? -1 : 0;
        float right = column == LockerLayout.COLUMNS - 1 ? 17 : 16;
        return box(left, 16, -1, right, 19, 17, "#roof", "#roof",
                faces(List.of(Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN), List.of(), outerEnds(column)), List.of(),
                Uv.CORNER).with(Direction.UP, "#roof_top").with(Direction.DOWN, "#roof_top");
    }

    /** The locker's outer ends a part of that column shows. */
    private static List<Direction> outerEnds(int column) {
        if (column == 0) {
            return List.of(LEFT);
        }
        return column == LockerLayout.COLUMNS - 1 ? List.of(RIGHT) : List.of();
    }

    @SafeVarargs
    private static List<Direction> faces(List<Direction>... groups) {
        List<Direction> faces = new ArrayList<>();
        for (List<Direction> group : groups) {
            faces.addAll(group);
        }
        return faces;
    }

    private static List<Direction> culled(Direction face, List<Direction> more) {
        return faces(List.of(face), more);
    }

    private static void element(BlockModelBuilder model, Box box) {
        var element = model.element().from(box.x0(), box.y0(), box.z0()).to(box.x1(), box.y1(), box.z1());
        if (box.lit()) {
            element.lightEmission(LIT);
        }
        for (Direction direction : box.faces()) {
            float[] uv = box.uv() == Uv.CORNER ? corner(box, direction) : uv(box, direction);
            var face = element.face(direction).texture(box.keyOf(direction)).uvs(uv[0], uv[1], uv[2], uv[3]);
            if (box.culled().contains(direction)) {
                face.cullface(direction);
            }
        }
    }

    /** UVs of the face's own size from the texture's corner, at most the whole texture. */
    private static float[] corner(Box box, Direction direction) {
        float w = switch (direction) {
            case NORTH, SOUTH, UP, DOWN -> box.x1() - box.x0();
            case EAST, WEST -> box.z1() - box.z0();
        };
        float h = switch (direction) {
            case UP, DOWN -> box.z1() - box.z0();
            default -> box.y1() - box.y0();
        };
        return new float[] {0, 0, Math.min(16, w), Math.min(16, h)};
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
