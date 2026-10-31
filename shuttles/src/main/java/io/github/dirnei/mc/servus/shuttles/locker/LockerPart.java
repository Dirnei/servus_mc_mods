package io.github.dirnei.mc.servus.shuttles.locker;

import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/**
 * The ten blocks of a Parcel Locker by their column, 0 to 4 from the left as
 * seen from the front, and their row, 0 below and 1 above. The dock holds
 * the locker's block entity.
 */
public enum LockerPart implements StringRepresentable {
    S_COLUMN_LOW(0, 0),
    S_COLUMN_HIGH(0, 1),
    MIXED_LOW(1, 0),
    MIXED_HIGH(1, 1),
    HEAD_DOCK(2, 0),
    HEAD_SCREEN(2, 1),
    M_COLUMN_LOW(3, 0),
    M_COLUMN_HIGH(3, 1),
    L_LOW(4, 0),
    L_HIGH(4, 1);

    private final int column;
    private final int row;

    LockerPart(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public int column() {
        return column;
    }

    public int row() {
        return row;
    }

    /** The direction that is right for someone looking at a locker whose front faces {@code facing}. */
    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    /** Where this part stands in a locker whose dock is at {@code dock}. */
    public BlockPos at(BlockPos dock, Direction facing) {
        return dock.relative(right(facing), column - LockerLayout.HEAD_COLUMN).above(row);
    }

    /** The dock of the locker this part at {@code pos} belongs to. */
    public BlockPos dock(BlockPos pos, Direction facing) {
        return pos.relative(right(facing), LockerLayout.HEAD_COLUMN - column).below(row);
    }

    /**
     * The boxes whose lamp sits on this part, at most two, top first: a box
     * shows its lamp near its top edge, in the block that edge lies in.
     */
    public List<LockerLayout.Box> lampBoxes() {
        return LockerLayout.boxesIn(column, row).stream()
                .filter(box -> box.top() > row * LockerLayout.BLOCK && box.top() <= (row + 1) * LockerLayout.BLOCK)
                .toList();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
