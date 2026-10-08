package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.foundation.block.SideConfigData;
import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

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
    public static final int HOTBAR_Y = 142;

    private final CraftingStationBlockEntity station;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int inputsStart;
    private final int outputsStart;
    private final int inventoryStart;
    private final DataSlot progress = DataSlot.standalone();
    private final DataSlot workTicks = DataSlot.standalone();

    public CraftingStationMenu(int containerId, Inventory playerInventory, CraftingStationBlockEntity station) {
        super(ServusStorageMenus.CRAFTING_STATION.get(), containerId);
        this.station = station;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());
        inputsStart = slots.size();
        for (int i = 0; i < CraftingStationBlockEntity.INPUT_SLOTS; i++) {
            addSlot(new SlotItemHandler(station.inputs(), i, INPUTS_X + (i % 3) * 18, INPUTS_Y + (i / 3) * 18));
        }
        outputsStart = slots.size();
        for (int i = 0; i < CraftingStationBlockEntity.OUTPUT_SLOTS; i++) {
            addSlot(new SlotItemHandler(station.outputs(), i, OUTPUTS_X + (i % 2) * 18, OUTPUTS_Y + (i / 2) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        inventoryStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
        addDataSlot(progress);
        addDataSlots(new SideConfigData(station.sideConfig()));
        addDataSlot(workTicks);
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

    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide()) {
            progress.set(station.progress());
            workTicks.set(station.workTicks());
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
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.CRAFTING_STATION.get());
    }
}
