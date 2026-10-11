package io.github.dirnei.mc.servus.storage.outbound;

import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.component.DataComponentGetter;
import io.github.dirnei.mc.servus.foundation.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.OutboundStationMenu;
import io.github.dirnei.mc.servus.storage.network.DeliveryTarget;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.storage.network.WarehouseEvent;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import net.minecraft.core.component.DataComponentMap;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * The outbound dock: the warehouse keeps the buffer stocked at the keep
 * amounts, Push sides hand the buffer to neighbours, Output sides let
 * neighbours take from it. The machine it serves stands behind it. Deliveries
 * for work orders land here as well.
 */
public class OutboundStationBlockEntity extends StorageNodeBlockEntity implements WoodHolder, MenuProvider, DeliveryTarget, SidedBlockEntity {

    private final WoodSlot woodSlot = new WoodSlot(this);

    public static final int KEEP_LIMIT = 4096;

    private final ItemSlots buffer = new ItemSlots(StorageStatsManager.INSTANCE.inboundBufferSlots()) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            network().ifPresent(network -> network.post(new WarehouseEvent.OutputChanged(worldPosition)));
        }
    };

    /** Ghost stacks whose count is the amount to keep. */
    private final ItemSlots keep = new ItemSlots(StorageStatsManager.INSTANCE.inboundBufferSlots()) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return KEEP_LIMIT;
        }
    };

    /** The outside view on Output sides: takes only, never inserts. */
    private final LegacyItemHandler extractOnly = new LegacyItemHandler() {
        @Override
        public int getSlots() {
            return buffer.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return buffer.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return buffer.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return buffer.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    private final SideConfig sides = SideConfig.outbound();
    private int pushTimer;
    private ItemStack shortStack = ItemStack.EMPTY;
    private String shortReason = "";

    public OutboundStationBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.OUTBOUND_STATION.get(), pos, state);
        // The front is where the player stands: never an item side. The machine sits behind the station.
        sides.lock(RelativeSide.FRONT, SideMode.DISABLED);
        sides.set(RelativeSide.BACK, SideMode.PUSH);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.STATION;
    }

    @Override
    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(OutboundStationBlock.FACING) ? state.getValue(OutboundStationBlock.FACING) : Direction.NORTH;
    }

    /** The block behind the station, the machine it serves when it is one. */
    public BlockPos machinePos() {
        return worldPosition.relative(facing().getOpposite());
    }

    public ItemSlots buffer() {
        return buffer;
    }

    public ItemSlots keep() {
        return keep;
    }

    @Override
    public ItemSlots deliveries() {
        return buffer;
    }

    // Sides

    @Override
    public SideConfig sideConfig() {
        return sides;
    }

    /** The item handler a neighbour on that side sees: the extract view where extraction is allowed, nothing elsewhere. */
    public ResourceHandler<ItemResource> handlerFor(Direction side) {
        if (side == null || sideMode(side).allowsExtract()) {
            return extractOnly;
        }
        return null;
    }

    @Override
    public SideMode cycleSide(RelativeSide side) {
        SideMode mode = sides.cycle(side);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(worldPosition);
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        return mode;
    }

    public boolean isEmpty() {
        for (int i = 0; i < buffer.getSlots(); i++) {
            if (!buffer.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** How many of the key sit in the buffer. */
    public int bufferCount(ItemKey key) {
        int count = 0;
        for (int i = 0; i < buffer.getSlots(); i++) {
            ItemStack stack = buffer.getStackInSlot(i);
            if (!stack.isEmpty() && ItemKey.of(stack).equals(key)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** Sets a keep slot; an empty item or an amount below one clears it. */
    public void setKeep(int slot, ItemStack item, int amount) {
        if (slot < 0 || slot >= keep.getSlots()) {
            return;
        }
        if (item.isEmpty() || amount < 1) {
            keep.setStackInSlot(slot, ItemStack.EMPTY);
        } else {
            keep.setStackInSlot(slot, item.copyWithCount(Math.min(KEEP_LIMIT, amount)));
        }
    }

    // Restocking state, written by the warehouse

    public ItemStack shortStack() {
        return shortStack;
    }

    public String shortReason() {
        return shortReason;
    }

    /** Records what the last check could not fill and sets the activity lamp. */
    public void setRestockState(ItemStack shortStack, String shortReason, boolean pending) {
        this.shortStack = shortStack.copy();
        this.shortReason = shortReason == null ? "" : shortReason;
        setActivity(!this.shortStack.isEmpty() ? StorageNodeBlocks.Activity.BLOCKED
                : pending ? StorageNodeBlocks.Activity.WORKING : StorageNodeBlocks.Activity.IDLE);
    }

    private void setActivity(StorageNodeBlocks.Activity activity) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(StorageNodeBlocks.ACTIVITY) && state.getValue(StorageNodeBlocks.ACTIVITY) != activity) {
            level.setBlock(worldPosition, state.setValue(StorageNodeBlocks.ACTIVITY, activity), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    // Pushing

    /** Pushes the buffer into every Push side's neighbour every push interval. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, OutboundStationBlockEntity station) {
        if (++station.pushTimer < StorageStatsManager.INSTANCE.pushInterval()) {
            return;
        }
        station.pushTimer = 0;
        if (station.isEmpty()) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (!station.sideMode(direction).pushes()) {
                continue;
            }
            ItemView neighbour = ItemView.of(level.getCapability(Capabilities.Item.BLOCK, pos.relative(direction), direction.getOpposite()));
            if (neighbour == null) {
                continue;
            }
            for (int i = 0; i < station.buffer.getSlots(); i++) {
                ItemStack stack = station.buffer.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack left = neighbour.insertItem(stack.copy(), false);
                if (left.getCount() != stack.getCount()) {
                    station.buffer.setStackInSlot(i, left);
                }
            }
        }
    }

    /** Drops every stack in the buffer at the station; keep slots are configuration and stay. */
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < buffer.getSlots(); i++) {
            ItemStack stack = buffer.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                buffer.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    // Menu

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OutboundStationMenu(containerId, playerInventory, this);
    }

    // Persistence and sync

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        woodSlot.save(output);
        tag.put("Items", buffer.serializeNBT(registries));
        tag.put("Keep", keep.serializeNBT(registries));
        tag.putByteArray("Sides", sides.toBytes());
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        woodSlot.load(input);
        if (tag.contains("Items")) {
            load(buffer, tag.getCompoundOrEmpty("Items"), registries);
        }
        if (tag.contains("Keep")) {
            load(keep, tag.getCompoundOrEmpty("Keep"), registries);
        }
        if (tag.contains("Sides")) {
            sides.fromBytes(tag.getByteArray("Sides").orElse(new byte[0]));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        woodSlot.save(tag);
        tag.putByteArray("Sides", sides.toBytes());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static void load(ItemSlots into, CompoundTag tag, HolderLookup.Provider registries) {
        ItemSlots saved = new ItemSlots() {
            @Override
            public int getSlotLimit(int slot) {
                return KEEP_LIMIT;
            }
        };
        saved.deserializeNBT(registries, tag);
        for (int i = 0; i < into.getSlots(); i++) {
            into.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
        }
    }

    // Wood: the planks the block was crafted from, shown as its boards

    @Override
    public WoodSlot woodSlot() {
        return woodSlot;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        woodSlot.apply(input.get(WoodContent.WOOD.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        woodSlot.collect(builder);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        WoodSlot.removeFromTag(output);
    }

    @Override
    public ModelData getModelData() {
        return woodSlot.modelData();
    }

    /** Moved from the block's removal hook (26.1): runs on the server when the block is removed. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Level level = this.level;
        if (level == null) {
            return;
        }
        OutboundStationBlockEntity station = this;
        station.dropContents(level, pos);
    }
}
