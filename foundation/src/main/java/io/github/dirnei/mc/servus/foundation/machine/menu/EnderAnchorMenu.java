package io.github.dirnei.mc.servus.foundation.machine.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EnderAnchorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

public class EnderAnchorMenu extends MachineMenu {

    public EnderAnchorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(MachineContent.ENDER_ANCHOR_MENU.get(), containerId, playerInventory, machine);
    }

    private EnderAnchorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(MachineContent.ENDER_ANCHOR_MENU.get(), containerId, playerInventory, machine, data);
    }

    public static EnderAnchorMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new EnderAnchorMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData());
    }

    @Override
    protected void addMachineSlots() {
    }

    public int status() {
        return data().get(EnderAnchorBlockEntity.DATA_STATUS);
    }
}
