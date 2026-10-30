package io.github.dirnei.mc.servus.shuttles.network;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachines;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkBlockEntity;
import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import io.github.dirnei.mc.servus.shuttles.zone.Zones;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Base of every block that takes part in a warehouse network. */
public abstract class StorageNodeBlockEntity extends NetworkBlockEntity<StorageNodeBlockEntity> {

    private static final String ZONE_ID = "ZoneId";
    private static final String CODE = "Code";
    private static final String CODE_BAY = "CodeBay";

    /** The id of the team zone this block is in, {@link ZoneList#NONE} when unzoned. */
    private int zoneId;
    /** The location code relative to the warehouse's controller, empty without one. */
    private String locationCode = "";
    /** Whether this bay is the one its shelf's code is measured from, where the sticker hangs. */
    private boolean codeBay;
    /** What the warehouse keeps of this node while the node is in no network: shuttles, queue or held loads. */
    private CompoundTag warehouseState = new CompoundTag();
    private boolean leavingForGood;

    protected StorageNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The id of this block's zone, {@link ZoneList#NONE} when unzoned; a deleted zone's id reads as unzoned once resolved. */
    public int zoneId() {
        return zoneId;
    }

    /** The zone resolved through the team zones of this side, or null when unzoned or the zone was deleted. */
    public @Nullable ZoneDefinition zoneDefinition() {
        return Zones.definition(level, zoneId);
    }

    /** The zone name for display, empty when unzoned. */
    public String zone() {
        ZoneDefinition zone = zoneDefinition();
        return zone == null ? "" : zone.name();
    }

    /** The filter this block applies; empty for unzoned blocks and name only zones. */
    public ZoneFilter zoneFilter() {
        ZoneDefinition zone = zoneDefinition();
        return zone == null ? ZoneFilter.EMPTY : zone.filter();
    }

    /** The location code of this bay's shelf relative to its warehouse's controller, empty without a controller. */
    public String locationCode() {
        return locationCode;
    }

    /** Whether the shelf's code is measured from this bay, so its sticker hangs here. */
    public boolean codeBay() {
        return codeBay;
    }

    /** Sets the shelf's location code and whether this is its code bay, and syncs clients when either changed. Server side, called by the warehouse. */
    public void setLocationCode(String code, boolean codeBay) {
        if (code.equals(locationCode) && codeBay == this.codeBay) {
            return;
        }
        locationCode = code;
        this.codeBay = codeBay;
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * The node's own slots as the warehouse reads and fills them, or null for
     * nodes that hold no stock. A docked rack exposes no item capability.
     */
    public @Nullable ItemView stock() {
        return null;
    }

    /** Whether the handheld may put this node into a zone: rack bays, not the controller or terminal. */
    public boolean zonable() {
        return false;
    }

    /** Puts the block into the zone with that id ({@link ZoneList#NONE} clears), syncs clients and tells the network. */
    public void setZone(int id) {
        if (id == zoneId) {
            return;
        }
        zoneId = id;
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            network().ifPresent(network -> network.post(new WarehouseEvent.ZoneChanged(worldPosition)));
        }
    }

    /** Writes the zone id and the location code into an update tag. */
    protected void writeZone(CompoundTag tag) {
        writeZoneId(tag);
        tag.putString(CODE, locationCode);
        tag.putBoolean(CODE_BAY, codeBay);
    }

    private void writeZoneId(CompoundTag tag) {
        if (zoneId != ZoneList.NONE) {
            tag.putInt(ZONE_ID, zoneId);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        writeZoneId(tag);
        CompoundTag state = isInNetwork() ? network().map(network -> network.saveState(this)).orElse(warehouseState) : warehouseState;
        if (!state.isEmpty()) {
            tag.put("Warehouse", state);
        }
        output.store(tag);
        NamedMachines.save(this, output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        zoneId = tag.getIntOr(ZONE_ID, ZoneList.NONE);
        locationCode = tag.getStringOr(CODE, "");
        codeBay = tag.getBooleanOr(CODE_BAY, false);
        warehouseState = tag.getCompoundOrEmpty("Warehouse");
        NamedMachines.load(this, input);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        NamedMachines.writeUpdate(this, tag);
        return tag;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        NamedMachines.apply(this, input);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        NamedMachines.collect(this, builder);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        NamedMachines.removeFromTag(this, output);
    }

    /** Marks the node as leaving for good before it leaves its network: broken or replaced, not unloaded. */
    @Override
    public void setRemoved() {
        leavingForGood = true;
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        leavingForGood = false;
        super.clearRemoved();
    }

    /** Whether the node leaves its network because the block is gone; a chunk unload, split or link change is no such leave. */
    public boolean leavingForGood() {
        return leavingForGood;
    }

    /** Keeps what the warehouse held for this node while it is in no network; saved with the block. */
    public void stashWarehouseState(CompoundTag state) {
        warehouseState = state;
    }

    /** Hands the kept warehouse state to the network the node joins, and forgets it here. */
    public CompoundTag takeWarehouseState() {
        CompoundTag state = warehouseState;
        warehouseState = new CompoundTag();
        return state;
    }

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

    /** Whether a rail on that side hangs on this node; the sides it docks on by default. */
    public boolean holdsRail(Side side) {
        return acceptsRail(side);
    }

    /** What this node is for linking: stock holding blocks by default. */
    public NodeKind kind() {
        return NodeKind.STOCK;
    }

    /** Whether rails dock onto this node. */
    public final boolean docksRail() {
        NodeKind kind = kind();
        return kind == NodeKind.STOCK || kind == NodeKind.STATION || kind == NodeKind.GARAGE || kind == NodeKind.RACK
                || kind == NodeKind.FLUID_RACK || kind == NodeKind.FLUID_STATION;
    }

    /**
     * Whether a rail that docks onto this node draws the dock piece towards it.
     * Nodes that take the rail as a plain arm, such as the garage, say no.
     */
    public boolean showsDock() {
        return docksRail();
    }

    /**
     * Whether this node may be indexed: racks and the controller. Only nodes
     * with {@link #stock()} are sources; inventories touching a node never are.
     */
    public final boolean holdsStock() {
        return kind() == NodeKind.STOCK || kind() == NodeKind.RACK;
    }

    /**
     * Whether this node links to the warehouse node on that side beyond the
     * kind rules. Racks use it to link only to bays of their own shelf.
     */
    public boolean linksTowards(Side side, StorageNodeBlockEntity neighbour) {
        return true;
    }

    /** Re-evaluates this node's links after a change the graph cannot see, such as a shelf join. */
    public void refreshLinks() {
        refreshNetworkConnections();
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
}
