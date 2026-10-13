package io.github.dirnei.mc.servus.foundation.machine.menu;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.anchor.AnchorArea;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EnderAnchorBlockEntity;
import io.github.dirnei.mc.servus.foundation.upgrade.AnchorUpgradeSlots;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * Ender Anchor GUI: two Anchor Upgrade slots, the area and the team's load as
 * data, and the arrow buttons: ids 0 to 3 shift the area north, south, east
 * or west ({@link AnchorArea.Shift#byId}).
 */
public class EnderAnchorMenu extends MachineMenu {

    public static final int UPGRADE_X = 8;
    public static final int UPGRADE_Y = 18;

    public EnderAnchorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(MachineContent.ENDER_ANCHOR_MENU.get(), containerId, playerInventory, machine);
    }

    private EnderAnchorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(MachineContent.ENDER_ANCHOR_MENU.get(), containerId, playerInventory, machine, data);
    }

    public static EnderAnchorMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new EnderAnchorMenu(containerId, playerInventory, readMachine(playerInventory, buf),
                clientData(EnderAnchorBlockEntity.DATA_COUNT_ANCHOR));
    }

    @Override
    protected void addMachineSlots() {
        AnchorUpgradeSlots upgrades = ((EnderAnchorBlockEntity) machine).upgrades();
        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            addSlot(new ResourceHandlerSlot(upgrades, upgrades::set, slot, UPGRADE_X + slot * 18, UPGRADE_Y));
        }
    }

    public EnderAnchorBlockEntity anchor() {
        return (EnderAnchorBlockEntity) machine;
    }

    public int status() {
        return data().get(EnderAnchorBlockEntity.DATA_STATUS);
    }

    /** The area as the server last reported it. */
    public AnchorArea area() {
        return AnchorArea.of(data().get(EnderAnchorBlockEntity.DATA_RADIUS),
                data().get(EnderAnchorBlockEntity.DATA_OFFSET_X), data().get(EnderAnchorBlockEntity.DATA_OFFSET_Z));
    }

    public int teamLoad() {
        return data().get(EnderAnchorBlockEntity.DATA_TEAM_LOAD);
    }

    public int teamCap() {
        return data().get(EnderAnchorBlockEntity.DATA_TEAM_CAP);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        AnchorArea.Shift shift = AnchorArea.Shift.byId(id);
        if (shift != null) {
            anchor().shift(shift);
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
