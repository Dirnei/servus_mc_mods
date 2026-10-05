package io.github.dirnei.mc.servus.manufacturing.machine.assembler;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineStats;
import io.github.dirnei.mc.servus.manufacturing.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.menu.AssemblerMenu;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblerRecipeInput;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipe;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipes;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** Crafts the selected assembling recipe from four input slots. */
public class AssemblerBlockEntity extends PoweredMachineBlockEntity {

    public static final int SLOT_INPUT_FIRST = 0;
    public static final int INPUT_SLOTS = 4;
    public static final int SLOT_OUTPUT = 4;
    public static final int DATA_SELECTED_RECIPE = 6;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "assembler");
    private static final MachineStats DEFAULTS = MachineStats.powered(1, 20, 8000);

    private ResourceLocation selectedRecipeId;
    private RecipeHolder<AssemblingRecipe> cachedRecipe;
    private boolean cacheValid;

    public AssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.ASSEMBLER.get(), pos, state, INPUT_SLOTS + 1);
    }

    // Selection

    public Optional<ResourceLocation> selectedRecipeId() {
        return Optional.ofNullable(selectedRecipeId);
    }

    public void selectRecipe(ResourceLocation id) {
        if (id == null ? selectedRecipeId == null : id.equals(selectedRecipeId)) {
            return;
        }
        selectedRecipeId = id;
        cacheValid = false;
        progress = 0;
        setChanged();
    }

    public Optional<RecipeHolder<AssemblingRecipe>> selectedRecipe() {
        if (!cacheValid) {
            cachedRecipe = selectedRecipeId == null || level == null
                    ? null
                    : AssemblingRecipes.byId(level, selectedRecipeId).orElse(null);
            cacheValid = level != null;
        }
        return Optional.ofNullable(cachedRecipe);
    }

    private AssemblerRecipeInput input() {
        List<ItemStack> stacks = new ArrayList<>(INPUT_SLOTS);
        for (int slot = SLOT_INPUT_FIRST; slot < SLOT_INPUT_FIRST + INPUT_SLOTS; slot++) {
            stacks.add(inventory.getStackInSlot(slot));
        }
        return new AssemblerRecipeInput(stacks);
    }

    // Machine contract

    @Override
    protected ResourceLocation machineId() {
        return ID;
    }

    @Override
    protected MachineStats defaultStats() {
        return DEFAULTS;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot >= SLOT_INPUT_FIRST && slot < SLOT_INPUT_FIRST + INPUT_SLOTS;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        if (level == null) {
            return false;
        }
        return selectedRecipe()
                .map(holder -> AssemblingRecipes.isIngredientOf(holder.value(), stack))
                .orElseGet(() -> AssemblingRecipes.isIngredientOfAny(level, stack));
    }

    @Override
    protected boolean canWork() {
        if (level == null) {
            return false;
        }
        return selectedRecipe().map(holder -> {
            AssemblingRecipe recipe = holder.value();
            AssemblerRecipeInput input = input();
            return recipe.matches(input, level) && canInsertOutput(SLOT_OUTPUT, recipe.assemble(input, level.registryAccess()));
        }).orElse(false);
    }

    @Override
    protected void finishWork() {
        selectedRecipe().ifPresent(holder -> {
            AssemblingRecipe recipe = holder.value();
            ItemStack result = recipe.assemble(input(), level.registryAccess());
            for (SizedIngredient ingredient : recipe.ingredients()) {
                int remaining = ingredient.count();
                for (int slot = SLOT_INPUT_FIRST; slot < SLOT_INPUT_FIRST + INPUT_SLOTS && remaining > 0; slot++) {
                    ItemStack stack = inventory.getStackInSlot(slot);
                    if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                        int take = Math.min(remaining, stack.getCount());
                        inventory.extractItem(slot, take, false);
                        remaining -= take;
                    }
                }
            }
            insertOutput(SLOT_OUTPUT, result);
        });
    }

    @Override
    protected int getData(int index) {
        return switch (index) {
            case DATA_WORK_TICKS -> selectedRecipe().map(holder -> holder.value().time()).orElse(0);
            case DATA_SELECTED_RECIPE -> selectedIndex();
            default -> super.getData(index);
        };
    }

    private int selectedIndex() {
        if (selectedRecipeId == null || level == null) {
            return -1;
        }
        List<RecipeHolder<AssemblingRecipe>> sorted = AssemblingRecipes.sorted(level);
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).id().equals(selectedRecipeId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (selectedRecipeId != null) {
            tag.putString("SelectedRecipe", selectedRecipeId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        selectedRecipeId = tag.contains("SelectedRecipe") ? ResourceLocation.tryParse(tag.getString("SelectedRecipe")) : null;
        cacheValid = false;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AssemblerMenu(containerId, playerInventory, this);
    }
}
