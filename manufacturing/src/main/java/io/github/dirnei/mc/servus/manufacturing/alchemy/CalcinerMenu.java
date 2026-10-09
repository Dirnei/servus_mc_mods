package io.github.dirnei.mc.servus.manufacturing.alchemy;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineSlot;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;

/** The Calciner's input and fuel slots plus its synced principle tanks. */
public class CalcinerMenu extends MachineMenu implements PrincipleMenu {

    public CalcinerMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusManufacturingMenus.CALCINER.get(), containerId, playerInventory, machine);
    }

    private CalcinerMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusManufacturingMenus.CALCINER.get(), containerId, playerInventory, machine, data);
    }

    public static CalcinerMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new CalcinerMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData(AlchemyMachines.DATA_COUNT));
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, CalcinerBlockEntity.SLOT_INPUT, 56, 17));
        addSlot(new MachineSlot(machine, CalcinerBlockEntity.SLOT_FUEL, 56, 53));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        Principle principle = PrincipleBuckets.gaugeOf(id);
        if (principle != null) {
            return machine instanceof CalcinerBlockEntity calciner && PrincipleBuckets.clickGauge(player, this, calciner.tanks(), principle);
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public int principleAmount(Principle principle) {
        return data().get(AlchemyMachines.DATA_PRINCIPLE_FIRST + principle.ordinal());
    }

    @Override
    public int principleCapacity() {
        return data().get(AlchemyMachines.DATA_TANK_CAPACITY);
    }
}
