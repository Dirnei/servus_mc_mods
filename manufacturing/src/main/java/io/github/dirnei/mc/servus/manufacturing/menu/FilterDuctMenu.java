package io.github.dirnei.mc.servus.manufacturing.menu;

import io.github.dirnei.mc.servus.manufacturing.duct.FilterDuctBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Nine ghost slots for the filter list plus the player inventory. */
public class FilterDuctMenu extends AbstractContainerMenu {

    private final FilterDuctBlockEntity duct;
    private final ContainerLevelAccess access;

    public FilterDuctMenu(int containerId, Inventory playerInventory, FilterDuctBlockEntity duct) {
        super(ServusManufacturingMenus.FILTER_DUCT.get(), containerId);
        this.duct = duct;
        this.access = ContainerLevelAccess.create(duct.getLevel(), duct.getBlockPos());
        for (int i = 0; i < FilterDuctBlockEntity.FILTER_SLOTS; i++) {
            addSlot(new PatternSlot(duct.filter(), i, 8 + i * 18, 20));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 51 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 109));
        }
    }

    public static FilterDuctMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof FilterDuctBlockEntity duct) {
            return new FilterDuctMenu(containerId, playerInventory, duct);
        }
        throw new IllegalStateException("No filter duct at " + pos);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < FilterDuctBlockEntity.FILTER_SLOTS && slots.get(slotId) instanceof PatternSlot ghost) {
            ghost.setGhost(getCarried());
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** Shift clicking an inventory item adds it to the first free list slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= FilterDuctBlockEntity.FILTER_SLOTS) {
            ItemStack stack = slots.get(index).getItem();
            if (!stack.isEmpty()) {
                for (int i = 0; i < FilterDuctBlockEntity.FILTER_SLOTS; i++) {
                    if (duct.filter().getStackInSlot(i).isEmpty()) {
                        duct.filter().setStackInSlot(i, stack.copyWithCount(1));
                        break;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusManufacturingBlocks.FILTER_DUCT.get());
    }
}
