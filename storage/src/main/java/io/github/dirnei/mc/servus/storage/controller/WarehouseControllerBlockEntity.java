package io.github.dirnei.mc.servus.storage.controller;

import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.zone.ZoneRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exactly one per network makes the warehouse online. Also remembers how
 * many shuttles the warehouse owns; the live fleet lives on the network.
 */
public class WarehouseControllerBlockEntity extends StorageNodeBlockEntity {

    private int shuttleCount;
    private final ZoneRules rules = new ZoneRules();

    public WarehouseControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.WAREHOUSE_CONTROLLER.get(), pos, state);
        rules.onChanged(() -> {
            setChanged();
            network().ifPresent(network -> network.rulesChanged());
        });
    }

    /** The put-away rules of this warehouse. */
    public ZoneRules rules() {
        return rules;
    }

    public int shuttleCount() {
        return shuttleCount;
    }

    /** @return whether a shuttle was added; refused at the cap */
    public boolean addShuttle() {
        if (shuttleCount >= StorageStatsManager.INSTANCE.maxShuttles()) {
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
        network().ifPresent(network -> network.fleetChanged(this));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Shuttles", shuttleCount);
        tag.put("ZoneRules", rules.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        shuttleCount = tag.getInt("Shuttles");
        if (tag.contains("ZoneRules")) {
            rules.load(tag.getCompound("ZoneRules"), registries);
        }
    }
}
