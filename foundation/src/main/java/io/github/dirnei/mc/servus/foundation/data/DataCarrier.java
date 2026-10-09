package io.github.dirnei.mc.servus.foundation.data;

import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import io.github.dirnei.mc.servus.foundation.network.NetworkNode;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Makes a block entity carry the data network to touching data nodes, like a
 * piece of cable: terminals and other devices that sit side by side form one
 * network. The block is a member of that network with the roles of its own
 * capabilities, but never makes its other neighbours members. The owner calls
 * {@link #join()} when it loads or becomes able to carry, {@link #leave()}
 * when it is removed, unloads or stops carrying.
 */
public final class DataCarrier implements DataNode, NetworkNode<DataNode> {

    private final BlockEntity owner;
    private boolean joined;

    public DataCarrier(BlockEntity owner) {
        this.owner = owner;
    }

    public void join() {
        if (!joined && owner.getLevel() instanceof ServerLevel server && !owner.isRemoved()) {
            NetworkManager.of(server).join(owner.getBlockPos(), this);
            joined = true;
        }
    }

    public void leave() {
        if (joined && owner.getLevel() instanceof ServerLevel server) {
            NetworkManager.of(server).leave(owner.getBlockPos(), this);
            joined = false;
        }
    }

    /** Joins when {@code carry} is true, leaves otherwise. */
    public void carry(boolean carry) {
        if (carry) {
            join();
        } else {
            leave();
        }
    }

    public boolean joined() {
        return joined;
    }

    @Override
    public NetworkType<DataNode, ?, ?> networkType() {
        return DataNetwork.TYPE;
    }

    @Override
    public DataNode networkNode() {
        return this;
    }

    @Override
    public Level dataLevel() {
        return owner.getLevel();
    }

    @Override
    public boolean dataRemoved() {
        return owner.isRemoved() || !joined;
    }

    @Override
    public boolean dataSideOpen(Direction side) {
        return true;
    }

    @Override
    public boolean dataCable() {
        return false;
    }
}
