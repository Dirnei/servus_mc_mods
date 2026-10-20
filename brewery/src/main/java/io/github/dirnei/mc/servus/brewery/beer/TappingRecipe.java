package io.github.dirnei.mc.servus.brewery.beer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

/**
 * Which empty glass a Cask's tap fills with which beer ({@code servus_brewery:tapping}). Never in the recipe book.
 *
 * @param glass          the empty glass
 * @param beer           the beer it takes
 * @param amount         millibuckets one glass takes from the Cask
 * @param resultTemplate the full glass
 */
public record TappingRecipe(Ingredient glass, FluidIngredient beer, int amount, ItemStackTemplate resultTemplate)
        implements Recipe<TappingInput> {

    public static final MapCodec<TappingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("glass").forGetter(TappingRecipe::glass),
            FluidIngredient.CODEC.fieldOf("beer").forGetter(TappingRecipe::beer),
            Codec.INT.fieldOf("amount").forGetter(TappingRecipe::amount),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(TappingRecipe::resultTemplate)
    ).apply(instance, TappingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TappingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, TappingRecipe::glass,
            FluidIngredient.STREAM_CODEC, TappingRecipe::beer,
            ByteBufCodecs.VAR_INT, TappingRecipe::amount,
            ItemStackTemplate.STREAM_CODEC, TappingRecipe::resultTemplate,
            TappingRecipe::new);

    public static final RecipeSerializer<TappingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    /** Whether this recipe pairs {@code input}'s glass with its beer, regardless of how much beer there is. */
    public boolean pairs(TappingInput input) {
        return glass.test(input.glass()) && !input.beer().isEmpty() && beer.test(input.beer());
    }

    /** A new full glass. */
    public ItemStack result() {
        return resultTemplate.create();
    }

    @Override
    public boolean matches(TappingInput input, Level level) {
        return pairs(input) && input.beer().getAmount() >= amount;
    }

    @Override
    public ItemStack assemble(TappingInput input) {
        return result();
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
    public RecipeSerializer<TappingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<TappingRecipe> getType() {
        return ServusBreweryRecipes.TAPPING.get();
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
