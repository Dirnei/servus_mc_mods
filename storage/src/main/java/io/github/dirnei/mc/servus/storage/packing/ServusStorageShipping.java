package io.github.dirnei.mc.servus.storage.packing;

import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Asks a warehouse to ship items to a data member. Server thread only. */
public final class ServusStorageShipping {

    private ServusStorageShipping() {
    }

    /** Ships through the warehouse of the controller at {@code controller}; refusal {@code no_warehouse} when there is none. */
    public static WarehouseNetwork.Shipping ship(ServerLevel level, BlockPos controller, BlockPos target, ItemStack like, int amount) {
        Optional<WarehouseNetwork> network = level.getBlockEntity(controller) instanceof WarehouseControllerBlockEntity block
                ? block.network() : Optional.empty();
        if (network.isEmpty() || like.isEmpty() || amount < 1) {
            return new WarehouseNetwork.Shipping("no_warehouse", Optional.empty(), null);
        }
        return network.get().ship(target, ItemKey.of(like), amount);
    }
}
