package io.github.dirnei.mc.servus.manufacturing.alchemy;

import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.PlacementInfo;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * A Calciner recipe: one counted input burned into fixed amounts of the three principles.
 *
 * @param input  what is consumed, with count
 * @param output the principles it gives
 * @param time   ticks per cycle
 */
public record CalcinationRecipe(SizedIngredient input, Principles output, int time) implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_TIME = 80;

    public static final MapCodec<CalcinationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.fieldOf("input").forGetter(CalcinationRecipe::input),
            PrincipleCodecs.CODEC.fieldOf("output").forGetter(CalcinationRecipe::output),
            Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(CalcinationRecipe::time)
    ).apply(instance, CalcinationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CalcinationRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, CalcinationRecipe::input,
            PrincipleCodecs.STREAM_CODEC.cast(), CalcinationRecipe::output,
            ByteBufCodecs.VAR_INT, CalcinationRecipe::time,
            CalcinationRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        ItemStack stack = recipeInput.item();
        return accepts(stack) && stack.getCount() >= input.count();
    }

    /** Whether the item matches regardless of count, for slot acceptance. */
    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && input.ingredient().test(stack);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    /** Never placed by the recipe book; the machines and JEI read the recipe's own fields. */
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<CalcinationRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<CalcinationRecipe> getType() {
        return ServusManufacturingRecipes.CALCINATION.get();
    }

    public static final RecipeSerializer<CalcinationRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
