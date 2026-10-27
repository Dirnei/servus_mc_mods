package io.github.dirnei.mc.servus.shuttles.network;

import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * What the activity lamp of a block with tanker served tanks shows, and why
 * it is red: the fluid that no fluid store has room for, no Tanker shuttle
 * reaching it, or the fluid a kept tank lacks that no fluid store holds.
 */
public record FluidActivity(StorageNodeBlocks.Activity lamp, @Nullable Fluid noRoom, boolean noTanker, @Nullable Fluid missing) {

    public static final FluidActivity IDLE = new FluidActivity(StorageNodeBlocks.Activity.IDLE, null, false, null);
}
