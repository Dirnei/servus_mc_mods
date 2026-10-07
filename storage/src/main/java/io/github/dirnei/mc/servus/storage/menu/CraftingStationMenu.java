package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Pattern grid, recipe cells, inputs and outputs above the player inventory.
 * Save and remove go through menu buttons.
 */
public class CraftingStationMenu extends AbstractContainerMenu {

    public static final int BUTTON_SAVE = 100;
    public static final int BUTTON_REMOVE_FIRST = 200;
    public static final int GRID_X = 8;
    public static final int GRID_Y = 18;
    public static final int INPUTS_X = 80;
    public static final int INPUTS_Y = 36;
    public static final int OUTPUTS_X = 134;
    public static final int OUTPUTS_Y = 36;
    public static final int INVENTORY_Y = 104;
    public static final int HOTBAR_Y = 162;
    /** Message codes shown in the action line. */
    public static final String[] MESSAGES = {"", "empty", "no_match", "duplicate", "full", "saved"};

    private final CraftingStationBlockEntity station;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int inputsStart;
    private final int outputsStart;
    private final int inventoryStart;
    private final DataSlot progress = DataSlot.standalone();
    private final DataSlot workTicks = DataSlot.standalone();
    private final DataSlot message = DataSlot.standalone();

    public CraftingStationMenu(int containerId, Inventory playerInventory, CraftingStationBlockEntity station) {
        super(ServusStorageMenus.CRAFTING_STATION.get(), containerId);
        this.station = station;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());
        for (int i = 0; i < CraftingStationBlockEntity.GRID; i++) {
            addSlot(new GhostSlot(station::pattern, i, GRID_X + (i % 3) * 18, GRID_Y + (i / 3) * 18));
        }
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
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, HOTBAR_Y));
        }
        addDataSlot(progress);
        addDataSlot(workTicks);
        addDataSlot(message);
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

    public int inventoryStart() {
        return inventoryStart;
    }

    public int progress() {
        return progress.get();
    }

    public int workTicks() {
        return workTicks.get();
    }

    /** The last refusal or confirmation, as a key of {@link #MESSAGES}. */
    public String message() {
        int code = message.get();
        return code >= 0 && code < MESSAGES.length ? MESSAGES[code] : "";
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
        if (id == BUTTON_SAVE) {
            String result = station.saveFromPattern();
            message.set(code(result.isEmpty() ? "saved" : result));
            return true;
        }
        if (id >= BUTTON_REMOVE_FIRST && id < BUTTON_REMOVE_FIRST + 64) {
            station.removeRecipe(id - BUTTON_REMOVE_FIRST);
            message.set(0);
            return true;
        }
        return false;
    }

    private static int code(String key) {
        for (int i = 0; i < MESSAGES.length; i++) {
            if (MESSAGES[i].equals(key)) {
                return i;
            }
        }
        return 0;
    }

    /** Pattern cells take a copy of the cursor and never hand anything out. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < inputsStart && slots.get(slotId) instanceof GhostSlot ghost) {
            if (!player.level().isClientSide()) {
                ghost.setGhost(getCarried());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** Shift click: inventory stacks go to the inputs when a recipe uses them, station slots go back to the inventory. */
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
