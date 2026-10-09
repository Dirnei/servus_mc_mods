package io.github.dirnei.mc.servus.foundation.machine.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.loader.ChunkLoaderBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

public class ChunkLoaderMenu extends MachineMenu {

    public ChunkLoaderMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(MachineContent.CHUNK_LOADER_MENU.get(), containerId, playerInventory, machine);
    }

    private ChunkLoaderMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(MachineContent.CHUNK_LOADER_MENU.get(), containerId, playerInventory, machine, data);
    }

    public static ChunkLoaderMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new ChunkLoaderMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData());
    }

    @Override
    protected void addMachineSlots() {
    }

    public int status() {
        return data().get(ChunkLoaderBlockEntity.DATA_STATUS);
    }
}
