package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.upgrade.AnchorUpgradeSlots;
import io.github.dirnei.mc.servus.storage.controller.SiteStatusPayload;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.controller.WirelessUpgradeSlots;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.site.SiteRecord;
import io.github.dirnei.mc.servus.storage.site.SiteRegistry;
import io.github.dirnei.mc.servus.storage.site.WirelessReach;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.terminal.RemoteTransfers;
import io.github.dirnei.mc.servus.storage.terminal.SiteRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * The controller GUI: the site's name, status, FE and chunk map come as a
 * payload; the Anchor Upgrade row, the Wireless Upgrade slot and the
 * player inventory are slots.
 */
public class WarehouseControllerMenu extends AbstractContainerMenu {

    public static final int WIDTH = 176;
    public static final int MAP_X = 8;
    public static final int MAP_Y = 36;
    public static final int MAP_SIZE = 60;
    public static final int STATUS_X = 74;
    public static final int UPGRADES_Y = 112;
    /** The Wireless Upgrade slot, right of the Anchor Upgrade row, in line with the last inventory column. */
    public static final int WIRELESS_X = 152;
    public static final int INVENTORY_Y = 144;
    public static final int IMAGE_HEIGHT = INVENTORY_Y + 3 * 18 + 4 + 18 + 6;

    private final WarehouseControllerBlockEntity controller;
    private final ContainerLevelAccess access;
    private final Player player;
    private final BlockPos pos;
    private final int upgradeSlots;
    private final PayloadSync<SiteStatusPayload> sync = new PayloadSync<>(10, true, this::status);
    private String name;

    /** Server side: the controller's own slots. */
    public WarehouseControllerMenu(int containerId, Inventory playerInventory, WarehouseControllerBlockEntity controller) {
        this(containerId, playerInventory, controller, controller.getBlockPos(), controller.upgrades(), controller.wirelessUpgrade(),
                controller.siteName());
    }

    private WarehouseControllerMenu(int containerId, Inventory playerInventory, WarehouseControllerBlockEntity controller,
                                    BlockPos pos, ItemSlots upgrades, ItemSlots wireless, String name) {
        super(ServusStorageMenus.WAREHOUSE_CONTROLLER.get(), containerId);
        this.controller = controller;
        this.player = playerInventory.player;
        this.pos = pos;
        this.name = name;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.upgradeSlots = upgrades.getSlots();
        for (int slot = 0; slot < upgradeSlots; slot++) {
            addSlot(new ResourceHandlerSlot(upgrades, upgrades::set, slot, upgradeX(slot), UPGRADES_Y));
        }
        addSlot(new ResourceHandlerSlot(wireless, wireless::set, 0, WIRELESS_X, UPGRADES_Y));
        PlayerInventorySlots.add(this::addSlot, playerInventory, 8, INVENTORY_Y);
    }

    /** Client side: the server writes the position, the slot count and the name. */
    public static WarehouseControllerMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int slots = buf.readVarInt();
        String name = buf.readUtf(64);
        return new WarehouseControllerMenu(containerId, playerInventory, null, pos, new AnchorUpgradeSlots(slots, null),
                new WirelessUpgradeSlots(null), name);
    }

    /** What the server writes for {@link #fromNetwork}. */
    public static void writeOpening(RegistryFriendlyByteBuf buf, WarehouseControllerBlockEntity controller) {
        buf.writeBlockPos(controller.getBlockPos());
        buf.writeVarInt(controller.upgrades().getSlots());
        buf.writeUtf(controller.siteName(), 64);
    }

    public BlockPos pos() {
        return pos;
    }

    /** The name as opened; the client screen fills its field with it. */
    public String name() {
        return name;
    }

    public int upgradeSlots() {
        return upgradeSlots;
    }

    /** The upgrade row starts at the left edge, in line with the inventory. */
    public int upgradeX(int slot) {
        return 8 + slot * 18;
    }

    public int hotbarY() {
        return INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET;
    }

    /** Server side: the name typed in the GUI, cleaned by the controller. */
    public void rename(String typed) {
        if (controller != null && stillValid(player)) {
            name = controller.setSiteName(typed);
            sync.soon();
        }
    }

    /** Shift click moves Anchor and Wireless Upgrades between their slots and the player inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int own = upgradeSlots + 1;
        if (index < own) {
            if (!moveItemStackTo(stack, own, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(MachineContent.ANCHOR_UPGRADE.get())) {
            if (!moveItemStackTo(stack, 0, upgradeSlots, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!stack.is(ServusStorageItems.WIRELESS_UPGRADE.get()) || !moveItemStackTo(stack, upgradeSlots, own, false)) {
            return ItemStack.EMPTY;
        }
        QuickMoves.settle(slot, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get());
    }

    /** What the GUI shows about the controller's site right now. */
    public static SiteStatusPayload status(WarehouseControllerBlockEntity controller) {
        Optional<WarehouseNetwork> network = controller.network();
        int online = network.map(warehouse -> switch (warehouse.status()) {
            case ONLINE -> 0;
            case NO_CONTROLLER -> 1;
            case MULTIPLE_CONTROLLERS -> 2;
        }).orElse(1);
        Component summary = network.map(WarehouseNetwork::summary).orElse(Component.empty());
        boolean capable = controller.capable();
        int hint = capable ? SiteStatusPayload.HINT_NONE
                : controller.hasWirelessUpgrade() ? SiteStatusPayload.HINT_NO_POWER : SiteStatusPayload.HINT_NO_UPGRADE;
        long needs = WirelessReach.needs(StorageStatsManager.INSTANCE.wirelessFePerItem());
        return new SiteStatusPayload(controller.siteName(), controller.displayName(), online, summary,
                new ArrayList<>(controller.span()), controller.homeChunk(), controller.upgrades().count(), controller.capacity(),
                controller.siteState().ordinal(), controller.wakeLine(), controller.wakeAlert(), controller.energyStored(),
                controller.energy().capacity(), capable, hint, needs, controller.usedLastSecond(),
                online == 0 && controller.noPower(), capable ? sites(controller) : List.of());
    }

    /** The other sites of the controller's owner in its dimension, by name. */
    private static List<SiteRow> sites(WarehouseControllerBlockEntity controller) {
        if (!(controller.getLevel() instanceof ServerLevel level) || controller.team().isEmpty()) {
            return List.of();
        }
        UUID team = controller.team().get();
        String self = SiteRecord.key(level.dimension(), controller.getBlockPos());
        List<SiteRow> rows = new ArrayList<>();
        for (var site : SiteRegistry.of(level).ofTeam(team, level.dimension())) {
            if (!site.key().equals(self)) {
                rows.add(new SiteRow(site.key(), site.name(), RemoteTransfers.state(level, site).ordinal(),
                        RemoteTransfers.canTrade(level, site)));
            }
        }
        return rows;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (controller != null) {
            sync.tick(player);
        }
    }

    private SiteStatusPayload status() {
        return status(controller);
    }
}
