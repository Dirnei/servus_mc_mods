package io.github.dirnei.mc.servus.foundation.machine;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * Cached energy capabilities of the six neighbours of a position. NeoForge
 * invalidates the caches when neighbours change, so there is no scanning.
 */
public final class EnergyNeighbors {

    private final BlockCapabilityCache<EnergyHandler, Direction>[] caches;

    public EnergyNeighbors(ServerLevel level, BlockPos pos) {
        this(level, pos, null);
    }

    /** The neighbours of every face but {@code skipped}, which is never looked at; null skips none. */
    @SuppressWarnings("unchecked")
    public EnergyNeighbors(ServerLevel level, BlockPos pos, Direction skipped) {
        caches = new BlockCapabilityCache[Direction.values().length];
        for (Direction direction : Direction.values()) {
            if (direction != skipped) {
                caches[direction.ordinal()] = BlockCapabilityCache.create(
                        Capabilities.Energy.BLOCK, level, pos.relative(direction), direction.getOpposite());
            }
        }
    }

    public EnergyHandler get(Direction direction) {
        BlockCapabilityCache<EnergyHandler, Direction> cache = caches[direction.ordinal()];
        return cache == null ? null : cache.getCapability();
    }

    /** Visits every neighbour that currently offers an energy capability. */
    public void forEach(BiConsumer<Direction, EnergyHandler> visitor) {
        for (Direction direction : Direction.values()) {
            EnergyHandler storage = get(direction);
            if (storage != null) {
                visitor.accept(direction, storage);
            }
        }
    }
}
