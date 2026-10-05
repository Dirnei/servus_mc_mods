package io.github.dirnei.mc.servus.manufacturing.recipe;

import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * An assembler recipe: counted ingredients, a result and a time in ticks.
 * Ingredients of one recipe must not overlap (no two ingredients may match
 * the same item), because matching sums stacks per ingredient.
 *
 * @param ingredients what is consumed, with counts
 * @param result      what is produced
 * @param time        ticks per craft
 * @param tech        research that unlocks the recipe, if any (not enforced yet)
 */
public record AssemblingRecipe(List<SizedIngredient> ingredients, ItemStack result, int time,
                               Optional<ResourceLocation> tech) implements Recipe<AssemblerRecipeInput> {

    @Override
    public boolean matches(AssemblerRecipeInput input, Level level) {
        for (SizedIngredient ingredient : ingredients) {
            int available = 0;
            for (int i = 0; i < input.size(); i++) {
                ItemStack stack = input.getItem(i);
                if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                    available += stack.getCount();
                }
            }
            if (available < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(AssemblerRecipeInput input, HolderLookup.Provider registries) {
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
        ingredients.forEach(sized -> list.add(sized.ingredient()));
        return list;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ServusManufacturingRecipes.ASSEMBLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ServusManufacturingRecipes.ASSEMBLING.get();
    }
}
