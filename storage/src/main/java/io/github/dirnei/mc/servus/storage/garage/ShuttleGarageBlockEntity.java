package io.github.dirnei.mc.servus.storage.garage;

import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Keeps a few shuttles and parks them on the rail in front of it. The live
 * shuttles are data on the network's fleet; this only remembers how many.
 */
public class ShuttleGarageBlockEntity extends StorageNodeBlockEntity {

    private int shuttleCount;

    public ShuttleGarageBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.SHUTTLE_GARAGE.get(), pos, state);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.GARAGE;
    }

    public int shuttleCount() {
        return shuttleCount;
    }

    public int capacity() {
        return StorageStatsManager.INSTANCE.garageCapacity();
    }

    /** @return whether a shuttle was added; refused when full */
    public boolean addShuttle() {
        if (shuttleCount >= capacity()) {
            return false;
        }
        shuttleCount++;
        fleetChanged();
        return true;
    }

    /** @return whether a shuttle was taken out */
    public boolean takeShuttle() {
        if (shuttleCount <= 0) {
            return false;
        }
        shuttleCount--;
        fleetChanged();
        return true;
    }

    /** Forgets the shuttles, for when they were dropped as items. */
    public void clearShuttles() {
        shuttleCount = 0;
        fleetChanged();
    }

    private void fleetChanged() {
        setChanged();
        network().ifPresent(network -> network.garageChanged(this));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Shuttles", shuttleCount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        shuttleCount = tag.getInt("Shuttles");
    }
}
