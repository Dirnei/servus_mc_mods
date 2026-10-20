package io.github.dirnei.mc.servus.foundation.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Block shapes from part boxes in pixels of models built facing north. */
public final class PartShapes {

    private PartShapes() {
    }

    /** Quarter turns from the north built models to {@code facing}, as the blockstate rotates them. */
    public static int quarters(Direction facing) {
        return ((int) facing.toYRot() + 180) % 360 / 90;
    }

    /** A box in pixels as a shape. */
    public static VoxelShape box(float[] box) {
        return Shapes.box(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0, box[4] / 16.0, box[5] / 16.0);
    }

    /** The union of the parts turned to {@code facing}. */
    public static VoxelShape shapeOf(float[][] parts, Direction facing) {
        VoxelShape shape = Shapes.empty();
        for (float[] part : parts) {
            shape = Shapes.or(shape, box(PixelBoxes.rotated(part, quarters(facing))));
        }
        return shape;
    }
}
