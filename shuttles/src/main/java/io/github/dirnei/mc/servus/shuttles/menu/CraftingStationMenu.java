package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.block.SideConfigData;
import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * Inputs, the job area and outputs above the player inventory. The job comes
 * from the Crafting CPU; the station has nothing to configure but its sides.
 */
public class CraftingStationMenu extends AbstractContainerMenu {

    public static final int WIDTH = 200;
    public static final int HEIGHT = 166;
    public static final int INPUTS_X = 8;
    public static final int INPUTS_Y = 17;
    public static final int JOB_X = 72;
    public static final int JOB_Y = 17;
    public static final int JOB_WIDTH = 58;
    public static final int JOB_HEIGHT = 54;
    public static final int PROGRESS_X = 114;
    public static final int PROGRESS_Y = 44;
    public static final int OUTPUTS_X = 140;
    public static final int OUTPUTS_Y = 26;
    public static final int INVENTORY_X = 20;
    public static final int INVENTORY_Y = 84;

    private final CraftingStationBlockEntity station;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int inputsStart;
    private final int outputsStart;
    private final int inventoryStart;
    private final DataSlot progress = DataSlot.standalone();
    private final DataSlot workTicks = DataSlot.standalone();
    private final DataSlot noPower = DataSlot.standalone();

    public CraftingStationMenu(int containerId, Inventory playerInventory, CraftingStationBlockEntity station) {
        super(ServusShuttlesMenus.CRAFTING_STATION.get(), containerId);
        this.station = station;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());
        inputsStart = slots.size();
        for (int i = 0; i < CraftingStationBlockEntity.INPUT_SLOTS; i++) {
            addSlot(new ResourceHandlerSlot(station.inputs(), station.inputs()::set, i, INPUTS_X + (i % 3) * 18, INPUTS_Y + (i / 3) * 18));
        }
        outputsStart = slots.size();
        for (int i = 0; i < CraftingStationBlockEntity.OUTPUT_SLOTS; i++) {
            addSlot(new ResourceHandlerSlot(station.outputs(), station.outputs()::set, i, OUTPUTS_X + (i % 2) * 18, OUTPUTS_Y + (i / 2) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        inventoryStart = slots.size();
        PlayerInventorySlots.add(this::addSlot, playerInventory, INVENTORY_X, INVENTORY_Y);
        addDataSlot(progress);
        addDataSlots(new SideConfigData(station.sideConfig()));
        addDataSlot(workTicks);
        addDataSlot(noPower);
    }

    public static CraftingStationMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof CraftingStationBlockEntity station) {
            return new CraftingStationMenu(containerId, playerInventory, station);
        }
        throw new IllegalStateException("No crafting station at " + pos);
    }

    public CraftingStationBlockEntity station() {
        return station;
    }

    public SideMode sideMode(RelativeSide side) {
        return station.sideConfig().mode(side);
    }

    public int inventoryStart() {
        return inventoryStart;
    }

    public int progress() {
        return progress.get();
    }

    public int workTicks() {
        return workTicks.get();
    }

    /** Whether the station's craft waits for power. */
    public boolean waitingForPower() {
        return noPower.get() != 0;
    }

    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide()) {
            progress.set(station.progress());
            workTicks.set(station.workTicks());
            noPower.set(station.waitingForPower() ? 1 : 0);
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide()) {
            return false;
        }
        RelativeSide side = SideConfigMenus.sideOf(id);
        if (side != null) {
            station.cycleSide(side);
            return true;
        }
        return false;
    }

    /** Shift click: inventory stacks go to the inputs when the job uses them, station slots go back to the inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < inputsStart || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < inventoryStart) {
            moved = moveItemStackTo(stack, inventoryStart, slots.size(), true);
        } else {
            if (!station.uses(stack)) {
                return ItemStack.EMPTY;
            }
            moved = moveItemStackTo(stack, inputsStart, outputsStart, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        QuickMoves.settle(slot, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusShuttlesBlocks.CRAFTING_STATION.get());
    }
}
