package io.github.dirnei.mc.servus.storage.controller;

import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Exactly one per network makes the warehouse online. Shuttles live in garages. */
public class WarehouseControllerBlockEntity extends StorageNodeBlockEntity {

    /** Shuttles saved by an older build, dropped as items on the first load. */
    private int legacyShuttles;

    public WarehouseControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.WAREHOUSE_CONTROLLER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (legacyShuttles > 0 && level instanceof ServerLevel) {
            Block.popResource(level, worldPosition, new ItemStack(ServusStorageItems.SHUTTLE.get(), legacyShuttles));
            legacyShuttles = 0;
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        legacyShuttles = tag.getInt("Shuttles");
    }
}
