package io.github.dirnei.mc.servus.storage.network;

import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.lib.network.NetworkBlockEntity;
import io.github.dirnei.mc.servus.lib.network.NetworkManager;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Base of every block that takes part in a warehouse network. */
public abstract class StorageNodeBlockEntity extends NetworkBlockEntity<StorageNodeBlockEntity> {

    protected StorageNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public NetworkType<StorageNodeBlockEntity, ?, ?> networkType() {
        return WarehouseNetwork.TYPE;
    }

    @Override
    public StorageNodeBlockEntity networkNode() {
        return this;
    }

    /**
     * Whether this node is indexed as a source and pulls neighbouring
     * inventories into the warehouse. Connector nodes such as rails return false.
     */
    public boolean holdsStock() {
        return true;
    }

    /** The warehouse network this node belongs to, when loaded on the server. */
    public Optional<WarehouseNetwork> network() {
        if (!(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        return NetworkManager.of(server).set(WarehouseNetwork.TYPE).networkAt(Conversions.toNodePos(worldPosition));
    }

    /** Tells the network this node's contents changed. */
    protected void postDirty() {
        network().ifPresent(network -> network.post(new WarehouseEvent.Dirty(worldPosition)));
    }

    /** Called by storage blocks from {@code neighborChanged}. */
    public static void neighborsChanged(Level level, BlockPos pos) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageNodeBlockEntity node) {
            node.network().ifPresent(network -> network.post(new WarehouseEvent.NeighborsChanged(pos)));
        }
    }
}
