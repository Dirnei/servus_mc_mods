package io.github.dirnei.mc.servus.foundation.logistics.duct;

import io.github.dirnei.mc.servus.foundation.data.CableTier;
import io.github.dirnei.mc.servus.foundation.data.CableTiersManager;
import io.github.dirnei.mc.servus.foundation.data.DataMembership;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNode;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import io.github.dirnei.mc.servus.foundation.network.NetworkNode;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeItem;
import io.github.dirnei.mc.servus.foundation.logistics.menu.UtilityDuctMenu;
import io.github.dirnei.mc.servus.foundation.logistics.network.DuctEvent;
import io.github.dirnei.mc.servus.foundation.logistics.network.EnergyChannelNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.network.ItemChannelNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlockEntities;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A utility duct: a set of lines (Item Pipe, Power Cable, Fluid Pipe, data line), each with a mode
 * and a channel per side, plus item filters and priorities per side. Each
 * present line joins its own network type, so items, energy and fluids form separate
 * graphs through the same blocks. The duct also keeps which sides carry an
 * arm per line and which arms end at an inventory or machine, synced to the
 * client for the model. The data line has no channels; its sides are only on
 * (both) or off (disabled), and it joins the Foundation data network as a
 * {@link DataNode}.
 */
public class UtilityDuctBlockEntity extends BlockEntity implements DataNode {

    public static final int FILTER_SLOTS = 9;
    public static final int MIN_PRIORITY = -9;
    public static final int MAX_PRIORITY = 9;

    private static final int SIDES = Direction.values().length;
    private static final Direction[] DIRECTIONS = Direction.values();

    private final EnumSet<Line> lines = EnumSet.noneOf(Line.class);
    private final SideMode[][] modes = new SideMode[Line.VALUES.length][SIDES];
    private final byte[][] channels = new byte[Line.VALUES.length][SIDES];
    private final ItemStackHandler[] filters = new ItemStackHandler[SIDES];
    private final int[] priorities = new int[SIDES];
    private final int[] arms = new int[Line.VALUES.length];
    private int endpoints;
    private final EnumSet<Line> joined = EnumSet.noneOf(Line.class);
    private final Map<Line, NetworkNode<?>> nodes = new EnumMap<>(Line.class);
    private final Map<Direction, DuctItemHandler> itemHandlers = new EnumMap<>(Direction.class);
    private final Map<Direction, DuctFluidHandler> fluidHandlers = new EnumMap<>(Direction.class);
    /** Panel block per side, null without a panel. */
    private final BlockState[] panels = new BlockState[SIDES];
    /** Six bit mask of the sides whose panel is hollow. */
    private int hollowPanels;
    /** The data line's tier: the id of the data cable item that placed it. */
    private ResourceLocation dataTier = CableTiersManager.BASE_TIER;

    public UtilityDuctBlockEntity(BlockPos pos, BlockState state) {
        super(ServusLogisticsBlockEntities.UTILITY_DUCT.get(), pos, state);
        for (SideMode[] row : modes) {
            Arrays.fill(row, SideMode.BOTH);
        }
        for (Direction direction : DIRECTIONS) {
            filters[direction.ordinal()] = new ItemStackHandler(FILTER_SLOTS) {
                @Override
                protected void onContentsChanged(int slot) {
                    configChanged();
                }
            };
        }
        for (Line line : Line.VALUES) {
            nodes.put(line, line == Line.DATA ? new DataLineNode() : new LineNode(line));
        }
    }

    // Lines

    public Set<Line> lines() {
        return lines;
    }

    public boolean has(Line line) {
        return lines.contains(line);
    }

    /** @return true when the line was added */
    public boolean addLine(Line line) {
        if (!lines.add(line)) {
            return false;
        }
        join(line);
        changed();
        return true;
    }

    /** Adds the data line in the tier of the given cable item; see {@link #addLine(Line)}. */
    public boolean addDataLine(ResourceLocation tier) {
        if (has(Line.DATA)) {
            return false;
        }
        dataTier = tier == null ? CableTiersManager.BASE_TIER : tier;
        return addLine(Line.DATA);
    }

