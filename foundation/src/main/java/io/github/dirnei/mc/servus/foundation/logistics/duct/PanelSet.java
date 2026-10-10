package io.github.dirnei.mc.servus.foundation.logistics.duct;

import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The panels of a duct for its model: the block per side (null without a
 * panel) and the six bit mask of hollow sides. Immutable.
 */
public final class PanelSet {

    private final BlockState[] states;
    private final int hollowMask;

    public PanelSet(BlockState[] states, int hollowMask) {
        this.states = states.clone();
        this.hollowMask = hollowMask;
    }

    /** The panel block on a side, or null. */
    public BlockState state(Direction side) {
        return states[side.ordinal()];
    }

    public boolean isHollow(Direction side) {
        return (hollowMask & DuctModelData.bit(side)) != 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PanelSet set && set.hollowMask == hollowMask && Arrays.equals(set.states, states);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(states) + hollowMask;
    }
}
