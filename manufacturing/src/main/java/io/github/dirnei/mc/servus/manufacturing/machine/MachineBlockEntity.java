package io.github.dirnei.mc.servus.manufacturing.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Base block entity of every Servus machine. Owns the inventory, exposes it
 * with slot role rules, pushes outputs to neighbours and feeds menu data.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_WORK_TICKS = 1;
    public static final int DATA_BURN_TIME = 2;
    public static final int DATA_BURN_DURATION = 3;
    public static final int DATA_ENERGY = 4;
    public static final int DATA_ENERGY_CAPACITY = 5;
    /** Index 6 is free for machine specific data, see the assembler. */
    public static final int DATA_COUNT = 7;

    private static final int PUSH_INTERVAL = 8;

    protected final ItemStackHandler inventory;
    private final IItemHandler sideHandler = new SideHandler();
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return getData(index);
        }

        @Override
        public void set(int index, int value) {
            setData(index, value);
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    protected int progress;
    private MachineStats stats;
    private int pushCooldown;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);
        // The raw inventory accepts anything so the machine itself can fill
        // output slots. Role rules live in the side handler and the menu slots.
        this.inventory = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    // Subclass contract

    /** Id used to look up stats in {@code data/<ns>/machine/}. */
    protected abstract ResourceLocation machineId();

    protected abstract MachineStats defaultStats();

    protected abstract boolean isInputSlot(int slot);

    protected abstract boolean isOutputSlot(int slot);

    protected boolean isFuelSlot(int slot) {
        return false;
    }

    /** Whether the stack may go into an input slot. Fuel slots are checked separately. */
    protected abstract boolean acceptsInput(int slot, ItemStack stack);

    /** One server tick of machine logic. Return true while the machine is working, for the lit state. */
    protected abstract boolean tick();

    // Shared behavior

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        boolean working = machine.tick();
        machine.pushOutputs();
        machine.updateLit(state, working);
    }

    public MachineStats stats() {
        if (stats == null) {
            stats = MachineStatsManager.INSTANCE.get(machineId(), defaultStats());
        }
        return stats;
    }

    /** Forget cached stats, for example after a datapack reload. */
    public void invalidateStats() {
        stats = null;
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public IItemHandler sideHandler() {
        return sideHandler;
    }

    public ContainerData containerData() {
        return data;
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        if (isFuelSlot(slot)) {
            return stack.getBurnTime(null) > 0;
        }
        if (isInputSlot(slot)) {
            return acceptsInput(slot, stack);
        }
        return false;
    }

    protected int getData(int index) {
        return switch (index) {
            case DATA_PROGRESS -> progress;
            case DATA_WORK_TICKS -> stats().workTicks();
            default -> 0;
        };
    }

    protected void setData(int index, int value) {
        if (index == DATA_PROGRESS) {
            progress = value;
        }
    }

    protected boolean canInsertOutput(int slot, ItemStack stack) {
        return inventory.insertItem(slot, stack, true).isEmpty();
    }

    protected void insertOutput(int slot, ItemStack stack) {
        inventory.insertItem(slot, stack, false);
    }

    private void pushOutputs() {
        if (level == null || ++pushCooldown < PUSH_INTERVAL) {
            return;
        }
        pushCooldown = 0;
        for (Direction direction : Direction.values()) {
            IItemHandler neighbour = level.getCapability(Capabilities.ItemHandler.BLOCK,
                    worldPosition.relative(direction), direction.getOpposite());
            if (neighbour == null) {
                continue;
            }
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                if (!isOutputSlot(slot)) {
                    continue;
                }
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack remaining = ItemHandlerHelper.insertItem(neighbour, stack.copy(), false);
                if (remaining.getCount() != stack.getCount()) {
                    inventory.setStackInSlot(slot, remaining);
                }
            }
        }
    }

    private void updateLit(BlockState state, boolean working) {
        if (level != null && state.hasProperty(MachineBlock.LIT) && state.getValue(MachineBlock.LIT) != working) {
            level.setBlock(worldPosition, state.setValue(MachineBlock.LIT, working), Block.UPDATE_CLIENTS);
        }
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        NonNullList<ItemStack> stacks = NonNullList.withSize(inventory.getSlots(), ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            stacks.set(slot, inventory.getStackInSlot(slot));
        }
        Containers.dropContents(level, worldPosition, stacks);
    }

    // Persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        progress = tag.getInt("Progress");
    }

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    /** Exposes the inventory to automation with the slot role rules applied. */
    private final class SideHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!isOutputSlot(slot)) {
                return ItemStack.EMPTY;
            }
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return (isInputSlot(slot) || isFuelSlot(slot)) && MachineBlockEntity.this.isItemValid(slot, stack);
        }
    }
}
