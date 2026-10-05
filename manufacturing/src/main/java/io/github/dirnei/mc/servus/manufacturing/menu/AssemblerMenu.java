package io.github.dirnei.mc.servus.manufacturing.menu;

import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.AssemblerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.ConfiguredRecipe;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;

public class AssemblerMenu extends MachineMenu {

    /** Button ids: remove configured recipe at index, add from pattern, clear pattern. */
    public static final int BUTTON_REMOVE_FIRST = 0;
    public static final int BUTTON_ADD_PATTERN = 100;
    public static final int BUTTON_CLEAR_PATTERN = 101;

    private final AssemblerBlockEntity assembler;
    private int patternSlotFirst;

    public AssemblerMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        this(containerId, playerInventory, machine, machine.containerData());
    }

    private AssemblerMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusManufacturingMenus.ASSEMBLER.get(), containerId, playerInventory, machine, data);
        this.assembler = (AssemblerBlockEntity) machine;
    }

    public static AssemblerMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new AssemblerMenu(containerId, playerInventory, readMachine(playerInventory, buf),
                new SimpleContainerData(MachineBlockEntity.DATA_COUNT));
    }

    @Override
    protected void addMachineSlots() {
        AssemblerBlockEntity assembler = (AssemblerBlockEntity) machine;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                addSlot(new MachineSlot(machine, AssemblerBlockEntity.SLOT_INPUT_FIRST + row * 6 + col, 8 + col * 18, 17 + row * 18));
            }
        }
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                addSlot(new OutputSlot(machine, AssemblerBlockEntity.SLOT_OUTPUT_FIRST + row * 2 + col, 202 + col * 18, 17 + row * 18));
            }
        }
        patternSlotFirst = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new PatternSlot(assembler.pattern(), row * 3 + col, 124 + col * 18, 17 + row * 18));
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

    public boolean isPatternSlot(int slotId) {
        return slotId >= patternSlotFirst && slotId < patternSlotFirst + ConfiguredRecipe.GRID;
    }

    /** Clicking a ghost slot copies the carried item into it, or clears it when empty handed. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (isPatternSlot(slotId)) {
            if (slots.get(slotId) instanceof PatternSlot ghost) {
                ghost.setGhost(getCarried());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_ADD_PATTERN) {
            assembler.addFromPattern(player);
            return true;
        }
        if (id == BUTTON_CLEAR_PATTERN) {
            assembler.clearPattern();
            return true;
        }
        if (id >= BUTTON_REMOVE_FIRST && id < BUTTON_REMOVE_FIRST + ConfiguredRecipe.GRID) {
            assembler.removeRecipe(id - BUTTON_REMOVE_FIRST);
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
