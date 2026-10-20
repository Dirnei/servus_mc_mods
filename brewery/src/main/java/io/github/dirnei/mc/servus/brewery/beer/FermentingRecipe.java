package io.github.dirnei.mc.servus.brewery.beer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

/**
 * What a Fermenting Vat makes of a wort ({@code servus_brewery:fermenting}): with the yeast in, the whole tank turns
 * into the same amount of the result after {@code time} game ticks. The yeast is never used up. Never in the recipe book.
 *
 * @param yeast  the yeast that must sit in the vat
 * @param wort   the fluid that ferments
 * @param result the beer it becomes
 * @param time   game ticks the batch takes
 */
public record FermentingRecipe(Ingredient yeast, FluidIngredient wort, Fluid result, int time) implements Recipe<FermentingInput> {

    public static final MapCodec<FermentingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("yeast").forGetter(FermentingRecipe::yeast),
            FluidIngredient.CODEC.fieldOf("wort").forGetter(FermentingRecipe::wort),
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("result").forGetter(FermentingRecipe::result),
            Codec.INT.fieldOf("time").forGetter(FermentingRecipe::time)
    ).apply(instance, FermentingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FermentingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, FermentingRecipe::yeast,
            FluidIngredient.STREAM_CODEC, FermentingRecipe::wort,
            ByteBufCodecs.registry(Registries.FLUID), FermentingRecipe::result,
            ByteBufCodecs.VAR_INT, FermentingRecipe::time,
            FermentingRecipe::new);

    public static final RecipeSerializer<FermentingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    /** Whether this recipe ferments {@code fluid}, regardless of amount. */
    public boolean fermentsFluid(FluidStack fluid) {
        return !fluid.isEmpty() && wort.test(fluid);
    }

    /** The beer the whole tank of {@code fluid} becomes. */
    public FluidStack beer(FluidStack fluid) {
        return new FluidStack(result, fluid.getAmount());
    }

    @Override
    public boolean matches(FermentingInput input, Level level) {
        return fermentsFluid(input.wort()) && yeast.test(input.yeast());
    }

    @Override
    public ItemStack assemble(FermentingInput input) {
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
    public RecipeSerializer<FermentingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<FermentingRecipe> getType() {
        return ServusBreweryRecipes.FERMENTING.get();
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
