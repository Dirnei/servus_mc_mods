package io.github.dirnei.mc.servus.shuttles.packing;

import io.github.dirnei.mc.servus.foundation.production.ItemSupplier;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.order.ProductionLedger;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalWarehouses;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The supplier capability of a Warehouse Controller or a Packing Station: ships
 * what the warehouse holds to a data member, like any shipment. The ticket is
 * the shipment's ledger id, so it stays valid across a reload.
 */
public record WarehouseSupplier(StorageNodeBlockEntity node) implements ItemSupplier {

    @Override
    public Supply supply(BlockPos target, ItemStack like, int amount) {
        Optional<WarehouseNetwork> network = node.network();
        if (network.isEmpty() || like.isEmpty() || amount < 1 || !(node.getLevel() instanceof ServerLevel server)
                || lineConflict(server)) {
            return Supply.NONE;
        }
        ItemKey key = ItemKey.of(like);
        int asked = (int) Math.min(amount, network.get().available(key));
        if (asked < 1) {
            return Supply.NONE;
        }
        int requestId = ProductionLedger.get(server).nextRequestId();
        WarehouseNetwork.Shipping shipping = network.get().ship(target, key, asked, requestId);
        return shipping.shipment().map(shipment -> new Supply(shipment.planned(), shipment.id())).orElse(Supply.NONE);
    }

    @Override
    public boolean open(long ticket) {
        if (!(node.getLevel() instanceof ServerLevel server)) {
            return false;
        }
        ProductionLedger.Transfer transfer = ProductionLedger.get(server).transfer(ticket);
        return transfer != null && !transfer.ended();
    }

    @Override
    public void cancel(long ticket) {
        node.network().ifPresent(network -> network.cancelShipment(ticket));
    }

    private boolean lineConflict(ServerLevel server) {
        return TerminalWarehouses.line(server, node.getBlockPos()).conflict();
    }
}
