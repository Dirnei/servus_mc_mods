package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A machine that burns furnace fuel. Fuel is consumed only when work is
 * available; once lit it burns down like a furnace.
 */
public abstract class BurnerMachineBlockEntity extends MachineBlockEntity {

    protected int burnTime;
    protected int burnDuration;

    private String upkeepId = "";
    private ItemStack upkeepStack = ItemStack.EMPTY;
    private final UpkeepHolder upkeep = new UpkeepHolder() {
        @Override
        public ItemStack upkeepItem() {
            return upkeepStack();
        }

        @Override
        public int upkeepLevel() {
            return stats().upkeepLevel();
        }

        @Override
        public int upkeepCount() {
            ItemStack item = upkeepStack();
            ItemStack fuel = inventory.getStackInSlot(fuelSlot());
            return !item.isEmpty() && ItemStack.isSameItemSameComponents(item, fuel) ? fuel.getCount() : 0;
        }
    };

    protected BurnerMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    protected abstract int fuelSlot();

    /** Whether the machine has something to do right now (input present, output has room). */
    protected abstract boolean canWork();

    /** Performs one finished work cycle. Only called when {@link #canWork()} was true. */
    protected abstract void finishWork();

    /** The fuel slot as upkeep, or null when the machine's stats keep none. */
    public UpkeepHolder upkeep() {
        return stats().hasUpkeep() ? upkeep : null;
    }

    private ItemStack upkeepStack() {
        String id = stats().upkeepItem();
        if (!id.equals(upkeepId)) {
            upkeepId = id;
            ResourceLocation location = ResourceLocation.tryParse(id);
            upkeepStack = location == null || !BuiltInRegistries.ITEM.containsKey(location)
                    ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(location));
        }
        return upkeepStack;
    }

    @Override
    protected boolean isFuelSlot(int slot) {
        return slot == fuelSlot();
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    @Override
    protected boolean tick() {
        boolean hasWork = canWork();
        if (burnTime > 0) {
            burnTime--;
        }
        if (!hasWork) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return false;
        }
        if (burnTime <= 0) {
            consumeFuel();
        }
        if (burnTime <= 0) {
            return false;
        }
        progress++;
        if (progress >= workTicks()) {
            progress = 0;
            finishWork();
        }
        setChanged();
        return true;
    }

    private void consumeFuel() {
        ItemStack fuel = inventory.getStackInSlot(fuelSlot());
        if (fuel.isEmpty()) {
            return;
        }
        int time = fuel.getBurnTime(RecipeType.SMELTING);
        if (time <= 0) {
            return;
        }
        time = Math.max(1, time / stats().fuelDivisor());
        burnTime = time;
        burnDuration = time;
        ItemStack remainder = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            inventory.setStackInSlot(fuelSlot(), remainder);
        } else {
            inventory.setStackInSlot(fuelSlot(), fuel);
        }
    }

    @Override
    protected int getData(int index) {
        return switch (index) {
            case DATA_BURN_TIME -> burnTime;
            case DATA_BURN_DURATION -> burnDuration;
            default -> super.getData(index);
        };
    }

    @Override
    protected void setData(int index, int value) {
        switch (index) {
            case DATA_BURN_TIME -> burnTime = value;
            case DATA_BURN_DURATION -> burnDuration = value;
            default -> super.setData(index, value);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
    }
}
