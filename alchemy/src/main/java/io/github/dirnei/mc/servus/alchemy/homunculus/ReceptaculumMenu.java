package io.github.dirnei.mc.servus.alchemy.homunculus;

import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyMenus;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.OutputSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** The Receptaculum's gift slots and Elixir slot in the top row, its 27 yield slots below, then the player inventory. */
public class ReceptaculumMenu extends MachineMenu {

    public static final int GIFT_Y = 20;
    public static final int ELIXIR_X = 152;
    public static final int YIELD_Y = 44;
    public static final int INVENTORY_Y = 122;

    public ReceptaculumMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusAlchemyMenus.RECEPTACULUM.get(), containerId, playerInventory, machine);
    }

    private ReceptaculumMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusAlchemyMenus.RECEPTACULUM.get(), containerId, playerInventory, machine, data);
    }

    public static ReceptaculumMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new ReceptaculumMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData());
    }

    @Override
    protected void addMachineSlots() {
        for (int slot = 0; slot < ReceptaculumBlockEntity.YIELD_SLOTS; slot++) {
            addSlot(new OutputSlot(machine, slot, 8 + slot % 9 * 18, YIELD_Y + slot / 9 * 18));
        }
        for (int gift = 0; gift < ReceptaculumBlockEntity.GIFT_SLOTS; gift++) {
            addSlot(new MachineSlot(machine, ReceptaculumBlockEntity.GIFT_FIRST + gift, 8 + gift * 18, GIFT_Y));
        }
        addSlot(new MachineSlot(machine, ReceptaculumBlockEntity.SLOT_ELIXIR, ELIXIR_X, GIFT_Y));
    }

    @Override
    protected int playerInventoryY() {
        return INVENTORY_Y;
    }
}
