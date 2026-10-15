package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideTank;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Base block entity of every Servus machine. Owns the inventory, exposes it
 * per side with slot role and side mode rules, pushes outputs to neighbours
 * and feeds menu data.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider, SidedBlockEntity {

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_WORK_TICKS = 1;
    public static final int DATA_BURN_TIME = 2;
    public static final int DATA_BURN_DURATION = 3;
    public static final int DATA_ENERGY = 4;
    public static final int DATA_ENERGY_CAPACITY = 5;
    /** Index 6 is free for machine specific data. */
    public static final int DATA_SIDE_FIRST = 7;
    /** The six Fluids layer values; 0 for machines without a Fluids layer. */
    public static final int DATA_FLUID_SIDE_FIRST = DATA_SIDE_FIRST + 6;
    public static final int DATA_COUNT = DATA_FLUID_SIDE_FIRST + 6;
    /** Extra data of machines with a fluid buffer, after the common entries. */
    public static final int DATA_FLUID_AMOUNT = DATA_COUNT;
    public static final int DATA_FLUID_CAPACITY = DATA_COUNT + 1;
    public static final int DATA_FLUID_ID = DATA_COUNT + 2;
    /** 1 while the machine's source (the water below a pump) is valid. */
    public static final int DATA_SOURCE_VALID = DATA_COUNT + 3;
    public static final int DATA_COUNT_FLUID = DATA_COUNT + 4;

    private static final int PUSH_INTERVAL = 8;

    protected final ItemSlots inventory;
    private final SideConfig sideConfig = new SideConfig();
    private FluidSides fluidSides;
    private final LegacyItemHandler autoHandler = new SideHandler(null);
    private final Map<Direction, LegacyItemHandler> sideHandlers = new EnumMap<>(Direction.class);
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
            return dataCount();
        }
    };

    protected int progress;
    private MachineStats stats;
    private int pushCooldown;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);
        // Accepts anything so the machine can fill its own output slots; slot roles apply in the side handlers and menu slots.
        this.inventory = new ItemSlots(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        sideConfig.lock(RelativeSide.FRONT, SideMode.DISABLED);
    }

    /** Id used to look up stats in {@code data/<ns>/machine/}. */
    protected abstract Identifier machineId();

    protected abstract MachineStats defaultStats();

    /** Whether automation and the machine's menu may insert into {@code slot}; none by default. */
    protected boolean isInputSlot(int slot) {
        return false;
    }

    /** Whether the machine pushes and automation may extract from {@code slot}; none by default. */
    protected boolean isOutputSlot(int slot) {
        return false;
    }

    protected boolean isFuelSlot(int slot) {
        return false;
    }

    /** Whether the stack may go into an input slot. Fuel slots are checked separately. */
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return false;
    }

    /** Whether automation may put the stack into a slot the screen accepts it in; pipes and hoppers may be stricter. */
    protected boolean acceptsAutomationInput(int slot, ItemStack stack) {
        return true;
    }

    private boolean automationAccepts(int slot, ItemStack stack) {
        return isItemValid(slot, stack) && acceptsAutomationInput(slot, stack);
    }

    /** One server tick of machine logic. Return true while the machine is working, for the lit state. */
    protected abstract boolean tick();

    /** How many container data entries the menu syncs; machines with extra data override. */
    protected int dataCount() {
        return DATA_COUNT;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        boolean working = machine.tick();
        machine.pushOutputs();
        machine.updateLit(working);
    }

    /** Resets the progress of an idle machine. */
    protected void resetProgress() {
        if (progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    /** Advances the progress one tick; true when a work cycle finished and the progress started over. */
    protected boolean stepProgress() {
        progress++;
        setChanged();
        if (progress < workTicks()) {
            return false;
        }
        progress = 0;
        return true;
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

    /** Ticks per work cycle. Machines whose time depends on a recipe or tech override this. */
    protected int workTicks() {
        return stats().workTicks();
    }

    public ItemSlots inventory() {
        return inventory;
    }

    /**
     * The item capability on {@code side}: none on a side the block fixes to Off, so pipes and hoppers do not connect
     * there; otherwise {@link #sideHandler}.
     */
    public ResourceHandler<ItemResource> itemCapability(Direction side) {
        if (side != null && sideMode(side) == SideMode.DISABLED && sideConfig.isLocked(relativeSide(side))) {
            return null;
        }
        return sideHandler(side);
    }

    /** The item handler automation sees from {@code side}; null means no particular side. */
    public ResourceHandler<ItemResource> sideHandler(Direction side) {
        if (side == null) {
            return autoHandler;
        }
        return sideHandlers.computeIfAbsent(side, SideHandler::new);
    }

    public ContainerData containerData() {
        return data;
    }

    public SideConfig sideConfig() {
        return sideConfig;
    }

    @Override
    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(MachineBlock.FACING) ? state.getValue(MachineBlock.FACING) : Direction.NORTH;
    }

    public RelativeSide relativeSide(Direction absolute) {
        return RelativeSide.fromSide(Conversions.toSide(facing()), Conversions.toSide(absolute));
    }

    public SideMode sideMode(Direction absolute) {
        return sideConfig.mode(relativeSide(absolute));
    }

    @Override
    public SideMode cycleSide(RelativeSide side) {
        SideMode mode = sideConfig.cycle(side);
        sidesChanged();
        return mode;
    }

    /** The tanks of the Fluids layer; machines with several tanks override. Must not depend on instance state. */
    @Override
    public List<SideTank> sideTanks() {
        return List.of();
    }

    @Override
    public FluidSides fluidSides() {
        if (fluidSides == null && !sideTanks().isEmpty()) {
            fluidSides = new FluidSides(sideTanks().size());
            fluidSides.lock(RelativeSide.FRONT);
        }
        return fluidSides;
    }

    @Override
    public int cycleFluidSide(RelativeSide side) {
        FluidSides sides = fluidSides();
        if (sides == null) {
            return FluidSides.ALL;
        }
        int value = sides.cycle(side);
        sidesChanged();
        return value;
    }

    /** Whether the Fluids layer lets {@code tank} through the world face {@code absolute}; true without a Fluids layer. */
    public boolean fluidSideExposes(Direction absolute, int tank) {
        FluidSides sides = fluidSides();
        return sides == null || absolute == null || sides.exposes(relativeSide(absolute), tank);
    }

    /** After a side mode changed: save, refresh neighbour capability caches and show the markers. */
    private void sidesChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(worldPosition);
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        if (isFuelSlot(slot)) {
            return level != null && stack.getBurnTime(null, level.fuelValues()) > 0;
        }
        if (isInputSlot(slot)) {
            return acceptsInput(slot, stack);
        }
        return false;
    }

    protected int getData(int index) {
        if (index >= DATA_SIDE_FIRST && index < DATA_SIDE_FIRST + 6) {
            return sideConfig.mode(RelativeSide.byOrdinal(index - DATA_SIDE_FIRST)).ordinal();
        }
        if (index >= DATA_FLUID_SIDE_FIRST && index < DATA_FLUID_SIDE_FIRST + 6) {
            FluidSides sides = fluidSides();
            return sides == null ? 0 : sides.value(RelativeSide.byOrdinal(index - DATA_FLUID_SIDE_FIRST));
        }
        return switch (index) {
            case DATA_PROGRESS -> progress;
            case DATA_WORK_TICKS -> workTicks();
            default -> 0;
        };
    }

    protected void setData(int index, int value) {
        if (index >= DATA_SIDE_FIRST && index < DATA_SIDE_FIRST + 6) {
            sideConfig.set(RelativeSide.byOrdinal(index - DATA_SIDE_FIRST), SideMode.byOrdinal(value));
        } else if (index >= DATA_FLUID_SIDE_FIRST && index < DATA_FLUID_SIDE_FIRST + 6) {
            FluidSides sides = fluidSides();
            if (sides != null) {
                sides.set(RelativeSide.byOrdinal(index - DATA_FLUID_SIDE_FIRST), value);
            }
        } else if (index == DATA_PROGRESS) {
            progress = value;
        }
    }

    protected boolean canInsertOutput(int slot, ItemStack stack) {
        return inventory.insertItem(slot, stack, true).isEmpty();
    }

    protected void insertOutput(int slot, ItemStack stack) {
        inventory.insertItem(slot, stack, false);
    }

    /** Takes stacks the inputs or fuel slot accept from the neighbour on every pulling side, one move per side. */
    private void pullInputs() {
        for (Direction direction : Direction.values()) {
            if (!sideMode(direction).pulls()) {
                continue;
            }
            ResourceHandler<ItemResource> neighbour = neighbourItems(direction);
            if (neighbour != null) {
                pullFrom(neighbour);
            }
        }
    }

    private ResourceHandler<ItemResource> neighbourItems(Direction direction) {
        return level.getCapability(Capabilities.Item.BLOCK, worldPosition.relative(direction), direction.getOpposite());
    }

    private boolean pullFrom(ResourceHandler<ItemResource> neighbour) {
        for (int from = 0; from < neighbour.size(); from++) {
            ItemStack offered = Transfer.extractItem(neighbour, from, 64, true);
            if (offered.isEmpty()) {
                continue;
            }
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                if (!(isInputSlot(slot) || isFuelSlot(slot)) || !automationAccepts(slot, offered)) {
                    continue;
                }
                int moved = offered.getCount() - inventory.insertItem(slot, offered, true).getCount();
                if (moved <= 0) {
                    continue;
                }
                ItemStack taken = Transfer.extractItem(neighbour, from, moved, false);
                if (!taken.isEmpty()) {
                    inventory.insertItem(slot, taken, false);
                    return true;
                }
            }
        }
        return false;
    }

    private void pushOutputs() {
        if (level == null || ++pushCooldown < PUSH_INTERVAL) {
            return;
        }
        pushCooldown = 0;
        pullInputs();
        for (Direction direction : Direction.values()) {
            if (!sideMode(direction).pushes()) {
                continue;
            }
            ResourceHandler<ItemResource> neighbour = neighbourItems(direction);
            if (neighbour != null) {
                pushTo(neighbour);
            }
        }
    }

    private void pushTo(ResourceHandler<ItemResource> neighbour) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!isOutputSlot(slot)) {
                continue;
            }
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remaining = Transfer.insertItem(neighbour, stack.copy(), false);
            if (remaining.getCount() != stack.getCount()) {
                inventory.setStackInSlot(slot, remaining);
            }
        }
    }

    private void updateLit(boolean working) {
        if (level == null) {
            return;
        }
        // Read the state again: the tick may have changed other properties.
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(MachineBlock.LIT) && state.getValue(MachineBlock.LIT) != working) {
            level.setBlock(worldPosition, state.setValue(MachineBlock.LIT, working), Block.UPDATE_CLIENTS);
        }
    }

    /** Drops the inventory when the block is removed. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        dropContents();
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        NonNullList<ItemStack> stacks = NonNullList.withSize(inventory.getSlots(), ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            stacks.set(slot, inventory.getStackInSlot(slot));
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
        Containers.dropContents(level, worldPosition, stacks);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.save(output, "Inventory");
        output.putInt("Progress", progress);
        if (!sideConfig.isDefault()) {
            ValueIo.putBytes(output, "Sides", sideConfig.toBytes());
        }
        FluidSides sides = fluidSides();
        if (sides != null && !sides.isDefault()) {
            ValueIo.putBytes(output, "FluidSides", sides.toBytes());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // A saved inventory size must never resize the machine.
        if (input.child("Inventory").isPresent()) {
            inventory.load(input, "Inventory");
        }
        progress = input.getIntOr("Progress", 0);
        sideConfig.fromBytes(ValueIo.getBytes(input, "Sides"));
        FluidSides sides = fluidSides();
        if (sides != null) {
            sides.fromBytes(ValueIo.getBytes(input, "FluidSides"));
        }
    }

    /** Sends the side modes along, for the client side markers. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putIntArray("Sides", ValueIo.toInts(sideConfig.toBytes()));
        FluidSides sides = fluidSides();
        if (sides != null) {
            tag.putIntArray("FluidSides", ValueIo.toInts(sides.toBytes()));
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    /** Exposes the inventory to automation with slot role and side mode rules applied. */
    private final class SideHandler extends LegacyItemHandler {

        private final Direction side;

        private SideHandler(Direction side) {
            this.side = side;
        }

        private SideMode mode() {
            return side == null ? SideMode.AUTO : sideMode(side);
        }

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
            if (!mode().allowsInsert() || !automationAccepts(slot, stack)) {
                return stack;
            }
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!mode().allowsExtract() || !isOutputSlot(slot)) {
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
            return mode().allowsInsert() && (isInputSlot(slot) || isFuelSlot(slot)) && automationAccepts(slot, stack);
        }
    }
}
