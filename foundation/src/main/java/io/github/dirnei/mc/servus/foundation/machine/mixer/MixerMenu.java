package io.github.dirnei.mc.servus.foundation.machine.mixer;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.OutputSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** The Mixer's 2x2 inputs and output, with the work collected as the progress arrow and the tank for the gauge. */
public class MixerMenu extends MachineMenu {

    private static final int[][] INPUT_POSITIONS = {{34, 26}, {52, 26}, {34, 44}, {52, 44}};

    public MixerMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(MachineContent.MIXER_MENU.get(), containerId, playerInventory, machine);
    }

    private MixerMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(MachineContent.MIXER_MENU.get(), containerId, playerInventory, machine, data);
    }

    public static MixerMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new MixerMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData(MachineBlockEntity.DATA_COUNT_FLUID));
    }

    @Override
    protected void addMachineSlots() {
        for (int slot = 0; slot < MixerBlockEntity.INPUTS; slot++) {
            addSlot(new MachineSlot(machine, slot, INPUT_POSITIONS[slot][0], INPUT_POSITIONS[slot][1]));
        }
        addSlot(new OutputSlot(machine, MixerBlockEntity.SLOT_OUTPUT, 116, 35));
    }
}
