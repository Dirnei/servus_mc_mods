package io.github.dirnei.mc.servus.manufacturing.menu;

import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
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

/**
 * Base menu for machines: machine slots first, then the player inventory,
 * with progress and burn data synced through {@link ContainerData}.
 */
public abstract class MachineMenu extends AbstractContainerMenu {

    protected final MachineBlockEntity machine;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Block block;
    private int machineSlots;

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
        addPlayerSlots(playerInventory);
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
        return new SimpleContainerData(MachineBlockEntity.DATA_COUNT);
    }

    protected abstract void addMachineSlots();

    private void addPlayerSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    public MachineBlockEntity machine() {
        return machine;
    }

    // Data for screens

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

    // Vanilla contract

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
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }
}
