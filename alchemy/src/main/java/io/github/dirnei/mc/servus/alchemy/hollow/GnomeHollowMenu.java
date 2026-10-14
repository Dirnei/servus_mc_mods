package io.github.dirnei.mc.servus.alchemy.hollow;

import io.github.dirnei.mc.servus.alchemy.PrincipleMenu;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyMenus;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.OutputSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** The Hollow's sample and output slots, its tanks, ripeness and gnome. */
public class GnomeHollowMenu extends PrincipleMenu {

    public GnomeHollowMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusAlchemyMenus.GNOME_HOLLOW.get(), containerId, playerInventory, machine);
    }

    private GnomeHollowMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusAlchemyMenus.GNOME_HOLLOW.get(), containerId, playerInventory, machine, data);
    }

    public static GnomeHollowMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new GnomeHollowMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData(GnomeHollowBlockEntity.DATA_COUNT));
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, GnomeHollowBlockEntity.SLOT_SAMPLE, 56, 35));
        addSlot(new OutputSlot(machine, GnomeHollowBlockEntity.SLOT_OUTPUT, 116, 35));
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
