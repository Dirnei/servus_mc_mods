package io.github.dirnei.mc.servus.manufacturing.menu;

import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.loader.ChunkLoaderBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

public class ChunkLoaderMenu extends MachineMenu {

    public ChunkLoaderMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusManufacturingMenus.CHUNK_LOADER.get(), containerId, playerInventory, machine);
    }

    private ChunkLoaderMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusManufacturingMenus.CHUNK_LOADER.get(), containerId, playerInventory, machine, data);
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
