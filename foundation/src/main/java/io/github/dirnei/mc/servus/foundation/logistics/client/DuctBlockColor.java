package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DataCableItem;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctChannels;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctParts;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Colors the ring faces of a duct arm in the channel color of that line and
 * side. Tint indices from {@link PanelQuads#PANEL_TINT_BASE} belong to a
 * panel's own model and are handed to the panel block's tint source. A block
 * registers one tint source per tint index, so {@link #sources()} lists one
 * source for every index the duct uses.
 */
public final class DuctBlockColor implements BlockTintSource {

    private static final int WHITE = 0xFFFFFFFF;
    /** The base Data Cable's ring colour, used when the tier item is unknown. */
    private static final int DATA_RING = 0xFFF4F4F4;

    private final int tintIndex;

    private DuctBlockColor(int tintIndex) {
        this.tintIndex = tintIndex;
    }

    /** The tint sources for tint indices 0 up to the last panel index. */
    public static List<BlockTintSource> sources() {
        int count = PanelQuads.PANEL_TINT_BASE + 6 * PanelQuads.TINTS_PER_SIDE;
        List<BlockTintSource> sources = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            sources.add(new DuctBlockColor(index));
        }
        return sources;
    }

    @Override
    public int color(BlockState state) {
        return WHITE;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct)) {
            return WHITE;
        }
        if (tintIndex >= PanelQuads.PANEL_TINT_BASE) {
            int offset = tintIndex - PanelQuads.PANEL_TINT_BASE;
            int face = offset / PanelQuads.TINTS_PER_SIDE;
            BlockState panel = face < 6 ? duct.panel(Direction.values()[face]) : null;
            if (panel == null) {
                return WHITE;
            }
            BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(panel, offset % PanelQuads.TINTS_PER_SIDE);
            return source == null ? WHITE : source.colorInWorld(panel, level, pos);
        }
        Line line = DuctParts.ringLine(tintIndex);
        if (line == null) {
            return WHITE;
        }
        if (line == Line.DATA) {
            // The data line has no channels; its ring shows the tier.
            return duct.lineItem(Line.DATA) instanceof DataCableItem cable ? cable.ringColour() | 0xFF000000 : DATA_RING;
        }
        return DuctChannels.color(duct.channel(line, DuctParts.ringSide(tintIndex))) | 0xFF000000;
    }
}
