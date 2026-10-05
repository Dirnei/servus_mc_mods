package io.github.dirnei.mc.servus.manufacturing.recipe;

import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * A crusher recipe: one counted input, a result and a time in ticks.
 *
 * @param input  what is consumed, with count
 * @param result what is produced
 * @param time   ticks per cycle
 */
public record CrushingRecipe(SizedIngredient input, ItemStack result, int time) implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        ItemStack stack = recipeInput.item();
        return !stack.isEmpty() && input.ingredient().test(stack) && stack.getCount() >= input.count();
    }

    /** Whether the item matches regardless of count, for slot acceptance. */
    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && input.ingredient().test(stack);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(input.ingredient());
        return list;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ServusManufacturingRecipes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ServusManufacturingRecipes.CRUSHING.get();
    }
}
