package io.github.dirnei.mc.servus.shuttles.packing;

import io.github.dirnei.mc.servus.foundation.production.ItemSupplier;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalWarehouses;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The supplier capability of a Warehouse Controller or a Packing Station: ships
 * what the warehouse holds to a data member, like any shipment.
 */
public record WarehouseSupplier(StorageNodeBlockEntity node) implements ItemSupplier {

    @Override
    public int supply(BlockPos target, ItemStack like, int amount) {
        Optional<WarehouseNetwork> network = node.network();
        if (network.isEmpty() || like.isEmpty() || amount < 1 || lineConflict()) {
            return 0;
        }
        ItemKey key = ItemKey.of(like);
        int asked = (int) Math.min(amount, network.get().available(key));
        if (asked < 1) {
            return 0;
        }
        WarehouseNetwork.Shipping shipping = network.get().ship(target, key, asked);
        return shipping.shipment().map(PackingStationBlockEntity.Shipment::planned).orElse(0);
    }

    private boolean lineConflict() {
        return node.getLevel() instanceof ServerLevel server && TerminalWarehouses.line(server, node.getBlockPos()).conflict();
    }
}
