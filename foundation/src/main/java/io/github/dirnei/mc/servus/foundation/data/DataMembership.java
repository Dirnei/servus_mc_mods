package io.github.dirnei.mc.servus.foundation.data;

import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Which blocks are data members: those exposing the production, stock or data link capabilities. Server side. */
public final class DataMembership {

    private DataMembership() {
    }

    /** Whether the block at {@code pos} exposes a data capability on {@code side}. */
    public static boolean isMember(Level level, BlockPos pos, Direction side) {
        return level.getCapability(ServusFoundationCapabilities.PRODUCTION, pos, side) != null
                || level.getCapability(ServusFoundationCapabilities.STOCK_PROVIDER, pos, side) != null
                || level.getCapability(ServusFoundationCapabilities.STOCK_SINK, pos, side) != null
                || level.getCapability(ServusFoundationCapabilities.DATA_LINK, pos, side) != null
                || level.getCapability(ServusFoundationCapabilities.UPKEEP, pos, side) != null;
    }
}
