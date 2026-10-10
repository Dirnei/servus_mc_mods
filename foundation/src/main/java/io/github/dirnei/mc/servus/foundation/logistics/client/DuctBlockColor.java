package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DataCableItem;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctChannels;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctParts;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Colors the ring faces of a duct arm in the channel color of that line and
 * side. Tint indices below the ring range belong to a facade's own model and
 * are handed to the facade block's color.
 */
public final class DuctBlockColor implements BlockColor {

    private static final int WHITE = 0xFFFFFF;
    /** The base Data Cable's ring colour, used when the tier item is unknown. */
    private static final int DATA_RING = 0xF4F4F4;

    @Override
    public int getColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct)) {
            return WHITE;
        }
        Line line = DuctParts.ringLine(tintIndex);
        if (line == null) {
            return WHITE;
        }
        if (line == Line.DATA) {
            // The data line has no channels; its ring shows the tier.
            return duct.lineItem(Line.DATA) instanceof DataCableItem cable ? cable.ringColour() & WHITE : DATA_RING;
        }
        return DuctChannels.color(duct.channel(line, DuctParts.ringSide(tintIndex))) & WHITE;
    }
}
