package io.github.dirnei.mc.servus.storage.station;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/** What each world side of a station does, cycled by the carrot in a fixed order. */
public final class SideModes {

    public enum Mode {
        OFF('-'), ACCEPT('+'), PUSH('>'), EXTRACT('<');

        private final char symbol;

        Mode(char symbol) {
            this.symbol = symbol;
        }

        /** One character for the GUI and the sync payload. */
        public char symbol() {
            return symbol;
        }

        public static Mode bySymbol(char symbol) {
            for (Mode mode : values()) {
                if (mode.symbol == symbol) {
                    return mode;
                }
            }
            return OFF;
        }
    }

    private final Map<Direction, Mode> modes = new EnumMap<>(Direction.class);
    private final List<Mode> cycle;

    public SideModes(List<Mode> cycle, Mode initial) {
        this.cycle = List.copyOf(cycle);
        for (Direction direction : Direction.values()) {
            modes.put(direction, initial);
        }
    }

    public Mode mode(Direction side) {
        return modes.getOrDefault(side, Mode.OFF);
    }

    public void set(Direction side, Mode mode) {
        modes.put(side, cycle.contains(mode) ? mode : cycle.get(0));
    }

    /** Advances the side to the next mode of the cycle and returns it. */
    public Mode cycle(Direction side) {
        int index = cycle.indexOf(mode(side));
        Mode next = cycle.get((index + 1) % cycle.size());
        modes.put(side, next);
        return next;
    }

    /** Six symbols in {@link Direction} order, for the sync payload. */
    public String symbols() {
        StringBuilder builder = new StringBuilder();
        for (Direction direction : Direction.values()) {
            builder.append(mode(direction).symbol());
        }
        return builder.toString();
    }

    public void save(CompoundTag tag) {
        tag.putString("Sides", symbols());
    }

    /** Loads the modes when the tag has them; otherwise keeps the current ones. */
    public void load(CompoundTag tag) {
        if (!tag.contains("Sides")) {
            return;
        }
        String symbols = tag.getString("Sides");
        for (Direction direction : Direction.values()) {
            if (direction.ordinal() < symbols.length()) {
                set(direction, Mode.bySymbol(symbols.charAt(direction.ordinal())));
            }
        }
    }
}
