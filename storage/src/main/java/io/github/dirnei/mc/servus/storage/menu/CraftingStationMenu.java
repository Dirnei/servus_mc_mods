package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.lib.block.RecipeEditorButtons;
import io.github.dirnei.mc.servus.lib.block.SideConfigData;
import io.github.dirnei.mc.servus.lib.block.SideConfigMenus;
import io.github.dirnei.mc.servus.lib.side.RelativeSide;
import io.github.dirnei.mc.servus.lib.side.SideMode;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * Inputs, recipe cells and outputs above the player inventory, plus the
 * recipe editor state like the Assembler's: the ghost grid lives in nine
 * pattern slots that are only active while the editor popup is open, and the
 * station slots under the popup are inactive meanwhile.
 */
public class CraftingStationMenu extends AbstractContainerMenu {

    public static final int WIDTH = 200;
    public static final int HEIGHT = 166;
    /** Height of the station part of the GUI above the player inventory; dimmed while the popup is open. */
    public static final int MACHINE_AREA_HEIGHT = 80;
    public static final int POPUP_X = 20;
    public static final int POPUP_Y = 4;
    public static final int INPUTS_X = 8;
    public static final int INPUTS_Y = 17;
    public static final int CELLS_X = 72;
    public static final int CELLS_Y = 17;
    public static final int PLUS_X = 112;
    public static final int PLUS_Y = 17;
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
    private final DataSlot active = DataSlot.standalone();
    private final DataSlot recipeSlots = DataSlot.standalone();
    private final DataSlot editing = DataSlot.standalone();
    private final int patternStart;

    public CraftingStationMenu(int containerId, Inventory playerInventory, CraftingStationBlockEntity station) {
        super(ServusStorageMenus.CRAFTING_STATION.get(), containerId);
        this.station = station;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());
        editing.set(RecipeEditorButtons.EDIT_CLOSED);
        patternStart = slots.size();
        for (int i = 0; i < CraftingStationBlockEntity.GRID; i++) {
            addSlot(new GhostSlot(station::pattern, i, RecipeEditorButtons.gridX(POPUP_X) + (i % 3) * 18, RecipeEditorButtons.gridY(POPUP_Y) + (i / 3) * 18) {
                @Override
                public boolean isActive() {
                    return isEditing();
                }
            });
        }
        inputsStart = slots.size();
        for (int i = 0; i < CraftingStationBlockEntity.INPUT_SLOTS; i++) {
            addSlot(new SlotItemHandler(station.inputs(), i, INPUTS_X + (i % 3) * 18, INPUTS_Y + (i / 3) * 18) {
                @Override
                public boolean isActive() {
                    return !isEditing();
                }
            });
        }
        outputsStart = slots.size();
        for (int i = 0; i < CraftingStationBlockEntity.OUTPUT_SLOTS; i++) {
            addSlot(new SlotItemHandler(station.outputs(), i, OUTPUTS_X + (i % 2) * 18, OUTPUTS_Y + (i / 2) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean isActive() {
                    return !isEditing();
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
        addDataSlot(active);
        addDataSlot(recipeSlots);
        addDataSlot(editing);
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

    /** Index of the recipe being crafted, or -1. */
    public int activeIndex() {
        return active.get();
    }

    /** How many recipes the station can hold, from the server's data. */
    public int recipeSlots() {
        return recipeSlots.get();
    }

    // Editor state

    /** {@link RecipeEditorButtons#EDIT_CLOSED}, {@link RecipeEditorButtons#EDIT_NEW} or the index being edited. */
    public int editing() {
        return editing.get();
    }

    public boolean isEditing() {
        return editing() != RecipeEditorButtons.EDIT_CLOSED;
    }

    private void closeEditor() {
        station.clearPattern();
        editing.set(RecipeEditorButtons.EDIT_CLOSED);
    }

    /** Opens the editor prefilled with a vanilla crafting recipe from the recipe viewer. */
    public void openEditorWith(ResourceLocation id) {
        if (player.level().isClientSide()) {
            return;
        }
        CraftingStationBlockEntity.templateOf(player.level(), id).ifPresent(template -> {
            station.loadPattern(template);
            if (!isEditing()) {
                editing.set(RecipeEditorButtons.EDIT_NEW);
            }
        });
    }

    public boolean isPatternSlot(int slotId) {
        return slotId >= patternStart && slotId < patternStart + CraftingStationBlockEntity.GRID;
    }

    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide()) {
            progress.set(station.progress());
            workTicks.set(station.workTicks());
            active.set(station.activeIndex());
            recipeSlots.set(station.recipeSlots());
        }
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && isEditing()) {
            station.clearPattern();
        }
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
        if (id == RecipeEditorButtons.OPEN_NEW) {
            station.clearPattern();
            editing.set(RecipeEditorButtons.EDIT_NEW);
            return true;
        }
        int edit = RecipeEditorButtons.editIndexOf(id);
        if (edit >= 0) {
            if (edit < station.recipes().size()) {
                station.loadPattern(station.recipes().get(edit).template());
                editing.set(edit);
            }
            return true;
        }
        if (id == RecipeEditorButtons.SAVE && isEditing()) {
            String result = station.saveFromPattern(editing());
            if (result.isEmpty()) {
                closeEditor();
            } else {
                player.displayClientMessage(Component.translatable("gui.servus_storage.crafting_station.message." + result)
                        .withStyle(ChatFormatting.RED), true);
            }
            return true;
        }
        if (id == RecipeEditorButtons.DELETE && isEditing()) {
            if (editing() >= 0) {
                station.removeRecipe(editing());
            }
            closeEditor();
            return true;
        }
        if (id == RecipeEditorButtons.CANCEL) {
            closeEditor();
            return true;
        }
        return false;
    }

    /** Pattern cells take a copy of the cursor and never hand anything out. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (isPatternSlot(slotId)) {
            if (isEditing() && !player.level().isClientSide() && slots.get(slotId) instanceof GhostSlot ghost) {
                ghost.setGhost(getCarried());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** Shift click: inventory stacks go to the inputs when a recipe uses them, station slots go back to the inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < inputsStart || index >= slots.size() || isEditing()) {
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
