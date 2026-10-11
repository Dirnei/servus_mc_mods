package io.github.dirnei.mc.servus.storage.garage;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.ShuttleGarageMenu;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleProfile;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Keeps shuttles as items, one per slot, and sends them out through its
 * back onto the rail. The live shuttles are data on the network's fleet;
 * this only holds the items and tells the network what each slot's shuttle can do.
 */
public class ShuttleGarageBlockEntity extends StorageNodeBlockEntity implements WoodHolder, MenuProvider {

    private final WoodSlot woodSlot = new WoodSlot(this);

    private final ItemStackHandler shuttles = new ItemStackHandler(StorageStatsManager.INSTANCE.garageCapacity()) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ServusStorageItems.SHUTTLE.get());
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
        super(ServusStorageBlockEntities.SHUTTLE_GARAGE.get(), pos, state);
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
    public ItemStackHandler shuttles() {
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
        return addShuttle(new ItemStack(ServusStorageItems.SHUTTLE.get()));
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

    /** What each slot's shuttle can do, null for an empty slot; the fleet keeps one shuttle per entry. */
    public List<ShuttleProfile> profiles() {
        List<ShuttleProfile> profiles = new ArrayList<>();
        int cargoHoldStacks = StorageStatsManager.INSTANCE.cargoHoldStacks();
        for (int i = 0; i < shuttles.getSlots(); i++) {
            ItemStack stack = shuttles.getStackInSlot(i);
            profiles.add(stack.isEmpty() ? null : ShuttleModules.profile(stack, cargoHoldStacks));
        }
        return profiles;
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
        for (int i = 0; i < shuttles.getSlots(); i++) {
            ItemStack stack = shuttles.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                shuttles.setStackInSlot(i, ItemStack.EMPTY);
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
        return new ShuttleGarageMenu(containerId, playerInventory, this);
    }

    // Persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        woodSlot.save(tag);
        tag.put("ShuttleItems", shuttles.serializeNBT(registries));
        tag.putBoolean("Active", active);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        woodSlot.load(tag);
        active = !tag.contains("Active") || tag.getBoolean("Active");
        if (tag.contains("ShuttleItems")) {
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("ShuttleItems"));
            for (int i = 0; i < shuttles.getSlots(); i++) {
                shuttles.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
        } else if (tag.contains("Shuttles")) {
            // Older saves kept a plain count.
            int count = Math.min(tag.getInt("Shuttles"), shuttles.getSlots());
            for (int i = 0; i < count; i++) {
                shuttles.setStackInSlot(i, new ItemStack(ServusStorageItems.SHUTTLE.get()));
            }
        }
    }

    // Wood: the planks the block was crafted from, shown as its boards

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
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
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
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        WoodSlot.removeFromTag(tag);
    }

    @Override
    public ModelData getModelData() {
        return woodSlot.modelData();
    }
}
