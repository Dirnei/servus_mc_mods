package io.github.dirnei.mc.servus.logistics.duct;

import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.lib.network.NetworkManager;
import io.github.dirnei.mc.servus.lib.network.NetworkNode;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.lib.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.lib.side.SideMode;
import io.github.dirnei.mc.servus.logistics.client.UtilityDuctModel;
import io.github.dirnei.mc.servus.logistics.facade.FacadeItem;
import io.github.dirnei.mc.servus.logistics.menu.UtilityDuctMenu;
import io.github.dirnei.mc.servus.logistics.network.DuctEvent;
import io.github.dirnei.mc.servus.logistics.network.EnergyChannelNetwork;
import io.github.dirnei.mc.servus.logistics.network.ItemChannelNetwork;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlockEntities;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A utility duct: a set of channels, each with a mode per side, plus item
 * filters and priorities per side. Each present channel joins its own network
 * type, so items and energy form separate graphs through the same blocks.
 */
public class UtilityDuctBlockEntity extends BlockEntity implements MenuProvider {

    public static final int FILTER_SLOTS = 9;
    public static final int MIN_PRIORITY = -9;
    public static final int MAX_PRIORITY = 9;

    private final EnumSet<Channel> channels = EnumSet.noneOf(Channel.class);
    private final SideMode[][] modes = new SideMode[Channel.VALUES.length][Direction.values().length];
    private final ItemStackHandler[] filters = new ItemStackHandler[Direction.values().length];
    private final int[] priorities = new int[Direction.values().length];
    private final EnumSet<Channel> joined = EnumSet.noneOf(Channel.class);
    private final Map<Channel, ChannelNode> nodes = new EnumMap<>(Channel.class);
    private final Map<Direction, DuctItemHandler> itemHandlers = new EnumMap<>(Direction.class);
    private BlockState facade;

    public UtilityDuctBlockEntity(BlockPos pos, BlockState state) {
        super(ServusLogisticsBlockEntities.UTILITY_DUCT.get(), pos, state);
        for (SideMode[] row : modes) {
            Arrays.fill(row, SideMode.BOTH);
        }
        for (Direction direction : Direction.values()) {
            filters[direction.ordinal()] = new ItemStackHandler(FILTER_SLOTS) {
                @Override
                protected void onContentsChanged(int slot) {
                    configChanged();
                }
            };
        }
        for (Channel channel : Channel.VALUES) {
            nodes.put(channel, new ChannelNode(channel));
        }
    }

    // Channels

    public Set<Channel> channels() {
        return channels;
    }

    public boolean has(Channel channel) {
        return channels.contains(channel);
    }

    /** @return true when the channel was added */
    public boolean addChannel(Channel channel) {
        if (!channels.add(channel)) {
            return false;
        }
        join(channel);
        changed();
        return true;
    }

    /** @return true when the channel was present */
    public boolean removeChannel(Channel channel) {
        if (!channels.remove(channel)) {
            return false;
        }
        leave(channel);
        changed();
        return true;
    }

    // Modes

    public SideMode mode(Channel channel, Direction side) {
        return modes[channel.ordinal()][side.ordinal()];
    }

    public void setMode(Channel channel, Direction side, SideMode mode) {
        if (mode == SideMode.AUTO) {
            mode = SideMode.BOTH;
        }
        if (modes[channel.ordinal()][side.ordinal()] == mode) {
            return;
        }
        modes[channel.ordinal()][side.ordinal()] = mode;
        configChanged();
    }

    /** The next mode in the duct cycle: both, input, output, disabled. */
    public static SideMode cycle(SideMode mode) {
        return switch (mode) {
            case BOTH, AUTO -> SideMode.INPUT;
            case INPUT -> SideMode.OUTPUT;
            case OUTPUT -> SideMode.DISABLED;
            case DISABLED -> SideMode.BOTH;
        };
    }

    // Facade

    /** The block this duct looks like, or null. */
    public BlockState facade() {
        return facade;
    }

    public void setFacade(BlockState facade) {
        this.facade = facade;
        changed();
    }

    /** Takes the facade off and returns its item, empty when there was none. */
    public ItemStack removeFacade() {
        if (facade == null) {
            return ItemStack.EMPTY;
        }
        ItemStack item = FacadeItem.of(facade.getBlock(), 1);
        facade = null;
        changed();
        return item;
    }

    @Override
    public ModelData getModelData() {
        return facade == null ? ModelData.EMPTY : ModelData.builder().with(DuctModelData.FACADE, facade).build();
    }

