package io.github.dirnei.mc.servus.storage.network;

import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.lib.network.NetworkBlockEntity;
import io.github.dirnei.mc.servus.lib.network.NetworkManager;
import io.github.dirnei.mc.servus.lib.graph.Side;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.storage.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.storage.zone.ZoneNames;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Base of every block that takes part in a warehouse network. */
public abstract class StorageNodeBlockEntity extends NetworkBlockEntity<StorageNodeBlockEntity> {

    /** The zone name as the player typed it; empty when unzoned. */
    private String zone = "";

    protected StorageNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // Zones

    /** The zone name for display, empty when unzoned. */
    public String zone() {
        return zone;
    }

    /** The zone key for matching, empty when unzoned. */
    public String zoneKey() {
        return ZoneNames.key(zone);
    }

    /** Whether a Zone Label may stamp this node: stock blocks that are not the controller or terminal. */
    public boolean zonable() {
        return false;
    }

    /** Stamps or clears (empty name) the zone, syncs clients and tells the network. */
    public void setZone(String name) {
        String display = ZoneNames.display(name);
        if (display.equals(zone)) {
            return;
        }
        zone = display;
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            network().ifPresent(network -> network.post(new WarehouseEvent.ZoneChanged(worldPosition)));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!zone.isEmpty()) {
            tag.putString("Zone", zone);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        zone = tag.contains("Zone") ? ZoneNames.display(tag.getString("Zone")) : "";
    }

    // Linking

    @Override
    public NetworkType<StorageNodeBlockEntity, ?, ?> networkType() {
        return WarehouseNetwork.TYPE;
    }

    @Override
    public StorageNodeBlockEntity networkNode() {
        return this;
    }

    /**
     * Whether a rail on that side of this node links to it and docks onto it.
     * Stock blocks take rails on every side unless they say otherwise.
     */
    public boolean acceptsRail(Side side) {
        return true;
    }

    /** What this node is for linking: stock holding blocks by default. */
    public NodeKind kind() {
        return NodeKind.STOCK;
    }

    /** Whether rails dock onto this node: stock blocks and dispatch stations. */
    public final boolean docksRail() {
        NodeKind kind = kind();
        return kind == NodeKind.STOCK || kind == NodeKind.STATION;
    }

    /**
     * Whether this node is indexed as a source and pulls neighbouring
     * inventories into the warehouse. Connector nodes such as rails do not.
     */
    public final boolean holdsStock() {
        return kind() == NodeKind.STOCK;
    }

    /** The warehouse network this node belongs to, when loaded on the server. */
    public Optional<WarehouseNetwork> network() {
        if (!(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        return NetworkManager.of(server).set(WarehouseNetwork.TYPE).networkAt(Conversions.toNodePos(worldPosition));
    }

    /** Tells the network this node's contents changed. */
    protected void postDirty() {
        network().ifPresent(network -> network.post(new WarehouseEvent.Dirty(worldPosition)));
    }

    /** Called by storage blocks from {@code neighborChanged}. */
    public static void neighborsChanged(Level level, BlockPos pos) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageNodeBlockEntity node) {
            node.network().ifPresent(network -> network.post(new WarehouseEvent.NeighborsChanged(pos)));
        }
    }
}
