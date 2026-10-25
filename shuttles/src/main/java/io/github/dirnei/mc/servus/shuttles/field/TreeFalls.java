package io.github.dirnei.mc.servus.shuttles.field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tells the players near a felled tree how it falls, on the server thread,
 * before its blocks are removed in the same tick: the payload goes out at
 * once while the block changes follow with the chunk's next flush, so no
 * client draws a frame without either tree.
 */
public final class TreeFalls {

    /** The percent of the topple part the fall and the break apart take; the head swings home in the rest. */
    public static final int FALL_SHARE = HarvesterTimeline.FALL_PERCENT;

    private TreeFalls() {
    }

    /**
     * @param base  the plots the tree stands on
     * @param rail  the rail the Lumber shuttle stands on
     * @param tree  the blocks felling removes
     * @param drops the tree's drops
     * @param ticks how long the fall and break apart last
     */
    public static void send(ServerLevel level, FieldHeadBlockEntity head, Collection<BlockPos> base, BlockPos rail, TreeWalk.Felled tree,
                            List<ItemStack> drops, int ticks) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int y = Integer.MAX_VALUE;
        double sumX = 0;
        double sumZ = 0;
        for (BlockPos plot : base) {
            minX = Math.min(minX, plot.getX());
            minZ = Math.min(minZ, plot.getZ());
            y = Math.min(y, plot.getY());
            sumX += plot.getX();
            sumZ += plot.getZ();
        }
        BlockPos corner = new BlockPos(minX, y, minZ);
        boolean runsAlongX = head.rails().contains(rail.east()) || head.rails().contains(rail.west());
        FellDirection.Way way = FellDirection.away(sumX / base.size() - rail.getX(), sumZ / base.size() - rail.getZ(), runsAlongX);
        List<TreeFallPayload.FallingBlock> blocks = new ArrayList<>();
        for (BlockPos pos : HarvesterWork.treeBlocks(tree)) {
            blocks.add(new TreeFallPayload.FallingBlock(pos.getX() - minX, pos.getY() - y, pos.getZ() - minZ, level.getBlockState(pos)));
        }
        TreeFallPayload payload = new TreeFallPayload(corner, direction(way), rail, ticks, blocks, TreeFallPayload.merged(drops));
        PacketDistributor.sendToPlayersTrackingChunk(level, ChunkPos.containing(corner), payload);
    }

    private static Direction direction(FellDirection.Way way) {
        return switch (way) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
        };
    }
}
