package io.github.dirnei.mc.servus.shuttles.field;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

/**
 * Keeps the ripeness of the fields current from game events instead of
 * scanning: crop growth updates a plot at once, bone meal, a sapling growing
 * into a tree, breaking, placing and trampling queue the plot for the head's
 * next tick, when the block has changed. Chunk loads refresh the fields that
 * reach into them.
 */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID)
public final class FieldEvents {

    private FieldEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCropGrow(CropGrowEvent.Post event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            FieldHeads.coveringPlot(level, event.getPos()).ifPresent(head -> head.update(event.getPos(), event.getState()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBonemeal(BonemealEvent event) {
        if (!event.isCanceled()) {
            queue(event.getLevel(), event.getPos());
        }
    }

    /**
     * A sapling is about to grow into a tree; the tree may start from any of
     * the four saplings of a square, so its neighbours are read too, on the
     * next tick, after the tree stands or failed to grow.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGrowFeature(BlockGrowFeatureEvent event) {
        if (event.isCanceled()) {
            return;
        }
        BlockPos pos = event.getPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                queue(event.getLevel(), pos.offset(dx, 0, dz));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BreakBlockEvent event) {
        if (!event.isCanceled()) {
            queue(event.getLevel(), event.getPos());
            queue(event.getLevel(), event.getPos().above());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.isCanceled()) {
            queue(event.getLevel(), event.getPos());
            queue(event.getLevel(), event.getPos().above());
        }
    }

    /** A hoe or another tool changed the ground under a plot. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!event.isCanceled() && !event.isSimulated()) {
            queue(event.getLevel(), event.getPos().above());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!event.isCanceled()) {
            queue(event.getLevel(), event.getPos().above());
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // Fired while the chunk loads, possibly off the server thread: the refresh runs on the server thread later.
        if (event.getLevel() instanceof ServerLevel level) {
            ChunkPos chunk = event.getChunk().getPos();
            level.getServer().execute(() -> FieldHeads.chunkLoaded(level, chunk));
        }
    }

    private static void queue(LevelAccessor accessor, BlockPos pos) {
        if (accessor instanceof Level level && !level.isClientSide()) {
            FieldHeads.coveringPlot(level, pos).ifPresent(head -> head.recheck(pos));
        }
    }
}
