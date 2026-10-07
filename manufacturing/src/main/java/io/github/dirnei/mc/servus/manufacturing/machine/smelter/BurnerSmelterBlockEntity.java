package io.github.dirnei.mc.servus.manufacturing.machine.smelter;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.BurnerMachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineDefaults;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineStats;
import io.github.dirnei.mc.servus.manufacturing.menu.BurnerSmelterMenu;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;

/** Smelts vanilla smelting recipes at a fixed rate while fueled. */
public class BurnerSmelterBlockEntity extends BurnerMachineBlockEntity {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_INPUT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "burner_smelter");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    public BurnerSmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.BURNER_SMELTER.get(), pos, state, 3);
    }

    @Override
    protected ResourceLocation machineId() {
        return ID;
    }

    @Override
    protected MachineStats defaultStats() {
        return DEFAULTS;
    }

    @Override
    protected int fuelSlot() {
        return SLOT_FUEL;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot == SLOT_INPUT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return recipeFor(stack).isPresent();
    }

    private Optional<RecipeHolder<SmeltingRecipe>> recipeFor(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
    }

    private ItemStack resultFor(ItemStack input) {
        if (level == null) {
            return ItemStack.EMPTY;
        }
        return recipeFor(input)
                .map(holder -> holder.value().assemble(new SingleRecipeInput(input), level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    @Override
    protected boolean canWork() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) {
            return false;
        }
        ItemStack result = resultFor(input);
        return !result.isEmpty() && canInsertOutput(SLOT_OUTPUT, result);
    }

    @Override
    protected void finishWork() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        ItemStack result = resultFor(input);
        if (result.isEmpty()) {
            return;
        }
        inventory.extractItem(SLOT_INPUT, 1, false);
        insertOutput(SLOT_OUTPUT, result);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BurnerSmelterMenu(containerId, playerInventory, this);
    }
}
