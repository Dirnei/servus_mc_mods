package io.github.dirnei.mc.servus.brewery.brewhouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import io.github.dirnei.mc.servus.delicacies.cooking.SlotMatcher;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * A Brew Kettle recipe ({@code servus_brewery:brewing}): counted items and a counted fluid brewed into a wort over a hot
 * Brewhouse. Never in the recipe book.
 *
 * @param ingredients the counted items, each served by an input stack of its own
 * @param fluid       what one brew drains from the water tank
 * @param result      the wort it adds to the wort tank
 * @param time        ticks over a hot firebox
 */
public record BrewingRecipe(List<SizedIngredient> ingredients, SizedFluidIngredient fluid, FluidStackTemplate result, int time)
        implements Recipe<BrewingInput> {

    public static final MapCodec<BrewingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.listOf().fieldOf("ingredients").forGetter(BrewingRecipe::ingredients),
            SizedFluidIngredient.CODEC.fieldOf("fluid").forGetter(BrewingRecipe::fluid),
            FluidStackTemplate.CODEC.fieldOf("result").forGetter(BrewingRecipe::result),
            Codec.INT.fieldOf("time").forGetter(BrewingRecipe::time)
    ).apply(instance, BrewingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BrewingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), BrewingRecipe::ingredients,
            SizedFluidIngredient.STREAM_CODEC, BrewingRecipe::fluid,
            FluidStackTemplate.STREAM_CODEC, BrewingRecipe::result,
            ByteBufCodecs.VAR_INT, BrewingRecipe::time,
            BrewingRecipe::new);

    public static final RecipeSerializer<BrewingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    /** Whether some ingredient takes {@code stack}, regardless of count. */
    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && ingredients.stream().anyMatch(ingredient -> ingredient.ingredient().test(stack));
    }

    /** Whether this recipe's fluid is {@code fluid}, regardless of amount. */
    public boolean acceptsFluid(FluidStack fluid) {
        return this.fluid.ingredient().test(fluid);
    }

    /** Which input stack serves each ingredient, or empty when the items do not fit. */
    public Optional<int[]> slotsFor(BrewingInput input) {
        List<Predicate<ItemStack>> wants = ingredients.stream().<Predicate<ItemStack>>map(ingredient -> ingredient::test).toList();
        return SlotMatcher.match(wants, input.items(), ItemStack::isEmpty);
    }

    /** A new stack of the wort it makes. */
    public FluidStack wort() {
        return result.create();
    }

    @Override
    public boolean matches(BrewingInput input, Level level) {
        return fluid.test(input.water()) && slotsFor(input).isPresent();
    }

    @Override
    public ItemStack assemble(BrewingInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<BrewingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<BrewingRecipe> getType() {
        return ServusBreweryRecipes.BREWING.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
