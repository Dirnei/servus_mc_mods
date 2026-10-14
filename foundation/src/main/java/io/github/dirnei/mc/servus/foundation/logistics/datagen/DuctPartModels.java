package io.github.dirnei.mc.servus.foundation.logistics.datagen;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctParts;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctShapes;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import java.util.List;
import net.minecraft.core.Direction;

/**
 * Writes the duct part models: a centre cube per line set and run axis, a
 * fitting per line set and, per direction, an arm per line set, for every
 * non empty set of lines. Parts with one line use radius 2, two lines radius
 * 3, three or more lines radius 4; a fitting of several lines is one pixel
 * larger. A part with several lines shows its lines as bands running along
 * it, in line order from the negative side of the across axis, and the outer
 * line's strip on the two faces across the bands. The dynamic model sets the
 * UVs of the side faces (see {@code DuctUv}), so the UVs written here only
 * matter for the end faces. Every arm ends in a ring
 * frame whose faces carry the tint index of that line and side, split between
 * the lines like the bands; the dynamic model drops those faces when the side
 * is not an endpoint.
 */
final class DuctPartModels {

    private static final float RING_LIP = 0.5F;

    private DuctPartModels() {
    }

    /** @return the thin item centre, used as the block state's base model */
    static ModelFile generate(BlockStateProvider provider) {
        ModelFile base = null;
        for (int lines : DuctParts.lineSets()) {
            ModelFile center = center(provider, lines, Direction.Axis.Z);
            if (lines == DuctParts.ITEM) {
                base = center;
            }
            junction(provider, lines);
            if (Integer.bitCount(lines) >= 2) {
                center(provider, lines, Direction.Axis.X);
                center(provider, lines, Direction.Axis.Y);
            }
            for (Direction direction : Direction.values()) {
                arm(provider, direction, lines);
            }
        }
        return base;
    }

    private static int radius(int lines) {
        return DuctShapes.radius(Integer.bitCount(lines));
    }

    private static BlockModelBuilder builder(BlockStateProvider provider, String name, int lines) {
        List<Line> present = DuctParts.lines(lines);
        BlockModelBuilder model = provider.models().getBuilder("block/duct/" + name)
                .texture("particle", provider.modLoc("block/duct_" + present.get(0).id()));
        int thickness = 2 * radius(lines);
        for (Line line : Line.VALUES) {
            model.texture(line.id(), provider.modLoc("block/duct_" + line.id() + (present.size() >= 2 ? "_" + thickness : "")));
        }
        if (present.size() >= 2) {
            model.texture("bands", provider.modLoc("block/duct_" + DuctParts.suffix(lines)));
        }
        return model.texture("ring", provider.modLoc("block/duct_ring"))
                .texture("fitting", provider.modLoc("block/duct_fitting_" + DuctShapes.fittingRadius(present.size())));
    }

    private static void junction(BlockStateProvider provider, int lines) {
        BlockModelBuilder model = builder(provider, DuctParts.centerName(lines, null), lines);
        int r = DuctShapes.fittingRadius(Integer.bitCount(lines));
        float lo = 8 - r;
        float hi = 8 + r;
        model.element().from(lo, lo, lo).to(hi, hi, hi).allFaces((direction, face) -> face.texture("#fitting")).end();
    }

    private static ModelFile center(BlockStateProvider provider, int lines, Direction.Axis along) {
        BlockModelBuilder model = builder(provider, DuctParts.centerName(lines, along), lines);
        int r = radius(lines);
        float lo = 8 - r;
        float hi = 8 + r;
        skinBox(model, lines, along, lo, lo, lo, hi, hi, hi);
        return model;
    }

    private static void arm(BlockStateProvider provider, Direction direction, int lines) {
        BlockModelBuilder model = builder(provider, DuctParts.armName(direction, lines), lines);
        int r = radius(lines);
        Direction.Axis along = direction.getAxis();
        float lo = 8 - r;
        float hi = 8 + r;
        boolean positive = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        // The arm runs from the block edge to the centre face, 1 px inside a thicker centre.
        float inner = positive ? hi : lo;
        float outer = positive ? 16 : 0;
        float[] min = {lo, lo, lo};
        float[] max = {hi, hi, hi};
        set(min, along, Math.min(inner, outer));
        set(max, along, Math.max(inner, outer));
        skinBox(model, lines, along, min[0], min[1], min[2], max[0], max[1], max[2]);
        ring(model, direction, lines, r);
    }

