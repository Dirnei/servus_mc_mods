package io.github.dirnei.mc.servus.shuttles.garage;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleGarageMenu;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * Keeps shuttles as items, one per slot, and sends them out through its
 * back onto the rail. The live shuttles are data on the network's fleet;
 * this only holds the items and tells the network what each slot's shuttle can do.
 */
public class ShuttleGarageBlockEntity extends StorageNodeBlockEntity implements WoodHolder, MenuProvider {

    private final WoodSlot woodSlot = new WoodSlot(this);

    private final ItemSlots shuttles = new ItemSlots(ShuttlesStatsManager.INSTANCE.garageCapacity()) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ShuttleItem.isShuttle(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            network().ifPresent(network -> network.garageChanged(ShuttleGarageBlockEntity.this));
        }
    };

    private boolean active = true;

    /** Whether the garage hands out shuttles for new jobs. */
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        setChanged();
        network().ifPresent(network -> network.garageChanged(this));
    }

    public ShuttleGarageBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.SHUTTLE_GARAGE.get(), pos, state);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.GARAGE;
    }

    /** Shuttles drive in and out through the back: the rail links there only. */
    @Override
    public boolean acceptsRail(Side side) {
        return side == Conversions.toSide(getBlockState().getValue(ShuttleGarageBlock.FACING).getOpposite());
    }

    /** The rail runs into the doorway as a plain arm, not a dock piece. */
    @Override
    public boolean showsDock() {
        return false;
    }

    /** The shuttle items, one slot per place. */
    public ItemSlots shuttles() {
        return shuttles;
    }

    public int capacity() {
        return shuttles.getSlots();
    }

    public int shuttleCount() {
        int count = 0;
        for (int i = 0; i < shuttles.getSlots(); i++) {
            if (!shuttles.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /** How many of this garage's shuttles are out on a job right now. */
    public int outCount() {
        return network().map(network -> network.garageOut(worldPosition)).orElse(0);
    }

    /** Puts a plain shuttle into the first free slot. @return false when full */
    public boolean addShuttle() {
        return addShuttle(new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
    }

    /** Puts one shuttle like the given one, modules and all, into the first free slot. @return false when full */
    public boolean addShuttle(ItemStack shuttle) {
        for (int i = 0; i < shuttles.getSlots(); i++) {
            if (shuttles.getStackInSlot(i).isEmpty()) {
                shuttles.setStackInSlot(i, shuttle.copyWithCount(1));
                return true;
            }
        }
        return false;
    }

    /** The shuttle of each slot, an empty stack for an empty slot; the fleet keeps one shuttle per entry. */
    public List<ItemStack> slots() {
        List<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < shuttles.getSlots(); i++) {
            slots.add(shuttles.getStackInSlot(i).copy());
        }
        return slots;
    }

    /** Slots whose shuttle is out on a job, one bit per slot. */
    public int outSlots() {
        return network().map(network -> network.garageOutSlots(worldPosition)).orElse(0);
    }

    /** Takes the last shuttle item out. @return false when empty */
    public boolean takeShuttle() {
        for (int i = shuttles.getSlots() - 1; i >= 0; i--) {
            if (!shuttles.getStackInSlot(i).isEmpty()) {
                shuttles.setStackInSlot(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    /** Drops every shuttle item at the garage, for when the block is destroyed anyway. */
    public void dropShuttles(Level level, BlockPos pos) {
        shuttles.dropAll(level, pos);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ShuttleGarageMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        woodSlot.save(output);
        tag.put("ShuttleItems", shuttles.serializeNBT(registries));
        output.putBoolean("Active", active);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        woodSlot.load(input);
        active = !tag.contains("Active") || input.getBooleanOr("Active", false);
        if (tag.contains("ShuttleItems")) {
            shuttles.fillFrom(registries, tag.getCompoundOrEmpty("ShuttleItems"));
        } else if (tag.contains("Shuttles")) {
            // A saved count instead of shuttle items fills that many slots with Mk1 shuttles.
            int count = Math.min(input.getIntOr("Shuttles", 0), shuttles.getSlots());
            for (int i = 0; i < count; i++) {
                shuttles.setStackInSlot(i, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
            }
        }
    }

    @Override
    public WoodSlot woodSlot() {
        return woodSlot;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        woodSlot.save(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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

    /** Drops the shuttle items on the server when the block is removed. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            dropShuttles(level, pos);
        }
    }
}
