package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.field.FieldWork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Field Head GUI: one ghost slot for the crop, a harvest only toggle, the
 * field's numbers as container data, the player inventory.
 */
public class FieldHeadMenu extends AbstractContainerMenu {

    public static final int CROP_X = 8;
    public static final int CROP_Y = 20;
    public static final int INVENTORY_Y = 96;
    public static final int HOTBAR_Y = 154;
    public static final int INVENTORY_START = 1;
    public static final int BUTTON_HARVEST_ONLY = 0;
    private static final int COLUMNS = 9;
    private static final int FLAG_HYDRATED = 1;
    private static final int FLAG_HARVEST_ONLY = 2;
    private static final int FLAG_DUE = 4;

    private final FieldHeadBlockEntity head;
    private final ContainerLevelAccess access;
    private final Player player;
    private final DataSlot waterLow = DataSlot.standalone();
    private final DataSlot waterHigh = DataSlot.standalone();
    private final DataSlot capacityLow = DataSlot.standalone();
    private final DataSlot capacityHigh = DataSlot.standalone();
    private final DataSlot rails = DataSlot.standalone();
    private final DataSlot plots = DataSlot.standalone();
    private final DataSlot planted = DataSlot.standalone();
    private final DataSlot ripe = DataSlot.standalone();
    private final DataSlot status = DataSlot.standalone();
    private final DataSlot flags = DataSlot.standalone();
    /** The head's work reason plus one; zero while it serves no field. */
    private final DataSlot work = DataSlot.standalone();

    public FieldHeadMenu(int containerId, Inventory playerInventory, FieldHeadBlockEntity head) {
        super(ServusStorageMenus.FIELD_HEAD.get(), containerId);
        this.head = head;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(head.getLevel(), head.getBlockPos());
        addSlot(new GhostSlot(head::cropHandler, 0, CROP_X, CROP_Y));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, HOTBAR_Y));
        }
        for (DataSlot slot : new DataSlot[] {waterLow, waterHigh, capacityLow, capacityHigh, rails, plots, planted, ripe, status, flags, work}) {
            addDataSlot(slot);
        }
        update();
    }

    public static FieldHeadMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof FieldHeadBlockEntity head) {
            return new FieldHeadMenu(containerId, playerInventory, head);
        }
        throw new IllegalStateException("No field head at " + pos);
    }

    /** Copies the head's numbers into the data slots. Server side. */
    private void update() {
        if (player.level().isClientSide()) {
            return;
        }
        waterLow.set(head.water() & 0xFFFF);
        waterHigh.set(head.water() >>> 16);
        capacityLow.set(head.capacity() & 0xFFFF);
        capacityHigh.set(head.capacity() >>> 16);
        rails.set(clamp(head.rails().size()));
        plots.set(clamp(head.plots().size()));
        planted.set(clamp(head.plantedCount()));
        ripe.set(clamp(head.ripeCount()));
        status.set(head.status().ordinal());
        FieldWork reason = head.work();
        work.set(reason == null ? 0 : reason.ordinal() + 1);
        flags.set((head.hydrated() ? FLAG_HYDRATED : 0) | (head.harvestOnly() ? FLAG_HARVEST_ONLY : 0) | (head.harvestDue() ? FLAG_DUE : 0));
    }

    /** Container data travels as a short. */
    private static int clamp(int value) {
        return Math.min(Short.MAX_VALUE, value);
    }

    private static int join(DataSlot low, DataSlot high) {
        return (low.get() & 0xFFFF) | ((high.get() & 0xFFFF) << 16);
    }

    @Override
    public void broadcastChanges() {
        update();
        super.broadcastChanges();
    }

    public FieldHeadBlockEntity head() {
        return head;
    }

    public int water() {
        return join(waterLow, waterHigh);
    }

    public int capacity() {
        return join(capacityLow, capacityHigh);
    }

    public int rails() {
        return rails.get();
    }

    public int plots() {
        return plots.get();
    }

    public int planted() {
        return planted.get();
    }

    public int ripe() {
        return ripe.get();
    }

    public FieldStatus status() {
        return FieldStatus.byId(status.get());
    }

    /** What the field is doing or waits for; null while the head serves no field. */
    public FieldWork work() {
        return work.get() == 0 ? null : FieldWork.byId(work.get() - 1);
    }

    public boolean hydrated() {
        return (flags.get() & FLAG_HYDRATED) != 0;
    }

    public boolean harvestOnly() {
        return (flags.get() & FLAG_HARVEST_ONLY) != 0;
    }

    public boolean harvestDue() {
        return (flags.get() & FLAG_DUE) != 0;
    }

    /** The crop as the ghost slot shows it. */
    public ItemStack crop() {
        return slots.get(0).getItem();
    }

    /** The toggle turns harvest only on or off. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_HARVEST_ONLY) {
            return false;
        }
        if (!player.level().isClientSide()) {
            head.setHarvestOnly(!head.harvestOnly());
            update();
        }
        return true;
    }

    /** The crop slot takes a copy of a seed on the cursor, an empty cursor clears it, other items are refused. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == 0) {
            if (!player.level().isClientSide()) {
                setCrop(getCarried());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void setCrop(ItemStack stack) {
        if (stack.isEmpty() || FieldHeadBlockEntity.isCropItem(stack)) {
            head.setCrop(stack);
            update();
        }
    }

    /** Shift clicking a seed in the inventory makes it the crop. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= INVENTORY_START && index < slots.size() && !player.level().isClientSide()) {
            ItemStack stack = slots.get(index).getItem();
            if (FieldHeadBlockEntity.isCropItem(stack)) {
                setCrop(stack);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.FIELD_HEAD.get());
    }
}
