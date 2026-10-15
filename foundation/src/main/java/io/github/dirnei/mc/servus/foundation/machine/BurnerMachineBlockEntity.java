package io.github.dirnei.mc.servus.foundation.machine;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
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

    /** The fuel slot as upkeep, or null when the machine's stats keep none or an eternal fire burns. */
    public UpkeepHolder upkeep() {
        return stats().hasUpkeep() && !eternalFire() ? upkeep : null;
    }

    private ItemStack upkeepStack() {
        String id = stats().upkeepItem();
        if (!id.equals(upkeepId)) {
            upkeepId = id;
            Identifier location = Identifier.tryParse(id);
            upkeepStack = location == null || !BuiltInRegistries.ITEM.containsKey(location)
                    ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(location));
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

    /**
     * Whether a fire that never goes out burns in this machine. While true the machine works without fuel, its burn
     * does not count down and it reports no upkeep. False by default.
     */
    protected boolean eternalFire() {
        return false;
    }

    @Override
    protected boolean tick() {
        boolean hasWork = canWork();
        if (eternalFire()) {
            return workOverEternalFire(hasWork);
        }
        if (burnTime > 0) {
            burnTime--;
        }
        if (!hasWork) {
            resetProgress();
            return false;
        }
        if (burnTime <= 0) {
            consumeFuel();
        }
        if (burnTime <= 0) {
            return false;
        }
        if (stepProgress()) {
            finishWork();
        }
        return true;
    }

    private boolean workOverEternalFire(boolean hasWork) {
        if (!hasWork) {
            resetProgress();
            return false;
        }
        if (stepProgress()) {
            finishWork();
        }
        return true;
    }

    private void consumeFuel() {
        ItemStack fuel = inventory.getStackInSlot(fuelSlot());
        if (fuel.isEmpty() || level == null) {
            return;
        }
        int time = fuel.getBurnTime(RecipeType.SMELTING, level.fuelValues());
        if (time <= 0) {
            return;
        }
        time = Math.max(1, time / stats().fuelDivisor());
        burnTime = time;
        burnDuration = time;
        ItemStackTemplate remainderTemplate = fuel.getCraftingRemainder();
        ItemStack remainder = remainderTemplate == null ? ItemStack.EMPTY : remainderTemplate.create();
        fuel.shrink(1);
        inventory.setStackInSlot(fuelSlot(), fuel.isEmpty() ? remainder : fuel);
    }

    /** An eternal fire shows as a full burn. */
    @Override
    protected int getData(int index) {
        if (eternalFire() && (index == DATA_BURN_TIME || index == DATA_BURN_DURATION)) {
            return 1;
        }
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("BurnTime", burnTime);
        output.putInt("BurnDuration", burnDuration);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnTime = input.getIntOr("BurnTime", 0);
        burnDuration = input.getIntOr("BurnDuration", 0);
    }
}
