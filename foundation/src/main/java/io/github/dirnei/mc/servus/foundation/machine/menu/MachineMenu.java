package io.github.dirnei.mc.servus.foundation.machine.menu;

import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Base menu for machines: machine slots first, then the player inventory,
 * with progress and burn data synced through {@link ContainerData}.
 */
public abstract class MachineMenu extends AbstractContainerMenu {

    protected final MachineBlockEntity machine;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Block block;
    private final int machineSlots;

    protected MachineMenu(MenuType<?> type, int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        this(type, containerId, playerInventory, machine, machine.containerData());
    }

    protected MachineMenu(MenuType<?> type, int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(type, containerId);
        this.machine = machine;
        this.data = data;
        this.access = ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos());
        this.block = machine.getBlockState().getBlock();
        addMachineSlots();
        this.machineSlots = slots.size();
        PlayerInventorySlots.add(this::addSlot, playerInventory, playerInventoryX(), playerInventoryY());
        addDataSlots(data);
    }

    /** Resolves the machine on the client from the position the server wrote into the open packet. */
    protected static MachineBlockEntity readMachine(Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            return machine;
        }
        throw new IllegalStateException("No machine at " + pos);
    }

    protected static ContainerData clientData() {
        return clientData(MachineBlockEntity.DATA_COUNT);
    }

    protected static ContainerData clientData(int count) {
        return new SimpleContainerData(count);
    }

    protected abstract void addMachineSlots();

    /** Left edge of the player inventory; wider GUIs centre it. */
    protected int playerInventoryX() {
        return 8;
    }

    /** Top edge of the player inventory; taller GUIs move it down. */
    protected int playerInventoryY() {
        return 84;
    }

    public MachineBlockEntity machine() {
        return machine;
    }

    protected ContainerData data() {
        return data;
    }

    public int progressScaled(int pixels) {
        int work = data.get(MachineBlockEntity.DATA_WORK_TICKS);
        int progress = data.get(MachineBlockEntity.DATA_PROGRESS);
        return work <= 0 ? 0 : progress * pixels / work;
    }

    public boolean isBurning() {
        return data.get(MachineBlockEntity.DATA_BURN_TIME) > 0;
    }

    public int burnScaled(int pixels) {
        int duration = data.get(MachineBlockEntity.DATA_BURN_DURATION);
        int time = data.get(MachineBlockEntity.DATA_BURN_TIME);
        if (duration <= 0) {
            duration = 200;
        }
        return time * pixels / duration;
    }

    public int energyStored() {
        return data.get(MachineBlockEntity.DATA_ENERGY);
    }

    public int energyCapacity() {
        return data.get(MachineBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public int energyScaled(int pixels) {
        int capacity = energyCapacity();
        return capacity <= 0 ? 0 : (int) ((long) energyStored() * pixels / capacity);
    }

    /** Fluid entries are only synced by machines with a fluid buffer. */
    private int dataOrZero(int index) {
        return index < data.getCount() ? data.get(index) : 0;
    }

    public boolean hasFluid() {
        return data.getCount() > MachineBlockEntity.DATA_SOURCE_VALID;
    }

    public FluidStack fluid() {
        int amount = dataOrZero(MachineBlockEntity.DATA_FLUID_AMOUNT);
        Fluid fluid = BuiltInRegistries.FLUID.byId(dataOrZero(MachineBlockEntity.DATA_FLUID_ID));
        return amount <= 0 || fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    public int fluidCapacity() {
        return dataOrZero(MachineBlockEntity.DATA_FLUID_CAPACITY);
    }

    public boolean sourceValid() {
        return dataOrZero(MachineBlockEntity.DATA_SOURCE_VALID) != 0;
    }

    /** Button ids from this value on cycle the relative side with that offset. */
    public static final int SIDE_BUTTON_FIRST = SideConfigMenus.BUTTON_FIRST;

    public SideMode sideMode(RelativeSide side) {
        return SideMode.byOrdinal(data.get(MachineBlockEntity.DATA_SIDE_FIRST + side.ordinal()));
    }

    /** The Fluids layer value of a side as synced to the client. */
    public int fluidSide(RelativeSide side) {
        return data.get(MachineBlockEntity.DATA_FLUID_SIDE_FIRST + side.ordinal());
    }

    @Override

    public boolean clickMenuButton(Player player, int id) {
        RelativeSide side = SideConfigMenus.sideOf(id);
        if (side != null) {
            machine.cycleSide(side);
            return true;
        }
        RelativeSide fluidSide = SideConfigMenus.fluidSideOf(id);
        if (fluidSide != null && machine.fluidSides() != null) {
            machine.cycleFluidSide(fluidSide);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, block);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int invStart = machineSlots;
        int hotbarStart = machineSlots + 27;
        int end = machineSlots + 36;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, invStart, end, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, machineSlots, false)) {
            if (index < hotbarStart) {
                if (!moveItemStackTo(stack, hotbarStart, end, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, invStart, hotbarStart, false)) {
                return ItemStack.EMPTY;
            }
        }
        QuickMoves.settle(slot, stack);
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }
}
