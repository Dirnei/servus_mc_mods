package io.github.dirnei.mc.servus.foundation.drive;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.upgrade.UpgradeSlots;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/** The Electric Motor's upgrade slot and its synced energy buffer. */
public class ElectricMotorMenu extends MachineMenu {

    public static final int UPGRADE_X = 80;
    public static final int UPGRADE_Y = 35;

    public ElectricMotorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(MachineContent.ELECTRIC_MOTOR_MENU.get(), containerId, playerInventory, machine);
    }

    private ElectricMotorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(MachineContent.ELECTRIC_MOTOR_MENU.get(), containerId, playerInventory, machine, data);
    }

    public static ElectricMotorMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new ElectricMotorMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData());
    }

    @Override
    protected void addMachineSlots() {
        UpgradeSlots upgrades = ((ElectricMotorBlockEntity) machine).upgrades();
        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            addSlot(new ResourceHandlerSlot(upgrades, upgrades::set, slot, UPGRADE_X + slot * 18, UPGRADE_Y));
        }
    }
}
