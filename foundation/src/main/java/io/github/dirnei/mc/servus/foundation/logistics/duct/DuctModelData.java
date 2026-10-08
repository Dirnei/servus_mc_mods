package io.github.dirnei.mc.servus.foundation.logistics.duct;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * Model data keys of the utility duct, kept out of client only classes. Side
 * masks use {@code 1 << direction.ordinal()}; the per line arrays are indexed
 * by {@link Line#ordinal()}.
 */
public final class DuctModelData {

    /** The facade block state, absent without a facade. */
    public static final ModelProperty<BlockState> FACADE = new ModelProperty<>();

    /** Bitmask of the present lines, {@code 1 << line.ordinal()}. */
    public static final ModelProperty<Integer> LINES = new ModelProperty<>();

    /** Per line, the six bit mask of sides that carry an arm of that line. */
    public static final ModelProperty<int[]> ARMS = new ModelProperty<>();

    /** Six bit mask of sides whose arm ends at an inventory or machine. */
    public static final ModelProperty<Integer> ENDPOINTS = new ModelProperty<>();

    /** Channel per line and side, index {@code line.ordinal() * 6 + side.ordinal()}. */
    public static final ModelProperty<int[]> CHANNELS = new ModelProperty<>();

    private DuctModelData() {
    }

    public static int bit(net.minecraft.core.Direction side) {
        return 1 << side.ordinal();
    }

    public static int channelIndex(Line line, net.minecraft.core.Direction side) {
        return line.ordinal() * 6 + side.ordinal();
    }
}
