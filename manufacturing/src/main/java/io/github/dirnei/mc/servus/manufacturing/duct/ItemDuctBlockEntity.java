package io.github.dirnei.mc.servus.manufacturing.duct;

import io.github.dirnei.mc.servus.lib.network.NetworkBlockEntity;
import io.github.dirnei.mc.servus.lib.network.NetworkManager;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/** A duct node. Exposes one item handler per side that routes into the network. */
public class ItemDuctBlockEntity extends NetworkBlockEntity<ItemDuctBlockEntity> {

    private final Map<Direction, IItemHandler> handlers = new EnumMap<>(Direction.class);
    private IItemHandler unsidedHandler;

    public ItemDuctBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.ITEM_DUCT.get(), pos, state);
    }

    @Override
    public NetworkType<ItemDuctBlockEntity, ?, ?> networkType() {
        return ItemDuctNetwork.TYPE;
    }

    @Override
    public ItemDuctBlockEntity networkNode() {
        return this;
    }

    /** The network this duct belongs to, when loaded on the server. */
    public Optional<ItemDuctNetwork> network() {
        if (!(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        return NetworkManager.of(server).set(ItemDuctNetwork.TYPE).networkAt(
                io.github.dirnei.mc.servus.lib.network.Conversions.toNodePos(worldPosition));
    }

    /**
     * Handler for items arriving from {@code side} (the side of this duct the
     * neighbour touches), or from nowhere in particular when null.
     */
    public IItemHandler handlerFor(Direction side) {
        if (side == null) {
            if (unsidedHandler == null) {
                unsidedHandler = new ItemDuctHandler(this, null);
            }
            return unsidedHandler;
        }
        return handlers.computeIfAbsent(side, s -> new ItemDuctHandler(this, s));
    }
}