    // Filters and priorities (item channel)

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
        for (Channel channel : channels) {
            network(channel).ifPresent(network -> network.post(new DuctEvent.ConfigChanged(Conversions.toNodePos(worldPosition))));
        }
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            if (getBlockState().getBlock() instanceof UtilityDuctBlock duct) {
                duct.refreshConnections(level, worldPosition);
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // Capabilities

    /** Item handler shown to the neighbour on {@code side}, null without an item channel. */
    public IItemHandler itemHandler(Direction side) {
        if (!has(Channel.ITEM) || side == null) {
            return null;
        }
        return itemHandlers.computeIfAbsent(side, s -> new DuctItemHandler(this, s));
    }

    // Networks

    @SuppressWarnings("unchecked")
    public <T extends ServusNetwork<UtilityDuctBlockEntity, DuctEvent>> Optional<T> network(Channel channel) {
        if (!(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        NetworkType<UtilityDuctBlockEntity, DuctEvent, T> type = (NetworkType<UtilityDuctBlockEntity, DuctEvent, T>) typeOf(channel);
        return NetworkManager.of(server).set(type).networkAt(Conversions.toNodePos(worldPosition));
    }

    private static NetworkType<UtilityDuctBlockEntity, DuctEvent, ?> typeOf(Channel channel) {
        return channel == Channel.ITEM ? ItemChannelNetwork.TYPE : EnergyChannelNetwork.TYPE;
    }

    private void join(Channel channel) {
        if (level instanceof ServerLevel server && !isRemoved() && joined.add(channel)) {
            NetworkManager.of(server).join(worldPosition, nodes.get(channel));
        }
    }

    private void leave(Channel channel) {
        if (level instanceof ServerLevel server && joined.remove(channel)) {
            NetworkManager.of(server).leave(worldPosition, nodes.get(channel));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            DuctTracker.add(this);
            return;
        }
        for (Channel channel : channels) {
            join(channel);
        }
    }

    @Override
    public void setRemoved() {
        DuctTracker.remove(this);
        for (Channel channel : Channel.VALUES) {
            leave(channel);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        DuctTracker.remove(this);
        for (Channel channel : Channel.VALUES) {
            leave(channel);
        }
    }

    /** Adapter so one block entity can sit in two network types. */
    private final class ChannelNode implements NetworkNode<UtilityDuctBlockEntity> {

        private final Channel channel;

        private ChannelNode(Channel channel) {
            this.channel = channel;
        }

        @Override
        public NetworkType<UtilityDuctBlockEntity, ?, ?> networkType() {
            return typeOf(channel);
        }

        @Override
        public UtilityDuctBlockEntity networkNode() {
            return UtilityDuctBlockEntity.this;
        }
    }

    // Menu

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new UtilityDuctMenu(containerId, playerInventory, this);
    }

    // Persistence and sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        channels.forEach(channel -> list.add(StringTag.valueOf(channel.id())));
        tag.put("Channels", list);
        CompoundTag all = new CompoundTag();
        for (Channel channel : Channel.VALUES) {
            CompoundTag perSide = new CompoundTag();
            for (Direction direction : Direction.values()) {
                SideMode mode = mode(channel, direction);
                if (mode != SideMode.BOTH) {
                    perSide.putString(direction.getSerializedName(), mode.name().toLowerCase(Locale.ROOT));
                }
            }
            if (!perSide.isEmpty()) {
                all.put(channel.id(), perSide);
            }
        }
        tag.put("Modes", all);
        CompoundTag filterTag = new CompoundTag();
        CompoundTag priorityTag = new CompoundTag();
        for (Direction direction : Direction.values()) {
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
        if (facade != null) {
            tag.put("Facade", NbtUtils.writeBlockState(facade));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        channels.clear();
        for (Tag entry : tag.getList("Channels", Tag.TAG_STRING)) {
            Channel channel = Channel.byId(entry.getAsString());
            if (channel != null) {
                channels.add(channel);
            }
        }
        for (SideMode[] row : modes) {
            Arrays.fill(row, SideMode.BOTH);
        }
        CompoundTag all = tag.getCompound("Modes");
        for (Channel channel : Channel.VALUES) {
            CompoundTag perSide = all.getCompound(channel.id());
            for (Direction direction : Direction.values()) {
                String name = perSide.getString(direction.getSerializedName());
                if (!name.isEmpty()) {
                    try {
                        modes[channel.ordinal()][direction.ordinal()] = SideMode.valueOf(name.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        // Unknown mode names fall back to BOTH.
                    }
                }
            }
        }
        CompoundTag filterTag = tag.getCompound("Filters");
        CompoundTag priorityTag = tag.getCompound("Priorities");
        for (Direction direction : Direction.values()) {
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
        facade = tag.contains("Facade") ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Facade")) : null;
        if (facade != null && facade.isAir()) {
            facade = null;
        }
        if (level != null && level.isClientSide()) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        if (level instanceof ServerLevel) {
            // A data merge or reload changed the configuration while loaded: reconcile memberships.
            for (Channel channel : Channel.VALUES) {
                if (channels.contains(channel)) {
                    join(channel);
                    network(channel).ifPresent(network ->
                            network.post(new DuctEvent.ConfigChanged(Conversions.toNodePos(worldPosition))));
                } else {
                    leave(channel);
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
