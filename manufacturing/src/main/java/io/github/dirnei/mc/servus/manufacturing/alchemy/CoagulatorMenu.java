package io.github.dirnei.mc.servus.manufacturing.alchemy;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.OutputSlot;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The Coagulator's slots, its principle tanks and the recipe selection by menu button. */
public class CoagulatorMenu extends MachineMenu implements PrincipleMenu {

    /** Button ids from this value on select the recipe with that index in the sorted list. */
    public static final int RECIPE_BUTTON_FIRST = 100;

    public CoagulatorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusManufacturingMenus.COAGULATOR.get(), containerId, playerInventory, machine);
    }

    private CoagulatorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusManufacturingMenus.COAGULATOR.get(), containerId, playerInventory, machine, data);
    }

    public static CoagulatorMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new CoagulatorMenu(containerId, playerInventory, readMachine(playerInventory, buf),
                clientData(CoagulatorBlockEntity.DATA_COUNT));
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, CoagulatorBlockEntity.SLOT_INGREDIENT, 56, 35));
        addSlot(new OutputSlot(machine, CoagulatorBlockEntity.SLOT_OUTPUT, 116, 35));
    }

    @Override
    public int principleAmount(Principle principle) {
        return data().get(AlchemyMachines.DATA_PRINCIPLE_FIRST + principle.ordinal());
    }

    @Override
    public int principleCapacity() {
        return data().get(AlchemyMachines.DATA_TANK_CAPACITY);
    }

    /** Index of the selected recipe in the sorted list, or -1. */
    public int selectedIndex() {
        return data().get(CoagulatorBlockEntity.DATA_SELECTED) - 1;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int index = id - RECIPE_BUTTON_FIRST;
        if (index >= 0 && id < SIDE_BUTTON_FIRST && machine instanceof CoagulatorBlockEntity coagulator) {
            List<RecipeHolder<CoagulationRecipe>> recipes = CoagulatorBlockEntity.sortedRecipes(player.level().getRecipeManager());
            if (index < recipes.size()) {
                RecipeHolder<CoagulationRecipe> holder = recipes.get(index);
                if (!coagulator.unlocked(holder.value())) {
                    return false;
                }
                coagulator.select(holder.id());
                return true;
            }
            return false;
        }
        return super.clickMenuButton(player, id);
    }
}
