package io.github.dirnei.mc.servus.lib.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity that joins its network when loaded and leaves it when removed
 * or when its chunk unloads. Subclasses implement {@link #networkType()} and
 * {@link #networkNode()} and call {@link #refreshNetworkConnections()} after
 * a change that affects which neighbours they connect to.
 *
 * @param <N> node type stored in the graph
 */
public abstract class NetworkBlockEntity<N> extends BlockEntity implements NetworkNode<N> {

    private boolean joined;

    protected NetworkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!joined && level instanceof ServerLevel server) {
            NetworkManager.of(server).join(worldPosition, this);
            joined = true;
        }
    }

    @Override
    public void setRemoved() {
        leaveNetwork();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        leaveNetwork();
    }

    public final boolean isInNetwork() {
        return joined;
    }

    protected final void leaveNetwork() {
        if (joined && level instanceof ServerLevel server) {
            NetworkManager.of(server).leave(worldPosition, this);
            joined = false;
        }
    }

    protected final void refreshNetworkConnections() {
        if (joined && level instanceof ServerLevel server) {
            NetworkManager.of(server).refresh(worldPosition, this);
        }
    }
}
