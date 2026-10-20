package io.github.dirnei.mc.servus.brewery.brewhouse;

import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Cell;
import io.github.dirnei.mc.servus.brewery.brewhouse.ModelSlicer.Piece;
import io.github.dirnei.mc.servus.foundation.machine.PartShapes;
import io.github.dirnei.mc.servus.foundation.machine.PixelBoxes;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Block shapes of the formed Brewhouse cells from the same parts as their models, and structure points in the world. */
public final class BrewhouseShapes {

    private static final Map<Cell, Map<Direction, VoxelShape>> SHAPES = new EnumMap<>(Cell.class);

    static {
        for (Cell cell : Cell.values()) {
            List<float[]> boxes = new ArrayList<>();
            for (Piece piece : ModelSlicer.piecesIn(BrewhouseModel.PARTS, cell)) {
                float[] clipped = piece.clipped();
                if (clipped != null) {
                    boxes.add(clipped);
                }
            }
            float[][] parts = boxes.toArray(float[][]::new);
            Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                byFacing.put(facing, PartShapes.shapeOf(parts, facing));
            }
            SHAPES.put(cell, byFacing);
        }
    }

    private BrewhouseShapes() {
    }

    /** The shape of a formed cell whose Brewhouse faces {@code facing}. */
    public static VoxelShape shape(Cell cell, Direction facing) {
        return SHAPES.get(cell).get(facing);
    }

    /** A structure point (pixels, see {@link ModelSlicer}) in the world, seen from the block of {@code cell} at {@code pos}. */
    public static Vec3 worldPoint(BlockPos pos, Cell cell, Direction facing, float u, float v, float y) {
        float[] local = ModelSlicer.localPoint(cell, u, v, y);
        float[] turned = PixelBoxes.rotatedPoint(local[0], local[2], PartShapes.quarters(facing));
        return new Vec3(pos.getX() + turned[0] / 16.0, pos.getY() + local[1] / 16.0, pos.getZ() + turned[1] / 16.0);
    }
}
