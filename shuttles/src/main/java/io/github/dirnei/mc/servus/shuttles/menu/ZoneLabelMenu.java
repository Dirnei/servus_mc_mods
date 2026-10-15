package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneLabelItem;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneNames;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;

/**
 * Edits the Zone Label in one hand: its name, nine ghost filter items and a
 * line of tags. Every change is written straight back onto the stack.
 */
public class ZoneLabelMenu extends AbstractContainerMenu {

    public static final int GHOST_Y = 40;
    public static final int INVENTORY_Y = 84;

    private final InteractionHand hand;
    private final Player player;
    private final ItemSlots filter = new ItemSlots(ZoneFilter.SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            writeBack();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private final int inventoryStart;
    private boolean loading;

    public ZoneLabelMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ServusShuttlesMenus.ZONE_LABEL.get(), containerId);
        this.hand = hand;
        this.player = playerInventory.player;
        loadFilter();
        for (int i = 0; i < ZoneFilter.SLOTS; i++) {
            addSlot(new GhostSlot(() -> filter, i, 8 + i * 18, GHOST_Y));
        }
        inventoryStart = slots.size();
        PlayerInventorySlots.add(this::addSlot, playerInventory, 8, INVENTORY_Y);
    }

    public static ZoneLabelMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new ZoneLabelMenu(containerId, playerInventory, buf.readEnum(InteractionHand.class));
    }

    /** The label being edited. */
    public ItemStack label() {
        return player.getItemInHand(hand);
    }

    /** The zone name on the label, empty when unnamed. */
    public String name() {
        ItemStack label = label();
        return label.has(DataComponents.CUSTOM_NAME) ? label.getHoverName().getString() : "";
    }

    public ZoneFilter zoneFilter() {
        return label().getOrDefault(ServusShuttlesComponents.ZONE_FILTER.get(), ZoneFilter.EMPTY);
    }

    private void loadFilter() {
        loading = true;
        ZoneFilter current = zoneFilter();
        for (int i = 0; i < ZoneFilter.SLOTS; i++) {
            ItemStack stack = i < current.items().size() ? current.items().get(i).copyWithCount(1) : ItemStack.EMPTY;
            filter.setStackInSlot(i, stack);
        }
        loading = false;
    }

    /** Copies the ghost slots and the current tags onto the stack. Server side only. */
    private void writeBack() {
        if (loading || player.level().isClientSide()) {
            return;
        }
        ItemStack label = label();
        if (!(label.getItem() instanceof ZoneLabelItem)) {
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < filter.getSlots(); i++) {
            ItemStack stack = filter.getStackInSlot(i);
            items.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        }
        int last = items.size();
        while (last > 0 && items.get(last - 1).isEmpty()) {
            last--;
        }
        store(label, new ZoneFilter(items.subList(0, last), zoneFilter().tags()));
    }

    private static void store(ItemStack label, ZoneFilter filter) {
        if (filter.isEmpty()) {
            label.remove(ServusShuttlesComponents.ZONE_FILTER.get());
        } else {
            label.set(ServusShuttlesComponents.ZONE_FILTER.get(), filter);
        }
    }

    /** Names the label; a blank name removes it. Server side. */
    public void setName(String name) {
        ItemStack label = label();
        if (!(label.getItem() instanceof ZoneLabelItem)) {
            return;
        }
        String display = ZoneNames.display(name);
        if (display.isEmpty()) {
            label.remove(DataComponents.CUSTOM_NAME);
        } else {
            label.set(DataComponents.CUSTOM_NAME, Component.literal(display));
        }
    }

    /** Sets the label's tags from a typed line. Server side. */
    public void setTags(String line) {
        ItemStack label = label();
        if (label.getItem() instanceof ZoneLabelItem) {
            store(label, new ZoneFilter(zoneFilter().items(), ZoneFilter.parseTags(line)));
        }
    }

    /** Ghost slots take a copy of the cursor and never hand anything out. */
    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 0 && slotId < inventoryStart && slots.get(slotId) instanceof GhostSlot ghost) {
            if (!player.level().isClientSide()) {
                ghost.setGhost(getCarried());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** Shift clicking an inventory item adds it to the first free ghost slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= inventoryStart && index < slots.size() && !player.level().isClientSide()) {
            ItemStack stack = slots.get(index).getItem();
            if (!stack.isEmpty()) {
                for (int i = 0; i < ZoneFilter.SLOTS; i++) {
                    if (filter.getStackInSlot(i).isEmpty()) {
                        ((GhostSlot) slots.get(i)).setGhost(stack);
                        break;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof ZoneLabelItem;
    }
}