    /** The data line's tier id (the cable item id), the base Data Cable when unset. */
    public ResourceLocation dataTierId() {
        return dataTier;
    }

    /**
     * Changes the data line's tier in place: sides, modes and connections stay.
     * Running deliveries pick their path again.
     */
    public void setDataTier(ResourceLocation tier) {
        ResourceLocation next = tier == null ? CableTiersManager.BASE_TIER : tier;
        if (next.equals(dataTier)) {
            return;
        }
        dataTier = next;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            if (level instanceof ServerLevel && joined.contains(Line.DATA)) {
                dataNetwork().ifPresent(network -> network.tierChanged(Conversions.toNodePos(worldPosition)));
            }
        }
    }

    /** The item a line of this duct drops: the tier's cable for the data line, the line's item otherwise. */
    public Item lineItem(Line line) {
        if (line == Line.DATA && BuiltInRegistries.ITEM.getOptional(dataTier).orElse(null) instanceof DataCableItem cable) {
            return cable;
        }
        return ServusLogisticsItems.lineItem(line);
    }

    /** @return true when the line was present */
    public boolean removeLine(Line line) {
        if (!lines.remove(line)) {
            return false;
        }
        leave(line);
        changed();
        return true;
    }

    // Modes

    public SideMode mode(Line line, Direction side) {
        return modes[line.ordinal()][side.ordinal()];
    }

    public void setMode(Line line, Direction side, SideMode mode) {
        if (mode == SideMode.AUTO) {
            mode = SideMode.BOTH;
        }
        if (line == Line.DATA && mode != SideMode.DISABLED) {
            // The data line is only on or off.
            mode = SideMode.BOTH;
        }
        if (modes[line.ordinal()][side.ordinal()] == mode) {
            return;
        }
        modes[line.ordinal()][side.ordinal()] = mode;
        configChanged();
    }

    /** The next mode in the duct cycle: both, input, output, disabled. */
    public static SideMode cycle(SideMode mode) {
        return switch (mode) {
            case BOTH, AUTO -> SideMode.INPUT;
            case INPUT -> SideMode.OUTPUT;
            case OUTPUT -> SideMode.DISABLED;
            case DISABLED, PULL, PUSH -> SideMode.BOTH;
        };
    }

    // Channels

    /** The channel of a line's side, 0 to {@link DuctChannels#COUNT} minus one. */
    public int channel(Line line, Direction side) {
        return channels[line.ordinal()][side.ordinal()];
    }

    public void setChannel(Line line, Direction side, int channel) {
        byte clamped = (byte) DuctChannels.clamp(channel);
        if (channels[line.ordinal()][side.ordinal()] == clamped) {
            return;
        }
        channels[line.ordinal()][side.ordinal()] = clamped;
        configChanged();
    }

    // Arms and endpoints

    /** Six bit mask of the sides that carry an arm of the line, {@code 1 << side.ordinal()}. */
    public int arms(Line line) {
        return arms[line.ordinal()];
    }

    /** A copy of the arm masks of every line, indexed by {@link Line#ordinal()}. */
    public int[] armMasks() {
        return arms.clone();
    }

    public boolean hasArm(Line line, Direction side) {
        return (arms[line.ordinal()] & DuctModelData.bit(side)) != 0;
    }

    /** Six bit mask of the sides whose arm ends at an inventory or machine, not at another duct. */
    public int endpoints() {
        return endpoints;
    }

    public boolean isEndpoint(Direction side) {
        return (endpoints & DuctModelData.bit(side)) != 0;
    }

    /** Union of the arms of all lines: the block state connection flags. */
    public int connections() {
        int mask = 0;
        for (int value : arms) {
            mask |= value;
        }
        return mask;
    }

    /**
     * Whether the line reaches through that side right now: the line is
     * present, the side is not disabled, and the neighbour is a duct carrying
     * the line or a block offering the matching capability on that face.
     * Server only; the client reads the synced masks.
     */
    public boolean computeArm(Line line, Direction side) {
        if (!has(line) || mode(line, side) == SideMode.DISABLED || closed(side) || !(level instanceof Level realLevel)) {
            return false;
        }
        BlockPos neighborPos = worldPosition.relative(side);
        if (realLevel.getBlockEntity(neighborPos) instanceof UtilityDuctBlockEntity neighbour) {
            return neighbour.has(line) && !neighbour.closed(side.getOpposite());
        }
        return switch (line) {
            case ITEM -> realLevel.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, side.getOpposite()) != null;
            case ENERGY -> realLevel.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, side.getOpposite()) != null;
            case FLUID -> realLevel.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, side.getOpposite()) != null;
            case DATA -> DataMembership.isMember(realLevel, neighborPos, side.getOpposite());
        };
    }

    /**
     * Recomputes the arm and endpoint masks from the neighbours. Server only.
     *
     * @return true when a mask changed
     */
    public boolean recomputeArms() {
        if (level == null || level.isClientSide()) {
            return false;
        }
        boolean changedMask = false;
        int newEndpoints = 0;
        for (Line line : Line.VALUES) {
            int mask = 0;
            for (Direction side : DIRECTIONS) {
                if (computeArm(line, side)) {
                    mask |= DuctModelData.bit(side);
                }
            }
            if (arms[line.ordinal()] != mask) {
                arms[line.ordinal()] = mask;
                changedMask = true;
            }
        }
        for (Direction side : DIRECTIONS) {
            if ((connections() & DuctModelData.bit(side)) != 0
                    && !(level.getBlockEntity(worldPosition.relative(side)) instanceof UtilityDuctBlockEntity)) {
                newEndpoints |= DuctModelData.bit(side);
            }
        }
        if (endpoints != newEndpoints) {
            endpoints = newEndpoints;
            changedMask = true;
        }
        return changedMask;
    }

    /**
     * A neighbour was placed, removed or changed: refresh the masks and the
     * block state flags and, when anything differs, tell the client.
     */
    public void neighbourChanged() {
        if (level == null || level.isClientSide() || isRemoved()) {
            return;
        }
        if (recomputeArms()) {
            setChanged();
            if (getBlockState().getBlock() instanceof UtilityDuctBlock duct) {
                duct.refreshConnections(level, worldPosition);
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // Panels

    /** The panel block on a side, or null. */
    public BlockState panel(Direction side) {
        return panels[side.ordinal()];
    }

    public boolean isHollow(Direction side) {
        return (hollowPanels & DuctModelData.bit(side)) != 0;
    }

    public boolean hasPanels() {
        for (BlockState panel : panels) {
            if (panel != null) {
                return true;
            }
        }
        return false;
    }

    /** Whether a solid panel closes the side for every line. */
    public boolean closed(Direction side) {
        return panels[side.ordinal()] != null && !isHollow(side);
    }

    /** Six bit mask of the sides with a solid panel. */
    public int solidPanelMask() {
        int mask = 0;
        for (Direction side : DIRECTIONS) {
            if (closed(side)) {
                mask |= DuctModelData.bit(side);
            }
        }
        return mask;
    }

    /** Six bit mask of the sides with a hollow panel. */
    public int hollowPanelMask() {
        return hollowPanels;
    }

    /**
     * Puts a panel on a free side.
     *
     * @return false when the side already has a panel
     */
    public boolean setPanel(Direction side, BlockState block, boolean hollow) {
        if (panels[side.ordinal()] != null) {
            return false;
        }
        panels[side.ordinal()] = block;
        hollowPanels = hollow ? hollowPanels | DuctModelData.bit(side) : hollowPanels & ~DuctModelData.bit(side);
        panelsChanged();
        return true;
    }

    /** Takes the panel off a side and returns its item, empty when there was none. */
    public ItemStack removePanel(Direction side) {
        BlockState panel = panels[side.ordinal()];
        if (panel == null) {
            return ItemStack.EMPTY;
        }
        ItemStack item = FacadeItem.of(panel.getBlock(), 1, isHollow(side));
        panels[side.ordinal()] = null;
        hollowPanels &= ~DuctModelData.bit(side);
        panelsChanged();
        return item;
    }

    /**
     * A panel came or went: arms, handlers and endpoints follow through the
     * config path, and the graph edges are read again, because a solid panel
     * closes the side for the item, energy and fluid graphs too. The graph asks
     * both nodes of an edge, so refreshing this duct covers the neighbour.
     */
    private void panelsChanged() {
        configChanged();
        if (level instanceof ServerLevel server) {
            for (Line line : joined) {
                if (line != Line.DATA) {
                    NetworkManager.of(server).refresh(worldPosition, nodes.get(line));
                }
            }
        }
    }

    /** Graph rule of the item, energy and fluid networks: a solid panel closes the side. */
    public static boolean sideOpen(UtilityDuctBlockEntity duct, Side side) {
        return !duct.closed(Conversions.toDirection(side));
    }

    /** The items of every panel, taken off without updates; for a duct being removed. */
    public List<ItemStack> dropPanels() {
        List<ItemStack> items = new ArrayList<>();
        for (Direction side : DIRECTIONS) {
            BlockState panel = panels[side.ordinal()];
            if (panel != null) {
                items.add(FacadeItem.of(panel.getBlock(), 1, isHollow(side)));
                panels[side.ordinal()] = null;
            }
        }
        hollowPanels = 0;
        return items;
    }

    @Override
    public ModelData getModelData() {
        int[] channelData = new int[Line.VALUES.length * SIDES];
        for (Line line : Line.VALUES) {
            for (Direction side : DIRECTIONS) {
                channelData[DuctModelData.channelIndex(line, side)] = channels[line.ordinal()][side.ordinal()];
            }
        }
        ModelData.Builder builder = ModelData.builder()
                .with(DuctModelData.LINES, linesMask())
                .with(DuctModelData.ARMS, arms.clone())
                .with(DuctModelData.ENDPOINTS, endpoints)
                .with(DuctModelData.CHANNELS, channelData);
        if (hasPanels()) {
            builder.with(DuctModelData.PANELS, new PanelSet(panels, hollowPanels));
        }
        if (has(Line.DATA) && lineItem(Line.DATA) instanceof DataCableItem cable && !cable.skin().isEmpty()) {
            builder.with(DuctModelData.DATA_SKIN, cable.skin());
        }
        return builder.build();
    }

    /** Bitmask of the present lines, {@code 1 << line.ordinal()}. */
    public int linesMask() {
        int mask = 0;
        for (Line line : lines) {
            mask |= 1 << line.ordinal();
        }
        return mask;
    }

    // Filters and priorities (item line)

    public ItemStackHandler filter(Direction side) {
        return filters[side.ordinal()];
    }

    /** Whether the side's whitelist lets the stack through; an empty list passes everything. */
    public boolean filterAllows(Direction side, ItemStack stack) {
        ItemStackHandler filter = filters[side.ordinal()];
        boolean any = false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack entry = filter.getStackInSlot(i);
            if (entry.isEmpty()) {
                continue;
            }
            any = true;
            if (ItemStack.isSameItem(entry, stack)) {
                return true;
            }
        }
        return !any;
    }

    public int priority(Direction side) {
        return priorities[side.ordinal()];
    }

    public void setPriority(Direction side, int priority) {
        int clamped = Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY, priority));
        if (priorities[side.ordinal()] == clamped) {
            return;
        }
        priorities[side.ordinal()] = clamped;
        configChanged();
    }

    private void configChanged() {
        changed();
        for (Line line : lines) {
            network(line).ifPresent(network -> network.post(new DuctEvent.ConfigChanged(Conversions.toNodePos(worldPosition))));
        }
        refreshData();
    }

    /** Data sides decide which neighbours the data graph links to, so the node's edges are re-evaluated. */
    private void refreshData() {
        if (level instanceof ServerLevel server && joined.contains(Line.DATA)) {
            NetworkManager.of(server).refresh(worldPosition, nodes.get(Line.DATA));
            DataNetwork.sidesChanged(server, worldPosition);
        }
    }

    /**
     * Lines, modes, channels, filters, priorities or the facade changed: save,
     * refresh and sync. Neighbouring ducts refresh too, because their arms
     * toward this duct depend on its lines.
     */
    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            // Which sides expose item and fluid handlers depends on the lines and modes.
            level.invalidateCapabilities(worldPosition);
            recomputeArms();
            if (getBlockState().getBlock() instanceof UtilityDuctBlock duct) {
                duct.refreshConnections(level, worldPosition);
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            // Neighbours that draw something toward the duct (a pump's pipe stub) learn about the new handlers.
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            for (Direction side : DIRECTIONS) {
                if (level.getBlockEntity(worldPosition.relative(side)) instanceof UtilityDuctBlockEntity neighbour) {
                    neighbour.neighbourChanged();
                }
            }
        }
    }

    // Capabilities

    /** Item handler shown to the neighbour on {@code side}, null without an Item Pipe. */
    public IItemHandler itemHandler(Direction side) {
        if (!has(Line.ITEM) || side == null || closed(side)) {
            return null;
        }
        return itemHandlers.computeIfAbsent(side, s -> new DuctItemHandler(this, s));
    }

    /** Fluid handler shown to the neighbour on {@code side}, null without a Fluid Pipe or when the side takes no input. */
    public IFluidHandler fluidHandler(Direction side) {
        if (!has(Line.FLUID) || side == null || closed(side) || !mode(Line.FLUID, side).allowsInsert()) {
            return null;
        }
        return fluidHandlers.computeIfAbsent(side, s -> new DuctFluidHandler(this, s));
    }

    // Networks

    @SuppressWarnings("unchecked")
    public <T extends ServusNetwork<UtilityDuctBlockEntity, DuctEvent>> Optional<T> network(Line line) {
        if (!(level instanceof ServerLevel server) || line == Line.DATA) {
            return Optional.empty();
        }
        NetworkType<UtilityDuctBlockEntity, DuctEvent, T> type = (NetworkType<UtilityDuctBlockEntity, DuctEvent, T>) typeOf(line);
        return NetworkManager.of(server).set(type).networkAt(Conversions.toNodePos(worldPosition));
    }

    private static NetworkType<UtilityDuctBlockEntity, DuctEvent, ?> typeOf(Line line) {
        return switch (line) {
            case ITEM -> ItemChannelNetwork.TYPE;
            case ENERGY -> EnergyChannelNetwork.TYPE;
            case FLUID -> FluidChannelNetwork.TYPE;
            case DATA -> throw new IllegalArgumentException("The data line joins the data network, see dataNetwork()");
        };
    }

    /** The data network of this duct's data line, empty without the line or on the client. */
    public Optional<DataNetwork> dataNetwork() {
        if (!(level instanceof ServerLevel server) || !has(Line.DATA)) {
            return Optional.empty();
        }
        return NetworkManager.of(server).set(DataNetwork.TYPE).networkAt(Conversions.toNodePos(worldPosition));
    }

    // DataNode

    @Override
    public Level dataLevel() {
        return level;
    }

    @Override
    public boolean dataRemoved() {
        return isRemoved();
    }

    @Override
    public boolean dataSideOpen(Direction side) {
        return has(Line.DATA) && mode(Line.DATA, side) != SideMode.DISABLED && !closed(side);
    }

    @Override
    public CableTier dataTier() {
        return CableTiersManager.INSTANCE.tier(dataTier);
    }

    private void join(Line line) {
        if (level instanceof ServerLevel server && !isRemoved() && joined.add(line)) {
            NetworkManager.of(server).join(worldPosition, nodes.get(line));
        }
    }

    private void leave(Line line) {
        if (level instanceof ServerLevel server && joined.remove(line)) {
            NetworkManager.of(server).leave(worldPosition, nodes.get(line));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            DuctTracker.add(this);
            return;
        }
        for (Line line : lines) {
            join(line);
        }
    }

    @Override
    public void setRemoved() {
        DuctTracker.remove(this);
        for (Line line : Line.VALUES) {
            leave(line);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        DuctTracker.remove(this);
        for (Line line : Line.VALUES) {
            leave(line);
        }
    }

    /** The data line's node: the duct as a {@link DataNode} in the Foundation data network. */
    private final class DataLineNode implements NetworkNode<DataNode> {

        @Override
        public NetworkType<DataNode, ?, ?> networkType() {
            return DataNetwork.TYPE;
        }

        @Override
        public DataNode networkNode() {
            return UtilityDuctBlockEntity.this;
        }
    }

    /** Adapter so one block entity can sit in several network types. */
    private final class LineNode implements NetworkNode<UtilityDuctBlockEntity> {

        private final Line line;

        private LineNode(Line line) {
            this.line = line;
        }

        @Override
        public NetworkType<UtilityDuctBlockEntity, ?, ?> networkType() {
            return typeOf(line);
        }

        @Override
        public UtilityDuctBlockEntity networkNode() {
            return UtilityDuctBlockEntity.this;
        }
    }

    // Menu

    /** The name of the block the side touches, for the endpoint GUI title. */
    public Component touchedBlockName(Direction side) {
        if (level == null) {
            return Component.empty();
        }
        return level.getBlockState(worldPosition.relative(side)).getBlock().getName();
    }

    /** The endpoint GUI of one side: "Utility Duct, West: Chest". */
    public MenuProvider menuProvider(Direction side) {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.servus_foundation.utility_duct.title",
                        Component.translatable("gui.servus_foundation.side." + side.getSerializedName()), touchedBlockName(side));
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new UtilityDuctMenu(containerId, playerInventory, UtilityDuctBlockEntity.this, side);
            }
        };
    }

    // Persistence and sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        lines.forEach(line -> list.add(StringTag.valueOf(line.id())));
        tag.put("Lines", list);
        CompoundTag modeTag = new CompoundTag();
        CompoundTag channelTag = new CompoundTag();
        for (Line line : Line.VALUES) {
            CompoundTag perSideModes = new CompoundTag();
            CompoundTag perSideChannels = new CompoundTag();
            for (Direction direction : DIRECTIONS) {
                SideMode mode = mode(line, direction);
                if (mode != SideMode.BOTH) {
                    perSideModes.putString(direction.getSerializedName(), mode.name().toLowerCase(Locale.ROOT));
                }
                int channel = channel(line, direction);
                if (channel != 0) {
                    perSideChannels.putInt(direction.getSerializedName(), channel);
                }
            }
            if (!perSideModes.isEmpty()) {
                modeTag.put(line.id(), perSideModes);
            }
            if (!perSideChannels.isEmpty()) {
                channelTag.put(line.id(), perSideChannels);
            }
        }
        tag.put("Modes", modeTag);
        tag.put("Channels", channelTag);
        CompoundTag filterTag = new CompoundTag();
        CompoundTag priorityTag = new CompoundTag();
        for (Direction direction : DIRECTIONS) {
            ItemStackHandler filter = filters[direction.ordinal()];
            boolean empty = true;
            for (int i = 0; i < FILTER_SLOTS && empty; i++) {
                empty = filter.getStackInSlot(i).isEmpty();
            }
            if (!empty) {
                filterTag.put(direction.getSerializedName(), filter.serializeNBT(registries));
            }
            if (priorities[direction.ordinal()] != 0) {
                priorityTag.putInt(direction.getSerializedName(), priorities[direction.ordinal()]);
            }
        }
        tag.put("Filters", filterTag);
        tag.put("Priorities", priorityTag);
        tag.putIntArray("Arms", arms.clone());
        tag.putInt("Endpoints", endpoints);
        if (hasPanels()) {
            CompoundTag panelTag = new CompoundTag();
            for (Direction side : DIRECTIONS) {
                BlockState panel = panels[side.ordinal()];
                if (panel != null) {
                    CompoundTag entry = new CompoundTag();
                    entry.put("State", NbtUtils.writeBlockState(panel));
                    entry.putBoolean("Hollow", isHollow(side));
                    panelTag.put(side.getSerializedName(), entry);
                }
            }
            tag.put("Panels", panelTag);
        }
        if (has(Line.DATA) && !dataTier.equals(CableTiersManager.BASE_TIER)) {
            tag.putString("DataTier", dataTier.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lines.clear();
        for (Tag entry : tag.getList("Lines", Tag.TAG_STRING)) {
            Line line = Line.byId(entry.getAsString());
            if (line != null) {
                lines.add(line);
            }
        }
        for (SideMode[] row : modes) {
            Arrays.fill(row, SideMode.BOTH);
        }
        for (byte[] row : channels) {
            Arrays.fill(row, (byte) 0);
        }
        CompoundTag modeTag = tag.getCompound("Modes");
        CompoundTag channelTag = tag.getCompound("Channels");
        for (Line line : Line.VALUES) {
            CompoundTag perSideModes = modeTag.getCompound(line.id());
            CompoundTag perSideChannels = channelTag.getCompound(line.id());
            for (Direction direction : DIRECTIONS) {
                String name = perSideModes.getString(direction.getSerializedName());
                if (!name.isEmpty()) {
                    try {
                        modes[line.ordinal()][direction.ordinal()] = SideMode.valueOf(name.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        // Unknown mode names fall back to BOTH.
                    }
                }
                channels[line.ordinal()][direction.ordinal()] =
                        (byte) DuctChannels.clamp(perSideChannels.getInt(direction.getSerializedName()));
            }
        }
        CompoundTag filterTag = tag.getCompound("Filters");
        CompoundTag priorityTag = tag.getCompound("Priorities");
        for (Direction direction : DIRECTIONS) {
            ItemStackHandler filter = filters[direction.ordinal()];
            ItemStackHandler saved = new ItemStackHandler();
            if (filterTag.contains(direction.getSerializedName())) {
                saved.deserializeNBT(registries, filterTag.getCompound(direction.getSerializedName()));
            }
            for (int i = 0; i < FILTER_SLOTS; i++) {
                filter.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
            priorities[direction.ordinal()] = Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY,
                    priorityTag.getInt(direction.getSerializedName())));
        }
        int[] savedArms = tag.getIntArray("Arms");
        Arrays.fill(arms, 0);
        for (int i = 0; i < arms.length && i < savedArms.length; i++) {
            arms[i] = savedArms[i];
        }
        endpoints = tag.getInt("Endpoints");
        Arrays.fill(panels, null);
        hollowPanels = 0;
        CompoundTag panelTag = tag.getCompound("Panels");
        for (Direction side : DIRECTIONS) {
            if (panelTag.contains(side.getSerializedName())) {
                CompoundTag entry = panelTag.getCompound(side.getSerializedName());
                BlockState panel = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), entry.getCompound("State"));
                if (!panel.isAir()) {
                    panels[side.ordinal()] = panel;
                    if (entry.getBoolean("Hollow")) {
                        hollowPanels |= DuctModelData.bit(side);
                    }
                }
            }
        }
        ResourceLocation savedTier = tag.contains("DataTier") ? ResourceLocation.tryParse(tag.getString("DataTier")) : null;
        dataTier = savedTier == null ? CableTiersManager.BASE_TIER : savedTier;
        if (level != null && level.isClientSide()) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        if (level instanceof ServerLevel) {
            // A data merge or reload changed the configuration while loaded: reconcile memberships.
            for (Line line : Line.VALUES) {
                if (lines.contains(line)) {
                    join(line);
                    network(line).ifPresent(network ->
                            network.post(new DuctEvent.ConfigChanged(Conversions.toNodePos(worldPosition))));
                    if (line == Line.DATA) {
                        refreshData();
                    }
                } else {
                    leave(line);
                }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
