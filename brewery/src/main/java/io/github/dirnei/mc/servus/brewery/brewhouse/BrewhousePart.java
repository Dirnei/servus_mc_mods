package io.github.dirnei.mc.servus.brewery.brewhouse;

import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Cell;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** The cell a formed Brewhouse part takes, as a block state value; the twin of {@link Cell}. */
public enum BrewhousePart implements StringRepresentable {
    KETTLE_FRONT_LEFT(Cell.KETTLE_FRONT_LEFT),
    KETTLE_FRONT_RIGHT(Cell.KETTLE_FRONT_RIGHT),
    KETTLE_BACK_LEFT(Cell.KETTLE_BACK_LEFT),
    KETTLE_BACK_RIGHT(Cell.KETTLE_BACK_RIGHT),
    KILN_FRONT(Cell.KILN_FRONT),
    KILN_BACK(Cell.KILN_BACK),
    COPPER_FRONT_LEFT(Cell.COPPER_FRONT_LEFT),
    COPPER_FRONT_RIGHT(Cell.COPPER_FRONT_RIGHT),
    COPPER_BACK_LEFT(Cell.COPPER_BACK_LEFT),
    COPPER_BACK_RIGHT(Cell.COPPER_BACK_RIGHT);

    private final Cell cell;

    BrewhousePart(Cell cell) {
        this.cell = cell;
    }

    public Cell cell() {
        return cell;
    }

    public static BrewhousePart of(Cell cell) {
        return values()[cell.ordinal()];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
