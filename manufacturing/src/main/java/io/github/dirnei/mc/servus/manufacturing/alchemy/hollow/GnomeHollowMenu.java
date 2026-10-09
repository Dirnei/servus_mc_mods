package io.github.dirnei.mc.servus.manufacturing.alchemy.hollow;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.OutputSlot;
import io.github.dirnei.mc.servus.manufacturing.alchemy.AlchemyMachines;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleMenu;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** The Hollow's sample and output slots, its tanks, ripeness and gnome. */
public class GnomeHollowMenu extends MachineMenu implements PrincipleMenu {

    public GnomeHollowMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusManufacturingMenus.GNOME_HOLLOW.get(), containerId, playerInventory, machine);
    }

    private GnomeHollowMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusManufacturingMenus.GNOME_HOLLOW.get(), containerId, playerInventory, machine, data);
    }

    public static GnomeHollowMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new GnomeHollowMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData(GnomeHollowBlockEntity.DATA_COUNT));
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, GnomeHollowBlockEntity.SLOT_SAMPLE, 56, 35));
        addSlot(new OutputSlot(machine, GnomeHollowBlockEntity.SLOT_OUTPUT, 116, 35));
    }

    @Override
    public int principleAmount(Principle principle) {
        return data().get(AlchemyMachines.DATA_PRINCIPLE_FIRST + principle.ordinal());
    }

    @Override
    public int principleCapacity() {
        return data().get(AlchemyMachines.DATA_TANK_CAPACITY);
    }

    public int ripeness() {
        return data().get(GnomeHollowBlockEntity.DATA_RIPENESS);
    }

    public int steps() {
        return data().get(GnomeHollowBlockEntity.DATA_STEPS);
    }

    public boolean bound() {
        return data().get(GnomeHollowBlockEntity.DATA_BOUND) != 0;
    }
}
