package io.github.dirnei.mc.servus.delicacies.cooking;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * A recipe a vessel cooks over a hot firebox: counted items, for the Pot maybe a counted fluid, a result and a time.
 * Never in the recipe book; vessels and JEI read its fields.
 */
public interface VesselRecipe extends Recipe<VesselInput> {

    /** The counted items, each served by an input stack of its own. */
    List<SizedIngredient> ingredients();

    /** The fluid one cooking drains, if any. */
    Optional<SizedFluidIngredient> fluid();

    ItemStackTemplate resultTemplate();

    /** Ticks over a hot firebox one cooking takes. */
    int time();

    /** A new stack of what it makes. */
    default ItemStack result() {
        return resultTemplate().create();
    }

    /** Whether some ingredient takes {@code stack}, regardless of count. */
    default boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && ingredients().stream().anyMatch(ingredient -> ingredient.ingredient().test(stack));
    }

    /** Whether this recipe's fluid is {@code fluid}, regardless of amount. */
    default boolean acceptsFluid(FluidStack fluid) {
        return fluid().map(wanted -> wanted.ingredient().test(fluid)).orElse(false);
    }

    /** Which input stack serves each ingredient, or empty when the items do not fit. */
    default Optional<int[]> slotsFor(VesselInput input) {
        List<Predicate<ItemStack>> wants = ingredients().stream().<Predicate<ItemStack>>map(ingredient -> ingredient::test).toList();
        return SlotMatcher.match(wants, input.items(), ItemStack::isEmpty);
    }

    @Override
    default boolean matches(VesselInput input, Level level) {
        return fluid().map(wanted -> wanted.test(input.fluid())).orElse(true) && slotsFor(input).isPresent();
    }

    @Override
    default ItemStack assemble(VesselInput input) {
        return result();
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

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
}
