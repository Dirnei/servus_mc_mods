package io.github.dirnei.mc.servus.manufacturing.menu;

import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.AssemblerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.ConfiguredRecipe;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Assembler menu: machine slots plus the recipe editor state. The editor's
 * ghost grid lives in nine pattern slots that are only active while editing;
 * the machine slots under the popup are inactive meanwhile.
 */
public class AssemblerMenu extends MachineMenu {

    public static final int EDIT_CLOSED = -1;
    public static final int EDIT_NEW = -2;

    public static final int BUTTON_OPEN_NEW = 102;
    public static final int BUTTON_SAVE = 103;
    public static final int BUTTON_DELETE = 104;
    public static final int BUTTON_CANCEL = 105;
    public static final int BUTTON_OPEN_EDIT_FIRST = 200;

    /** Popup geometry shared with the screen. */
    public static final int POPUP_X = 48;
    public static final int POPUP_Y = 4;
    public static final int POPUP_WIDTH = 160;
    public static final int POPUP_HEIGHT = 72;
    public static final int GRID_X = 54;
    public static final int GRID_Y = 20;

    private final AssemblerBlockEntity assembler;
    private final SimpleContainerData editData = new SimpleContainerData(1);
    private int patternSlotFirst;

    public AssemblerMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        this(containerId, playerInventory, machine, machine.containerData());
    }

    private AssemblerMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusManufacturingMenus.ASSEMBLER.get(), containerId, playerInventory, machine, data);
        this.assembler = (AssemblerBlockEntity) machine;
        editData.set(0, EDIT_CLOSED);
        addDataSlots(editData);
    }

    public static AssemblerMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new AssemblerMenu(containerId, playerInventory, readMachine(playerInventory, buf),
                new SimpleContainerData(MachineBlockEntity.DATA_COUNT));
    }

    @Override
    protected void addMachineSlots() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                addSlot(new MachineSlot(machine, AssemblerBlockEntity.SLOT_INPUT_FIRST + row * 6 + col, 8 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean isActive() {
                        return !isEditing();
                    }
                });
            }
        }
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                addSlot(new OutputSlot(machine, AssemblerBlockEntity.SLOT_OUTPUT_FIRST + row * 2 + col, 202 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean isActive() {
                        return !isEditing();
                    }
                });
            }
        }
        patternSlotFirst = slots.size();
        AssemblerBlockEntity assembler = (AssemblerBlockEntity) machine;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new PatternSlot(assembler.pattern(), row * 3 + col, GRID_X + col * 18, GRID_Y + row * 18) {
                    @Override
                    public boolean isActive() {
                        return isEditing();
                    }
                });
            }
        }
    }

    @Override
    protected int playerInventoryX() {
        return 48;
    }

    public AssemblerBlockEntity assembler() {
        return assembler;
    }

    public List<ConfiguredRecipe> recipes() {
        return assembler.recipes();
    }

    public int recipeSlots() {
        return assembler.recipeSlots();
    }

    public int activeIndex() {
        return data().get(AssemblerBlockEntity.DATA_ACTIVE_RECIPE);
    }

    // Editor state

    /** -1 closed, -2 new recipe, otherwise the index being edited. */
    public int editing() {
        return editData.get(0);
    }

    public boolean isEditing() {
        return editing() != EDIT_CLOSED;
    }

    private void setEditing(int value) {
        editData.set(0, value);
    }

    private void closeEditor() {
        assembler.clearPattern();
        setEditing(EDIT_CLOSED);
    }

    /** Opens the editor prefilled with a recipe from the recipe viewer. */
    public void openEditorWith(ResourceLocation id, Player player) {
        Optional<ConfiguredRecipe> recipe = assembler.configuredFor(id, player);
        if (recipe.isEmpty()) {
            return;
        }
        assembler.loadPattern(recipe.get().template());
        if (!isEditing()) {
            setEditing(EDIT_NEW);
        }
    }

    public boolean isPatternSlot(int slotId) {
        return slotId >= patternSlotFirst && slotId < patternSlotFirst + ConfiguredRecipe.GRID;
    }

    /** Clicking a ghost slot copies the carried item into it, or clears it when empty handed. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (isPatternSlot(slotId)) {
            if (isEditing() && slots.get(slotId) instanceof PatternSlot ghost) {
                ghost.setGhost(getCarried());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (isEditing() && index < patternSlotFirst) {
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, index);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_OPEN_NEW) {
            assembler.clearPattern();
            setEditing(EDIT_NEW);
            return true;
        }
        if (id >= BUTTON_OPEN_EDIT_FIRST && id < BUTTON_OPEN_EDIT_FIRST + ConfiguredRecipe.GRID) {
            int index = id - BUTTON_OPEN_EDIT_FIRST;
            if (index < recipes().size()) {
                assembler.loadPattern(recipes().get(index).template());
                setEditing(index);
            }
            return true;
        }
        if (id == BUTTON_SAVE) {
            int editing = editing();
            boolean saved = editing >= 0 ? assembler.replaceFromPattern(editing, player) : assembler.addFromPattern(player);
            if (saved) {
                closeEditor();
            }
            return true;
        }
        if (id == BUTTON_DELETE) {
            int editing = editing();
            if (editing >= 0) {
                assembler.removeRecipe(editing);
            }
            closeEditor();
            return true;
        }
        if (id == BUTTON_CANCEL) {
            closeEditor();
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
