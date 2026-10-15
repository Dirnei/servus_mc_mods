package io.github.dirnei.mc.servus.alchemy.forge;

import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyMenus;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.OutputSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** A furnace's input and output slots; the eternal flame burns where a furnace keeps its fuel. */
public class SalamanderForgeMenu extends MachineMenu {

    public SalamanderForgeMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusAlchemyMenus.SALAMANDER_FORGE.get(), containerId, playerInventory, machine);
    }

    private SalamanderForgeMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusAlchemyMenus.SALAMANDER_FORGE.get(), containerId, playerInventory, machine, data);
    }

    public static SalamanderForgeMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new SalamanderForgeMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData());
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, SalamanderForgeBlockEntity.SLOT_INPUT, 56, 17));
        addSlot(new OutputSlot(machine, SalamanderForgeBlockEntity.SLOT_OUTPUT, 116, 35));
    }
}
