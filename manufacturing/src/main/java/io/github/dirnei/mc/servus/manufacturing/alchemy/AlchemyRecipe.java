package io.github.dirnei.mc.servus.manufacturing.alchemy;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** A recipe only the alchemy machines run: never in the recipe book; machines and JEI read its own fields. */
public interface AlchemyRecipe extends Recipe<SingleRecipeInput> {

    /** Ticks one cycle of the recipe takes. */
    int time();

    @Override
    default boolean showNotification() {
        return false;
    }

    @Override
    default String group() {
        return "";
    }

    @Override
    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    /** An alchemy recipe that takes one counted item. */
    interface CountedInput extends AlchemyRecipe {

        /** What is consumed, with count. */
        SizedIngredient input();

        /** Whether the item matches regardless of count, for slot acceptance. */
        default boolean accepts(ItemStack stack) {
            return !stack.isEmpty() && input().ingredient().test(stack);
        }

        @Override
        default boolean matches(SingleRecipeInput recipeInput, Level level) {
            ItemStack stack = recipeInput.item();
            return accepts(stack) && stack.getCount() >= input().count();
        }
    }
}
