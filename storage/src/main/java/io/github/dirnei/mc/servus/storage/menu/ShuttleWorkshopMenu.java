package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.shuttle.module.ModuleSlot;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModuleItem;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModules;
import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Shuttle Workshop: one shuttle slot and its two module slots. The module
 * slots are a view of the shuttle's modules component, so putting a module in
 * fits it and taking it out removes it. The workshop keeps nothing; closing
 * hands the shuttle back.
 */
public class ShuttleWorkshopMenu extends AbstractContainerMenu {

    public static final int SHUTTLE_X = 44;
    public static final int TOOL_X = 98;
    public static final int UTILITY_X = 124;
    public static final int SLOTS_Y = 40;
    public static final int INVENTORY_Y = 96;
    public static final int HOTBAR_Y = 154;
    public static final int SHUTTLE_SLOT = 0;
    public static final int TOOL_SLOT = 1;
    public static final int UTILITY_SLOT = 2;
    public static final int INVENTORY_START = 3;

    private final ContainerLevelAccess access;
    private final SimpleContainer shuttle = new SimpleContainer(1) {
        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };

    public ShuttleWorkshopMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public ShuttleWorkshopMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ServusStorageMenus.SHUTTLE_WORKSHOP.get(), containerId);
        this.access = access;
        shuttle.addListener(container -> slotsChanged(container));
        addSlot(new Slot(shuttle, 0, SHUTTLE_X, SLOTS_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ServusStorageItems.SHUTTLE.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        ModuleContainer modules = new ModuleContainer(shuttle);
        addSlot(new ModuleSlotView(modules, ModuleSlot.TOOL, TOOL_X, SLOTS_Y));
        addSlot(new ModuleSlotView(modules, ModuleSlot.UTILITY, UTILITY_X, SLOTS_Y));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, HOTBAR_Y));
        }
    }

    /** The shuttle in the workshop, or empty. */
    public ItemStack shuttle() {
        return shuttle.getItem(0);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.SHUTTLE_WORKSHOP.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, shuttle));
    }

    /**
     * Shift click: a shuttle goes into the shuttle slot, a module into the
     * free slot of its kind, and anything in the workshop back to the inventory.
     */
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
        ItemStack original = stack.copy();
        int end = slots.size();
        if (index < INVENTORY_START) {
            if (!moveItemStackTo(stack, INVENTORY_START, end, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ServusStorageItems.SHUTTLE.get())) {
            if (!moveItemStackTo(stack, SHUTTLE_SLOT, SHUTTLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ShuttleModuleItem module) {
            int target = module.spec().slot() == ModuleSlot.TOOL ? TOOL_SLOT : UTILITY_SLOT;
            if (!moveItemStackTo(stack, target, target + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    /** The two module places of the shuttle in the workshop, read from and written to its component. */
    static final class ModuleContainer implements Container {

        private final Container shuttle;

        ModuleContainer(Container shuttle) {
            this.shuttle = shuttle;
        }

        private static ModuleSlot slotOf(int index) {
            return index == 0 ? ModuleSlot.TOOL : ModuleSlot.UTILITY;
        }

        @Override
        public int getContainerSize() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return getItem(0).isEmpty() && getItem(1).isEmpty();
        }

        @Override
        public ItemStack getItem(int index) {
            ItemStack held = shuttle.getItem(0);
            if (held.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return ShuttleModules.of(held).get(slotOf(index)).map(ItemStack::new).orElse(ItemStack.EMPTY);
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            ItemStack module = getItem(index);
            if (!module.isEmpty() && count > 0) {
                setItem(index, ItemStack.EMPTY);
            }
            return module;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            return removeItem(index, 1);
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            ItemStack held = shuttle.getItem(0);
            if (held.isEmpty()) {
                return;
            }
            ItemStack edited = held.copy();
            ShuttleModules.set(edited, ShuttleModules.of(held).with(slotOf(index),
                    stack.isEmpty() ? Optional.empty() : Optional.of(stack.getItem())));
            shuttle.setItem(0, edited);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            shuttle.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            // A view: the modules belong to the shuttle.
        }
    }

    /** A module place: takes one module of its kind, and only while a shuttle is in the workshop. */
    static final class ModuleSlotView extends Slot {

        private final ModuleSlot kind;

        ModuleSlotView(ModuleContainer container, ModuleSlot kind, int x, int y) {
            super(container, kind == ModuleSlot.TOOL ? 0 : 1, x, y);
            this.kind = kind;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !((ModuleContainer) container).shuttle.getItem(0).isEmpty()
                    && stack.getItem() instanceof ShuttleModuleItem module && module.spec().slot() == kind;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
