package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.foundation.naming.MachineNameMenu;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLayout;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlockEntity.BoxView;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * The Parcel Locker GUI of one player: a map of the ten boxes, which are
 * free, theirs or someone else's, and the slots of their own boxes to take
 * items from as from a chest. Nothing goes into a box by hand. Which boxes
 * are theirs is fixed when the GUI opens.
 */
public class ParcelLockerMenu extends AbstractContainerMenu implements MachineNameMenu {

    public static final int MAP_Y = 16;
    /** Height of the box map: the locker's 32 pixels, 9 for each S box's 8. */
    public static final int MAP_H = 36;
    public static final int GRID_Y = MAP_Y + MAP_H + 13;
    public static final int GRID_ROWS = 5;
    public static final int INVENTORY_Y = GRID_Y + GRID_ROWS * 18 + 14;
    public static final int WIDTH = 176;
    public static final int HEIGHT = INVENTORY_Y + 82;
    private static final int COLUMNS = 9;

    private final ParcelLockerBlockEntity locker;
    private final List<BoxView> views;
    private final ContainerLevelAccess access;
    private final int boxSlots;

    public ParcelLockerMenu(int containerId, Inventory playerInventory, ParcelLockerBlockEntity locker, List<BoxView> views) {
        super(ServusShuttlesMenus.PARCEL_LOCKER.get(), containerId);
        this.locker = locker;
        this.views = List.copyOf(views);
        this.access = ContainerLevelAccess.create(locker.getLevel(), locker.getBlockPos());
        List<Integer> mine = mySlots();
        for (int i = 0; i < mine.size(); i++) {
            int slot = mine.get(i);
            addSlot(new ResourceHandlerSlot(locker.boxes(), locker.boxes()::set, slot, 8 + (i % COLUMNS) * 18, GRID_Y + (i / COLUMNS) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        boxSlots = slots.size();
        PlayerInventorySlots.add(this::addSlot, playerInventory, 8, INVENTORY_Y);
    }

    public static ParcelLockerMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        List<BoxView> views = new ArrayList<>();
        for (int i = 0; i < LockerLayout.BOXES.size(); i++) {
            views.add(buf.readEnum(BoxView.class));
        }
        if (playerInventory.player.level().getBlockEntity(pos) instanceof ParcelLockerBlockEntity locker) {
            return new ParcelLockerMenu(containerId, playerInventory, locker, views);
        }
        throw new IllegalStateException("No Parcel Locker at " + pos);
    }

    /** The slots of the opening player's boxes, box after box, at most what the grid shows. */
    private List<Integer> mySlots() {
        List<Integer> mine = new ArrayList<>();
        for (int box = 0; box < views.size(); box++) {
            if (views.get(box) == BoxView.MINE) {
                LockerLayout.Range range = locker.range(box);
                for (int slot = range.first(); slot < range.first() + range.count() && mine.size() < GRID_ROWS * COLUMNS; slot++) {
                    mine.add(slot);
                }
            }
        }
        return mine;
    }

    public ParcelLockerBlockEntity locker() {
        return locker;
    }

    /** What the player sees of each box, in {@link LockerLayout#BOXES} order. */
    public List<BoxView> views() {
        return views;
    }

    /** How many of the menu's slots are box slots; the player inventory follows. */
    public int boxSlots() {
        return boxSlots;
    }

    @Override
    public Optional<NamedMachine> namedMachine() {
        return Optional.of(locker);
    }

    /** Shift click takes a box slot into the player inventory; nothing moves the other way. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (index >= boxSlots || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (!moveItemStackTo(stack, boxSlots, slots.size(), true)) {
            return ItemStack.EMPTY;
        }
        QuickMoves.settle(slot, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusShuttlesBlocks.PARCEL_LOCKER.get());
    }
}