    private static Direction.Axis across(Direction.Axis along) {
        return along == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    /** A box with the line skin; a several line box shows the bands running along {@code along}. */
    private static void skinBox(BlockModelBuilder model, int lines, Direction.Axis along,
                                float x0, float y0, float z0, float x1, float y1, float z1) {
        BlockModelBuilder.ElementBuilder element = model.element().from(x0, y0, z0).to(x1, y1, z1);
        List<Line> present = DuctParts.lines(lines);
        if (present.size() == 1) {
            element.allFaces((direction, face) -> face.texture("#" + present.get(0).id()));
            element.end();
            return;
        }
        Direction.Axis across = across(along);
        for (Direction face : Direction.values()) {
            BlockModelBuilder.ElementBuilder.FaceBuilder builder = element.face(face);
            if (face.getAxis() == across) {
                Line outer = face.getAxisDirection() == Direction.AxisDirection.NEGATIVE
                        ? present.get(0) : present.get(present.size() - 1);
                builder.texture("#" + outer.id());
            } else {
                builder.texture("#bands");
            }
            builder.end();
        }
        element.end();
    }

    /**
     * The ring frame at the outer end of an arm: four slabs around the arm's
     * cross section, 1 px deep and half a pixel proud, without the faces
     * that touch the arm. With several lines the frame is split between them
     * like the bands: the slab on the negative across side belongs to the
     * first line, the one on the positive side to the last, and the slabs on
     * the third axis are cut into one equal piece per line.
     */
    private static void ring(BlockModelBuilder model, Direction direction, int lines, int r) {
        Direction.Axis along = direction.getAxis();
        Direction.Axis across = across(along);
        Direction.Axis third = third(along, across);
        boolean positive = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        float a0 = positive ? 16 - DuctShapes.RING_DEPTH : 0;
        float a1 = positive ? 16 : DuctShapes.RING_DEPTH;
        float in0 = 8 - r;
        float in1 = 8 + r;
        float out0 = in0 - RING_LIP;
        float out1 = in1 + RING_LIP;
        List<Line> present = DuctParts.lines(lines);
        int first = DuctParts.ringTint(present.get(0), direction);
        int last = DuctParts.ringTint(present.get(present.size() - 1), direction);
        // Slabs on the across axis span the full width; slabs on the third axis sit between them.
        slab(model, along, a0, a1, across, out0, in0, third, out0, out1, Direction.get(Direction.AxisDirection.POSITIVE, across), first);
        slab(model, along, a0, a1, across, in1, out1, third, out0, out1, Direction.get(Direction.AxisDirection.NEGATIVE, across), last);
        int n = present.size();
        for (int i = 0; i < n; i++) {
            int tint = DuctParts.ringTint(present.get(i), direction);
            float c0 = in0 + (in1 - in0) * i / n;
            float c1 = in0 + (in1 - in0) * (i + 1) / n;
            slab(model, along, a0, a1, third, out0, in0, across, c0, c1, Direction.get(Direction.AxisDirection.POSITIVE, third), tint);
            slab(model, along, a0, a1, third, in1, out1, across, c0, c1, Direction.get(Direction.AxisDirection.NEGATIVE, third), tint);
        }
    }

    private static Direction.Axis third(Direction.Axis a, Direction.Axis b) {
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (axis != a && axis != b) {
                return axis;
            }
        }
        throw new IllegalArgumentException("axes must differ");
    }

    /** One ring slab spanning the given ranges per axis, all faces tinted except the one facing {@code skip}. */
    private static void slab(BlockModelBuilder model, Direction.Axis alongAxis, float a0, float a1,
                             Direction.Axis axisB, float b0, float b1, Direction.Axis axisC, float c0, float c1,
                             Direction skip, int tint) {
        float[] min = new float[3];
        float[] max = new float[3];
        set(min, alongAxis, a0);
        set(max, alongAxis, a1);
        set(min, axisB, b0);
        set(max, axisB, b1);
        set(min, axisC, c0);
        set(max, axisC, c1);
        BlockModelBuilder.ElementBuilder element = model.element().from(min[0], min[1], min[2]).to(max[0], max[1], max[2]);
        for (Direction face : Direction.values()) {
            if (face == skip) {
                continue;
            }
            element.face(face).texture("#ring").tintindex(tint).end();
        }
        element.end();
    }

    private static void set(float[] vector, Direction.Axis axis, float value) {
        vector[axis.ordinal()] = value;
    }
}
