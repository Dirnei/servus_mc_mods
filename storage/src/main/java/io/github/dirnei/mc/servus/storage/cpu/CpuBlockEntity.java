package io.github.dirnei.mc.servus.storage.cpu;

import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One block of a Crafting CPU. Every formed block is a link member of the
 * data line it touches; the master (part 0) owns the CPU's work orders.
 */
public class CpuBlockEntity extends BlockEntity implements DataLink {

    public CpuBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.CRAFTING_CPU.get(), pos, state);
    }

    /** This block as a data link while formed, else nothing. */
    public DataLink dataLink() {
        return CpuBlock.isFormed(getBlockState()) ? this : null;
    }

    /** Orders one CPU runs at a time. */
    public int slots() {
        return Math.max(1, StorageStatsManager.INSTANCE.cpuOrderSlots());
    }

    void serverTick() {
    }

    /** Cancels every open order; called when the cube unforms. */
    void cancelAll() {
    }

    Component summary() {
        return Component.translatable("message.servus_storage.cpu.summary", 0, slots(), 0);
    }
}
