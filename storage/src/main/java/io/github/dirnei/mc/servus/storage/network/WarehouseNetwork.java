package io.github.dirnei.mc.servus.storage.network;

import io.github.dirnei.mc.servus.lib.graph.ConnectionRule;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.lib.network.core.NetworkSet;
import io.github.dirnei.mc.servus.lib.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.resources.ResourceLocation;

/**
 * One connected set of storage blocks: crates, racks, controller, terminals.
 * Membership only for now; the controller change adds indexing.
 */
public final class WarehouseNetwork extends ServusNetwork<StorageNodeBlockEntity, Object> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "warehouse");

    public static final NetworkType<StorageNodeBlockEntity, Object, WarehouseNetwork> TYPE =
            new NetworkType<>(ID, ConnectionRule.always(), WarehouseNetwork::new);

    public WarehouseNetwork(int id, NetworkSet<StorageNodeBlockEntity, Object, ?> owner) {
        super(id, owner);
    }

    @Override
    protected void handle(Object event) {
    }
}
