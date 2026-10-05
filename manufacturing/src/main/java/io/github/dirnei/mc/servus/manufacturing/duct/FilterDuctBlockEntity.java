package io.github.dirnei.mc.servus.manufacturing.duct;

import io.github.dirnei.mc.servus.manufacturing.menu.FilterDuctMenu;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Duct with a whitelist of ghost items for deliveries to its neighbours. */
public class FilterDuctBlockEntity extends ItemDuctBlockEntity implements MenuProvider {

    public static final int FILTER_SLOTS = 9;

    private final ItemStackHandler filter = new ItemStackHandler(FILTER_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public FilterDuctBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.FILTER_DUCT.get(), pos, state);
    }

    public ItemStackHandler filter() {
        return filter;
    }

    public boolean isEmptyFilter() {
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (!filter.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Whether the stack may be delivered to this duct's neighbours. */
    public boolean allows(ItemStack stack) {
        boolean any = false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack entry = filter.getStackInSlot(i);
            if (entry.isEmpty()) {
                continue;
            }
            any = true;
            if (ItemStack.isSameItem(entry, stack)) {
                return true;
            }
        }
        return !any;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Filter", filter.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Filter")) {
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("Filter"));
            for (int i = 0; i < FILTER_SLOTS; i++) {
                filter.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FilterDuctMenu(containerId, playerInventory, this);
    }
}
